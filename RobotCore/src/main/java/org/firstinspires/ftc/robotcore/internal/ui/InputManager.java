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
package org.firstinspires.ftc.robotcore.internal.ui;

import android.content.Context;
import android.os.Handler;
import android.view.InputDevice;
import android.view.InputEvent;

import com.qualcomm.robotcore.util.ClassUtil;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.lang.reflect.Method;

/**
 * {@link InputManager} provides additional access to {@link android.hardware.input.InputManager}
 * by unhiding some of its methods.
 */
@SuppressWarnings("WeakerAccess")
public class InputManager
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2;

    protected static InputManager theInstance = new InputManager();
    public static InputManager getInstance() { return theInstance; }

    protected android.hardware.input.InputManager   inputManager;
    protected Method                                methodInjectInputEvent;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected InputManager()
        {
        inputManager = (android.hardware.input.InputManager) AppUtil.getInstance().getActivity().getSystemService(Context.INPUT_SERVICE);
        try {
            methodInjectInputEvent = android.hardware.input.InputManager.class.getMethod("injectInputEvent", InputEvent.class, int.class);
            }
        catch (NoSuchMethodException ignored)
            {
            }
        }

    //----------------------------------------------------------------------------------------------
    // Access
    //----------------------------------------------------------------------------------------------

    public boolean injectInputEvent(InputEvent event, int mode)
        {
        return (boolean) ClassUtil.invoke(inputManager, methodInjectInputEvent, event, mode);
        }

    public InputDevice getInputDevice(int id)
        {
        return inputManager.getInputDevice(id);
        }

    public int[] getInputDeviceIds()
        {
        return inputManager.getInputDeviceIds();
        }

    public void registerInputDeviceListener(android.hardware.input.InputManager.InputDeviceListener listener, Handler handler)
        {
        inputManager.registerInputDeviceListener(listener, handler);
        }

    public void unregisterInputDeviceListener(android.hardware.input.InputManager.InputDeviceListener listener)
        {
        inputManager.unregisterInputDeviceListener(listener);
        }
    }
