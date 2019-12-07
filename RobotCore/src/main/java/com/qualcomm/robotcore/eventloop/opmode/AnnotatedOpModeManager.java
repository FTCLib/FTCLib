/*
 * Copyright (c) 2015 Robert Atkinson
 *
 *    Ported from the Swerve library by Craig MacFarlane
 *    Based upon contributions and original idea by dmssargent.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Robert Atkinson, Craig MacFarlane nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.qualcomm.robotcore.eventloop.opmode;

/**
 * OpModeManager instances are used as part of a decentralized OpMode registration
 * mechanism.
 *
 * @see OpModeManager
 * @see OpModeRegistrar
 */
public interface AnnotatedOpModeManager extends OpModeManager
    {
    /**
     * Register a class for display on the driver station and availability for game play.
     * New instances of this class will be created as needed. The name used on the driver
     * station menu is taken from a {@link TeleOp} or {@link Autonomous} annotation on the
     * class itself (any {@link Disabled} attribute which may also be present is ignored).
     * If the class lacks both a {@link TeleOp} and a {@link Autonomous} annotation, then
     * no registration takes place.
     *
     * @param opModeClass   the class of the OpMode to create
     * @see TeleOp
     * @see Autonomous
     * @see #register(String, Class)
     */
    void register(Class opModeClass);
    }
