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
package com.qualcomm.robotcore.hardware.usb.serial;

import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.util.RobotLog;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link SerialPort} is a simple wrapper around some native code that give us access to serial ports.
 */
@SuppressWarnings("WeakerAccess")
public class SerialPort
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected static final String TAG = "SerialPort";

    protected File              file;
    protected FileDescriptor    fileDescriptor;
    protected FileInputStream   fileInputStream;
    protected FileOutputStream  fileOutputStream;
    protected int               baudRate;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public SerialPort(File file, int baudRate) throws IOException
        {
        this.file = file;
        ensureReadWriteable(file);

        this.baudRate = baudRate;
        this.fileDescriptor = open(file.getAbsolutePath(), baudRate, isDragonboard());
        if (this.fileDescriptor==null)
            throw new IOException(String.format("SerialPort.SerialPort: failed: path=%s", file.getAbsolutePath()));

        this.fileInputStream = new FileInputStream(this.fileDescriptor);
        this.fileOutputStream = new FileOutputStream(this.fileDescriptor);
        }

    @Override protected void finalize() throws Throwable
        {
        this.close();
        super.finalize();
        }

    public synchronized void close()
        {
        if (this.fileDescriptor != null)
            {
            close(this.fileDescriptor);
            this.fileDescriptor = null;
            }
        }

    private boolean isDragonboard()
        {
        return LynxConstants.getControlHubVersion() == LynxConstants.DRAGONBOARD_CH_VERSION;
        }

    /**
     * Attempts to ensure that the indicated file is read-write.
     * @param  file                 the file who's properties we're interested in
     * @throws SecurityException    if the file cannot be made read-write
     */
    protected static void ensureReadWriteable(File file) throws SecurityException
        {
        if (!file.canRead() || !file.canWrite())
            {
            RobotLog.vv(TAG, "making RW: %s", file.getAbsolutePath());
            try
                {
                // Setting up permissions correctly should be taken care of statically
                // inside of FTCAndroid itself.
                throw new RuntimeException("incorrect perms on " + file.getAbsolutePath());
                }
            catch (Exception e)
                {
                RobotLog.logStacktrace(e);
                throw new SecurityException(String.format("SerialPort.ensureReadWriteFile: exception: path=%s", file.getAbsolutePath()), e);
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public String getName()
        {
        return this.file.getAbsolutePath();
        }

    public InputStream getInputStream()
        {
        return this.fileInputStream;
        }

    public OutputStream getOutputStream()
        {
        return this.fileOutputStream;
        }

    public int getBaudRate()
        {
        return baudRate;
        }

    //----------------------------------------------------------------------------------------------
    // Native methods
    //----------------------------------------------------------------------------------------------

    private native static FileDescriptor open(String path, int baudrate, boolean isDragonboard);

    public native static void close(FileDescriptor fileDescriptor);

    static
        {
        System.loadLibrary("RobotCore");
        }
    }
