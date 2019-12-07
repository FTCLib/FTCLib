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
package org.firstinspires.ftc.robotcore.external.navigation;

import java.util.List;

/**
 * {@link VuforiaTrackables} represents a set of targets that can be visually tracked.
 * @see VuforiaLocalizer#loadTrackablesFromAsset(String)
 * @see VuforiaLocalizer#loadTrackablesFromFile(String)
 */
public interface VuforiaTrackables extends List<VuforiaTrackable>
    {
    /**
     * Sets the name for this {@link VuforiaTrackables} and any of its contained
     * trackables which do not already have a user-specified name
     * @param name the name for this trackables
     * @see #getName()
     */
    void setName(String name);

    /**
     * Returns the user-specified name for this trackables.
     * @return the user-specified name for this trackables.
     * @see #setName(String)
     */
    String getName();

    /**
     * Activates this trackables so that its localizer is actively seeking the presence
     * of the trackables that it contains.
     * @see #deactivate()
     */
    void activate();

    /**
     * Deactivates this trackables, causing its localizer to no longer see the presence
     * of the trackables it contains.
     * @see #activate()
     */
    void deactivate();

    /**
     * Returns the {@link VuforiaLocalizer} which manages this list of trackables.
     * @return the {@link VuforiaLocalizer} which manages this list of trackables.
     */
    VuforiaLocalizer getLocalizer();
    }
