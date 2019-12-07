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
package org.firstinspires.ftc.robotcore.internal.opmode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.robocol.TelemetryMessage;

/*
 * The OpModeServices interface is an internal interface used to provide services callbacks
 * to an OpMode. The interface is declared package-scope so as to prevent its direct use by
 * OpMode's; it may only be used indirectly through wrappers provided in the OpMode class itself.
 */
public interface OpModeServices
    {
    /**
     * Update's the user portion of the driver station screen with the contents of the telemetry object
     * here provided if a sufficiently long duration has passed since the last update.
     * @param telemetry the telemetry object to send
     * @param sInterval the required minimum interval. NaN indicates that a system default interval should be used.
     *                  A value of zero will cause immediate transmission.    
     *
     * @see com.qualcomm.robotcore.eventloop.EventLoop#TELEMETRY_DEFAULT_INTERVAL
     */
    void refreshUserTelemetry(TelemetryMessage telemetry, double sInterval);

    /**
     * If the indicated OpMode is the currently active OpMode, cause that OpMode to stop as if
     * the stop button had been pressed on the driver station
     * @param opModeToStopIfActive the OpMode to stop if it is the currently active OpMode
     * @see OpMode#requestOpModeStop()
     */
    void requestOpModeStop(OpMode opModeToStopIfActive);
    }
