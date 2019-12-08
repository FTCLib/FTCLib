/*
Copyright (c) 2018 Noah Andrews

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Noah Andrews nor the names of his contributors may be used to
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
package com.qualcomm.robotcore.hardware.configuration.typecontainers;

import androidx.annotation.Nullable;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.configuration.ConstructorPrototype;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * {@link DigitalIoDeviceConfigurationType} contains the meta-data for a user-defined digital device driver.
 */
public final class DigitalIoDeviceConfigurationType extends InstantiableUserConfigurationType {

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------
    private static final ConstructorPrototype ctorDigitalDevice = new ConstructorPrototype(DigitalChannelController.class, int.class);

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public DigitalIoDeviceConfigurationType(Class<? extends HardwareDevice> clazz, String xmlTag) {
        super(clazz, DeviceFlavor.DIGITAL_IO, xmlTag, new ConstructorPrototype[]{ctorDigitalDevice});
    }

    // Used by gson deserialization
    public DigitalIoDeviceConfigurationType() {
        super(DeviceFlavor.DIGITAL_IO);
    }

    //----------------------------------------------------------------------------------------------
    // Instance creation
    //----------------------------------------------------------------------------------------------

    public @Nullable HardwareDevice createInstance(DigitalChannelController controller, int port) {
        try {
            Constructor<HardwareDevice> ctor;

            ctor = findMatch(ctorDigitalDevice);
            if (null != ctor) {
                return ctor.newInstance(controller, port);
            }
        } catch (IllegalAccessException|InstantiationException|InvocationTargetException e) {
            handleConstructorExceptions(e);
            return null;
        }
        throw new RuntimeException("internal error: unable to locate constructor for user device type " + getName());
    }

    //----------------------------------------------------------------------------------------------
    // Serialization (used in local marshalling during configuration editing)
    //----------------------------------------------------------------------------------------------

    private Object writeReplace() {
        return new SerializationProxy(this);
    }
}
