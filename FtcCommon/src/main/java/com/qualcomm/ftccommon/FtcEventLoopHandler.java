/* Copyright (c) 2015 Qualcomm Technologies Inc

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

import android.content.Context;
import android.support.annotation.Nullable;

import com.qualcomm.ftccommon.configuration.USBScanManager;
import com.qualcomm.hardware.HardwareFactory;
import com.qualcomm.hardware.lynx.LynxUsbDevice;
import com.qualcomm.hardware.lynx.LynxUsbDeviceImpl;
import com.qualcomm.robotcore.eventloop.EventLoopManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareDeviceCloseOnTearDown;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.ScannedDevices;
import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationUtility;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.hardware.usb.RobotArmingStateNotifier;
import com.qualcomm.robotcore.robocol.TelemetryMessage;
import com.qualcomm.robotcore.robot.RobotState;
import com.qualcomm.robotcore.util.BatteryChecker;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.MovingStatistics;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.external.function.Supplier;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class FtcEventLoopHandler implements BatteryChecker.BatteryWatcher {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  public static final String TAG = "FtcEventLoopHandler";

  /** This string is sent in the robot battery telemetry payload to identify
   *  that no voltage sensor is available on the robot. */
  public static final String NO_VOLTAGE_SENSOR = "$no$voltage$sensor$";

  protected static final boolean DEBUG = false;

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  protected final UpdateUI.Callback callback;
  protected final HardwareFactory   hardwareFactory;
  protected final Context           robotControllerContext;

  protected EventLoopManager  eventLoopManager;

  protected BatteryChecker    robotControllerBatteryChecker;
  protected double            robotControllerBatteryCheckerInterval = 180.0; // in seconds

  protected ElapsedTime       robotBatteryTimer         = new ElapsedTime();
  protected double            robotBatteryInterval      = 3.00; // in seconds
  protected MovingStatistics  robotBatteryStatistics    = new MovingStatistics(10);
  protected ElapsedTime       robotBatteryLoggingTimer  = null;
  protected double            robotBatteryLoggingInterval = robotControllerBatteryCheckerInterval;

  protected ElapsedTime       userTelemetryTimer        = new ElapsedTime(0); // 0 so we get an initial report
  protected double            userTelemetryInterval     = 0.250; // in seconds
  protected final Object      refreshUserTelemetryLock  = new Object();

  protected ElapsedTime       updateUITimer             = new ElapsedTime();
  protected double            updateUIInterval          = 0.250; // in seconds

  /** the actual hardware map seen by the user */
  protected HardwareMap       hardwareMap               = null;
  /** the hardware map in which we keep any extra devices (ones not used by the user) we need to instantiate */
  protected HardwareMap       hardwareMapExtra          = null;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public FtcEventLoopHandler(HardwareFactory hardwareFactory, UpdateUI.Callback callback, Context robotControllerContext) {
    this.hardwareFactory        = hardwareFactory;
    this.callback               = callback;
    this.robotControllerContext = robotControllerContext;

    long milliseconds = (long)(robotControllerBatteryCheckerInterval * 1000); //milliseconds
    robotControllerBatteryChecker = new BatteryChecker(this, milliseconds);
    if (DEBUG) robotBatteryLoggingTimer = new ElapsedTime(0);
  }

  //------------------------------------------------------------------------------------------------
  // Loop life cycle
  //------------------------------------------------------------------------------------------------

  public void init(EventLoopManager eventLoopManager) {
    this.eventLoopManager = eventLoopManager;
    robotControllerBatteryChecker.startBatteryMonitoring();
  }

  public void close() {

    // shutdown everything we have open
    closeHardwareMap(hardwareMap);
    closeHardwareMap(hardwareMapExtra);

    // Stop the battery monitoring so we don't send stale telemetry
    closeBatteryMonitoring();

    // Paranoia: shut down interactions for absolute certain
    eventLoopManager = null;
  }

  protected static void closeHardwareMap(HardwareMap hardwareMap) {

    // Close motor and servo controllers first, since some of them may reside on top
    // of legacy modules: closing first just keeps things more graceful
    closeMotorControllers(hardwareMap);
    closeServoControllers(hardwareMap);

    // Now close everything that's USB-connected (yes that might re-close a motor or servo
    // controller, but that's ok
    closeAutoCloseOnTeardown(hardwareMap);
  }


  //------------------------------------------------------------------------------------------------
  // Accessing
  //------------------------------------------------------------------------------------------------

  public EventLoopManager getEventLoopManager() {
    return eventLoopManager;
  }

  public HardwareMap getHardwareMap() throws RobotCoreException, InterruptedException {
    synchronized (hardwareFactory) {
      if (hardwareMap==null) {

        // Create a newly-active hardware map
        hardwareMap = hardwareFactory.createHardwareMap(eventLoopManager);
        hardwareMapExtra = new HardwareMap(robotControllerContext);
      }
      return hardwareMap;
    }
  }

  public List<LynxUsbDeviceImpl> getExtantLynxDeviceImpls() {
    synchronized (hardwareFactory) {
      List<LynxUsbDeviceImpl> result = new ArrayList<LynxUsbDeviceImpl>();
      if (hardwareMap != null) {
        for (LynxUsbDevice lynxUsbDevice : hardwareMap.getAll(LynxUsbDevice.class)) {
          if (lynxUsbDevice.getArmingState()==RobotArmingStateNotifier.ARMINGSTATE.ARMED) {
            result.add(lynxUsbDevice.getDelegationTarget());
          }
        }
      }
      if (hardwareMapExtra != null) {
        for (LynxUsbDevice lynxUsbDevice : hardwareMapExtra.getAll(LynxUsbDevice.class)) {
          if (lynxUsbDevice.getArmingState()==RobotArmingStateNotifier.ARMINGSTATE.ARMED) {
            result.add(lynxUsbDevice.getDelegationTarget());
          }
        }
      }
      return result;
    }
  }

  /**
   * Returns the device whose serial number is the one indicated, from the hardware map if possible
   * but instantiating / opening it if necessary. null is returned if the object cannot be
   * accessed.
   *
   * @param classOrInterface        the interface to retrieve on the returned object
   * @param serialNumber            the serial number of the object to retrieve
   * @param usbScanManagerSupplier  how to get a {@link USBScanManager} if it ends up we need one
   */
  public @Nullable <T> T getHardwareDevice(Class<? extends T> classOrInterface, final SerialNumber serialNumber, Supplier<USBScanManager> usbScanManagerSupplier) {
    synchronized (hardwareFactory) {
      RobotLog.vv(TAG, "getHardwareDevice(%s)...", serialNumber);
      try {
        getHardwareMap();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null;
      } catch (RobotCoreException e) {
        return null;
      }

      Object oResult = hardwareMap.get(Object.class, serialNumber);

      if (oResult == null) {
        oResult = hardwareMapExtra.get(Object.class, serialNumber);
      }

      if (oResult == null) {
        /** the device isn't in the hwmap. but is it actually attached? */
        /** first, check for it's scannable */
        final SerialNumber scannableSerialNumber = serialNumber.getScannableDeviceSerialNumber();

        boolean tryScannable = true;
        if (!scannableSerialNumber.equals(serialNumber)) { // already did that check
          if (hardwareMap.get(Object.class, scannableSerialNumber) != null || hardwareMapExtra.get(Object.class, scannableSerialNumber) != null) {
            RobotLog.ee(TAG, "internal error: %s absent but scannable %s present", serialNumber, scannableSerialNumber);
            tryScannable = false;
          }
        }

        if (tryScannable) {
          final USBScanManager usbScanManager = usbScanManagerSupplier.get();
          if (usbScanManager != null) {
            try {
              ScannedDevices scannedDevices = usbScanManager.awaitScannedDevices();
              if (scannedDevices.containsKey(scannableSerialNumber)) {
                /** yes, it's there. build a new configuration for it */
                ConfigurationUtility configurationUtility = new ConfigurationUtility();
                ControllerConfiguration controllerConfiguration = configurationUtility.buildNewControllerConfiguration(scannableSerialNumber, scannedDevices.get(scannableSerialNumber), usbScanManager.getLynxModuleMetaListSupplier(scannableSerialNumber));
                if (controllerConfiguration != null) {
                  controllerConfiguration.setEnabled(true);
                  controllerConfiguration.setKnownToBeAttached(true);
                  /** get access to the actual device */
                  hardwareFactory.instantiateConfiguration(hardwareMapExtra, controllerConfiguration, eventLoopManager);
                  oResult = hardwareMapExtra.get(Object.class, serialNumber);
                  RobotLog.ii(TAG, "found %s: hardwareMapExtra:", serialNumber);
                  hardwareMapExtra.logDevices();
                } else {
                  RobotLog.ee(TAG, "buildNewControllerConfiguration(%s) failed", scannableSerialNumber);
                }
              } else {
                RobotLog.ee(TAG, "");
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (RobotCoreException e) {
              RobotLog.ee(TAG, e, "exception in getHardwareDevice(%s)", serialNumber);
            }
          } else {
            RobotLog.ee(TAG, "usbScanManager supplied as null");
          }
        }
      }

      T result = null;
      if (oResult != null && classOrInterface.isInstance(oResult)) {
        result = classOrInterface.cast(oResult);
      }

      RobotLog.vv(TAG, "...getHardwareDevice(%s)=%s,%s", serialNumber, oResult, result);
      return result;
    }
  }

  //------------------------------------------------------------------------------------------------
  // Operations
  //------------------------------------------------------------------------------------------------

  public void displayGamePadInfo(String activeOpModeName) {
    if (updateUITimer.time() > updateUIInterval) {
      updateUITimer.reset();

      // Get access to gamepad 1 and 2
      Gamepad gamepads[] = getGamepads();
      callback.updateUi(activeOpModeName, gamepads);
    }
  }

  public Gamepad[] getGamepads() {
    return eventLoopManager != null
            ? eventLoopManager.getGamepads()
            : new Gamepad[2];
  }

  /**
   * Updates the (indicated) user's telemetry: the telemetry is transmitted if a sufficient
   * interval has passed since the last transmission. If the telemetry is transmitted, the
   * telemetry is cleared and the timer is reset. A battery voltage key may be added to the
   * message before transmission.
   *
   * @param telemetry         the telemetry data to send
   * @param requestedInterval the minimum interval (s) since the last transmission. NaN indicates
   *                          that a default transmission interval should be used
   *
   * @see com.qualcomm.robotcore.eventloop.EventLoop#TELEMETRY_DEFAULT_INTERVAL
   */
  public void refreshUserTelemetry(TelemetryMessage telemetry, double requestedInterval) {
    synchronized (this.refreshUserTelemetryLock) {

      // NaN is an indicator to use the default interval, whereas zero will
      // cause immediate transmission.
      if (Double.isNaN(requestedInterval))
        requestedInterval = userTelemetryInterval;

      // We'll do a transmission just to see the user's new data if a sufficient interval
      // has elapsed since the last time we did.
      boolean transmitBecauseOfUser = userTelemetryTimer.seconds() >= requestedInterval;

      // The modern and legacy motor controllers have *radically* different read times for the battery
      // voltage. For the modern controller, since the ReadWriteRunnable constantly polls this state,
      // the read is always out of data already in cache, and takes about 30 microseconds (as measured).
      // The legacy motor controller, on the other hand, because of the modality of the underlying
      // legacy module, doesn't always automatically read this data. Indeed, if the user is doing mostly
      // writes (as is often the case in OpModes that basically just set the motor power), the legacy
      // module won't be switch to read mode ever, *except* for a voltage request here, and that switch
      // will take tens of milliseconds. To *always* take that timing hit when refreshing user telemetry
      // is unreasonable.
      //
      // Instead, we adopt an adaptive strategy. We keep track of the battery read time statistics
      // and if they're small enough, then we transmit battery data whenever the user's going to
      // send data OR when a sufficiently long interval has elapsed. If the battery read times are
      // too large, then we only do the latter.

      double msThreshold = 2;
      boolean transmitBecauseOfBattery = (robotBatteryTimer.seconds() >= robotBatteryInterval)
              || (transmitBecauseOfUser && robotBatteryStatistics.getMean() < msThreshold);

      if (transmitBecauseOfUser || transmitBecauseOfBattery) {

        if (transmitBecauseOfUser) {
          userTelemetryTimer.reset();
        }

        if (transmitBecauseOfBattery) {
          telemetry.addData(EventLoopManager.ROBOT_BATTERY_LEVEL_KEY, buildRobotBatteryMsg());
          robotBatteryTimer.reset();
          if ((DEBUG) && (robotBatteryLoggingTimer.seconds() > robotBatteryLoggingInterval)) {
            RobotLog.i("robot battery read duration: n=%d, mean=%.3fms sd=%.3fms", robotBatteryStatistics.getCount(), robotBatteryStatistics.getMean(), robotBatteryStatistics.getStandardDeviation());
            robotBatteryLoggingTimer.reset();
          }
        }

        // Send if there's anything to send. If we send, then we always clear, as the current
        // data has already been send.
        if (telemetry.hasData()) {
          if (eventLoopManager!=null) {
            eventLoopManager.sendTelemetryData(telemetry);
          }
          telemetry.clearData();
        }
      }
    }
  }

  /**
   * Send robot phone power % and robot battery voltage level to Driver station
   */
  public void sendBatteryInfo() {
    robotControllerBatteryChecker.pollBatteryLevel(this);
    String batteryMessage = buildRobotBatteryMsg();
    if (batteryMessage != null) {
      sendTelemetry(EventLoopManager.ROBOT_BATTERY_LEVEL_KEY, batteryMessage);
    }
  }

  /**
   * Build a string which indicates the lowest measured system voltage
   * @return String representing battery voltage
   */
  private String buildRobotBatteryMsg() {

    // Don't do anything if we're really early in the construction cycle
    if (this.hardwareMap==null) return null;

    double minBatteryLevel = Double.POSITIVE_INFINITY;

    // Determine the lowest battery voltage read from all motor controllers.
    //
    // If a voltage sensor becomes disconnected, it has been observed to read as zero.
    // Thus, we must account for that eventuality. While doing so, it's convenient for us
    // to rule out (other) unreasonable voltage levels in order to facilitate later string
    // conversion.
    //
    for (VoltageSensor sensor : this.hardwareMap.voltageSensor) {

      // Read the voltage, keeping track of how long it takes to do so
      long nanoBefore = System.nanoTime();
      double sensorVoltage = sensor.getVoltage();
      long nanoAfter = System.nanoTime();

      if (sensorVoltage >= 1.0 /* an unreasonable value to ever see in practice */) {
        // For valid reads, we add the read-duration to our statistics, in ms.
        robotBatteryStatistics.add((nanoAfter - nanoBefore) / (double) ElapsedTime.MILLIS_IN_NANO);

        // Keep track of the minimum valid value we find
        if (sensorVoltage < minBatteryLevel) {
          minBatteryLevel = sensorVoltage;
        }
      }
    }

    String msg;

    if (minBatteryLevel == Double.POSITIVE_INFINITY) {
      msg = NO_VOLTAGE_SENSOR;

    } else {
      // Convert double voltage into string with *two* decimal places (fast), given the
      // above-maintained fact the voltage is at least 1.0.
      msg = Integer.toString((int)(minBatteryLevel * 100));
      msg = new StringBuilder(msg).insert(msg.length()-2, ".").toString();
    }

    return (msg);
  }

  public void sendTelemetry(String tag, String msg) {
    TelemetryMessage telemetry = new TelemetryMessage();
    telemetry.setTag(tag);
    telemetry.addData(tag, msg);
    if (eventLoopManager != null) {
      eventLoopManager.sendTelemetryData(telemetry);
    } else {
      RobotLog.vv(TAG, "sendTelemetry() with null EventLoopManager; ignored");
    }
    telemetry.clearData();
  }

  protected static void closeMotorControllers(HardwareMap hardwareMap) {
    if (hardwareMap != null) {
      for (DcMotorController controller : hardwareMap.getAll(DcMotorController.class)) {
        controller.close();
      }
    }
  }

  protected static void closeServoControllers(HardwareMap hardwareMap)  {
    if (hardwareMap != null) {
      for (ServoController controller : hardwareMap.getAll(ServoController.class)) {
        controller.close();
      }
    }
  }

  protected static void closeAutoCloseOnTeardown(HardwareMap hardwareMap) {
    if (hardwareMap != null) {
      for (HardwareDeviceCloseOnTearDown device : hardwareMap.getAll(HardwareDeviceCloseOnTearDown.class)) {
        device.close();
      }
    }
  }

  protected void closeBatteryMonitoring() {
    robotControllerBatteryChecker.close();
  }

  public void restartRobot() {
    RobotLog.dd(TAG, "restarting robot...");
    closeBatteryMonitoring();   // probably not needed now that close() above does this too. but harmless here if so
    callback.restartRobot();
  }

  public String getOpMode(String extra) {
    if (eventLoopManager == null || eventLoopManager.state != RobotState.RUNNING) {
      return OpModeManager.DEFAULT_OP_MODE_NAME;
    }
    return extra;
  }

  public void updateBatteryStatus(BatteryChecker.BatteryStatus status) {
    sendTelemetry(EventLoopManager.RC_BATTERY_STATUS_KEY, status.serialize());
  }

}
