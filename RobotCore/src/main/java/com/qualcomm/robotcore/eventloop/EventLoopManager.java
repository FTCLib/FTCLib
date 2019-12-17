/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.robotcore.eventloop;

import android.content.Context;
import android.support.annotation.NonNull;

import com.qualcomm.robotcore.eventloop.opmode.EventLoopManagerClient;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.exception.RobotProtocolException;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.usb.RobotUsbModule;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.Heartbeat;
import com.qualcomm.robotcore.robocol.PeerDiscovery;
import com.qualcomm.robotcore.robocol.RobocolDatagram;
import com.qualcomm.robotcore.robocol.TelemetryMessage;
import com.qualcomm.robotcore.robot.RobotState;
import com.qualcomm.robotcore.util.*;
import com.qualcomm.robotcore.wifi.NetworkConnection;
import com.qualcomm.robotcore.wifi.NetworkType;

import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.PeerStatusCallback;
import org.firstinspires.ftc.robotcore.internal.network.RecvLoopRunnable;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;
import org.firstinspires.ftc.robotcore.internal.network.SocketConnect;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeManagerImpl;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Event Loop Manager
 * <p>
 * Takes RobocolDatagram messages, converts them into the appropriate data type, and then passes it
 * to the current EventLoop.
 */
@SuppressWarnings("unused,WeakerAccess")
public class EventLoopManager implements RecvLoopRunnable.RecvLoopCallback, NetworkConnection.NetworkConnectionCallback, PeerStatusCallback, SyncdDevice.Manager {

  //------------------------------------------------------------------------------------------------
  // Types
  //------------------------------------------------------------------------------------------------

  /**
   * Callback to monitor when event loop changes state
   */
  public interface EventLoopMonitor {
    void onStateChange(@NonNull RobotState state);
    void onTelemetryTransmitted();
    void onPeerConnected();
    void onPeerDisconnected();
  }

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  public  static final  String TAG = "EventLoopManager";
  private static final  boolean DEBUG = false;
  private static final  int HEARTBEAT_WAIT_DELAY = 500; // in milliseconds
  private static final  int MAX_COMMAND_CACHE = 8;

  // We use strings that are unlikely to inadvertently collide with user-specified telemetry keys
  public static final   String SYSTEM_NONE_KEY          = "$System$None$";
  public static final   String SYSTEM_ERROR_KEY         = "$System$Error$";
  public static final   String SYSTEM_WARNING_KEY       = "$System$Warning$";
  public static final   String ROBOT_BATTERY_LEVEL_KEY  = "$Robot$Battery$Level$";
  public static final   String RC_BATTERY_STATUS_KEY    = "$RobotController$Battery$Status$";

  /** If no heartbeat is received in this amount of time, forceable shut down the robot */
  private static final  double                SECONDS_UNTIL_FORCED_SHUTDOWN = 2.0;
  private final         EventLoop             idleEventLoop;
  public                RobotState            state                     = RobotState.NOT_STARTED;
  private               ExecutorService       executorEventLoop         = ThreadPool.newSingleThreadExecutor("executorEventLoop");
  private               ElapsedTime           lastHeartbeatReceived     = new ElapsedTime();
  private               EventLoop             eventLoop                 = null;
  private final         Object                eventLoopLock             = new Object();
  private final         Gamepad               gamepads[]                = { new Gamepad(), new Gamepad() };
  private               Heartbeat             heartbeat                 = new Heartbeat();
  private               boolean               attemptedSetTime          = false;
  private               EventLoopMonitor      callback                  = null;
  private final         Set<SyncdDevice>      syncdDevices              = new CopyOnWriteArraySet<SyncdDevice>(); // Would be nice if this held weak references
  private final         Command[]             commandRecvCache          = new Command[MAX_COMMAND_CACHE];
  private               int                   commandRecvCachePosition  = 0;
  private               InetAddress           remoteAddr;
  private final         Object                refreshSystemTelemetryLock  = new Object();
  private               String                lastSystemTelemetryMessage  = null;
  private               String                lastSystemTelemetryKey      = null;
  private               long                  lastSystemTelemetryNanoTime = 0;
  private final @NonNull Context              context;
  private final @NonNull EventLoopManagerClient eventLoopManagerClient;
  private               AppUtil               appUtil = AppUtil.getInstance();
  private               NetworkConnectionHandler networkConnectionHandler = NetworkConnectionHandler.getInstance();

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  /**
   * Constructor
   */
  public EventLoopManager(@NonNull Context context, @NonNull EventLoopManagerClient eventLoopManagerClient, @NonNull EventLoop idleEventLoop) {
    this.context = context;
    this.eventLoopManagerClient = eventLoopManagerClient;
    this.idleEventLoop = idleEventLoop;
    this.eventLoop = idleEventLoop;
    changeState(RobotState.NOT_STARTED);
    NetworkConnectionHandler.getInstance().registerPeerStatusCallback(this);
  }

