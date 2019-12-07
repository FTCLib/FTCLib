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

package com.qualcomm.ftccommon;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.eventloop.EventLoop;
import com.qualcomm.robotcore.eventloop.EventLoopManager;
import com.qualcomm.robotcore.eventloop.opmode.EventLoopManagerClient;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.factory.RobotFactory;
import com.qualcomm.robotcore.hardware.Blinker;
import com.qualcomm.robotcore.hardware.LightBlinker;
import com.qualcomm.robotcore.hardware.LightMultiplexor;
import com.qualcomm.robotcore.hardware.SwitchableLight;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationTypeManager;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.robot.Robot;
import com.qualcomm.robotcore.robot.RobotState;
import com.qualcomm.robotcore.robot.RobotStatus;
import com.qualcomm.robotcore.util.Device;
import com.qualcomm.robotcore.util.Intents;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;
import com.qualcomm.robotcore.util.WebServer;
import com.qualcomm.robotcore.wifi.NetworkConnection;
import com.qualcomm.robotcore.wifi.NetworkConnectionFactory;
import com.qualcomm.robotcore.wifi.NetworkType;

import org.firstinspires.ftc.robotcore.internal.hardware.android.AndroidBoard;
import org.firstinspires.ftc.robotcore.internal.hardware.android.DragonboardIndicatorLED;
import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.PeerStatus;
import org.firstinspires.ftc.robotcore.internal.network.PreferenceRemoterRC;
import org.firstinspires.ftc.robotcore.internal.network.WifiDirectAgent;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;
import org.firstinspires.ftc.robotserver.internal.webserver.CoreRobotWebServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FtcRobotControllerService extends Service implements NetworkConnection.NetworkConnectionCallback, WifiDirectAgent.Callback, EventLoopManagerClient {

  //----------------------------------------------------------------------------------------------
  // State
  //----------------------------------------------------------------------------------------------

  public final static String TAG = "FTCService";

  private final static int USB_WAIT = 5000; // in milliseconds
  private final static int NETWORK_WAIT = 1000; // in milliseconds

  private final IBinder binder = new FtcRobotControllerBinder();
  private final PreferencesHelper preferencesHelper = new PreferencesHelper(TAG);

  private volatile boolean robotSetupHasBeenStarted = false;

  private NetworkConnection networkConnection;
  private EventLoopManager  eventLoopManager;
  private Robot             robot;
  private EventLoop         eventLoop;
  private EventLoop         idleEventLoop;

  private NetworkConnection.NetworkEvent networkConnectionStatus = NetworkConnection.NetworkEvent.UNKNOWN;
  private RobotStatus       robotStatus = RobotStatus.NONE;

  private UpdateUI.Callback callback = null;
  private final EventLoopMonitor eventLoopMonitor = new EventLoopMonitor();

  private SwitchableLight bootIndicator = null;
  private Future          bootIndicatorOff = null;
  private LightBlinker    livenessIndicatorBlinker = null;
  private Future          robotSetupFuture = null;
  private WifiDirectAgent wifiDirectAgent = WifiDirectAgent.getInstance();
  private final Object    wifiDirectCallbackLock = new Object();

  private WebServer webServer;

  //----------------------------------------------------------------------------------------------
  // Initialization
  //----------------------------------------------------------------------------------------------

  public class FtcRobotControllerBinder extends Binder {
    public FtcRobotControllerService getService() {
      return FtcRobotControllerService.this;
    }
  }

  //----------------------------------------------------------------------------------------------
  // Types
  //----------------------------------------------------------------------------------------------

  private class EventLoopMonitor implements EventLoopManager.EventLoopMonitor {

  @Override
  public void onStateChange(@NonNull RobotState state) {
    if (callback == null) return;
    callback.updateRobotState(state);
    if (state == RobotState.RUNNING) {
      updateRobotStatus(RobotStatus.NONE);
    }
  }

  @Override public void onPeerConnected() {
    updatePeerStatus(PeerStatus.CONNECTED);

    // When we connect, send useful info to the driver station
    RobotLog.dd(TAG, "Sending user device types and preferences to driver station");
    ConfigurationTypeManager.getInstance().sendUserDeviceTypes();
    PreferenceRemoterRC.getInstance().sendAllPreferences(); // TODO: We should probably also do this periodically later in case this initial version didn't get through
  }

  @Override public void onPeerDisconnected() {
    updatePeerStatus(PeerStatus.DISCONNECTED);
  }

  private void updatePeerStatus(PeerStatus peerStatus) {
    if (callback == null) return;
    callback.updatePeerStatus(peerStatus);
  }

  @Override
  public void onTelemetryTransmitted() {
    if (callback == null) return;
    callback.refreshErrorTextOnUiThread();
  }

  } // End EventLoopMonitor inner class

  /** RobotSetupRunnable is run on a worker thread, and carries out the process of
   * getting the robot ready to run. If interrupted, it should return quickly and promptly. */
  private class RobotSetupRunnable implements Runnable {

    @Nullable Runnable runOnComplete;

    RobotSetupRunnable(@Nullable Runnable runOnComplete) {
      this.runOnComplete = runOnComplete;
    }

    //----------------------------------------------------------------------------------------------
    // Building blocks
    //----------------------------------------------------------------------------------------------

    void shutdownRobot() {
      // if an old robot is around, shut it down
      if (robot != null) {
        robot.shutdown();
        robot = null;
      }
    }

    void awaitUSB() throws InterruptedException {
      updateRobotStatus(RobotStatus.SCANNING_USB);
      /*
       * Give android a chance to finish scanning for USB devices before
       * we create our robot object.
       *
       * It takes Android some time per USB device plugged into a hub.
       * Higher quality hubs take less time.
       *
       * If USB hubs are chained this can take much longer.
       *
       * TODO: should be reviewed
       */
       Thread.sleep(USB_WAIT);
    }

    void initializeEventLoopAndRobot() throws RobotCoreException {
      if (eventLoopManager == null) {
        eventLoopManager = new EventLoopManager(FtcRobotControllerService.this, FtcRobotControllerService.this, idleEventLoop);
      }
      robot = RobotFactory.createRobot(eventLoopManager);
    }

    boolean waitForWifi() throws InterruptedException {
      updateRobotStatus(RobotStatus.WAITING_ON_WIFI);
      boolean waited = false;
      for (;;) {
        synchronized (wifiDirectCallbackLock) {
          if (wifiDirectAgent.isWifiEnabled()) return waited;
          waited = true;
          waitForNextWifiDirectCallback();
        }
      }
    }

    boolean waitForWifiDirect() throws InterruptedException {
      updateRobotStatus(RobotStatus.WAITING_ON_WIFI_DIRECT);
      boolean waited = false;
      for (;;) {
        synchronized (wifiDirectCallbackLock) {
          if (wifiDirectAgent.isWifiDirectEnabled()) return waited;
          waited = true;
          waitForNextWifiDirectCallback();
        }
      }
    }

    boolean waitForNetworkConnection() throws InterruptedException {
      RobotLog.vv(TAG, "Waiting for a connection to a wifi service");
      updateRobotStatus(RobotStatus.WAITING_ON_NETWORK_CONNECTION);
      boolean waited = false;
      for (;;) {
        if (networkConnection.isConnected()) {
          return waited;
        }
        waited = true;
        networkConnection.onWaitForConnection();
        Thread.sleep(NETWORK_WAIT);
      }
    }

    void waitForNetwork() throws InterruptedException {
      if (networkConnection.getNetworkType() == NetworkType.WIFIDIRECT) {
        waitForWifi();
        waitForWifiDirect();

        // Re-issue createConnection(): we might have just brought up the network in one of the
        // waits, so the previous createConnection() in bind above might have failed.
        networkConnection.createConnection();
      }
      // Wait until we're free and clear to go
      waitForNetworkConnection();
      webServer.start();
    }

    void startRobot() throws RobotCoreException {
      updateRobotStatus(RobotStatus.STARTING_ROBOT);
      robot.eventLoopManager.setMonitor(eventLoopMonitor);
      robot.start(eventLoop);
    }

    //----------------------------------------------------------------------------------------------
    // Core Operation
    //----------------------------------------------------------------------------------------------

    @Override public void run() {
      final boolean isFirstRun = !robotSetupHasBeenStarted;
      robotSetupHasBeenStarted = true;
      ThreadPool.logThreadLifeCycle("RobotSetupRunnable.run()", new Runnable() { @Override public void run() {

        RobotLog.vv(TAG, "Processing robot setup");
        try {

          shutdownRobot();
          awaitUSB();
          initializeEventLoopAndRobot();  // unclear why this step couldn't be folded into startRobot()
          waitForNetwork();
          startRobot();

        } catch (RobotCoreException e) {
          updateRobotStatus(RobotStatus.UNABLE_TO_START_ROBOT);
          RobotLog.setGlobalErrorMsg(e, getString(R.string.globalErrorFailedToCreateRobot));
        } catch (InterruptedException e) {
          updateRobotStatus(RobotStatus.ABORT_DUE_TO_INTERRUPT);
        } finally {
          if (runOnComplete != null) {
            runOnComplete.run();
          }
        }

        if (isFirstRun) {
          RobotLog.dd(TAG, "Detecting WiFi reset");
          networkConnection.detectWifiReset();
        }
      }});
    }
  }

  //----------------------------------------------------------------------------------------------
  // Wifi processing
  //----------------------------------------------------------------------------------------------

  @Override public void onReceive(Context context, Intent intent) {
    synchronized (wifiDirectCallbackLock) {
      wifiDirectCallbackLock.notifyAll();
    }
  }

  void waitForNextWifiDirectCallback() throws InterruptedException {
    synchronized (wifiDirectCallbackLock) {
      wifiDirectCallbackLock.wait();
    }
  }

  //----------------------------------------------------------------------------------------------
  // Accessing
  //----------------------------------------------------------------------------------------------

  public NetworkConnection getNetworkConnection() {
    return networkConnection;
  }

  public NetworkConnection.NetworkEvent getNetworkConnectionStatus() {
    return networkConnectionStatus;
  }

  public RobotStatus getRobotStatus() {
    return robotStatus;
  }

  public Robot getRobot() {
    return this.robot;
  }

  public @NonNull WebServer getWebServer() {
    return this.webServer;
  }

  @Override public void onCreate() {
    super.onCreate();
    RobotLog.vv(TAG, "onCreate()");
    wifiDirectAgent.registerCallback(this);
    startLEDS();
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    RobotLog.vv(TAG, "onStartCommand()");
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public IBinder onBind(Intent intent) {
    RobotLog.vv(TAG, "onBind()");

    preferencesHelper.writeBooleanPrefIfDifferent(getString(R.string.pref_wifip2p_remote_channel_change_works), Device.wifiP2pRemoteChannelChangeWorks());
    preferencesHelper.writeBooleanPrefIfDifferent(getString(R.string.pref_has_independent_phone_battery), !LynxConstants.isRevControlHub());
    boolean hasSpeaker = !LynxConstants.isRevControlHub();
    preferencesHelper.writeBooleanPrefIfDifferent(getString(R.string.pref_has_speaker), hasSpeaker);
    if (!hasSpeaker) {
      /** Turn off the sound if no speaker (helps UI; see {@link FtcRobotControllerSettingsActivity} */
      preferencesHelper.writeBooleanPrefIfDifferent(getString(R.string.pref_sound_on_off), false);
    }
    FtcLynxFirmwareUpdateActivity.initializeDirectories();

    NetworkType networkType = (NetworkType) intent.getSerializableExtra(NetworkConnectionFactory.NETWORK_CONNECTION_TYPE);
    webServer = new CoreRobotWebServer(networkType);

    networkConnection = NetworkConnectionFactory.getNetworkConnection(networkType, getBaseContext());
    networkConnection.setCallback(this);
    networkConnection.enable();
    networkConnection.createConnection();

    return binder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    RobotLog.vv(TAG, "onUnbind()");

    networkConnection.disable();
    shutdownRobot();

    if (eventLoopManager != null) {
      eventLoopManager.close();
      eventLoopManager = null;
    }

    return false; // don't have new clients call onRebind()
  }

  @Override public void onDestroy() {
    super.onDestroy();
    RobotLog.vv(TAG, "onDestroy()");
    webServer.stop();
    stopLEDS();
    wifiDirectAgent.unregisterCallback(this);
  }

  protected void startLEDS() {
    if (LynxConstants.useIndicatorLEDS()) {
      //
      // Reset state to something known
      for (int i = DragonboardIndicatorLED.LED_FIRST; i <= DragonboardIndicatorLED.LED_LAST; i++) {
        DragonboardIndicatorLED.forIndex(i).enableLight(false);
      }
      //
      bootIndicator = LightMultiplexor.forLight(DragonboardIndicatorLED.forIndex(LynxConstants.INDICATOR_LED_BOOT));
      bootIndicator.enableLight(true);
      bootIndicatorOff = ThreadPool.getDefaultScheduler().schedule(new Runnable() {
        @Override public void run() {
          bootIndicator.enableLight(false);
          }
        }, 10, TimeUnit.SECONDS);
      //
      livenessIndicatorBlinker = new LightBlinker(LightMultiplexor.forLight(DragonboardIndicatorLED.forIndex(LynxConstants.INDICATOR_LED_ROBOT_CONTROLLER_ALIVE)));
      int msLivenessLong = 5000;
      int msLivenessShort = 500;
      List<Blinker.Step> steps = new ArrayList<>();
      steps.add(new Blinker.Step(Color.GREEN, msLivenessLong - msLivenessShort, TimeUnit.MILLISECONDS));
      steps.add(new Blinker.Step(Color.BLACK, msLivenessShort, TimeUnit.MILLISECONDS));
      livenessIndicatorBlinker.setPattern(steps);
    }
  }

  protected void stopLEDS() {
    if (bootIndicatorOff != null) {
      bootIndicatorOff.cancel(false);
      bootIndicatorOff = null;
    }
    if (bootIndicator != null) {
      bootIndicator.enableLight(false);
      bootIndicator = null;
    }
    if (livenessIndicatorBlinker != null) {
      livenessIndicatorBlinker.stopBlinking();
      livenessIndicatorBlinker = null;
    }
  }

  public synchronized void setCallback(UpdateUI.Callback callback) {
    this.callback = callback;
    // Ensure that the callback starts with the correct peer status
    callback.updatePeerStatus(NetworkConnectionHandler.getInstance().isPeerConnected() ? PeerStatus.CONNECTED : PeerStatus.DISCONNECTED);
  }

  public synchronized void setupRobot(EventLoop eventLoop, EventLoop idleEventLoop, @Nullable Runnable runOnComplete) {

    /* 
     * (Possibly out-of-date comment:)
     * There is a bug in the Android activity life cycle with regards to apps
     * launched via USB. To work around this bug we will only honor this
     * method if setup is not currently running
     *
     * See: https://code.google.com/p/android/issues/detail?id=25701
     */

    shutdownRobotSetup();

    this.eventLoop = eventLoop;
    this.idleEventLoop = idleEventLoop;

    robotSetupFuture = ThreadPool.getDefault().submit(new RobotSetupRunnable(runOnComplete));
  }

  void shutdownRobotSetup() {
    if (robotSetupFuture != null) {
      ThreadPool.cancelFutureOrExitApplication(robotSetupFuture, 10, TimeUnit.SECONDS, "robot setup", "internal error");
      robotSetupFuture = null;
    }
  }

  public synchronized void shutdownRobot() {

    shutdownRobotSetup();

    // shut down the robot
    if (robot != null) robot.shutdown();
    robot = null; // need to set robot to null
    updateRobotStatus(RobotStatus.NONE);
  }

  @Override
  public CallbackResult onNetworkConnectionEvent(NetworkConnection.NetworkEvent event) {
    CallbackResult result = CallbackResult.NOT_HANDLED;
    RobotLog.ii(TAG, "onNetworkConnectionEvent: " + event.toString());
    switch (event) {
      case CONNECTED_AS_GROUP_OWNER:
        RobotLog.ii(TAG, "Wifi Direct - connected as group owner");
        if (!NetworkConnection.isDeviceNameValid(networkConnection.getDeviceName())) {
          RobotLog.ee(TAG, "Network Connection device name contains non-printable characters");
          ConfigWifiDirectActivity.launch(getBaseContext(), ConfigWifiDirectActivity.Flag.WIFI_DIRECT_DEVICE_NAME_INVALID);
          result = CallbackResult.HANDLED;
        }
        break;
      case CONNECTED_AS_PEER:
        RobotLog.ee(TAG, "Wifi Direct - connected as peer, was expecting Group Owner");
        ConfigWifiDirectActivity.launch(getBaseContext(), ConfigWifiDirectActivity.Flag.WIFI_DIRECT_FIX_CONFIG);
        result = CallbackResult.HANDLED;
        break;
      case CONNECTION_INFO_AVAILABLE:
        RobotLog.ii(TAG, "Network Connection Passphrase: " + networkConnection.getPassphrase());
        // Handling the case where we are changing networks and the web server has already been started.
        if (webServer.wasStarted()) {
          webServer.stop();
        }
        webServer.start();
        break;
      case ERROR:
        RobotLog.ee(TAG, "Network Connection Error: " + networkConnection.getFailureReason());
        break;
      case AP_CREATED:
        RobotLog.ii(TAG, "Network Connection created: " + networkConnection.getConnectionOwnerName());
      default:
        break;
    }

    updateNetworkConnectionStatus(event);
    return result;
  }

  private void updateNetworkConnectionStatus(final NetworkConnection.NetworkEvent event) {
    networkConnectionStatus = event;
    if (callback != null) callback.networkConnectionUpdate(networkConnectionStatus);
  }

  private void updateRobotStatus(@NonNull final RobotStatus status) {
    robotStatus = status;
    if (callback != null) {
      callback.updateRobotStatus(status);
    }
  }
}
