/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.ftccommon.configuration;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.ftccommon.CommandList;
import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.exception.DuplicateNameException;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.ControlSystem;
import com.qualcomm.robotcore.hardware.DeviceManager;
import com.qualcomm.robotcore.hardware.ScannedDevices;
import com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;
import com.qualcomm.robotcore.hardware.configuration.DeviceInterfaceModuleConfiguration;
import com.qualcomm.robotcore.hardware.configuration.LegacyModuleControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.LynxModuleConfiguration;
import com.qualcomm.robotcore.hardware.configuration.LynxUsbDeviceConfiguration;
import com.qualcomm.robotcore.hardware.configuration.ModernRoboticsConstants;
import com.qualcomm.robotcore.hardware.configuration.MotorControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.ReadXMLFileHandler;
import com.qualcomm.robotcore.hardware.configuration.ServoControllerConfiguration;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.RobocolDatagram;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.ThreadPool;
import com.qualcomm.robotcore.wifi.NetworkConnection;

import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RecvLoopRunnable;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;
import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@SuppressWarnings("WeakerAccess")
public class FtcConfigurationActivity extends EditActivity implements RecvLoopRunnable.RecvLoopCallback, NetworkConnection.NetworkConnectionCallback {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  protected static final boolean DEBUG = false;
  public static final String TAG = "FtcConfigTag";
  @Override public String getTag() { return TAG; }
  public static final RequestCode requestCode = RequestCode.EDIT_FILE;

  protected USBScanManager usbScanManager = null;
  protected ThreadPool.Singleton scanButtonSingleton = new ThreadPool.Singleton();
  protected final Object robotConfigMapLock = new Object();
  protected int idFeedbackAnchor = R.id.feedbackAnchor;
  protected Semaphore feedbackPosted = new Semaphore(0);
  protected long msSaveSplashDelay = 1000;
  protected NetworkConnectionHandler networkConnectionHandler = NetworkConnectionHandler.getInstance();

  //------------------------------------------------------------------------------------------------
  // Life cycle
  //------------------------------------------------------------------------------------------------

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    RobotLog.vv(TAG, "onCreate()");
    setContentView(R.layout.activity_ftc_configuration);

    try {
      EditParameters parameters = EditParameters.fromIntent(this, getIntent());
      deserialize(parameters);

      Button scanButton = (Button) findViewById(R.id.scanButton);
      scanButton.setVisibility(View.VISIBLE);

      Button doneButton = (Button) findViewById(R.id.doneButton);
      doneButton.setText(R.string.buttonNameSave);

      startExecutorService();

    } catch (RobotCoreException e) {
      RobotLog.ee(TAG, "exception thrown during FtcConfigurationActivity.onCreate()");
      finishCancel();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();

    if (remoteConfigure) {
      networkConnectionHandler.pushNetworkConnectionCallback(this);
      networkConnectionHandler.pushReceiveLoopCallback(this);
    }

    if (!remoteConfigure) {
      robotConfigFileManager.createConfigFolder();
    }

    // If we're dirty, then don't re-read as that will overwrite all the changes. If we
    // *aren't* dirty, then this might be our first time in. If it is, then be sure to
    // get the file.
    if (!currentCfgFile.isDirty()) {
      ensureConfigFileIsFresh();
    }
  }

