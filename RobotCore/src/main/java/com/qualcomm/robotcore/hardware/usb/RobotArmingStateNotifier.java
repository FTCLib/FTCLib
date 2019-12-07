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
package com.qualcomm.robotcore.hardware.usb;

import com.qualcomm.robotcore.util.SerialNumber;

/**
 * Created by bob on 2016-03-12.
 * @see RobotUsbModule
 */
public interface RobotArmingStateNotifier
    {
    enum ARMINGSTATE { ARMED, PRETENDING, DISARMED, CLOSED, TO_ARMED, TO_PRETENDING, TO_DISARMED }

    /**
     * Returns the serial number of this USB module
     * @return the serial number of this USB module
     */
    SerialNumber getSerialNumber();

    /**
     * Returns the current arming state of the object.
     * @return the current arming state of the object
     */
    ARMINGSTATE getArmingState();

    /**
     * Registers a callback for arming state notifications from this module. If this callback
     * is already registered for notifications from this module, this method has no effect. Note
     * that multiple callbacks may be simultaneously registered with a given one module: they all
     * receive state-change notifications, in an arbitrary order.
     *
     * @param callback
     * @see #unregisterCallback(Callback)
     */
    void registerCallback(Callback callback, boolean doInitialCallback);

    /**
     * Unregister a callback which has been registered for notifications with this module. If the
     * callback was not previously registered, this method has no effect.
     *
     * @param callback
     * @see #registerCallback(Callback, boolean)
     */
    void unregisterCallback(Callback callback);

    /**
     * The Callback interface can be used to receive notifications when a module changes
     * its arming state.
     */
    interface Callback
        {
        /**
         * Notifies the callback that a module with which it has registered for notifications
         * has undergone a change of state.
         *
         * @param module    the module whose state has changed
         * @param state     the state into which that module has transitioned
         *
         * @see #registerCallback(Callback)
         */
        void onModuleStateChange(RobotArmingStateNotifier module, ARMINGSTATE state);
        }

    }
