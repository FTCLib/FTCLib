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

import com.qualcomm.robotcore.exception.RobotCoreException;

/**
 * This interface can be used to control the activeness or aliveness of an object that controls
 * a piece of hardware such as a motor or servo controller. The object can be transitioned
 * amongst a series of states in which various degrees of functionality are available. The
 * states are as follows:
 *
 * armed:       the object controlling the hardware is fully functional in its intended, usual way.
 *              In this state, the object 'owns' full control of the hardware it represents.
 *
 * disarmed:    the object is quiescent, not manipulating or controlling the hardware. In this state,
 *              it is conceivable that some *other* object instance might be created and then be
 *              successfully armed on the same underlying hardware. In contrast, it is not expected
 *              that two object instances may be simultaneously armed against the same piece of
 *              hardware.
 *
 * pretending:  the object pretends as best it can to act as if it were armed on an actual
 *              underlying piece of hardware, but in reality the object is just making it all up:
 *              writes may be sent to the bit-bucket, reads might always return zeros, and so on.
 *              Though this may sound odd, having a hardware-controlling object function in this mode
 *              might minimize impact on upper software layers in the event that the desired actual
 *              hardware is disconnected or otherwise unavailable.
 *
 * closed:      this is much like disarmed, but more serious and permanent shutdown steps might
 *              be taken as an object transitions to the closed state.
 *
 * Transient 'toX' states are also present. The legal state transitions are as follows:
 *
 *              disarmed -> toArmed
 *              toArmed -> armed
 *
 *              disarmed -> toPretending
 *              toPretending -> pretending
 *
 *              armed -> toDisarmed
 *              toArmed -> toDisarmed
 *              pretending -> toDisarmed
 *              toPretending -> toDisarmed
 *              toDisarmed -> disarmed
 *
 *              armed -> closed
 *              toArmed -> closed
 *              pretending -> closed
 *              toPretending -> closed
 *              toDisarmed -> closed
 *              disarmed -> closed
  *
 * Notice that once closed, no further state transitions are possible. Conversely, it is possible
 * to close from any state and to disarm from any state except from closed. In particular, it is
 * possible to close or disarm from the transitional toArmed and toPretending states:
 * implementations *must* take care to ensure this is always possible.
 *
 * Typically, when first instantiated, an object is in the disarmed state.
 *
 * Objects should, generally, minimize the time they are in the disarmed state, as to many clients
 * they will appear dysfunctional and error prone in that state, since those clients may not have
 * been coded correctly to deal with an object that doesn't service read()s or write()s *at*all*.
 */
public interface RobotUsbModule extends RobotArmingStateNotifier
    {
    /**
     * Causes the module to attempt to enter the armed state. If the module is already
     * armed, this method has no effect.
     *
     * @see RobotUsbModule
     */
    void arm() throws RobotCoreException, InterruptedException;

    /**
     * Causes the module to attempt to enter the pretending state. If the module is already
     * pretending, this method has no effect.
     *
     * @see RobotUsbModule
     */
    void pretend() throws RobotCoreException, InterruptedException;

    /**
     * Causes the module to attempt to enter the armed state, but if that is not possible, to
     * enter the pretending state.
     *
     * @see RobotUsbModule
     */
    void armOrPretend() throws RobotCoreException, InterruptedException;

    /**
     * Causes the module to attempt to enter the disarmed state. If the module is already
     * disarmed, this method has no effect.
     *
     * @see RobotUsbModule
     */
    void disarm() throws RobotCoreException, InterruptedException;

    /**
     * Causes the module to attempt to enter the closed state. If the module is already
     * closed, this method has no effect.
     *
     * @see RobotUsbModule
     */
    void close();
    }
