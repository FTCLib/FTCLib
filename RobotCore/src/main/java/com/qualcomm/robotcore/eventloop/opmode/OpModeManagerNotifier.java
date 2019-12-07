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
package com.qualcomm.robotcore.eventloop.opmode;

import android.app.Activity;

import com.qualcomm.robotcore.hardware.HardwareDevice;

import org.firstinspires.ftc.robotcore.internal.opmode.OpModeManagerImpl;

/**
 * {@link OpModeManagerNotifier.Notifications} is an interface by which interested
 * parties can receive notification of the coming and going of opmodes.
 * @see OpModeManagerImpl#getOpModeManagerOfActivity(Activity)
 */
public interface OpModeManagerNotifier
    {
    /**
     * {@link Notifications} can be used to receive notifications of the comings
     * and goings of opmodes in the system. These notifications are sent to any
     * {@link HardwareDevice} in the hardware map that additional implements the
     * {@link Notifications} interface. Notifications may also be received by objects
     * that register themselves with the OpMode manager.
     */
    interface Notifications
        {
        /** The indicated opmode is just about to be initialized. */
        void onOpModePreInit(OpMode opMode);

        /** The indicated opmode is just about to be started. */
        void onOpModePreStart(OpMode opMode);

        /** The indicated opmode has just been stopped. */
        void onOpModePostStop(OpMode opMode);
        }

    /**
     * Registers an object as explicitly interested in receiving notifications as
     * to the coming and going of opmodes.
     * @param listener the object which is to receive notifications
     * @return the currently active opmode at the instant of registration
     * @see #unregisterListener(Notifications)
     */
    OpMode registerListener(OpModeManagerNotifier.Notifications listener);

    /**
     * Unregisters a previously registered listener. If the provided listener is in
     * fact not currently registered, the call has no effect.
     * @param listener the listener to be unregistered.
     */
    void unregisterListener(OpModeManagerNotifier.Notifications listener);
    }
