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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vuforia.TrackableResult;

import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;

/**
 * {@link VuforiaTrackable} provides access to an individual trackable Vuforia target.
 */
public interface VuforiaTrackable
    {
    /**
     * Sets an object that will receive notifications as the {@link VuforiaTrackable}
     * is tracked and is not tracked. If no listener is provided, then a default listener
     * is used.
     * @param listener the object which is to receive tracking notifications regarding this trackable.
     *                 If this is null, then a default listener is used. Thus, there is <em>always</em>
     *                 a listener associated with a {@link VuforiaTrackable}.
     * @see #getListener()
     * @see VuforiaTrackableDefaultListener
     */
    void setListener(@Nullable Listener listener);

    /**
     * Returns the current listener associated with this trackable.
     * @return the current listener associated with this trackable.
     * @see #setListener(Listener)
     * @see VuforiaTrackableDefaultListener
     */
    Listener getListener();

    /**
     * Sets the location of the trackable in the the FTC Field Coordinate System.
     * By default, the location is null, indicating that it is unknown.
     *
     * @param ftcFieldFromTarget the location of the trackable in the FTC Field Coordinate System. This
     *                           transformation maps coordinates in the Target Coordinate System
     *                           to coordinates in the FTC Field Coordinate System.
     * @see #getLocation()
     * @see VuforiaTrackableDefaultListener#getRobotLocation()
     * @see #setLocation(OpenGLMatrix)
     */
    void setLocationFtcFieldFromTarget(@NonNull OpenGLMatrix ftcFieldFromTarget);

    /**
     * A synonym for {@link #setLocationFtcFieldFromTarget(OpenGLMatrix)}
     */
    void setLocation(@NonNull OpenGLMatrix location);

    /**
     * Returns the location of the trackable in the FTC Field Coordinate System. The returned
     * transformation will map coordinates in the Target Coordinate System to coordinates in the
     * FTC Field Coordinate System.
     *
     * @return the location of the trackable in the FTC Field Coordinate System.
     * @see #setLocationFtcFieldFromTarget(OpenGLMatrix)
     * @see #getLocation()
     */
    @NonNull OpenGLMatrix getFtcFieldFromTarget();

    /**
     * Returns the location of the trackable in the FTC Field Coordinate System.
     * A synonym for {@link #getFtcFieldFromTarget()}.
     *
     * @return the location of the trackable in the FTC Field Coordinate System.
     */
    @NonNull OpenGLMatrix getLocation();

    /**
     * Sets user data to be associated with this trackable object. The SDK does not internally
     * use this functionality; rather, it is intended as a means by which user code can easily
     * keep track of trackable-specific state.
     *
     * @param object user data to be associated with this trackable object.
     * @see #getUserData()
     */
    void setUserData(Object object);

    /**
     * Retreives user data previously associated with this trackable object.
     *
     * @return user data previously associated with this trackable object.
     * @see #setUserData(Object)
     */
    Object getUserData();

    /**
     * Returns the {@link VuforiaTrackables} of which this {@link VuforiaTrackable} is a member
     *
     * @return the {@link VuforiaTrackables} of which this {@link VuforiaTrackable} is a member
     * @see VuforiaTrackables
     */
    VuforiaTrackables getTrackables();

    /**
     * Sets a user-determined name associated with this trackable. This is mostly useful for debugging.
     * @param name a user-determined name
     * @see #getName()
     * @see #setUserData(Object)
     */
    void setName(String name);

    /**
     * Returns the user-determined name associated with this trackable.
     * @return the user-determined name associated with this trackable.
     * @see #setName(String)
     * @see VuforiaTrackables#setName(String)
     */
    String getName();

    /**
     * Returns the parent trackable with which this trackable is associated, if any.
     * @return the parent trackable with which this trackable is associated, if any.
     */
    VuforiaTrackable getParent();

    //----------------------------------------------------------------------------------------------
    // Listeners
    //----------------------------------------------------------------------------------------------

    interface Listener
        {
        /**
         * {@link #onTracked} is called by the system to notify the listener that its associated trackable is currently visible.
         *
         * @param trackableResult the Vuforia trackable result object in which we were located
         * @param cameraName      the name of the camera used by Vuforia to track this object
         * @param camera          the {@link Camera} instance used in the tracking. Will be null if
         *                        a built-in camera is used
         */
        void onTracked(TrackableResult trackableResult, CameraName cameraName, @Nullable Camera camera, @Nullable VuforiaTrackable child);

        /**
         * Called by the system to inform the trackable that it is no longer visible.
         */
        void onNotTracked();

        /**
         * Adds a trackable on which this {@link Listener} is listening. May not be
         * called once tracking has begun. If the {@link Listener} is already listening
         * to this trackable, then the call has no effect.
         */
        void addTrackable(VuforiaTrackable trackable);
        }

    }
