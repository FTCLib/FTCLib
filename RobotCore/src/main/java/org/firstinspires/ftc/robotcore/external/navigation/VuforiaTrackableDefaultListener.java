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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.vuforia.Matrix34F;
import com.vuforia.TrackableResult;
import com.vuforia.VuMarkTarget;
import com.vuforia.VuMarkTargetResult;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraManager;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;
import org.firstinspires.ftc.robotcore.internal.vuforia.VuforiaPoseMatrix;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link VuforiaTrackableDefaultListener} is the default listener used for {@link VuforiaTrackable}
 * implementations. This listener facilitates polling for results of the tracking. (Advanced:) Alternate
 * listeners could make use of event-driven results by taking actions in the {@link VuforiaTrackable.Listener}
 * methods.
 *
 * @see VuforiaTrackable
 * @see VuforiaTrackable#getListener()
 */
@SuppressWarnings("WeakerAccess")
public class VuforiaTrackableDefaultListener implements VuforiaTrackable.Listener
    {
    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    protected static class PoseAndCamera
        {
        public final VuforiaPoseMatrix pose;
        public final CameraName cameraName;
        public PoseAndCamera(VuforiaPoseMatrix pose, CameraName cameraName)
            {
            this.pose = pose;
            this.cameraName = cameraName;
            }
        public VuforiaLocalizer.CameraDirection getCameraDirection()
            {
            if (cameraName instanceof BuiltinCameraName)
                {
                return ((BuiltinCameraName)cameraName).getCameraDirection();
                }
            return VuforiaLocalizer.CameraDirection.UNKNOWN;
            }

        @Override public String toString()
            {
            return Misc.formatForUser("PoseAndCamera(%s|%s)", pose, cameraName);
            }
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public final static String TAG = "Vuforia";

    protected final Object lock = new Object();
    protected VuforiaTrackable trackable;
    protected boolean newPoseAvailable;
    protected boolean newLocationAvailable;
    protected PoseAndCamera currentPoseAndCamera;
    protected PoseAndCamera lastTrackedPoseAndCamera;
    protected VuMarkInstanceId vuMarkInstanceId = null;

    protected final OpenGLMatrix phoneFromVuforiaCameraFront;
    protected final OpenGLMatrix phoneFromVuforiaCameraBack;
    protected final OpenGLMatrix vuforiaCameraFrontFromVuforiaCameraBack;
    protected final OpenGLMatrix ftcCameraFromVuforiaCamera;
    protected final OpenGLMatrix vuforiaCameraFromFtcCamera;
    protected final OpenGLMatrix phoneFromFtcCameraFront;
    protected final OpenGLMatrix phoneFromFtcCameraBack;
    protected final OpenGLMatrix ftcCameraFrontFromPhone;
    protected final OpenGLMatrix ftcCameraBackFromPhone;
    protected final Map<CameraName, OpenGLMatrix> ftcCameraFromRobotCoords = new HashMap<>();
    protected final Map<CameraName, OpenGLMatrix> robotFromFtcCameraCoords = new HashMap<>();

    protected final CameraName cameraNameFront;
    protected final CameraName cameraNameBack;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public VuforiaTrackableDefaultListener()
        {
        this(null);
        }

    /** If a null {@link VuforiaTrackable} is provided, then {@link #addTrackable(VuforiaTrackable)}
     * must be called later, before tracking actually begins. */
    public VuforiaTrackableDefaultListener(@Nullable VuforiaTrackable trackable)
        {
        this.trackable = trackable;
        newPoseAvailable = false;
        newLocationAvailable = false;
        currentPoseAndCamera = lastTrackedPoseAndCamera = null;
        vuMarkInstanceId = null;

        /**
         * "The nice thing about standards is that you have so many to choose from."
         *      Andrew S. Tanenbaum, Computer Networks, 2nd ed., p. 254.
         *
         * The pose matrix returned in {@link TrackableResult#getPose()} is in the Vuforia Camera
         * Coordinate System. The Vuforia Camera Coordinate System is unfortunately different than
         * the FTC Camera Coordinate system (which is described in the documentation in the
         * {@link ConceptVuforiaNavigation} opmode). Thus, careful attention to which coordinate
         * system one is using at any given time is required.
         *
         * In the Vuforia Camera Coordinate System, when you are looking at the face of the
         * camera (such that you would be in the picture), the axes of the camera are as follows:
         *
         * <ol>
         *     <li> the X axis is positive to your <em>left</em></li>
         *     <li> the Y axis is positive heading <em>down</em></li>
         *     <li> the Z axis is positive coming out of the camera heading toward you</li>
         * </ol>
         *
         * You can learn more about Vuforia Camera Coordinate System at the following location
         * <a href="https://library.vuforia.com/articles/Solution/Whats-New-in-Vuforia-5-5.html">Spatial
         * and Temporal Frame of Reference</a>
         *
         * When applied to the built-in cameras on a phone, the Vuforia Camera Coordinate System
         * models the phone as being in landscape mode:
         * <ol>
         *      <li> the <em>front</em> camera is model as the phone being in landscape mode with the
         *        <em>right</em> side of the phone down, whereas </li>
         *
         *      <li>the <em>back</em> camera is modelled as the phone being in landscape mode with the
         *        <em>left</em> side of the phone being down.</li>
         * </ol>
         *
         * So: to convert between the two, the phone is rotated 180deg along the longitudinal axis
         * of the phone.
         *
         * To convert from the Vuforia Camera Coordinate system to the FTC Camera Coordinate System,
         * one negates the X and Y axis values.
         */
        phoneFromVuforiaCameraFront = new OpenGLMatrix(new float[]
            {
                0,  1,  0,  0,
               -1,  0,  0,  0,
                0,  0,  1,  0,
                0,  0,  0,  1
            });

        phoneFromVuforiaCameraBack = new OpenGLMatrix(new float[]
            {
                0, -1,  0,  0,
               -1,  0,  0,  0,
                0,  0, -1,  0,
                0,  0,  0,  1
            });

        ftcCameraFromVuforiaCamera = new OpenGLMatrix(new float[]
            {
                -1,  0, 0, 0,
                 0, -1, 0, 0,
                 0,  0, 1, 0,
                 0,  0, 0, 1
            });

        vuforiaCameraFrontFromVuforiaCameraBack = phoneFromVuforiaCameraFront.inverted().multiplied(phoneFromVuforiaCameraBack);
        vuforiaCameraFromFtcCamera = ftcCameraFromVuforiaCamera.inverted();

        phoneFromFtcCameraFront = phoneFromVuforiaCameraFront.multiplied(vuforiaCameraFromFtcCamera);
        phoneFromFtcCameraBack = phoneFromVuforiaCameraBack.multiplied(vuforiaCameraFromFtcCamera);

        ftcCameraFrontFromPhone = phoneFromFtcCameraFront.inverted();
        ftcCameraBackFromPhone = phoneFromFtcCameraBack.inverted();

        CameraManager cameraManager = ClassFactory.getInstance().getCameraManager();
        cameraNameFront = cameraManager.nameFromCameraDirection(VuforiaLocalizer.CameraDirection.FRONT);
        cameraNameBack = cameraManager.nameFromCameraDirection(VuforiaLocalizer.CameraDirection.BACK);
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    /**
     * Informs the listener of the location of the phone on the robot and the identity of
     * the camera being used. This information is needed in order to compute the robot location.
     *
     * Note that this is only applicable and useful when using cameras builtin to a phone.
     *
     * @param robotFromPhone  the location of the phone on the robot. Maps phone coordinates to robot coordinates.
     * @param cameraDirection which camera on the phone is in use
     */
    public void setPhoneInformation(@NonNull OpenGLMatrix robotFromPhone, @NonNull VuforiaLocalizer.CameraDirection cameraDirection)
        {
        switch (cameraDirection)
            {
            case FRONT:
                setCameraLocationOnRobot(cameraNameFront, robotFromPhone.multiplied(phoneFromFtcCameraFront));
                break;
            case BACK:
                setCameraLocationOnRobot(cameraNameBack, robotFromPhone.multiplied(phoneFromFtcCameraBack));
                break;
            case DEFAULT:
            default:
                throw Misc.illegalArgumentException("cameraDirection:%s", cameraDirection);
            }
        }

    /**
     * Informs the {@link VuforiaTrackableDefaultListener} of the location of a particular camera
     * on the robot.
     *
     * @param cameraName the name of the camera in question
     * @param robotFromFtcCamera the location of that camera; it transforms FTC camera coordiantes
     *                           to robot coordinates
     */
    public void setCameraLocationOnRobot(@NonNull CameraName cameraName, @NonNull OpenGLMatrix robotFromFtcCamera)
        {
        OpenGLMatrix ftcCameraFromRobot = robotFromFtcCamera.inverted();
        synchronized (lock)
            {
            robotFromFtcCameraCoords.put(cameraName, robotFromFtcCamera);
            ftcCameraFromRobotCoords.put(cameraName, ftcCameraFromRobot);
            }
        }

    /**
     * Returns the location of the phone on the robot, as previously set, or null if that
     * has never been set.
     * @return the location of the phone on the robot, or null if that has never been set
     *
     * @deprecated Using {@link #getCameraLocationOnRobot(CameraName)} is a better choice.
     */
    @Deprecated @SuppressWarnings("DeprecatedIsStillUsed")
    public OpenGLMatrix getPhoneLocationOnRobot()
        {
        // We want 'robotFromPhone'.
        synchronized (lock)
            {
            OpenGLMatrix robotFromFtcCameraFront = robotFromFtcCameraCoords.get(cameraNameFront);
            if (robotFromFtcCameraFront != null)
                {
                return robotFromFtcCameraFront.multiplied(ftcCameraFrontFromPhone);
                }

            OpenGLMatrix robotFromFtcCameraBack = robotFromFtcCameraCoords.get(cameraNameBack);
            if (robotFromFtcCameraBack != null)
                {
                return robotFromFtcCameraBack.multiplied(ftcCameraBackFromPhone);
                }
            }
        return null;
        }

    /**
     * Returns information previously set in {@link #setCameraLocationOnRobot(CameraName, OpenGLMatrix)}
     * @param cameraName the camera whose location is sought
     * @return the location previously set, or the identity matrix if there is no such location
     */
    public @NonNull OpenGLMatrix getCameraLocationOnRobot(CameraName cameraName)
        {
        synchronized (lock)
            {
            OpenGLMatrix result = robotFromFtcCameraCoords.get(cameraName);
            if (result == null)
                {
                result = OpenGLMatrix.identityMatrix();
                }
            return result;
            }
        }

    protected @NonNull OpenGLMatrix getFtcCameraFromRobot(CameraName cameraName)
        {
        synchronized (lock)
            {
            OpenGLMatrix ftcCameraFromRobot = ftcCameraFromRobotCoords.get(currentPoseAndCamera.cameraName);
            if (ftcCameraFromRobot==null)
                {
                ftcCameraFromRobot = OpenGLMatrix.identityMatrix();
                }
            return ftcCameraFromRobot;
            }
        }

    /**
     * Returns the identity of the camera in use
     * @return the identity of the camera in use
     *
     * @deprecated This is of little use if a non-builtin camera is in use.
     */
    @Deprecated @SuppressWarnings("DeprecatedIsStillUsed")
    public @Nullable VuforiaLocalizer.CameraDirection getCameraDirection()
        {
        synchronized (lock)
            {
            if (currentPoseAndCamera != null)
                {
                return currentPoseAndCamera.getCameraDirection();
                }
            return null;
            }
        }

    /**
     * Returns the name of the camera most recently tracked, or null if tracking has never occurred.
     * @return the name of the camera most recently tracked, or null if tracking has never occurred.
     */
    public @Nullable CameraName getCameraName()
        {
        synchronized (lock)
            {
            if (lastTrackedPoseAndCamera != null)
                {
                return lastTrackedPoseAndCamera.cameraName;
                }
            return null;
            }
        }

    /**
     * <p>Returns the {@link OpenGLMatrix} transform that represents the location of the robot
     * on in the FTC Field Coordinate System, or null if that cannot be computed. The returned
     * transformation will map coordinates in the Robot Coordinate System to coordinates in the
     * FTC Field Coordinate System.</p>
     *
     * <p>The pose will be null if the trackable is not currently visible. The location of the trackable
     * will be null if a location wasn't previously provided with {@link VuforiaTrackable#setLocation(OpenGLMatrix)}.
     * The camera location on the robot will be null if {@link #setCameraLocationOnRobot}
     * has not been called. All three must be non-null for the location to be computable.</p>
     *
     * @return the location of the robot on the field
     * @see #getUpdatedRobotLocation()
     * @see #getPosePhone()
     * @see #setPhoneInformation(OpenGLMatrix, VuforiaLocalizer.CameraDirection)
     * @see VuforiaTrackable#setLocation(OpenGLMatrix)
     * @see #getRobotLocation()
     */
    public @Nullable OpenGLMatrix getFtcFieldFromRobot()
        {
        OpenGLMatrix result = null;
        synchronized (lock)
            {
            if (currentPoseAndCamera != null)
                {
                /* C = cameraLocationOnRobot    maps   camera coords -> robot coords
                 * P = tracker.getPose()        maps   image target coords -> camera coords
                 * L = redTargetLocationOnField maps   image target coords -> field coords */

                OpenGLMatrix vuforiaCameraFromTarget = getVuforiaCameraFromTarget();
                if (vuforiaCameraFromTarget != null)
                    {
                    OpenGLMatrix ftcFieldFromTarget = trackable.getFtcFieldFromTarget();
                    OpenGLMatrix targetFromVuforiaCamera = vuforiaCameraFromTarget.inverted();
                    OpenGLMatrix ftcCameraFromRobot = getFtcCameraFromRobot(currentPoseAndCamera.cameraName);
                    result = ftcFieldFromTarget
                            .multiplied(targetFromVuforiaCamera)
                            .multiplied(vuforiaCameraFromFtcCamera)
                            .multiplied(ftcCameraFromRobot);
                    }
                }
            }
        return result;
        }

    /**
     * Synonym for {@link #getFtcFieldFromRobot()}
     */
    public @Nullable OpenGLMatrix getRobotLocation()
        {
        return getFtcFieldFromRobot();
        }

    /**
     * The pose correction matrices correct for the different coordinate systems used
     * in Vuforia and our phone coordinate system here.
     *
     * @deprecated These phone-based matrices are no longer of much relevance, as the
     *             correct values, when of interest, are fixed and known.
     */
    @Deprecated @SuppressWarnings("DeprecatedIsStillUsed")
    public @Nullable OpenGLMatrix getPoseCorrectionMatrix(VuforiaLocalizer.CameraDirection direction)
        {
        switch (direction)
            {
            case FRONT: return phoneFromVuforiaCameraFront;
            case BACK: return phoneFromFtcCameraBack;
            }
        return null;
        }

    /**
     * A synonym for {@link #getPoseCorrectionMatrix}.
     */
    protected @Nullable OpenGLMatrix getPhoneFromVuforiaCamera(VuforiaLocalizer.CameraDirection direction)
        {
        return getPoseCorrectionMatrix(direction);
        }

    /** @see #getPoseCorrectionMatrix(VuforiaLocalizer.CameraDirection) */
    @Deprecated @SuppressWarnings("DeprecatedIsStillUsed")
    public void setPoseCorrectionMatrix(VuforiaLocalizer.CameraDirection direction, @NonNull OpenGLMatrix matrix)
        {
        throw new UnsupportedOperationException("this method has no longer has any effect");
        }

    /**
     * Returns the location of the robot, but only if a new location has been detected since
     * the last call to {@link #getUpdatedRobotLocation()}.
     *
     * @return the location of the robot
     * @see #getRobotLocation()
     */
    public OpenGLMatrix getUpdatedRobotLocation()
        {
        synchronized (lock)
            {
            if (this.newLocationAvailable)
                {
                this.newLocationAvailable = false;
                return getRobotLocation();
                }
            else
                return null;
            }
        }

    /**
     * Returns the pose of the trackable if it is currently visible. If it is not currently
     * visible, null is returned. The pose of the trackable is the location of the trackable
     * in the phone's coordinate system.
     *
     * Note that this function is of little use if a webcam is used instead of a builtin phone
     * camera. See {@link #getFtcCameraFromTarget()} for a better choice.
     *
     * <p>Note that whether a trackable is visible or not is constantly dynamically changing
     * in the background as the phone is moved about. Thus, just because one call to getPose()
     * returns a non-null matrix doesn't a second call a short time later will return the same
     * result.</p>
     *
     * @return the pose of the trackable, if visible; if not visible, then null is returned.
     * @see #getPhoneFromVuforiaCamera(VuforiaLocalizer.CameraDirection)
     * @see #getVuforiaCameraFromTarget()
     * @see #isVisible()
     * @see #getPose()
     */
    public @Nullable OpenGLMatrix getPosePhone()
        {
        synchronized (lock)
            {
            OpenGLMatrix vuforiaCameraFromTarget = getVuforiaCameraFromTarget();
            return vuforiaCameraFromTarget==null
                    ? null
                    : this.getPhoneFromVuforiaCamera(currentPoseAndCamera.getCameraDirection())
                        .multiplied(vuforiaCameraFromTarget);
            }
        }

    /**
     * A synonym for {@link #getPosePhone()}, the latter being more descriptive of the
     * coordinate system of the value returned. Note that this function is of little use
     * if a webcam is used instead of a builtin phone camera.
     */
    public synchronized @Nullable OpenGLMatrix getPose()
        {
        return getPosePhone();
        }

    /**
     * Returns the pose of the trackable if it is currently visible. If it is not currently
     * visible, null is returned. The pose of the trackable is the location of the trackable
     * in the FTC Camera Coordinate System.
     *
     * @see #getPosePhone()
     */
    public @Nullable OpenGLMatrix getFtcCameraFromTarget()
        {
        synchronized (lock)
            {
            OpenGLMatrix vuforiaCameraFromTarget = getVuforiaCameraFromTarget();
            if (vuforiaCameraFromTarget != null)
                {
                return ftcCameraFromVuforiaCamera.multiplied(vuforiaCameraFromTarget);
                }
            }
        return null;
        }

    /**
     * Returns the raw pose of the trackable as reported by Vuforia. This differs from the
     * value reported by {@link #getPosePhone()} because of the differing coordinate systems
     * used by Vuforia and FTC
     *
     * @return the raw pose of the trackable as reported by Vuforia
     * @see #getPhoneFromVuforiaCamera(VuforiaLocalizer.CameraDirection)
     * @see #getRawPose()
     */
    public @Nullable OpenGLMatrix getVuforiaCameraFromTarget()
        {
        synchronized (lock)
            {
            if (this.currentPoseAndCamera != null)
                {
                return this.currentPoseAndCamera.pose.toOpenGL();
                }
            else
                return null;
            }
        }

    /** @deprecated use {@link #getVuforiaCameraFromTarget()} instead */
    @Deprecated @SuppressWarnings("DeprecatedIsStillUsed")
    public @Nullable OpenGLMatrix getRawPose()
        {
        return getVuforiaCameraFromTarget();
        }

    /**
     * Returns the raw pose of the trackable, but only if a new pose is available since the last call
     * to {@link #getUpdatedVuforiaCameraFromTarget()}.
     *
     * @return the raw pose of the trackable
     * @see #getVuforiaCameraFromTarget()
     * @see #getRawUpdatedPose()
     */
    public @Nullable OpenGLMatrix getUpdatedVuforiaCameraFromTarget()
        {
        synchronized (lock)
            {
            if (this.newPoseAvailable)
                {
                this.newPoseAvailable = false;
                return getVuforiaCameraFromTarget();
                }
            else
                return null;
            }
        }

    /** @deprecated use {@link #getUpdatedVuforiaCameraFromTarget()} instead */
    @Deprecated @SuppressWarnings("DeprecatedIsStillUsed")
    public @Nullable OpenGLMatrix getRawUpdatedPose()
        {
        return getUpdatedVuforiaCameraFromTarget();
        }

    /**
     * Answers whether the associated trackable is currently visible or not
     *
     * @return whether the associated trackable is currently visible or not
     * @see #getVuforiaCameraFromTarget()
     */
    public boolean isVisible()
        {
        synchronized (lock)
            {
            return (currentPoseAndCamera != null);
            }
        }

    /**
     * Returns the pose associated with the last known tracked location of this trackable, if any.
     * This can be used to recall the last pose seen, even if the trackable is no longer visible.
     *
     * @return the pose associated with the last known tracked location of this trackable, if any.
     * @see #getVuforiaCameraFromTarget()
     */
    public OpenGLMatrix getLastTrackedPoseVuforiaCamera()
        {
        synchronized (lock)
            {
            return lastTrackedPoseAndCamera == null ? null : lastTrackedPoseAndCamera.pose.toOpenGL();
            }
        }

    /** @deprecated use {@link #getLastTrackedPoseVuforiaCamera()} instead */
    @Deprecated @SuppressWarnings("DeprecatedIsStillUsed")
    public OpenGLMatrix getLastTrackedRawPose()
        {
        return getLastTrackedPoseVuforiaCamera();
        }

    /**
     * Returns the instance id of the currently visible VuMark associated with this
     * VuMark template, if any presently exists.
     *
     * @return the instance id of the currently visible VuMark
     * @see <a href="https://library.vuforia.com/content/vuforia-library/en/reference/java/classcom_1_1vuforia_1_1VuMarkTemplate.html">VuMark template</a>
     */
    public @Nullable VuMarkInstanceId getVuMarkInstanceId()
        {
        synchronized (lock)
            {
            return vuMarkInstanceId;
            }
        }

    /**
     * {@link #onTracked} is called by the system to notify the listener that its associated trackable is currently visible.
     *
     * @param trackableResult the Vuforia trackable result object in which we were located
     * @param cameraName      the name of the camera used by Vuforia to track this object
     * @param camera          the {@link Camera} instance used in the tracking. Will be null if
     *                        a built-in camera is used
     */
    @Override public void onTracked(TrackableResult trackableResult, CameraName cameraName, Camera camera, VuforiaTrackable child)
        {
        synchronized (lock)
            {
            currentPoseAndCamera = new PoseAndCamera(new VuforiaPoseMatrix(trackableResult.getPose()), cameraName);
            newPoseAvailable = true;
            newLocationAvailable = true;
            lastTrackedPoseAndCamera = currentPoseAndCamera;

            if (trackableResult.isOfType(VuMarkTargetResult.getClassType()))
                {
                VuMarkTargetResult vuMarkTargetResult = (VuMarkTargetResult)trackableResult;
                VuMarkTarget vuMarkTarget = (VuMarkTarget) vuMarkTargetResult.getTrackable();
                vuMarkInstanceId = new VuMarkInstanceId(vuMarkTarget.getInstanceId());
                }
            }
        }

    /**
     * Called by the system to inform the trackable that it is no longer visible.
     */
    @Override public void onNotTracked()
        {
        synchronized (lock)
            {
            currentPoseAndCamera = null;
            newPoseAvailable = true;
            newLocationAvailable = true;
            vuMarkInstanceId = null;
            }
        }

    @Override public void addTrackable(VuforiaTrackable trackable)
        {
        synchronized (lock)
            {
            this.trackable = trackable;
            }
        }
    }
