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
package org.firstinspires.ftc.robotcore.internal.ftdi;

/**
 * Created by bob on 3/27/2017.
 */
@SuppressWarnings("WeakerAccess")
public enum UsbStandardRequest
    {
    // Table 9.5, USB Spec
    GET_STATUS(0),
    CLEAR_FEATURE(1),
    Reserved0(2),
    SET_FEATURE(3),
    Reserved1(4),
    SET_ADDRESS(5),
    GET_DESCRIPTOR(6),
    SET_DESCRIPTOR(7),
    GET_CONFIGURATION(8),
    SET_CONFIGURATION(9),
    GET_INTERFACE(10),
    SET_INTERFACE(11),
    SYNCH_FRAME(12),
    SET_ENCRYPTION(13),
    GET_ENCRYPTION(14),
    SET_HANDSHAKE(15),
    GET_HANDSHAKE(16),
    SET_CONNECTION(17),
    SET_SECURITY_DATA(18),
    GET_SECURITY_DATA(19),
    SET_WUSB_DATA(20),
    LOOPBACK_DATA_WRITE(21),
    LOOPBACK_DATA_READ(22),
    SET_INTERFACE_DS(23),
    SET_SEL(48),
    SET_ISOCH_DELAY(49),
    ;

    final int value;

    UsbStandardRequest(int value)
        {
        this.value = value;
        }
    }
