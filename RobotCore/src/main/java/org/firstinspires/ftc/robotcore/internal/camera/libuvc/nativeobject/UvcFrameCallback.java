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
import org.firstinspires.ftc.robotcore.internal.system.DestructOnFinalize;
import org.firstinspires.ftc.robotcore.internal.system.NativeObject;

/**
 * {@link UvcFrameCallback} is the object through which native code calls back into the Java
 * side of the world.
 */
@SuppressWarnings("WeakerAccess")
class UvcFrameCallback extends DestructOnFinalize
    {
    //------------------------------------------------------------------------------------------
    // Types
    //------------------------------------------------------------------------------------------

    protected static class UvcFrameCallbackData extends NativeObject
        {
        public UvcFrameCallbackData(UvcFrameCallback frameCallback)
            {
            super(allocate(frameCallback));
            }

        protected static long allocate(UvcFrameCallback frameCallback)
            {
            long pointer = nativeAllocCallbackState(frameCallback);
            if (0 == pointer) throw new IllegalStateException("unable to allocate streaming callback");
            return pointer;
            }

        public long getPointer()
            {
            return pointer;
            }

        @Override protected void destructor()
            {
            if (pointer != 0)
                {
                nativeReleaseCallbackState(pointer);
                clearPointer();
                }
            super.destructor();
            }
        }

    //------------------------------------------------------------------------------------------
    // State
    //------------------------------------------------------------------------------------------

    protected UvcContext uvcContext;
    protected UvcFrameCallbackData callbackData;
    protected final @NonNull Consumer<UvcFrame> callback;

    //------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------

    public UvcFrameCallback(@NonNull UvcContext uvcContext, @NonNull Consumer<UvcFrame> callback)
        {
        this.callback = callback;
        this.callbackData = new UvcFrameCallbackData(this);
        this.uvcContext = uvcContext;
        this.uvcContext.addRef();
        }

    @Override protected void destructor()
        {
        if (callbackData != null)
            {
            callbackData.releaseRef();
            callbackData = null;
            }
        if (uvcContext != null)
            {
            uvcContext.releaseRef();
            uvcContext = null;
            }
        super.destructor();
        }

    //------------------------------------------------------------------------------------------
    // Operations
    //------------------------------------------------------------------------------------------

    public long getCallbackPointer()
        {
        return callbackData.getPointer();
        }

    // NB: the name and signature of this method is known to native code
    public void onFrame(long framePointer)
        {
        // We're running here on a UVC worker thread.
        this.addRef();  // keep us alive while we call the user
        try
            {
            // Note: caller cannot hold on to the supplied UvcFrame beyond the
            // duration of the callback. If this is desired, then make a copy!
            UvcCallbackFrame frame = new UvcCallbackFrame(framePointer, uvcContext);
            try
                {
                callback.accept(frame);
                }
            finally
                {
                frame.callbackComplete();
                frame.releaseRef();
                }
            }
        finally
            {
            this.releaseRef();
            }
        }

    protected native static long nativeAllocCallbackState(UvcFrameCallback frameCallback);
    protected native static void nativeReleaseCallbackState(long pointer);
    }
