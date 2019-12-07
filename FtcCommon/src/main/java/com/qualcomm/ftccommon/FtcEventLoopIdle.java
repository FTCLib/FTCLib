package com.qualcomm.ftccommon;

import android.app.Activity;
import android.hardware.usb.UsbDevice;

import com.qualcomm.hardware.HardwareFactory;
import com.qualcomm.robotcore.eventloop.EventLoopManager;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegister;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.usb.RobotUsbModule;
import com.qualcomm.robotcore.robocol.TelemetryMessage;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.opmode.OpModeManagerImpl;

import java.util.concurrent.TimeUnit;

/**
 * {@link FtcEventLoopIdle} is an eventloop that runs whenever a full {@link FtcEventLoop}
 * is inappropriate.
 *
 * This event loop should be robust in the face of errors and exceptions, staying up until
 * it's explicitly shut down.
 */
public class FtcEventLoopIdle extends FtcEventLoopBase
    {
    public static final String TAG = "FtcEventLoopIdle";

    public FtcEventLoopIdle(HardwareFactory hardwareFactory, OpModeRegister userOpmodeRegister, UpdateUI.Callback callback, Activity activityContext)
        {
        super(hardwareFactory, userOpmodeRegister, callback, activityContext);
        }

    @Override
    public void init(EventLoopManager eventLoopManager) throws RobotCoreException, InterruptedException
        {
        RobotLog.ii(TAG, "------- idle init --------");
        try {
            super.init(eventLoopManager);
            }
        catch (Exception e)
            {
            RobotLog.vv(TAG, e, "exception in idle event loop init; ignored");
            }
        }

    @Override public void loop() throws RobotCoreException, InterruptedException
        {
        try {
            checkForChangedOpModes();
            }
        catch (Exception e)
            {
            RobotLog.vv(TAG, e, "exception in idle event loop loop; ignored");
            }
        }

    @Override public void refreshUserTelemetry(TelemetryMessage telemetry, double sInterval)
        {
        }

    @Override public void teardown() throws RobotCoreException, InterruptedException
        {
        RobotLog.ii(TAG, "------- idle teardown ----");
        try {
            super.teardown();
            }
        catch (Exception e)
            {
            RobotLog.vv(TAG, e, "exception in idle event loop teardown; ignored");
            }
        }

    @Override public void pendUsbDeviceAttachment(SerialNumber serialNumber, long time, TimeUnit unit)
        {
        }

    @Override public void onUsbDeviceAttached(UsbDevice usbDevice)
        {
        }

    @Override
    public void processedRecentlyAttachedUsbDevices() throws RobotCoreException, InterruptedException
        {
        }

    @Override
    public void handleUsbModuleDetach(RobotUsbModule module) throws RobotCoreException, InterruptedException
        {
        }

    @Override
    public void handleUsbModuleAttach(RobotUsbModule module) throws RobotCoreException, InterruptedException
        {
        }

    @Override public OpModeManagerImpl getOpModeManager()
        {
        return null;
        }

    @Override public void requestOpModeStop(OpMode opModeToStopIfActive)
        {
        }
    }