  //------------------------------------------------------------------------------------------------
  // Accessors
  //------------------------------------------------------------------------------------------------

  public @NonNull
  WebServer getWebServer() {
    return eventLoopManagerClient.getWebServer();
  }

  /**
   * Set a monitor for this event loop, which will immediately have the appropriate method called to
   * indicate the current peer status.
   *
   * @param monitor event loop monitor
   */
  public void setMonitor(EventLoopMonitor monitor) {
    callback = monitor;

    // Alert the callback to the current peer status
    if (NetworkConnectionHandler.getInstance().isPeerConnected()) {
      callback.onPeerConnected();
    } else {
      callback.onPeerDisconnected();
    }
  }

  /** return any event loop monitor previously set */
  public EventLoopMonitor getMonitor() {
    return callback;
  }

  /**
   * Get the current event loop
   *
   * @return current event loop
   */
  public EventLoop getEventLoop() {
    return eventLoop;
  }

  /**
   * Get the gamepad connected to a particular user
   * @param port user 0 and 1 are valid
   * @return gamepad
   */
  public Gamepad getGamepad(int port) {
    Range.throwIfRangeIsInvalid(port, 0, 1);
    return gamepads[port];
  }

  /**
   * Get the gamepads
   * <p>
   * Array index will match the user number
   * @return gamepad
   */
  public Gamepad[] getGamepads() {
    return gamepads;
  }

  /**
   * Get the current heartbeat state
   *
   * @return heartbeat
   */
  public Heartbeat getHeartbeat() {
    return heartbeat;
  }

  //------------------------------------------------------------------------------------------------
  // Runnables
  //------------------------------------------------------------------------------------------------

  @Override
  public CallbackResult telemetryEvent(RobocolDatagram packet) {
    return CallbackResult.NOT_HANDLED;
    }

  @Override
  public CallbackResult reportGlobalError(String error, boolean recoverable) {
    RobotLog.setGlobalErrorMsg(error);
    return CallbackResult.HANDLED;
    }

  @Override
  public CallbackResult packetReceived(RobocolDatagram packet) {
    EventLoopManager.this.refreshSystemTelemetry();
    return CallbackResult.NOT_HANDLED;  // if we said 'handled', that would suppress dispatch by packet type, always, which is pointless
  }

