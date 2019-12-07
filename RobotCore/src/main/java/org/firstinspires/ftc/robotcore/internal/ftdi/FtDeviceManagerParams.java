/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Derived in part from information in various resources, including FTDI, the
Android Linux implementation, FreeBsc, UsbSerial, and others.

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
package org.firstinspires.ftc.robotcore.internal.ftdi;

/**
 * Parameters with which {@link FtDeviceManager} is configured
 */
@SuppressWarnings("WeakerAccess")
public class FtDeviceManagerParams
    {
    public static final String TAG = "FtDeviceManagerParams";

    private int cbReadBufferMax         = 16384;
    private int packetBufferCacheSize   = 16;
    private int msBulkInReadTimeout     = 5000;
    private int retainedBufferCapacity  = 5;    // pretty arbitrary
    private boolean debugRetainBuffers  = true; // could reconsider, but is cheap and usefl

    public FtDeviceManagerParams()
        {
        }

    public void setMaxReadBufferSize(int cbReadBufferMax)
        {
        this.cbReadBufferMax = cbReadBufferMax;
        }

    public int getMaxReadBufferSize()
        {
        return this.cbReadBufferMax;
        }

    public void setPacketBufferCacheSize(int bufferCount)
        {
        this.packetBufferCacheSize = bufferCount;
        }

    public int getPacketBufferCacheSize()
        {
        return this.packetBufferCacheSize;
        }

    public void setBuildInReadTimeout(int msTimeout)
        {
        this.msBulkInReadTimeout = msTimeout;
        }

    public int getBulkInReadTimeout()
        {
        return this.msBulkInReadTimeout;
        }

    public int getRetainedBufferCapacity()
        {
        return retainedBufferCapacity;
        }

    public void setRetainedBufferCapacity(int retainedBufferCapacity)
        {
        this.retainedBufferCapacity = retainedBufferCapacity;
        }

    public boolean isDebugRetainBuffers()
        {
        return debugRetainBuffers;
        }

    public void setDebugRetainBuffers(boolean debugRetainBuffers)
        {
        this.debugRetainBuffers = debugRetainBuffers;
        }
    }