  protected void ensureConfigFileIsFresh() {
    if (haveRobotConfigMapParameter) {
      // Caller gave us the configuration explicitly. Just use it.
      populateListAndWarnDevices();
    } else if (remoteConfigure) {
      // Ask for the data of this configuration. We'll populate when we receive same.
      networkConnectionHandler.sendCommand(new Command(CommandList.CMD_REQUEST_PARTICULAR_CONFIGURATION, currentCfgFile.toString()));
    } else {
      // Read the config and populate right now
      readFile();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (remoteConfigure) {
      networkConnectionHandler.removeNetworkConnectionCallback(this);
      networkConnectionHandler.removeReceiveLoopCallback(this);
    }
  }

  @Override
  protected void onDestroy() {
    RobotLog.vv(TAG, "FtcConfigurationActivity.onDestroy()");
    super.onDestroy();
    stopExecutorService();
  }

  //------------------------------------------------------------------------------------------------
  // Life cycle support
  //------------------------------------------------------------------------------------------------

  DialogInterface.OnClickListener doNothingAndCloseListener = new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int button){
      // Do nothing. Dialog will dismiss itself upon return.
    }
  };
  public void onDevicesInfoButtonPressed(View v) {
    RobotLog.vv(TAG, "onDevicesInfoButtonPressed()");
    AlertDialog.Builder builder = utility.buildBuilder(getString(R.string.titleDevices), getString(R.string.msgInfoHowToUse));
    builder.setPositiveButton(getString(R.string.buttonNameOK), doNothingAndCloseListener);
    AlertDialog alert = builder.create();
    alert.show();
    TextView textView = (TextView) alert.findViewById(android.R.id.message);  // text view doesn't exist until after .show() is called
    textView.setTextSize(14);
  }

  public void onDoneInfoButtonPressed(View v) {
    RobotLog.vv(TAG, "onDoneInfoButtonPressed()");
    AlertDialog.Builder builder = utility.buildBuilder(getString(R.string.titleSaveConfiguration), getString(R.string.msgInfoSave));
    builder.setPositiveButton(getString(R.string.buttonNameOK), doNothingAndCloseListener);
    AlertDialog alert = builder.create();
    alert.show();
    TextView textView = (TextView) alert.findViewById(android.R.id.message);  // text view doesn't exist until after .show() is called
    textView.setTextSize(14);
  }

  public void onScanButtonPressed(View v) {
    dirtyCheckThenSingletonUSBScanAndUpdateUI(true);
  }

  void dirtyCheckThenSingletonUSBScanAndUpdateUI(final boolean showFeedback) {
    final Runnable runnable = new Runnable() {
      @Override public void run() {
        ThreadPool.logThreadLifeCycle("USB bus scan handler", new Runnable() {
          @Override public void run() {
            if (showFeedback) {
              synchronouslySetFeedbackWhile(getString(R.string.ftcConfigScanning), "", new Runnable() {
                @Override public void run() {
                  doUSBScanAndUpdateUI();
                }
              });
            } else {
              doUSBScanAndUpdateUI();
            }
          }
        });
      }
    };
    if (currentCfgFile.isDirty()) {
      AlertDialog.Builder builder = utility.buildBuilder(getString(R.string.titleUnsavedChanges), getString(R.string.msgAlertBeforeScan));
      DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface dialog, int button){
          scanButtonSingleton.submit(ThreadPool.Singleton.INFINITE_TIMEOUT, runnable);
        }
      };
      builder.setPositiveButton(R.string.buttonNameOK, okListener);
      builder.setNegativeButton(R.string.buttonNameCancel, doNothingAndCloseListener);
      builder.show();
    } else {
      scanButtonSingleton.submit(ThreadPool.Singleton.INFINITE_TIMEOUT, runnable);
    }
  }

  /**
   * @see com.qualcomm.ftccommon.FtcEventLoop#handleCommandScan(String)
   */
  protected void doUSBScanAndUpdateUI() {
  // Executed on a worker thread. We are guaranteed that only one instance
  // of this call is executed at any one time.
    RobotLog.vv(TAG, "doUSBScanAndUpdateUI()...");
    try {
      // Scan afresh, then wait, or wait for an extant scan to complete. Note that the scan will
      // execute either locally or remotely, depending on whether we're running on the RC or the DS.
      ThreadPool.SingletonResult<ScannedDevices> future = usbScanManager.startDeviceScanIfNecessary();
      ScannedDevices devices = future.await();
      //
      if (devices != null) {
        RobotLog.dd(TAG, "scan for devices on USB bus found %d devices", devices.size());

        // Use the results of the scan to figure out what we've got here
        buildRobotConfigMapFromScanned(devices);  // may take awhile, maybe a second or two

        // Back on the UI thread, update things accordingly
        appUtil.synchronousRunOnUiThread(new Runnable() {
          @Override public void run() {
            clearDuplicateWarning();
            currentCfgFile.markDirty();
            robotConfigFileManager.updateActiveConfigHeader(currentCfgFile);
            populateListAndWarnDevices();
            }
          });
        }
      else {
        RobotLog.ee(TAG, "scan for devices on USB bus failed");
        appUtil.showToast(UILocation.ONLY_LOCAL, getString(R.string.ftcConfigScanningFailed));
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      RobotLog.vv(TAG, "...doUSBScanAndUpdateUI()");
    }
  }

  private void startExecutorService() throws RobotCoreException {
    this.usbScanManager = new USBScanManager(this, remoteConfigure);
    this.usbScanManager.startExecutorService();

    // We reuse the executor service in the scan manager to limit the user to single
    // clicks of the scan button
    this.scanButtonSingleton.reset();
    this.scanButtonSingleton.setService(this.usbScanManager.getExecutorService());

    // Kick off a scan every time we edit a new configuration file, as we want to know,
    // definitively, which of those devices are currently attached and which might be missing.
    this.usbScanManager.startDeviceScanIfNecessary();
  }

  private void stopExecutorService() {
    this.usbScanManager.stopExecutorService();
    this.usbScanManager = null;
  }

  private boolean carryOver(SerialNumber serialNumber, RobotConfigMap existingControllers) {
    if (existingControllers == null)
      return false;
    if (!existingControllers.contains(serialNumber))
      return false;
    if (existingControllers.get(serialNumber).isSystemSynthetic()) {
      RobotLog.vv(TAG, "not carrying over synthetic controller: serial=%s", serialNumber);
      return false;
      }
    return true;
  }

  private RobotConfigMap buildRobotConfigMapFromScanned(RobotConfigMap existingControllers, ScannedDevices scannedDevices) {
  // Initialize deviceControllers using the set of serial numbers in the ScannedDevices. If the serial
  // number appears in our existing map, then just carry that configuration over; otherwise, make us
  // a new configuration appropriate for the type of the controller.

    RobotConfigMap newRobotConfigMap = new RobotConfigMap();

    configurationUtility.resetNameUniquifiers();
    for(Map.Entry<SerialNumber, DeviceManager.UsbDeviceType> entry : scannedDevices.entrySet()) {
      final SerialNumber serialNumber = entry.getKey();
      ControllerConfiguration controllerConfiguration = null;
      if (carryOver(serialNumber, existingControllers)) {
        RobotLog.vv(TAG, "carrying over %s", serialNumber);
        controllerConfiguration = existingControllers.get(serialNumber);
      } else {
        controllerConfiguration = configurationUtility.buildNewControllerConfiguration(serialNumber, entry.getValue(), usbScanManager.getLynxModuleMetaListSupplier(serialNumber));
      }
      if (controllerConfiguration != null) {
        controllerConfiguration.setKnownToBeAttached(true);
        newRobotConfigMap.put(serialNumber, controllerConfiguration);
      }
    }
    return newRobotConfigMap;
  }

  /**
   * This method parses the XML of the active configuration file, and calls methods to populate
   * the appropriate data structures to the configuration information can be displayed to the
   * user.
   */
  private void readFile() {

    // Note: we read the 'file', even if there's no actual configuration, in order to give
    // the system a chance to dynamically augment the configuration as it might find necessary.
    // In this way, for example, ReadXMLFileHandler can *guarantee* that the embedded lynx
    // module will *always* be present even if the saved config hasn't specifically mentioned
    // same, such as (most commonly) is the case in the <No Config Set> case.

    try {
      XmlPullParser xmlPullParser = currentCfgFile.getXml();
      if (xmlPullParser==null) throw new RobotCoreException("can't access configuration");
      //
      ReadXMLFileHandler parser = new ReadXMLFileHandler();
      List<ControllerConfiguration> controllerList = parser.parse(xmlPullParser);
      buildControllersFromXMLResults(controllerList);
      populateListAndWarnDevices();
      //
    } catch (Exception e) {
      String message = String.format(getString(R.string.errorParsingConfiguration), currentCfgFile.getName());
      RobotLog.ee(TAG, e, message);
      appUtil.showToast(UILocation.ONLY_LOCAL, message);
    }
  }

  private void populateListAndWarnDevices() {
    // Note: we may or may not be running on the network receive thread now. If we were to block while
    // the UI work happened, and that took a while, our peer might disconnect on us, or worse.
    appUtil.runOnUiThread(new Runnable() {
      @Override public void run() {
        // Running on the UI thread is *necessary* in order to manipulate the views.
        populateList();
        warnIncompleteDevices();
        }
      }
    );
  }

  private void warnIncompleteDevices() {
    String title = null;
    String message = null;

    if (scannedDevices.getErrorMessage() != null) {
      title = getString(R.string.errorScanningDevicesTitle);
      message = scannedDevices.getErrorMessage();
    } else if (!getRobotConfigMap().allControllersAreBound()) {
      title = getString(R.string.notAllDevicesFoundTitle);
      message = Misc.formatForUser(R.string.notAllDevicesFoundMessage, getString(R.string.noSerialNumber));
    } else if (getRobotConfigMap().size() == 0){
      title   = getString(R.string.noDevicesFoundTitle);
      message = getString(R.string.noDevicesFoundMessage);
      clearDuplicateWarning();
    }

    if (title != null || message != null) {
      if (title==null) title = "";
      if (message==null) message = "";
      utility.setFeedbackText(title, message, idFeedbackAnchor, R.layout.feedback, R.id.feedbackText0, R.id.feedbackText1, R.id.feedbackOKButton);
    } else {
      utility.hideFeedbackText(idFeedbackAnchor);
    }
  }

  // Synchronously execute a runnable, showing some feedback text while doing so
  private void synchronouslySetFeedbackWhile(final String title, final String message, Runnable runnable) {
    final CharSequence[] prev = utility.getFeedbackText(idFeedbackAnchor, R.layout.feedback, R.id.feedbackText0, R.id.feedbackText1);
    try {
      // This needs to be synchronous so that the teardown happens *after* runnable runs
      appUtil.synchronousRunOnUiThread(new Runnable() { @Override public void run() {
        utility.setFeedbackText(title, message, idFeedbackAnchor, R.layout.feedback, R.id.feedbackText0, R.id.feedbackText1);
        feedbackPosted.release();
      }});
      try { feedbackPosted.acquire(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      runnable.run();
      }
    finally {
      appUtil.runOnUiThread(new Runnable() { @Override public void run() {
        if (prev != null) {
          utility.setFeedbackText(prev, idFeedbackAnchor, R.layout.feedback, R.id.feedbackText0, R.id.feedbackText1);
        } else {
          utility.hideFeedbackText(idFeedbackAnchor);
        }
      }});
    }
  }

  private void warnDuplicateNames(String dupeMsg) {
    String msg0 ="Found " + dupeMsg;
    String msg1 = "Please fix and re-save.";
    utility.setFeedbackText(msg0, msg1, R.id.feedbackAnchorDuplicateNames, R.layout.feedback, R.id.feedbackText0, R.id.feedbackText1);
  }

  private void clearDuplicateWarning() {
    LinearLayout warning_layout = (LinearLayout) findViewById(R.id.feedbackAnchorDuplicateNames);
    warning_layout.removeAllViews();
    warning_layout.setVisibility(View.GONE);
  }

  /**
   * Populates the list with the relevant controllers from the deviceControllers variable.
   * That variable is either from scanned devices, or read in from an xml file.
   */
  private void populateList() {
    ListView controllerListView = (ListView) findViewById(R.id.controllersList);

    // Before we launch, we want the scan to have completed
    try {
      scannedDevices = usbScanManager.awaitScannedDevices();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Make sure we'll report serial numbers correctly as attached or not
    tellControllersAboutAttachment();

    DeviceInfoAdapter adapter = new DeviceInfoAdapter(this, android.R.layout.simple_list_item_2, new LinkedList<ControllerConfiguration>(getRobotConfigMap().controllerConfigurations()) );
    controllerListView.setAdapter(adapter);

    controllerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View v, int pos, long arg3)
        {
        ControllerConfiguration controllerConfiguration = (ControllerConfiguration) adapterView.getItemAtPosition(pos);
        ConfigurationType itemType = controllerConfiguration.getConfigurationType();
        if (itemType == BuiltInConfigurationType.MOTOR_CONTROLLER) {
          EditParameters parameters = initParameters(ModernRoboticsConstants.INITIAL_MOTOR_PORT,
                  DeviceConfiguration.class,
                  controllerConfiguration,
                  ((MotorControllerConfiguration)controllerConfiguration).getMotors());
          handleLaunchEdit(EditMotorControllerActivity.requestCode, EditMotorControllerActivity.class, parameters);
          }
        else if (itemType == BuiltInConfigurationType.SERVO_CONTROLLER) {
          EditParameters parameters = initParameters(ModernRoboticsConstants.INITIAL_SERVO_PORT,
                  DeviceConfiguration.class,
                  controllerConfiguration,
                  ((ServoControllerConfiguration)controllerConfiguration).getServos());
          parameters.setControlSystem(ControlSystem.MODERN_ROBOTICS);
          handleLaunchEdit(EditServoControllerActivity.requestCode, EditServoControllerActivity.class, parameters);
          }
        else if (itemType == BuiltInConfigurationType.LEGACY_MODULE_CONTROLLER) {
          EditParameters parameters = initParameters(0,
                  DeviceConfiguration.class,
                  controllerConfiguration,
                  ((LegacyModuleControllerConfiguration)controllerConfiguration).getDevices());
          handleLaunchEdit(EditLegacyModuleControllerActivity.requestCode, EditLegacyModuleControllerActivity.class, parameters);
          }
        else if (itemType == BuiltInConfigurationType.DEVICE_INTERFACE_MODULE) {
          EditParameters parameters = initParameters(0,
                  DeviceConfiguration.class,
                  controllerConfiguration,
                  ((DeviceInterfaceModuleConfiguration)controllerConfiguration).getDevices());
          handleLaunchEdit(EditDeviceInterfaceModuleActivity.requestCode, EditDeviceInterfaceModuleActivity.class, parameters);
          }
        else if (itemType == BuiltInConfigurationType.LYNX_USB_DEVICE) {
          EditParameters parameters = initParameters(0,
                  LynxModuleConfiguration.class,
                  controllerConfiguration,
                  ((LynxUsbDeviceConfiguration)controllerConfiguration).getDevices());
          handleLaunchEdit(EditLynxUsbDeviceActivity.requestCode, EditLynxUsbDeviceActivity.class, parameters);
          }
        else if (itemType == BuiltInConfigurationType.WEBCAM) {
          EditParameters parameters = initParameters(controllerConfiguration);
          handleLaunchEdit(EditWebcamActivity.requestCode, EditWebcamActivity.class, parameters);
          }
        }
      }
    );
  }

  <ITEM_T extends DeviceConfiguration> EditParameters initParameters(int initialPortNumber, Class<ITEM_T> itemClass, ControllerConfiguration controllerConfiguration, List<ITEM_T> currentItems) {
    EditParameters parameters = new EditParameters<ITEM_T>(this, controllerConfiguration, itemClass, currentItems);
    parameters.setInitialPortNumber(initialPortNumber);
    parameters.setScannedDevices(scannedDevices);
    parameters.setRobotConfigMap(this.getRobotConfigMap());
    return parameters;
  }

  <ITEM_T extends DeviceConfiguration> EditParameters initParameters(ControllerConfiguration controllerConfiguration) {
    return initParameters(0, DeviceConfiguration.class, controllerConfiguration, new ArrayList<DeviceConfiguration>());
  }


  @Override
  protected void onActivityResult(int requestCodeValue, int resultCode, Intent data) {
    try {
      logActivityResult(requestCodeValue, resultCode, data);
      if (resultCode == RESULT_CANCELED) {
        return;
      }
      RequestCode requestCode = RequestCode.fromValue(requestCodeValue);
      EditParameters parameters = EditParameters.fromIntent(this, data);

      RobotLog.vv(TAG, "onActivityResult(%s)", requestCode.toString());

      // Well, we were editing a controller, and we've been given new data. Many things
      // in the configuration might have been updated if swaps occurred.
      synchronized (robotConfigMapLock) {
        // We passed the whole config map in, and the editor passes the whole thing back out
        deserializeConfigMap(parameters);
      }

      // Refresh who is attached and who is not
      scannedDevices = usbScanManager.awaitScannedDevices();

      // Refresh our UI
      appUtil.runOnUiThread(new Runnable() {
        @Override public void run() {
          currentCfgFile.markDirty();
          robotConfigFileManager.updateActiveConfigHeader(currentCfgFile);
          populateList();
          }
        });

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void onBackPressed() {
    RobotLog.vv(TAG, "onBackPressed()");
    doBackOrCancel();
  }

  public void onCancelButtonPressed(View view) {
    RobotLog.vv(TAG, "onCancelButtonPressed()");
    doBackOrCancel();
  }

  // Called when either the user either backs out of the configuration editor
  // or hits the Cancel button.
  private void doBackOrCancel() {
    // If we're dirty, then we need to ask the user what to do. In response, we either will
    // exit (just like we'd do if we weren't dirty) or dismiss the dialog, in which case the
    // user needs to hit the 'Done' button in order to save and exit.
    if (currentCfgFile.isDirty()) {

      DialogInterface.OnClickListener exitWithoutSavingButtonListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {

          // We're about to toss our changes. We are the active config, in our dirty form, but
          // that's inappropriate once our changes our tossed. We don't bother updating the UI
          // header here since we're about to toss our screen; this avoids flicker. The screen
          // we return back to will update its own header on its own.
          currentCfgFile.markClean(); // will handle both the 'extant' and the 'no config' case correctly
          robotConfigFileManager.setActiveConfig(remoteConfigure, currentCfgFile);

          // exit editing this config
          finishCancel();
        }
      };

      AlertDialog.Builder builder = utility.buildBuilder(getString(R.string.saveChangesTitle), getString(R.string.saveChangesMessage));
      builder.setPositiveButton(R.string.buttonExitWithoutSaving, exitWithoutSavingButtonListener);
      builder.setNegativeButton(R.string.buttonNameCancel, doNothingAndCloseListener);
      builder.show();

    } else {

      // We're not dirty, so we just exit the configuration editor
      finishCancel();
    }
  }

  /**
   * A button-specific method, this gets called when you click the "Done" button.
   * This writes the current objects into an XML file located in the Configuration File Directory.
   * The user is prompted for the name of the file.
   * @param v the View from which this was called
   */
  public void onDoneButtonPressed(View v) {
    RobotLog.vv(TAG, "onDoneButtonPressed()");

    // Generate the XML. If that failed, we will already have complained to the user.
    final String data = robotConfigFileManager.toXml(getRobotConfigMap());
    if (data == null){
      return;
    }

    String message = getString(R.string.configNamePromptBanter);
    final EditText input = new EditText(this);
    input.setText(currentCfgFile.isNoConfig() ? "" : currentCfgFile.getName());

    AlertDialog.Builder builder = utility.buildBuilder(getString(R.string.configNamePromptTitle), message);
    builder.setView(input);

    DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener(){
      public void onClick(DialogInterface dialog, int button) {
        String newConfigurationName = input.getText().toString();

        RobotConfigFileManager.ConfigNameCheckResult checkResult = robotConfigFileManager.isPlausibleConfigName(currentCfgFile, newConfigurationName, extantRobotConfigurations);
        if (!checkResult.success) {
          // User hasn't given us file name that works
          String message = String.format(checkResult.errorFormat, newConfigurationName);
          appUtil.showToast(UILocation.ONLY_LOCAL, String.format("%s %s", message, getString(R.string.configurationNotSaved)));
          return;
        }

        try {
          /*
           * If the user changed the name then we create a new set of metadata for the
           * new name and set the active config to be the new metadata
           */
          if (!currentCfgFile.getName().equals(newConfigurationName)) {
            currentCfgFile = new RobotConfigFile(robotConfigFileManager, newConfigurationName);
          }
          robotConfigFileManager.writeToFile(currentCfgFile, remoteConfigure, data);
          robotConfigFileManager.setActiveConfigAndUpdateUI(remoteConfigure, currentCfgFile);

        } catch (DuplicateNameException e) {
          warnDuplicateNames(e.getMessage());
          RobotLog.ee(TAG, e.getMessage());
          return;
        } catch (RobotCoreException|IOException e) {
          appUtil.showToast(UILocation.ONLY_LOCAL, e.getMessage());
          RobotLog.ee(TAG, e.getMessage());
          return;
        }
        clearDuplicateWarning();
        confirmSave();
        pauseAfterSave();
        finishOk();
      }
    };

    builder.setPositiveButton(getString(R.string.buttonNameOK), okListener);
    builder.setNegativeButton(getString(R.string.buttonNameCancel), doNothingAndCloseListener);
    builder.show();
  }

  private void confirmSave(){
    Toast confirmation = Toast.makeText(this, R.string.toastSaved, Toast.LENGTH_SHORT);
    confirmation.setGravity(Gravity.BOTTOM, 0, 50);
    confirmation.show();
  }

  private void pauseAfterSave() {
    try { Thread.sleep(msSaveSplashDelay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }

  /**
   * Turns a list of device Controllers into a hashmap. When reading from an XML file,
   * you get a list back, so this builds up the hashmap from that list. The hashmap gets
   * used to populate the lists displaying the current devices.
   * @param deviceList a list of devices
   */
  private void buildControllersFromXMLResults(List<ControllerConfiguration> deviceList) {
    synchronized (robotConfigMapLock) {
      robotConfigMap = new RobotConfigMap(deviceList);
    }
  }

  private void buildRobotConfigMapFromScanned(ScannedDevices scannedDevices) {
    synchronized (robotConfigMapLock) {
      robotConfigMap = buildRobotConfigMapFromScanned(getRobotConfigMap(), scannedDevices);
    }
  }

  protected RobotConfigMap getRobotConfigMap() {
    synchronized (robotConfigMapLock) {
      return super.getRobotConfigMap();
    }
  }

  protected void tellControllersAboutAttachment() {
    for (ControllerConfiguration controllerConfiguration : getRobotConfigMap().controllerConfigurations()) {
        controllerConfiguration.setKnownToBeAttached(scannedDevices.containsKey(controllerConfiguration.getSerialNumber()));
        }
    }

  //------------------------------------------------------------------------------------------------
  // Remote handling
  //------------------------------------------------------------------------------------------------

  protected CallbackResult handleCommandScanResp(String extra) throws RobotCoreException {
    if (this.usbScanManager != null) {
      this.usbScanManager.handleCommandScanResponse(extra);
      // We might already be displaying the devices: the scan response might come in late.
      // In which case, they're not correctly showing attachment status. Update so they do.
      populateListAndWarnDevices();
    }
    return CallbackResult.HANDLED_CONTINUE;  // someone else in the chain might want the same data
  }

  protected CallbackResult handleCommandDiscoverLynxModulesResp(String extra) throws RobotCoreException {
    if (this.usbScanManager != null) {
      this.usbScanManager.handleCommandDiscoverLynxModulesResponse(extra);
    }
    return CallbackResult.HANDLED;
  }

  protected CallbackResult handleCommandRequestParticularConfigurationResp(String extra) throws RobotCoreException {
    ReadXMLFileHandler readXMLFileHandler = new ReadXMLFileHandler();
    List<ControllerConfiguration> controllerList = readXMLFileHandler.parse(new StringReader(extra));
    buildControllersFromXMLResults(controllerList);
    populateListAndWarnDevices();
    return CallbackResult.HANDLED;
  }

  @Override
  public CallbackResult commandEvent(Command command) {
    CallbackResult result = CallbackResult.NOT_HANDLED;
    try {
      String name = command.getName();
      String extra = command.getExtra();

      if (name.equals(CommandList.CMD_SCAN_RESP)) {
        result = handleCommandScanResp(extra);
      } else if (name.equals(CommandList.CMD_DISCOVER_LYNX_MODULES_RESP)) {
        result = handleCommandDiscoverLynxModulesResp(extra);
      } else if (name.equals(CommandList.CMD_REQUEST_PARTICULAR_CONFIGURATION_RESP)) {
        result = handleCommandRequestParticularConfigurationResp(extra);
      }
    } catch (RobotCoreException e) {
      RobotLog.logStacktrace(e);
    }
    return result;
  }

  @Override
  public CallbackResult onNetworkConnectionEvent(NetworkConnection.NetworkEvent event) {
    return CallbackResult.NOT_HANDLED;
  }

  @Override
  public CallbackResult packetReceived(RobocolDatagram packet) {
    return CallbackResult.NOT_HANDLED;
  }

  @Override
  public CallbackResult peerDiscoveryEvent(RobocolDatagram packet) {
    return CallbackResult.NOT_HANDLED;
  }

  @Override
  public CallbackResult heartbeatEvent(RobocolDatagram packet, long tReceived) {
    return CallbackResult.NOT_HANDLED;
  }

  @Override
  public CallbackResult telemetryEvent(RobocolDatagram packet) {
    return CallbackResult.NOT_HANDLED;
  }

  @Override
  public CallbackResult gamepadEvent(RobocolDatagram packet) {
    return CallbackResult.NOT_HANDLED;
  }

  @Override
  public CallbackResult emptyEvent(RobocolDatagram packet) {
    return CallbackResult.NOT_HANDLED;
  }

  @Override
  public CallbackResult reportGlobalError(String error, boolean recoverable) {
    return CallbackResult.NOT_HANDLED;
  }

}