  /*
   * Responsible for calling loop on the assigned event loop
   */
  private class EventLoopRunnable implements Runnable {
    @Override
    public void run() {
      ThreadPool.logThreadLifeCycle("opmode loop()", new Runnable() { @Override public void run() {

      try {
        ElapsedTime loopTime = new ElapsedTime();
        final double MIN_THROTTLE = 0.0010; // in seconds
        final long THROTTLE_RESOLUTION = 5; // in milliseconds

        while (!Thread.currentThread().isInterrupted()) {

          while (loopTime.time() < MIN_THROTTLE) {
            // don't go faster than throttle allows
            Thread.sleep(THROTTLE_RESOLUTION);
          }
          loopTime.reset();

          // Send any pending errors or warnings to other apps
          EventLoopManager.this.refreshSystemTelemetry();

          if (lastHeartbeatReceived.startTime() == 0.0) {
            // We haven't received a heartbeat so slow the whole thing down
            // Note that the actual disconnect is detected in the lower network layer
            Thread.sleep(HEARTBEAT_WAIT_DELAY);
          }

          // see if any devices have abnormally shutdown. if they have, then remember that
          // they've detached.
          for (SyncdDevice device : syncdDevices) {
            SyncdDevice.ShutdownReason shutdownReason = device.getShutdownReason();
            if (shutdownReason != SyncdDevice.ShutdownReason.NORMAL) {
              RobotLog.v("event loop: device has shutdown abnormally: %s", shutdownReason);
              RobotUsbModule robotUsbModule = device.getOwner();
              if (robotUsbModule != null) {
                RobotLog.vv(TAG, "event loop: detaching device %s", robotUsbModule.getSerialNumber());
                synchronized (eventLoopLock) {
                  eventLoop.handleUsbModuleDetach(robotUsbModule);

                  // If we're to automatically attempt an reopen, do that in a little bit so
                  // as to give the system a chance to settle down a bit to recover from, e.g.,
                  // a big ESD zap. At this point, the delay is more theoretically needed than
                  // practically demonstrated as required.
                  if (shutdownReason == SyncdDevice.ShutdownReason.ABNORMAL_ATTEMPT_REOPEN) {
                    RobotLog.vv(TAG, "event loop: auto-reattaching device %s", robotUsbModule.getSerialNumber());
                    eventLoop.pendUsbDeviceAttachment(robotUsbModule.getSerialNumber(), SyncdDevice.msAbnormalReopenInterval, TimeUnit.MILLISECONDS);
                  }
                }
              }
            }
          }

          // conversely, if any devices have attached, now is a good time for the eventLoop to process them
          synchronized (eventLoopLock) {
            eventLoop.processedRecentlyAttachedUsbDevices();
          }

          // run the event loop
          try {
            synchronized (eventLoopLock) {
              eventLoop.loop();
            }
          } catch (Exception e) {
            // we should catch everything, since we don't know what the event loop might throw
            RobotLog.ee(TAG, e, "Event loop threw an exception");

            // display error message. it will get reported to DS in the RobotCoreException handler below
            String errorMsg = e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : "");
            RobotLog.setGlobalErrorMsg("User code threw an uncaught exception: " + errorMsg);
            throw new RobotCoreException("EventLoop Exception in loop(): %s", errorMsg);
          }
        }
      } catch (InterruptedException e) {
        // interrupted: exit this loop
        RobotLog.v("EventLoopRunnable interrupted");
        Thread.currentThread().interrupt(); // pass on interrupt to caller
        changeState(RobotState.STOPPED);
      } catch (CancellationException e) {
        // interrupted, then cancel thrown: exit this loop
        RobotLog.v("EventLoopRunnable cancelled");
        changeState(RobotState.STOPPED);
      } catch (RobotCoreException e) {
        RobotLog.v("RobotCoreException in EventLoopManager: " + e.getMessage());
        changeState(RobotState.EMERGENCY_STOP);

        EventLoopManager.this.refreshSystemTelemetry();
      }

      // after loop finishes, close all the devices and tear down the event loop.
      try {
        // We synchronize on the eventLoopLock so that we won't try to start or stop the event
        // loop while it's busy processing a command, lest unexpected concurrency errors occur
        synchronized (eventLoopLock) {
          eventLoop.teardown();
        }
      } catch (Exception e) {
        RobotLog.ww(TAG, e, "Caught exception during looper teardown: " + e.toString());

        EventLoopManager.this.refreshSystemTelemetry();
      }
      }});
    }
  }

  //------------------------------------------------------------------------------------------------
  // Misc
  //------------------------------------------------------------------------------------------------

  /**
   * Forces an immediate refresh of the system telemetry
   */
  public void refreshSystemTelemetryNow() {
    lastSystemTelemetryNanoTime = 0;
    refreshSystemTelemetry();
  }

  /**
   * Do our best to maintain synchrony of the system error / warning state between applications
   * without incurring undo overhead.
   */
  public void refreshSystemTelemetry() {

    // As this method can be called from multiple or various threads, we enforce a
    // one-at-a-time policy in the execution
    synchronized (refreshSystemTelemetryLock) {

      String message;
      String key;
      long   now = System.nanoTime();

      String errorMessage   = RobotLog.getGlobalErrorMsg();
      String warningMessage = RobotLog.getGlobalWarningMessage();

      // Figure out what things *should* like
      if (!errorMessage.isEmpty()) {
        message = errorMessage;
        key = SYSTEM_ERROR_KEY;
      }
      else if (!warningMessage.isEmpty()) {
        message = warningMessage;
        key = SYSTEM_WARNING_KEY;
      }
      else {
        message = "";
        key = SYSTEM_NONE_KEY;
      }

      // If nothing has changed, we only retransmit periodically. As this is only to repair
      // the error/warning display on dropped-then-reestablished connections, we don't need to
      // do this very often.
      long nanoTimeRetransmitInterval = 5000 * ElapsedTime.MILLIS_IN_NANO;

      // If things don't look like they should, or it's been a while (perhaps the driver station
      // is just connected?) then retransmit. Log on the transition of those so we can better correlate
      // log entries with what the user sees.
      boolean shouldLog = !message.equals(lastSystemTelemetryMessage)
              || !key.equals(lastSystemTelemetryKey);
      boolean shouldTransmit = shouldLog
              || (now - lastSystemTelemetryNanoTime > nanoTimeRetransmitInterval);

      if (shouldLog) {
        RobotLog.d("system telemetry: key=%s msg=\"%s\"", key, message);
      }

      if (shouldTransmit) {
        lastSystemTelemetryMessage  = message;
        lastSystemTelemetryKey      = key;
        lastSystemTelemetryNanoTime = now;
        //
        this.buildAndSendTelemetry(key, message);
        if (callback != null) callback.onTelemetryTransmitted();
      }
    }
  }

  @Override public CallbackResult onNetworkConnectionEvent(NetworkConnection.NetworkEvent event) {
    CallbackResult result = CallbackResult.NOT_HANDLED;
    RobotLog.ii(TAG, "onNetworkConnectionEvent: " + event.toString());
    switch (event) {
      case PEERS_AVAILABLE:
        result = networkConnectionHandler.handlePeersAvailable();
        break;
      case CONNECTION_INFO_AVAILABLE:
        RobotLog.ii(RobocolDatagram.TAG, "Received network connection event");
        result = networkConnectionHandler.handleConnectionInfoAvailable(SocketConnect.DEFER);
        break;
    }
    return result;
  }

  /**
   * Starts up the {@link EventLoopManager}. This mostly involves setting up the network
   * connections and listeners and senders, then getting the event loop thread going.
   *
   * Note that shutting down the {@link EventLoopManager} does <em>not</em> do a full
   * complete inverse. Rather, it leaves the underlying network connection alive and
   * running, as this, among other things, helps remote toasts to continue to function
   * correctly. Thus, we must be aware of that possibility here as we start.
   *
   * @see #shutdown()
   */
  public void start(@NonNull EventLoop eventLoop) throws RobotCoreException {
    RobotLog.vv(RobocolDatagram.TAG, "EventLoopManager.start()");

    networkConnectionHandler.pushNetworkConnectionCallback(this);
    networkConnectionHandler.pushReceiveLoopCallback(this);

    NetworkType networkType = networkConnectionHandler.getDefaultNetworkType(context);
    networkConnectionHandler.init(networkType, context);  // idempotent

    // see also similar code in the driver station startup logic
    if (networkConnectionHandler.isNetworkConnected()) {
      // spoof a wifi direct event. Some devices won't send this event out,
      // so to complete our setup, we will spoof it to get all the necessary information.
      RobotLog.vv(RobocolDatagram.TAG, "Spoofing a Network Connection event...");
      onNetworkConnectionEvent(NetworkConnection.NetworkEvent.CONNECTION_INFO_AVAILABLE);
    } else {
      RobotLog.vv(RobocolDatagram.TAG, "Network not yet available, deferring network connection event...");
    }

    // setEventLoop() will throw if there's a hardware configuration issue. So
    // we do that *after* we start the threads we use to talk to the driver station.
    setEventLoop(eventLoop);
  }

  /**
   * Performs the logical inverse of {@link #start(EventLoop)}.
   * @see #start(EventLoop)
   */
  public void shutdown() {
    RobotLog.vv(RobocolDatagram.TAG, "EventLoopManager.shutdown()");
    stopEventLoop();
  }

  public void close() {
    RobotLog.vv(RobocolDatagram.TAG, "EventLoopManager.close()");
    networkConnectionHandler.shutdown();
    networkConnectionHandler.removeNetworkConnectionCallback(this);
    networkConnectionHandler.removeReceiveLoopCallback(this);
  }

  /**
   * Register a sync'd device
   *
   * @param device sync'd device
   * @see #unregisterSyncdDevice(SyncdDevice)
   */
  public void registerSyncdDevice(SyncdDevice device) {
  syncdDevices.add(device);
  }

  /**
   * Unregisters a device from this event loop. It is specifically permitted to unregister
   * a device which is not currently registered; such an operation has no effect.
   *
   * @param device the device to be unregistered. May not be null.
   * @see #registerSyncdDevice(SyncdDevice)
   */
  public void unregisterSyncdDevice(SyncdDevice device) {
  syncdDevices.remove(device);
  }

  /**
   * Replace the current event loop with a new event loop
   *
   * @param eventLoop new event loop
   * @throws RobotCoreException if event loop fails to init
   */
  public void setEventLoop(@NonNull EventLoop eventLoop) throws RobotCoreException {

    // cancel the old event loop
    stopEventLoop();

    synchronized (eventLoopLock) {
      // assign the new event loop
      this.eventLoop = eventLoop;
      RobotLog.vv(RobocolDatagram.TAG, "eventLoop=%s", this.eventLoop.getClass().getSimpleName());
    }

    // start the new event loop
    startEventLoop();
  }

  /**
   * Send telemetry data
   * <p>
   * Send the telemetry data, and then clear the sent data
   * @param telemetry telemetry data
   */
  public void sendTelemetryData(TelemetryMessage telemetry) {
    try {
      telemetry.setRobotState(this.state);  // conveying state here helps global errors always be portrayed as in EMERGENCY_STOP state rather than waiting until next heartbeat
      networkConnectionHandler.sendDatagram(new RobocolDatagram(telemetry.toByteArrayForTransmission()));
    } catch (RobotCoreException e) {
      RobotLog.ww(TAG, e, "Failed to send telemetry data");
    }

    // clear the stale telemetry data
    telemetry.clearData();
  }

  private void startEventLoop() throws RobotCoreException {
    // call the init method
    try {
      changeState(RobotState.INIT);
      synchronized (eventLoopLock) {
        eventLoop.init(this);
      }

    } catch (Exception e) {
      RobotLog.ww(TAG, e, "Caught exception during looper init: " + e.toString());
      changeState(RobotState.EMERGENCY_STOP);

      this.refreshSystemTelemetry();

      throw new RobotCoreException("Robot failed to start: " + e.getMessage());
    }

    // reset the heartbeat timer
    lastHeartbeatReceived = new ElapsedTime(0);

    // start the new event loop
    changeState(RobotState.RUNNING);

    executorEventLoop = ThreadPool.newSingleThreadExecutor("executorEventLoop");
    executorEventLoop.execute(new Runnable() {
      @Override public void run() {
        // Run the main event loop
        new EventLoopRunnable().run();
        // When that terminates (perhaps due to an exception), run the idle loop until we're asked
        // to shutdown. This keeps *some* event loop always running so long as we're not shutdown.
        eventLoop = idleEventLoop;
        boolean runIdle = true;
        if (!Thread.currentThread().isInterrupted()) {
          RobotLog.vv(TAG, "switching to idleEventLoop");
          try {
            synchronized (eventLoopLock) {
              eventLoop.init(EventLoopManager.this);
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } catch (RobotCoreException e) {
            RobotLog.ee(TAG, e, "internal error");
            runIdle = false;
          }
          if (runIdle) {
            new EventLoopRunnable().run();
          }
        }
      }
    });
  }

  private void stopEventLoop() {

    // Prevent stuck-loop LinearOpMode from staying in memory during Restart Robot action.
    if(eventLoop.getOpModeManager() != null)
    {
      eventLoop.getOpModeManager().stopActiveOpMode();
    }

    executorEventLoop.shutdownNow();
    ThreadPool.awaitTerminationOrExitApplication(executorEventLoop, 10, TimeUnit.SECONDS, "EventLoop", "possible infinite loop in user code?");

    // inform callback that event loop has been stopped
    changeState(RobotState.STOPPED);

    eventLoop = idleEventLoop;

    // unregister all sync'd devices
    syncdDevices.clear();
  }

  private void changeState(@NonNull RobotState state) {
    this.state = state;
    RobotLog.v("EventLoopManager state is " + state.toString());
    if (callback != null) callback.onStateChange(state);
    // Keep the DS informed of robot state in a timely way. See also Heartbeat & TelemetryMessage
    networkConnectionHandler.sendCommand(new Command(RobotCoreCommandList.CMD_NOTIFY_ROBOT_STATE, Integer.toString(state.asByte())));
  }

  /*
   * Event processing methods
   */

  @Override
  public CallbackResult gamepadEvent(RobocolDatagram packet) throws RobotCoreException {
    if (DEBUG) RobotLog.vv(RobocolDatagram.TAG, "processing gamepad event");
    Gamepad gamepad = new Gamepad();
    gamepad.fromByteArray(packet.getData());

    if (gamepad.getUser() == null) {
      // this gamepad user is invalid, we cannot use
      RobotLog.ee(TAG, "gamepad with user %d received; only users 1 and 2 are valid", gamepad.getUser().id);
      return CallbackResult.HANDLED;
    }

    // swap out old state of the gamepad for the new one
    int position = gamepad.getUser().id - 1;
    gamepads[position].copy(gamepad);

    // When the driver station sends a synthetic gamepad, that comes in with a defined and
    // definite user but an unusual gamepad identity. The DS always sends a synthetic gamepad
    // when a user looses access to a gamepad they previously had, either because the gamepad itself
    // disconnected, or because it was assigned to some other user
    if (gamepad.getGamepadId() == Gamepad.ID_SYNTHETIC) {
      RobotLog.vv(TAG, "synthetic gamepad received: id=%d user=%s atRest=%s ", gamepad.getGamepadId(), gamepad.getUser(), gamepad.atRest());
      gamepad.setGamepadId(Gamepad.ID_UNASSOCIATED);  // we no longer care where it came from; keep ID_SYNTHETIC only for transmission
    }
    return CallbackResult.HANDLED;
  }

  @Override
  public CallbackResult heartbeatEvent(RobocolDatagram packet, long tReceived) throws RobotCoreException  {

    Heartbeat currentHeartbeat = new Heartbeat();
    currentHeartbeat.fromByteArray(packet.getData());
    currentHeartbeat.setRobotState(state);

    /**
     * We've just received an indication of our partner's time. If our own time is insane, and theirs
     * is sane, and we haven't bothered trying before, then try to make our own time sane using their
     * sane value.
     */
    if (!attemptedSetTime) {
      boolean ourSanity = appUtil.isSaneWalkClockTime(appUtil.getWallClockTime());
      if (!ourSanity) {
        long theirMillis = currentHeartbeat.t0;
        boolean theirSanity = appUtil.isSaneWalkClockTime(theirMillis);
        // RobotLog.vv(TAG, "our sanity: %s their sanity: %s", ourSanity, theirSanity);
        if (theirSanity && !ourSanity) {
          attemptedSetTime = true;
          appUtil.setWallClockTime(theirMillis);
          appUtil.setTimeZone(currentHeartbeat.getTimeZoneId());
        }
      }
    }

    // Build up a response packet and send it on back, providing the clock information necessary
    // to understand the clock offsets between us and our partner.
    currentHeartbeat.t1 = tReceived;

    // Keep next three lines as close together in time as possible to maximize accuracy of timing calculation
    // Also, the refetch of the local wall clock time is intentional, for both the accuracy reasons and the
    // fact that we might have just adjusted the time.
    currentHeartbeat.t2 = appUtil.getWallClockTime();
    packet.setData(currentHeartbeat.toByteArrayForTransmission());
    networkConnectionHandler.sendDatagram(packet);

    lastHeartbeatReceived.reset();
    heartbeat = currentHeartbeat;
    return CallbackResult.HANDLED;
  }

  @Override public void onPeerConnected() {
    if (callback != null) {
      callback.onPeerConnected();
    }
  }

  @Override public void onPeerDisconnected() {
    if (callback != null) {
      callback.onPeerDisconnected();
    }

    // If we lose contact with the DS, then we auto-stop the robot
    OpModeManagerImpl opModeManager = eventLoop.getOpModeManager();

    // opModeManager will be null if not running FtcEventLoop right now
    if (opModeManager != null) {
      String msg = "Lost connection while running op mode: " + opModeManager.getActiveOpModeName();
      opModeManager.initActiveOpMode(OpModeManager.DEFAULT_OP_MODE_NAME);
      RobotLog.i(msg);
    }
    else {
      RobotLog.i("Lost connection while main event loop not active");
    }

    remoteAddr = null;
    lastHeartbeatReceived = new ElapsedTime(0);
  }

  public CallbackResult peerDiscoveryEvent(RobocolDatagram packet) throws RobotCoreException {

    try {
      networkConnectionHandler.updateConnection(packet);
    } catch (RobotProtocolException e) {
      RobotLog.ee(TAG, e.getMessage());
    }

    // Send a second PeerDiscovery() packet in response. That will inform the fellow
    // who sent the incoming PeerDiscovery() who *we* are.
    //
    // We should still send a peer discovery packet even if *we* already know the client,
    // because it could be that the connection dropped (e.g., while changing other settings)
    // and the other guy (ie: DS) needs to reconnect. If we don't send this, the connection will
    // never complete. These only get sent about once per second so it's not a huge load on the network.

    PeerDiscovery outgoing = new PeerDiscovery(PeerDiscovery.PeerType.PEER);
    RobocolDatagram outgoingDatagram = new RobocolDatagram(outgoing);
    networkConnectionHandler.sendDatagram(outgoingDatagram);

    return CallbackResult.HANDLED;
  }


  @Override
  public CallbackResult commandEvent(Command command) throws RobotCoreException {
  // called on RecvRunnable.run() thread

    CallbackResult result = CallbackResult.NOT_HANDLED;

    // check if it's in the cache to avoid duplicate executions
    for (Command c : commandRecvCache) {
      if (c != null && c.equals(command)) {
        // this command is in the cache, which means we've already handled it
        // no need to continue, just return now
        return CallbackResult.HANDLED;
      }
    }

    // cache the command
    commandRecvCache[(commandRecvCachePosition++) % commandRecvCache.length] = command;

    // process the command. We synchronize on the eventLoopLock so that we won't try to
    // start or stop the event loop while it's busy processing a command
    try {
      synchronized (eventLoopLock) {
        result = eventLoop.processCommand(command);
      }
    } catch (Exception e) {
      // we should catch everything, since we don't know what the event loop might throw
      RobotLog.ee(TAG, e, "Event loop threw an exception while processing a command");
    }

    return result;
  }

  @Override
  public CallbackResult emptyEvent(RobocolDatagram packet) {
    return CallbackResult.NOT_HANDLED;
    }

  public void buildAndSendTelemetry(String tag, String msg){
    TelemetryMessage telemetry = new TelemetryMessage();
    telemetry.setTag(tag);
    telemetry.addData(tag, msg);
    sendTelemetryData(telemetry);
  }
}
