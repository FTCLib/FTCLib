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
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qualcomm.ftccommon.CommandList;
import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.RobocolDatagram;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.ui.UILocation;
import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RecvLoopRunnable;

import java.io.File;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link FtcLoadFileActivity} is responsible for managing the list of robot configuration files
 * on the robot controller phone.
 */
public class FtcLoadFileActivity extends EditActivity implements RecvLoopRunnable.RecvLoopCallback {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  // Also in Android.manifest
  public  static final String TAG = FtcConfigurationActivity.TAG;
  @Override public String getTag() { return TAG; }
  @Override protected FrameLayout getBackBar() { return findViewById(R.id.backbar); }

  private List<RobotConfigFile> fileList = new CopyOnWriteArrayList<RobotConfigFile>();
  private NetworkConnectionHandler networkConnectionHandler = NetworkConnectionHandler.getInstance();

  //------------------------------------------------------------------------------------------------
  // Life Cycle
  //------------------------------------------------------------------------------------------------

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    RobotLog.vv(TAG, "FtcLoadFileActivity started");
    setContentView(R.layout.activity_load);

    EditParameters parameters = EditParameters.fromIntent(this, getIntent());
    deserialize(parameters);

    buildInfoButtons();

    if (remoteConfigure) {
      // Set up so that we'll hear the incoming network traffic. And we want to do that even
      // if we're not on the front of the screen so we reliably see messages from the RC as
      // we're transitioning back from editing particular files.
      networkConnectionHandler.pushReceiveLoopCallback(this);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();

    if (!remoteConfigure) {
      robotConfigFileManager.createConfigFolder();
    }

    if (!remoteConfigure) {
      fileList = robotConfigFileManager.getXMLFiles();
      warnIfNoFiles();
    } else {
      // Ask the RC to send us (the DS) the list of configuration files
      networkConnectionHandler.sendCommand(new Command(CommandList.CMD_REQUEST_CONFIGURATIONS));
    }
    populate();
  }

  // RC has informed DS of the list of configuration files. Take that as gospel and update.
  protected CallbackResult handleCommandRequestConfigFilesResp(String extra) throws RobotCoreException {
    fileList = robotConfigFileManager.deserializeXMLConfigList(extra);
    warnIfNoFiles();
    populate();
    return CallbackResult.HANDLED;
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (remoteConfigure) {
      networkConnectionHandler.removeReceiveLoopCallback(this);
    }
  }

//------------------------------------------------------------------------------------------------
  // Misc
  //------------------------------------------------------------------------------------------------

