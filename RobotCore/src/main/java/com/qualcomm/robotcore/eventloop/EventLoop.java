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

import android.hardware.usb.UsbDevice;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.usb.RobotUsbModule;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.TelemetryMessage;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeManagerImpl;

import java.util.concurrent.TimeUnit;

/**
 * Event loop interface
 * <p>
 * Event loops need to implement this interface. Contains methods for managing the life cycle of your robot.
 */
public interface EventLoop {

  /**
   * Init method, this will be called before the first call to loop. You should set up
   * your hardware in this method.
   *
   * Threading: called on the RobotSetupRunnable.run() thread, before the EventLoopRunnable.run()
   * thread is created.
   *
   * @param eventLoopManager event loop manager that is responsible for this event loop
   *
   * @throws RobotCoreException if a RobotCoreException is thrown, it will be handled
   *         by the event loop manager. The manager will report that the robot failed
   *         to start.
   */
  void init(EventLoopManager eventLoopManager) throws RobotCoreException, InterruptedException;

  /**
   * This method will be repeatedly called by the event loop manager.
   *
   * Threading: called on the EventLoopRunnable.run() thread.
   *
   * @throws RobotCoreException if a RobotCoreException is thrown, it will be handled
   *         by the event loop manager. The manager may decide to either stop processing
   *         this iteration of the loop, or it may decide to shut down the robot.
   */
  void loop() throws RobotCoreException, InterruptedException;

  /**
   * Update's the user portion of the driver station screen with the contents of the telemetry object
   * here provided if a sufficiently long duration has passed since the last update. 
   * @param telemetry the telemetry object to send
   * @param sInterval the required minimum interval. NaN indicates that a system default interval should be used.
   */
  void refreshUserTelemetry(TelemetryMessage telemetry, double sInterval);

  /**
   * The value to pass to {@link #refreshUserTelemetry(TelemetryMessage, double)} as the time interval
   * parameter in order to cause a system default interval to be used.
   */
  double TELEMETRY_DEFAULT_INTERVAL = Double.NaN;

  /**
   * Teardown method, this will be called after the last call to loop. You should place your robot
   * into a safe state before this method exits, since there will be no more changes to communicate
   * with your robot.
   *
   * Threading: called on the EventLoopRunnable.run() thread.
   *
   * @throws RobotCoreException if a RobotCoreException is thrown, it will be handled by the event
   *         loop manager. The manager will then attempt to shut down the robot without the benefit
   *         of the teardown method.
   */
  void teardown() throws RobotCoreException, InterruptedException;

  /**
   * Notifies the event loop that a UsbDevice has just been attached to the system. User interface
   * activities that receive UsbManager.ACTION_USB_DEVICE_ATTACHED notifications should retrieve
   * the UsbDevice using intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) then pass that along
   * to this method in their event loop for processing.
   * <p>
   * Implementations of this method should avoid doing significant processing during this notification.
   * Rather, they should squirrel the device away for processing during a later processedRecentlyAttachedUsbDevices
   * call.
   * </p>
   *
   * @param usbDevice the newly arrived device
   * @see #processedRecentlyAttachedUsbDevices()
   */
  void onUsbDeviceAttached(UsbDevice usbDevice);

  void pendUsbDeviceAttachment(SerialNumber serialNumber, long time, TimeUnit unit);

  /**
   * Process the batch of newly arrived USB devices. This is called on the EventLoop thread by
   * the EventLoopManager; there is sufficient time and correct context to, e.g., fully arm the
   * associated module software to make the device operational within the app.
   *
   * @throws RobotCoreException
   * @throws InterruptedException
   * @see #handleUsbModuleDetach(RobotUsbModule)
   * @see #onUsbDeviceAttached(UsbDevice)
   */
  void processedRecentlyAttachedUsbDevices() throws RobotCoreException, InterruptedException;

  /**
   * Process the fact that a usb module has now become detached from the system. This is called
   * on the EventLoop thread by the EventLoopManager; there is sufficient time and correct context
   * to, e.g., fully disarm and 'pretend' the associated module.
   *
   * @param module
   * @throws RobotCoreException
   * @throws InterruptedException
   * @see #processedRecentlyAttachedUsbDevices()
   */
  void handleUsbModuleDetach(RobotUsbModule module) throws RobotCoreException, InterruptedException;

  /**
   * Process the fact that (we believe) that the indicated module has now reappeared after a
   * previously observed detachment.
   *
   * @param module
   * @throws RobotCoreException
   * @throws InterruptedException
   */
  void handleUsbModuleAttach(RobotUsbModule module) throws RobotCoreException, InterruptedException;

  /**
   * Process command method, this will be called if the event loop manager receives a user defined
   * command. How this command is handled is up to the event loop implementation.
   *
   * Threading: called on the RecvRunnable.run() thread.
   *
   * @param command command to process
   */
  CallbackResult processCommand(Command command) throws InterruptedException, RobotCoreException;

  /**
   * Returns the OpModeManager associated with this event loop
   * @return the OpModeManager associated with this event loop
   */
  OpModeManagerImpl getOpModeManager();

  /**
   * Requests that an OpMode be stopped if it's the currently active one
   * @param opModeToStopIfActive the OpMode to stop if it's currently active
   */
  void requestOpModeStop(OpMode opModeToStopIfActive);
}
