/*
Copyright (c) 2017 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.internal.usb.exception;

import android.hardware.usb.UsbDeviceConnection;

/**
 * {@link RobotUsbStuckUsbWriteException} is thrown when the USB layer close the low-level
 * FT_Device in attempt to recover from a greater system calamity but still is of the opinion that
 * reopening the device is worthy of attempting in order to try to get the system back to normal.
 */
@SuppressWarnings("WeakerAccess")
public class RobotUsbStuckUsbWriteException extends RobotUsbException
    {
    public UsbDeviceConnection device = null;

    public RobotUsbStuckUsbWriteException(UsbDeviceConnection device, String format, Object... args)
        {
        this(format, args);
        this.device = device;
        }

    public RobotUsbStuckUsbWriteException(String message)
        {
        super(message);
        }

    public RobotUsbStuckUsbWriteException(String format, Object... args)
        {
        super(String.format(format, args));
        }

    protected RobotUsbStuckUsbWriteException(String message, Throwable cause)
        {
        super(message, cause);
        }

    public static RobotUsbStuckUsbWriteException createChained(Exception e, String format, Object... args)
        {
        return new RobotUsbStuckUsbWriteException(String.format(format, args), e);
        }
    }
