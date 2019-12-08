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
package org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject;

import androidx.annotation.NonNull;

import org.firstinspires.ftc.robotcore.external.Consumer;
import org.firstinspires.ftc.robotcore.internal.system.NativeObject;

import java.io.IOException;

/**
 * A wrapper around an open video stream (see uvc_stream_handle)
 */
@SuppressWarnings("WeakerAccess")
public class UvcStreamHandle extends NativeObject<UvcDeviceHandle>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = UvcStreamHandle.class.getSimpleName();
    public String getTag() { return TAG; }

    protected UvcFrameCallback frameCallback = null;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcStreamHandle(long pointer, @NonNull UvcDeviceHandle uvcDeviceHandle)
        {
        super(pointer);
        setParent(uvcDeviceHandle);
        }

    @Override protected void destructor()
        {
        if (pointer != 0)
            {
            stopStreaming();
            nativeCloseStreamHandle(pointer);
            clearPointer();
            }
        releaseFrameCallback(); // was before if(pointer) logic; seems safer here
        super.destructor();
        }

    protected void releaseFrameCallback() // call with lock held
        {
        if (frameCallback != null)
            {
            frameCallback.releaseRef();
            frameCallback = null;
            }
        }

    public UvcContext getUvcContext()
        {
        return getParent().getUvcContext();
        }

    //----------------------------------------------------------------------------------------------
    // Streaming
    //----------------------------------------------------------------------------------------------

    public void startStreaming(@NonNull final Consumer<UvcFrame> callback) throws IOException
        {
        if (callback==null) throw new IllegalArgumentException("callback must not be null");

        boolean startedStreaming = false;
        synchronized (lock)
            {
            stopStreaming();
            frameCallback = new UvcFrameCallback(getUvcContext(), callback);
            startedStreaming = nativeStartStreaming(pointer, frameCallback.getCallbackPointer());
            }
        if (!startedStreaming) throw new IOException("unable to start streaming");
        }

    public boolean isStreaming()
        {
        return nativeIsStreaming(pointer);
        }

    public void stopStreaming()
        {
        synchronized (lock)
            {
            nativeStopStreaming(pointer);
            releaseFrameCallback();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Native
    //----------------------------------------------------------------------------------------------

    protected native static boolean nativeStartStreaming(long pointer, long callbackPointer);
    protected native static boolean nativeIsStreaming(long pointer);
    protected native static void nativeStopStreaming(long pointer);
    protected native static void nativeCloseStreamHandle(long pointer);
    }
