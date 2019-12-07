/* Copyright (c) 2014 Qualcomm Technologies Inc

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

package com.qualcomm.robotcore.eventloop.opmode;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.robocol.TelemetryMessage;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryImpl;
import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryInternal;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeServices;

import java.util.concurrent.TimeUnit;

/**
 * Base class for user defined operation modes (op modes).
 */
public abstract class OpMode {

  /**
   * Gamepad 1
   */
  public Gamepad gamepad1 = null;   // will be set in OpModeManager.runActiveOpMode

  /**
   * Gamepad 2
   */
  public Gamepad gamepad2 = null;   // will be set in OpModeManager.runActiveOpMode

  /**
   * The {@link #telemetry} field contains an object in which a user may accumulate data which
   * is to be transmitted to the driver station. This data is automatically transmitted to the
   * driver station on a regular, periodic basis.
   */
  public Telemetry telemetry = new TelemetryImpl(this);

  /**
   * Hardware Mappings
   */
  public HardwareMap hardwareMap = null; // will be set in OpModeManager.runActiveOpMode

  /**
   * number of seconds this op mode has been running, this is
   * updated before every call to loop.
   */
  public double time = 0.0;

  // internal time tracking
  private long startTime = 0; // in nanoseconds

  /**
   * OpMode constructor
   * <p>
   * The op mode name should be unique. It will be the name displayed on the driver station. If
   * multiple op modes have the same name, only one will be available.
   */
  public OpMode() {
    startTime = System.nanoTime();
  }

  /**
   * User defined init method
   * <p>
   * This method will be called once when the INIT button is pressed.
   */
  abstract public void init();

  /**
   * User defined init_loop method
   * <p>
   * This method will be called repeatedly when the INIT button is pressed.
   * This method is optional. By default this method takes no action.
   */
  public void init_loop() {};

  /**
   * User defined start method.
   * <p>
   * This method will be called once when the PLAY button is first pressed.
   * This method is optional. By default this method takes not action.
   * Example usage: Starting another thread.
   *
   */
  public void start() {};

  /**
   * User defined loop method
   * <p>
   * This method will be called repeatedly in a loop while this op mode is running
   */
  abstract public void loop();

  /**
   * User defined stop method
   * <p>
   * This method will be called when this op mode is first disabled
   *
   * The stop method is optional. By default this method takes no action.
   */
  public void stop() {};

  /**
   * Requests that this OpMode be shut down if it the currently active opMode, much as if the stop
   * button had been pressed on the driver station; if this is not the currently active OpMode,
   * then this function has no effect. Note as part of this processing, the OpMode's {@link #stop()}
   * method will be called, as that is part of the usual shutdown logic. Note that {@link #requestOpModeStop()}
   * may be called from <em>any</em> thread.
   *
   * @see #stop()
   */
  public final void requestOpModeStop() {
    this.internalOpModeServices.requestOpModeStop(this);
  }

  /**
   * Get the number of seconds this op mode has been running
   * <p>
   * This method has sub millisecond accuracy.
   * @return number of seconds this op mode has been running
   */
  public double getRuntime() {
    final double NANOSECONDS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);
    return (System.nanoTime() - startTime) / NANOSECONDS_PER_SECOND;
  }

  /**
   * Reset the start time to zero.
   */
  public void resetStartTime() {
    startTime = System.nanoTime();
  }

  //----------------------------------------------------------------------------------------------
  // Telemetry management
  //----------------------------------------------------------------------------------------------

  /**
   * Refreshes the user's telemetry on the driver station with the contents of the provided telemetry
   * object if a nominal amount of time has passed since the last telemetry transmission. Once
   * transmitted, the contents of the telemetry object are (by default) cleared.
   *
   * @param telemetry the telemetry data to transmit
   * @see #telemetry
   * @see Telemetry#update()
   */
  public void updateTelemetry(Telemetry telemetry) {
    telemetry.update();
  }

  //----------------------------------------------------------------------------------------------
  // Safety Management
  //
  // These constants manage the duration we allow for callbacks to user code to run for before
  // such code is considered to be stuck (in an infinite loop, or wherever) and consequently
  // the robot controller application is restarted. They SHOULD NOT be modified except as absolutely
  // necessary as poorly chosen values might inadvertently compromise safety.
  //----------------------------------------------------------------------------------------------

  public int msStuckDetectInit     = 5000;
  public int msStuckDetectInitLoop = 5000;
  public int msStuckDetectStart    = 5000;
  public int msStuckDetectLoop     = 5000;
  public int msStuckDetectStop     = 1000;

  //----------------------------------------------------------------------------------------------
  // Internal
  //----------------------------------------------------------------------------------------------

  public void internalPreInit() {
    // Reset telemetry in case opmode instance gets reused from run to run
    if (telemetry instanceof TelemetryInternal) {
      ((TelemetryInternal)telemetry).resetTelemetryForOpMode();
    }
  }

  /** automatically update telemetry in a non-linear opmode */
  public void internalPostInitLoop() {
    telemetry.update();
    }

  /** automatically update telemetry in a non-linear opmode */
  public void internalPostLoop() {
    telemetry.update();
    }

  /** this is logically an internal field. DO NOT USE */
  public OpModeServices internalOpModeServices = null;

  /**
   * This is an internal SDK method, not intended for use by user opmodes.
   *
   * @param telemetry the telemetry data to transmit
   * @see #telemetry
   * @see Telemetry#update()
   */
  public final void internalUpdateTelemetryNow(TelemetryMessage telemetry) {
    this.internalOpModeServices.refreshUserTelemetry(telemetry, 0);
  }

}
