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

import static org.firstinspires.ftc.robotcore.internal.system.AppUtil.WEBCAM_CALIBRATIONS_DIR;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.view.ViewGroup;

import com.qualcomm.robotcore.R;
import com.vuforia.CameraCalibration;
import com.vuforia.CameraDevice;
import com.vuforia.Frame;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.stream.CameraStreamSource;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

/**
 * Robot "localization" denotes a robot's ability to establish its own position and
 * orientation within its frame of reference. The {@link VuforiaLocalizer} interface provides
 * an interface for interacting with subsystems that can help support localization through
 * visual means.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Mobile_robot_navigation">Mobile robot navigation</a>
 */
public interface VuforiaLocalizer extends CameraStreamSource
    {
    /**
     * Loads a Vuforia dataset from the indicated application asset, which must be of
     * type .XML. The corresponding .DAT asset must be a sibling. Note that this operation
     * can be extremely lengthy, possibly taking a few seconds to execute. Loading datasets
     * from an asset you stored in your application APK is the recommended approach to
     * packaging datasets so they always travel along with your code.
     *
     * <p>Datasets are created using the <a href=https://developer.vuforia.com/target-manager>Vuforia Target Manager</a></p>
     *
     * @param assetName the name of the .XML dataset asset to load
     * @return a DataSet that can be used to manipulate the loaded data
     * @see #loadTrackablesFromFile(String)
     */
    VuforiaTrackables loadTrackablesFromAsset(String assetName);

    /**
     * Loads a Vuforia dataset from the indicated file, which must be a .XML file
     * and contain the full file path. The corresponding .DAT file must be a sibling
     * file in the same directory. Note that this operation can be extremely lengthy,
     * possibly taking a few seconds to execute.
     *
     * <p>Datasets are created using the <a href=https://developer.vuforia.com/target-manager>Vuforia Target Manager</a></p>
     *
     * @param absoluteFileName the full path to the .XML file of the dataset
     * @return a DataSet that can be used to manipulate the loaded data
     * @see #loadTrackablesFromAsset(String)
     */
    VuforiaTrackables loadTrackablesFromFile(String absoluteFileName);

    /**
     * Provides access to the {@link Camera} used by Vuforia. Will be null if a built-in camera
     * is in use. Note that Vuforia is usually actively interacting with this {@link Camera}; users
     * should on their own not initiate streaming, close the camera, or the like.
     */
    @Nullable Camera getCamera();

    /**
     * Provides access to the name of the camera used by Vuforia.
     */
    @NonNull CameraName getCameraName();

    /**
     * (Advanced) Returns information about Vuforia's knowledge of the camera that it is using.
     * @return information about Vuforia's knowledge of the camera that it is using.
     *
     * @see com.vuforia.Tool#getProjectionGL(CameraCalibration, float, float)
     */
    CameraCalibration getCameraCalibration();

    /**
     * (Advanced) Returns a queue into which, if requested, Vuforia {@link Frame}s are placed
     * as they become available. This provides a means by which camera image data can be accessed
     * even while the Vuforia engine is running.
     *
     * <p>While the Vuforia engine is running, it takes exclusive ownership of the camera it is
     * using. This impedes the ability to also use that camera image data in alternative visual
     * processing algorithms. However, periodically (at a rate of tens of Hz), the Vuforia engine
     * makes available {@link Frame}s containing snapshots from the camera, and through which
     * camera image data can be retrieved. These can optionally be retrieved through the frame
     * queue.</p>
     *
     * <p>To access these {@link Frame}s, call {@link #setFrameQueueCapacity(int)} to enable the
     * frame queue. Once enabled, the frame queue can be accessed using {@link #getFrameQueue()}
     * and the methods thereon used to access {@link Frame}s as they become available.</p>
     *
     * <p>When {@link #setFrameQueueCapacity(int)} is called, any frame queue returned previously by
     * {@link #getFrameQueue()} becomes invalid and must be re-fetched.</p>
     *
     * @return a queue through which Vuforia {@link Frame}s may be retrieved.
     *
     * @see #setFrameQueueCapacity(int)
     * @see <a href="https://library.vuforia.com/sites/default/api/java/classcom_1_1vuforia_1_1Frame.html">Frame (Java)</a>
     * @see <a href="https://library.vuforia.com/sites/default/api/cpp/classVuforia_1_1Frame.html">Frame (C++)</a>
     * @see <a href="https://developer.android.com/reference/java/util/concurrent/BlockingQueue.html">BlockingQueue</a>
     */
    BlockingQueue<CloseableFrame> getFrameQueue();

    /**
     * (Advanced) Sets the maximum number of {@link Frame}s that will simultaneously be stored in the
     * frame queue. If the queue is full and new {@link Frame}s become available, older frames
     * will be discarded. The frame queue initially has a capacity of zero.
     *
     * <p>Note that calling this method invalidates any frame queue retrieved previously
     * through {@link #getFrameQueue()}.</p>
     *
     * @param capacity the maximum number of items that may be stored in the frame queue
     * @see #getFrameQueue()
     * @see #getFrameQueueCapacity()
     */
    void setFrameQueueCapacity(int capacity);

    /**
     * (Advanced) Returns the current capacity of the frame queue.
     * @return the current capacity of the frame queue.
     * @see #setFrameQueueCapacity(int)
     * @see #getFrameQueue()
     */
    int getFrameQueueCapacity();

    /**
     * (Advanced) Calls the indicated code with a frame from the video stream, exactly once.
     * @param frameConsumer the code to call with the to-be-processed frame.
     */
    void getFrameOnce(Continuation<? extends Consumer<Frame>> frameConsumer);

    /**
     * (Advanced) Ensures that Vuforia exposes necessary data formats in {@link Frame}s that allows
     * {@link #convertFrameToBitmap(Frame)} to function
     */
    void enableConvertFrameToBitmap();

    /**
     * (Advanced) Ask Vuforia to convert frames to the given requested formats. Returns
     * success/failure result for each format.
     * @param pixelFormats Formats to request Vuforia to convert to.
     */
    boolean[] enableConvertFrameToFormat(int... pixelFormats);

    /**
     * (Advanced) A helper utility that converts a Vuforia {@link Frame} into an Android {@link Bitmap}.
     */
    @Nullable Bitmap convertFrameToBitmap(Frame frame);


    /** {@link CloseableFrame} exposes a close() method so that one can proactively
     * reduce memory pressure when we're done with a Frame */
    class CloseableFrame extends Frame
        {
        /** creating a CloseableFrame also has an effect equivalent to calling frame.clone() */
        public CloseableFrame(Frame frame)
            {
            super(frame);
            }
        public void close()
            {
            super.delete();
            }
        }

    /**
     * {@link CameraDirection} enumerates the identities of the builtin phone cameras that Vuforia can use.
     * @see Parameters#cameraName
     * @see Parameters#cameraDirection
     */
    enum CameraDirection
        {
        //------------------------------------------------------------------------------------------
        // Values
        //------------------------------------------------------------------------------------------

        BACK(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_BACK),
        FRONT(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_FRONT),

        /** Using {@link #BACK} or {@link #FRONT} is a better choice than {@link #DEFAULT} */
        DEFAULT(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT),

        /** A direction whose particulars are not actually known */
        UNKNOWN(-1);

        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        protected final int direction;

        public int getDirection()
            {
            if (this==UNKNOWN)
                {
                throw new IllegalArgumentException("%s has no actual 'direction' value");
                }
            return direction;
            }

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        CameraDirection(int direction)
            {
            this.direction = direction;
            }

        /** Converts a string to the corresponding camera direction. Returns UNKNOWN on failure. */
        public static @NonNull CameraDirection from(String string)
            {
            for(CameraDirection direction : values())
                {
                if (direction.toString().equals(string))
                    {
                    return direction;
                    }
                }
            return UNKNOWN;
            }
        };

    /**
     * {@link Parameters} provides configuration information for instantiating the Vuforia localizer
     */
    class Parameters
        {
        /**
         * The license key with which to use Vuforia. Vuforia will not load without a valid license being
         * provided. Vuforia 'Development' license keys, which is what is needed here, can be obtained
         * free of charge from the Vuforia developer web site at https://developer.vuforia.com/license-manager
         *
         * Valid Vuforia license keys are always 380 characters long, and look as if they contain mostly
         * random data. As an example, here is a example of a fragment of a valid key:
         *      ... yIgIzTqZ4mWjk9wd3cZO9T1axEqzuhxoGlfOOI2dRzKS4T0hQ8kT ...
         * Once you've obtained a license key, copy the string form of the key from the Vuforia web site
         * and paste it in to your code as the value of the 'vuforiaLicenseKey' field of the
         * {@link Parameters} instance with which you initialize Vuforia.
         */
        public String vuforiaLicenseKey = "<visit https://developer.vuforia.com/license-manager to obtain a license key>";

        /**
         * If non-null, then this indicates the {@link Camera} to use with Vuforia. Otherwise,
         * the camera is determined using {@link #cameraName} and {@link #cameraDirection}.
         *
         * @see #cameraName
         * @see #cameraDirection
         */
        public @Nullable Camera camera = null;

        /**
         * If {@link #camera} is non-null, this value is ignored.
         *
         * Otherwise, if this value is neither null nor the 'unknown' camera name, the it indicates
         * the name of the camera to use with Vuforia. It will be opened as needed.
         *
         * Otherwise, if this value is the 'unknown' camera name, then this value is ignored and
         * {@link #cameraDirection} is used to indicate the camera.
         *
         * Otherwise, this value is null, and an error has occurred.
         *
         * @see #camera
         * @see #cameraDirection
         */
        public @Nullable CameraName cameraName = ClassFactory.getInstance().getCameraManager().nameForUnknownCamera();

        /**
         * If {@link #camera} is null and {@link #cameraName} is the 'unknown' camera name, then
         * this value is used to indicate the camera to use with Vuforia. Note that this value
         * can only indicate the use of built-in cameras found on a phone.
         *
         * @see #camera
         * @see #cameraName
         */
        public CameraDirection cameraDirection = CameraDirection.BACK;

        /**
         * Indicates whether to use Vuforia's extended tracking mode. Extended tracking
         * allows tracking to continue even once an image tracker has gone out of view.
         *
         * @see <a href="https://developer.vuforia.com/library/articles/Training/Extended-Tracking">Vuforia Extended Tracking</a>
         */
        public boolean useExtendedTracking = true;

        /**
         * {@link CameraMonitorFeedback} enumerates the kinds of positioning feedback that
         * may be drawn in the camera monitor window.
         * @see #cameraMonitorViewIdParent
         * @see #cameraMonitorFeedback
         */
        public enum CameraMonitorFeedback { NONE, AXES , TEAPOT, BUILDINGS };

        /**
         * Indicates the style of camera monitoring feedback to use. Null indicates that
         * a default feedback style is to be used. {@link CameraMonitorFeedback#NONE None} indicates
         * that the camera monitoring is to be provided, but no feedback is to be drawn thereon.
         * @see CameraMonitorFeedback
         * @see #cameraMonitorViewIdParent
          */
        public CameraMonitorFeedback cameraMonitorFeedback = CameraMonitorFeedback.AXES;

        /**
         * The resource id of the view within {@link #activity} that will be used
         * as the parent for a live monitor which provides feedback as to what the
         * camera is seeing. If both {@link #cameraMonitorViewIdParent} and {@link #cameraMonitorViewParent}
         * are specified, {@link #cameraMonitorViewParent} is used and {@link #cameraMonitorViewIdParent}
         * is ignored. Optional: if no view monitor parent is indicated, then no camera
         * monitoring is provided. The default is zero, which does not indicate a view parent.
         * @see #cameraMonitorViewParent
         * @see #fillCameraMonitorViewParent
         */
        public @IdRes int cameraMonitorViewIdParent = 0;

        /**
         * The view that will be used as the parent for a live monitor which provides
         * feedback as to what the the camera is seeing. If both {@link #cameraMonitorViewIdParent}
         * and {@link #cameraMonitorViewParent} are specified, {@link #cameraMonitorViewParent} is used
         * and {@link #cameraMonitorViewIdParent} is ignored. Optional: if no view monitor parent is
         * indicated, then no camera monitoring is provided. The default is null.
         * @see #cameraMonitorViewIdParent
         * @see #fillCameraMonitorViewParent
         */
        public ViewGroup cameraMonitorViewParent = null;

        /**
         * Controls whether the camera monitor should entirely fill the camera monitor view parent
         * or not. If true, then, depending on the aspect ratios of the camera and the view, some
         * of the camera's data might not be seen. The default is false, which renders the entire
         * camera image in the camera monitor view (if the latter is specified).
         * @see #cameraMonitorViewIdParent
         * @see #cameraMonitorViewParent
         */
        public boolean fillCameraMonitorViewParent = false;

        /**
         * The activity in which the localizer is to run. May be null, in which case
         * the contextually current activity will be used.
         */
        public Activity activity = null;

        /**
         * The resources (if any, may be empty or null) used to provide additional camera calibration data for
         * webcams used with Vuforia. Using calibrated camera helps increase the accuracy of
         * Vuforia image target tracking. Calibration for several cameras is built into the FTC SDK;
         * the optional resources here provides a means for teams to provide calibrations beyond this
         * built-in set.
         *
         * The format required of the XML resource is indicated in the teamwebcamcalibrations
         * resource provided.
         *
         * @see #webcamCalibrationFiles
         */
        public @XmlRes int[] webcamCalibrationResources = new int[] { R.xml.teamwebcamcalibrations };

        /**
         * Camera calibrations resident in files instead of resources.
         * @see #webcamCalibrationResources
         */
        public File[] webcamCalibrationFiles = new File[] {};

        /**
         * Controls the aspect ratios used with webcams. Webcams whose aspect ratio (defined as
         * width / height) is larger than this value will not be used unless no other viable options exist.
         *
         * Example values of some utility include 1920.0 / 1080.0, 640.0 / 480.0, and the like.
         *
         * @see #minWebcamAspectRatio
         */
        public double maxWebcamAspectRatio = Double.MAX_VALUE;

        /**
         * Controls the aspect ratios used with webcams. Webcams whose aspect ratio (defined as
         * width / height) is smaller than this value will not be used unless no other viable options exist.
         *
         * @see #maxWebcamAspectRatio
         */
        public double minWebcamAspectRatio = 0;

        /**
         * How long to wait for USB permissions when connecting to a webcam.
         */
        public int secondsUsbPermissionTimeout = 30;

        public Parameters() {}
        public Parameters(@IdRes int cameraMonitorViewIdParent) { this.cameraMonitorViewIdParent = cameraMonitorViewIdParent; }

        public void addWebcamCalibrationFile(String name)
            {
            addWebcamCalibrationFile(new File(WEBCAM_CALIBRATIONS_DIR, name));
            }

        public void addWebcamCalibrationFile(File file)
            {
            webcamCalibrationFiles = Arrays.copyOf(webcamCalibrationFiles, webcamCalibrationFiles.length + 1);
            webcamCalibrationFiles[webcamCalibrationFiles.length - 1] = file;
            }
        }
    }
