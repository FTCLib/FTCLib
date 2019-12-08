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

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.configuration.ConstructorPrototype;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.util.ClassUtil;
import com.qualcomm.robotcore.util.RobotLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

public abstract class InstantiableUserConfigurationType extends UserConfigurationType {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private Class<? extends HardwareDevice> clazz; // Null when running on the DS
    private List<Constructor> constructors; // Null when running on the DS

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected InstantiableUserConfigurationType(Class clazz, @NonNull DeviceFlavor flavor, @NonNull String xmlTag, ConstructorPrototype[] allowableConstructorPrototypes) {
        super(clazz, flavor, xmlTag);
        this.clazz = clazz;
        this.constructors = findUsableConstructors(allowableConstructorPrototypes);
    }

    // used by gson
    protected InstantiableUserConfigurationType(@NonNull DeviceFlavor flavor) {
        super(flavor);
    }

    @Override
    public void processAnnotation(@NonNull DeviceProperties deviceProperties) {
        super.processAnnotation(deviceProperties);
    }

    //----------------------------------------------------------------------------------------------
    // General methods
    //----------------------------------------------------------------------------------------------

    /**
     * Find the usable constructors of the underlying class, given a list of allowed prototypes
     */
    private List<Constructor> findUsableConstructors(ConstructorPrototype[] allowedPrototypes) {
        List<Constructor> result = new LinkedList<>();
        List<Constructor> constructors = ClassUtil.getDeclaredConstructors(getClazz());
        for (Constructor<?> ctor : constructors) {
            int requiredModifiers = Modifier.PUBLIC;
            if (!((ctor.getModifiers() & requiredModifiers) == requiredModifiers))
                continue;

            for (ConstructorPrototype allowedSignature : allowedPrototypes) {
                if (allowedSignature.matches(ctor)) {
                    result.add(ctor);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Finds a constructor of the underlying class that matches a given prototype
     */
    protected final Constructor<HardwareDevice> findMatch(ConstructorPrototype prototype) {
        // This method isn't being called until after ConfigurationTypeManager verifies that the class in question is a HardwareDevice.
        for (Constructor<HardwareDevice> ctor : constructors) {
            if (prototype.matches(ctor)) {
                return ctor;
            }
        }
        return null;
    }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public final boolean hasConstructors() {
        return this.constructors.size() > 0;
    }

    public final Class<? extends HardwareDevice> getClazz() {
        return this.clazz;
    }

    // Override this if the corresponding annotation can also be applied to noninstantiable classes
    public boolean classMustBeInstantiable() {
        return true;
    }


    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    /**
     * In the catch block, please return null after calling this method. This condition will
     * indicate that the device was unable to be created, but that it wasn't the user's fault, and
     * so we don't want to disable the robot.
     */
    protected final void handleConstructorExceptions(Exception e) {
        RobotLog.v("Creating user sensor %s failed: ", getName());
        RobotLog.logStackTrace(e);

        if (e instanceof InvocationTargetException) {
            Throwable targetException = ((InvocationTargetException) e).getTargetException();
            if (targetException != null) {
                RobotLog.e("InvocationTargetException caused by: ");
                RobotLog.logStackTrace(targetException);
            }
            if (!isBuiltIn()) {
                throw new RuntimeException("Constructor of device type " + getName() + " threw an exception. See log.");
            }
        }
        if (!isBuiltIn()) {
            throw new RuntimeException("Internal error while creating device of type " + getName() + ". See log.");
        }
    }

    //----------------------------------------------------------------------------------------------
    // Serialization (used in local marshalling during configuration editing)
    //----------------------------------------------------------------------------------------------

    private Object writeReplace() {
        return new SerializationProxy(this);
    }
}
