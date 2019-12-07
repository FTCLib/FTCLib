/*
Copyright (c) 2016 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.qualcomm.hardware.lynx;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.hardware.usb.RobotUsbManager;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;

/**
 * Created by bob on 2016-03-16.
 */
@SuppressWarnings("WeakerAccess")
public class LynxUsbUtil
    {
    public static RobotUsbDevice openUsbDevice(boolean doScan, RobotUsbManager robotUsbManager, SerialNumber serialNumber) throws RobotCoreException
        {
        if (doScan)
            {
            robotUsbManager.scanForDevices();
            }

        RobotUsbDevice result = null;
        try
            {
            result = robotUsbManager.openBySerialNumber(serialNumber);
            }
        catch (RobotCoreException e)
            {
            logMessageAndThrow("unable to open lynx USB device " + serialNumber + ": " + e.getMessage());
            }

        try
            {
            // Set BAUD rate for USB comm.
            result.setBaudRate(LynxConstants.USB_BAUD_RATE);
            result.setDataCharacteristics((byte) 8, (byte) 0, (byte) 0);

            // We make the latency timer as small as possible in order to minimize the
            // latency of reception of data: virtually *all* our packets have less than 62 bytes
            // of our payload, so this can be significant. The difference between 1ms and 2ms (which
            // is what the Modern Robotics USB uses as of current writing) has been observed with
            // a USB packet sniffer to at times be on the order of 5-10ms additional latency.
            result.setLatencyTimer(LynxConstants.LATENCY_TIMER);
            }
        catch (RobotUsbException e)
            {
            result.close();
            logMessageAndThrow("Unable to open lynx USB device " + serialNumber + " - " + result.getProductName() + ": " + e.getMessage());
            }

        return result;
        }

    private static void logMessageAndThrow(String message) throws RobotCoreException
        {
        System.err.println(message);
        throw new RobotCoreException(message);
        }

    /**
     * Documents that the value being passed is a dummy, placeholder value which
     * is being returned from a function in lieu of something more reasonable actually
     * being available.
     */
    public static <T> T makePlaceholderValue(T t)
        {
        return t;
        }

    /** A simple utility that helps us understand when we're using placeholders. We don't
     * log all the time for fear of swamping the log */
    public static class Placeholder<T>
        {
        private String tag;
        private String message;
        private boolean logged;
        public Placeholder(String tag, String format, Object... args)
            {
            this.tag = tag;
            this.message = String.format("placeholder: %s", String.format(format, args));
            this.logged = false;
            }
        public synchronized void reset()
            {
            this.logged = false;
            }
        public synchronized T log(T t)
            {
            if (!logged)
                {
                RobotLog.ee(tag, message);
                logged = true;
                }
            return t;
            }

        }

    }