  private void buildInfoButtons() {
    Button saveConfigButton = (Button) findViewById(R.id.files_holder).findViewById(R.id.info_btn);
    saveConfigButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        AlertDialog.Builder builder = utility.buildBuilder(getString(R.string.availableConfigListCaption), getString(R.string.availableConfigsInfoMessage));
        builder.setPositiveButton(getString(R.string.buttonNameOK), doNothingAndCloseListener);
        AlertDialog alert = builder.create();
        alert.show();
        TextView textView = (TextView) alert.findViewById(android.R.id.message);
        textView.setTextSize(14);
      }
    });

    Button configFromTemplateButton = (Button) findViewById(R.id.configureFromTemplateArea).findViewById(R.id.info_btn);
    configFromTemplateButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        AlertDialog.Builder builder = utility.buildBuilder(getString(R.string.configFromTemplateInfoTitle), getString(R.string.configFromTemplateInfoMessage));
        builder.setPositiveButton(getString(R.string.buttonNameOK), doNothingAndCloseListener);
        AlertDialog alert = builder.create();
        alert.show();
        TextView textView = (TextView) alert.findViewById(android.R.id.message);
        textView.setTextSize(14);
      }
    });
  }

  DialogInterface.OnClickListener doNothingAndCloseListener = new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int button) {
      //do nothing
    }
  };

  private void warnIfNoFiles() {
    if (fileList.size() == 0) {
      final String msg0 = getString(R.string.noFilesFoundTitle);
      final String msg1 = getString(R.string.noFilesFoundMessage);
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          utility.setFeedbackText(msg0, msg1, R.id.empty_filelist, R.layout.feedback, R.id.feedbackText0, R.id.feedbackText1);
        }
      });
    } else {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          ViewGroup empty_filelist = (ViewGroup) findViewById(R.id.empty_filelist);
          empty_filelist.removeAllViews();
          empty_filelist.setVisibility(View.GONE);
        }
      });
    }
  }

  private void populate() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        View readOnlyExplanation = findViewById(R.id.readOnlyExplanation);
        readOnlyExplanation.setVisibility(View.GONE);

        ViewGroup inclusionViewGroup = (ViewGroup) findViewById(R.id.inclusionlayout);
        inclusionViewGroup.removeAllViews();

        final Collator coll = Collator.getInstance();
        coll.setStrength(Collator.PRIMARY); // use case-insensitive compare
        Collections.sort(fileList, new Comparator<RobotConfigFile>() {
          @Override public int compare(RobotConfigFile lhs, RobotConfigFile rhs) {
            return coll.compare(lhs.getName(), rhs.getName());
            }
          });

        for (RobotConfigFile file : fileList) {
          View child = LayoutInflater.from(context).inflate(R.layout.file_info, null);
          inclusionViewGroup.addView(child);

          // Resource-based (as opposed to file based) configurations can't be deleted
          if (file.isReadOnly()) {
            Button deleteButton = (Button) child.findViewById(R.id.file_delete_button);
            deleteButton.setEnabled(false);
            deleteButton.setClickable(false);
            readOnlyExplanation.setVisibility(View.VISIBLE);
          }

          TextView name = (TextView) child.findViewById(R.id.filename_editText);
          name.setText(file.getName());
          name.setTag(file);

          child.findViewById(R.id.configIsReadOnlyFeedback).setVisibility(file.isReadOnly() ? View.VISIBLE : View.GONE);
        }
      }
    });
  }

  //------------------------------------------------------------------------------------------------
  // User responses
  //------------------------------------------------------------------------------------------------

  @Override
  protected void onActivityResult(int requestCodeValue, int resultCode, Intent data) {
    logActivityResult(requestCodeValue, resultCode, data);
    // The activity might have changed the current config (see EditActivity.onStart())
    this.currentCfgFile = robotConfigFileManager.getActiveConfigAndUpdateUI();
  }

  public void onNewButtonPressed(View v) {
    RobotConfigFile file = RobotConfigFile.noConfig(robotConfigFileManager);
    robotConfigFileManager.setActiveConfigAndUpdateUI(remoteConfigure, file);
    Intent intent = makeEditConfigIntent(FtcNewFileActivity.class, null);
    startActivityForResult(intent, FtcNewFileActivity.requestCode.value);
  }

  public void onFileEditButtonPressed(View v) {
    // Don't update the header here as that's just distracting. When we get to the
    // FtcConfigurationActivity page, it will have the right name, so it's also not necessary.
    RobotConfigFile file = getFile(v);
    robotConfigFileManager.setActiveConfig(remoteConfigure, file);
    Intent intent = makeEditConfigIntent(FtcConfigurationActivity.class, file);
    startActivityForResult(intent, FtcConfigurationActivity.requestCode.value);
  }

  public void onConfigureFromTemplatePressed(View v) {
    Intent intent = makeEditConfigIntent(ConfigureFromTemplateActivity.class, null);
    startActivityForResult(intent, ConfigureFromTemplateActivity.requestCode.value);
  }

  Intent makeEditConfigIntent(Class clazz, @Nullable RobotConfigFile configFile) {
    EditParameters parameters = new EditParameters(this);
    parameters.setExtantRobotConfigurations(fileList);
    // *Explicitly* indicate which file to edit in order to avoid races with background updates of the contextually current config
    if (configFile != null) parameters.setCurrentCfgFile(configFile);
    Intent intent = new Intent(context, clazz);
    parameters.putIntent(intent);
    return intent;
  }

  public void onFileActivateButtonPressed(View v) {
    RobotConfigFile file = getFile(v);
    robotConfigFileManager.setActiveConfigAndUpdateUI(remoteConfigure, file);

    if (remoteConfigure) {
      // DS tells the RC to activate a particular file
      networkConnectionHandler.sendCommand(new Command(CommandList.CMD_ACTIVATE_CONFIGURATION, file.toString()));
    }
  }

  public void onFileDeleteButtonPressed(View v) {
    // Ask the user if he really wants to delete the configuration
    final RobotConfigFile robotConfigFile = getFile(v);
    if (robotConfigFile.getLocation() == RobotConfigFile.FileLocation.LOCAL_STORAGE) {
      AlertDialog.Builder builder = utility.buildBuilder(getString(R.string.confirmConfigDeleteTitle), getString(R.string.confirmConfigDeleteMessage));
      DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface dialog, int button){
        doDeleteConfiguration(robotConfigFile);
        }
      };
      builder.setPositiveButton(R.string.buttonNameOK, okListener);
      builder.setNegativeButton(R.string.buttonNameCancel, doNothingAndCloseListener);
      builder.show();
    } else {
      // It's not a file-based configuration; we can't delete it
    }
  }

  /**
   * @see com.qualcomm.ftccommon.FtcEventLoop#handleCommandDeleteConfiguration(String)
   */
  void doDeleteConfiguration(RobotConfigFile robotConfigFile) {
    if (remoteConfigure) {
      if (robotConfigFile.getLocation() == RobotConfigFile.FileLocation.LOCAL_STORAGE) {
        networkConnectionHandler.sendCommand(new Command(CommandList.CMD_DELETE_CONFIGURATION, robotConfigFile.toString()));
        fileList.remove(robotConfigFile);
        populate();
      }
      // For robustness, refresh the list from the robot controller
      networkConnectionHandler.sendCommand(new Command(CommandList.CMD_REQUEST_CONFIGURATIONS));

    } else {
      if (robotConfigFile.getLocation() == RobotConfigFile.FileLocation.LOCAL_STORAGE) {
        File file = robotConfigFile.getFullPath();
        if (file.delete()) {
          // all is well
        } else {
          String filenameWExt = file.getName();
          appUtil.showToast(UILocation.ONLY_LOCAL, String.format(getString(R.string.configToDeleteDoesNotExist), filenameWExt));
          RobotLog.ee(TAG, "Tried to delete a file that does not exist: " + filenameWExt);
        }
      }
      fileList = robotConfigFileManager.getXMLFiles();
      populate();
    }

    RobotConfigFile cfgFile = RobotConfigFile.noConfig(robotConfigFileManager);
    robotConfigFileManager.setActiveConfigAndUpdateUI(remoteConfigure, cfgFile);
  }

  private RobotConfigFile getFile(View v) {
    LinearLayout horizontalButtons = (LinearLayout) v.getParent();
    LinearLayout linearLayout = (LinearLayout) horizontalButtons.getParent();
    TextView name = (TextView) linearLayout.findViewById(R.id.filename_editText);
    return (RobotConfigFile) name.getTag();
  }

  @Override
  public void onBackPressed() {
  // Back from this activity leads to the main RC and DS screens. They will auto-read the
  // current configuration; we don't need to pass it back using an Intent().
    logBackPressed();
    finishOk();
  }

  //------------------------------------------------------------------------------------------------
  // Remote handling
  //------------------------------------------------------------------------------------------------

  @Override
  public CallbackResult commandEvent(Command command) {
    CallbackResult result = CallbackResult.NOT_HANDLED;
    try {
      String name = command.getName();
      String extra = command.getExtra();

      if (name.equals(CommandList.CMD_REQUEST_CONFIGURATIONS_RESP)) {
        result = handleCommandRequestConfigFilesResp(extra);
      } else if (name.equals(CommandList.CMD_NOTIFY_ACTIVE_CONFIGURATION)) {
        result = handleCommandNotifyActiveConfig(extra);
      }
    } catch (RobotCoreException e) {
      RobotLog.logStacktrace(e);
    }
    return result;
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
