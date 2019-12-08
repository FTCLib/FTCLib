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
package org.firstinspires.ftc.robotcore.internal.camera;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.MovingStatistics;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSequenceId;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraFrame;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraManager;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.BmpFileWriter;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;
import org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider.CameraCallback;
import org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider.CameraMode;
import org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider.VuforiaWebcam;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * A simple illustration of how to use the UVC (Webcam) API. Think of it as providing a wholistic
 * conceptual overview of the flow of opening, rendering, and closing cameras. However, it is old
 * and a bit crufty, doesn't handle the error situations well, etc. If you want a <em>real</em>
 * example of how API actually gets used in practice, I suggest you look at {@link VuforiaWebcam#openCamera()},
 * and {@link VuforiaWebcam#startCamera(CameraMode, CameraCallback)}, as those are the entry points
 * that actually get used in practice when this infrastructure is used with Vuforia. As such, they
 * are much better tested than the conceptual flow here.
 */
@SuppressWarnings("WeakerAccess")
public class WebcamExample
    {
    //----------------------------------------------------------------------------------------------
    // Main
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "WebcamExample";

    public void example(Supplier<Boolean> continueExample)
        {
        CameraManager cameraManager = ClassFactory.getInstance().getCameraManager();
        for (CameraName cameraName : cameraManager.getAllWebcams())
            {
            new OneCameraExample(cameraManager, cameraName, continueExample).example();
            }
        }

    protected class OneCameraExample
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        final CameraManager     cameraManager;
        final CameraName        cameraName;
        final Executor          threadPool = ThreadPool.newSingleThreadExecutor("OneCameraExample");

        /** For now, we'll <em>demand</em> YUY2 format, as we think we know how to manipulate that
         * I *believe* its support is required of all UVC cameras. See USB Device Class Definition
         * for Video Devices: Uncompressed Payload, Table 2-1 */
        int                     imageFormatWanted = ImageFormat.YUY2;
        Size                    sizeWanted;
        Camera                  camera;
        CameraCharacteristics   characteristics;

        File                    bitmapOutputDir = new File(AppUtil.FIRST_FOLDER, "webcam");

        Supplier<Boolean> exampleShouldContinue;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        public OneCameraExample(CameraManager cameraManager, CameraName cameraName, Supplier<Boolean> exampleShouldContinue)
            {
            this.cameraManager = cameraManager;
            this.cameraName = cameraName;
            AppUtil.getInstance().ensureDirectoryExists(bitmapOutputDir);
            this.exampleShouldContinue = exampleShouldContinue != null
                    ? exampleShouldContinue
                    : new Supplier<Boolean>()
                        {
                        @Override public Boolean get()
                            {
                            return true;
                            }
                        };
            }

        //------------------------------------------------------------------------------------------
        // Example
        //------------------------------------------------------------------------------------------

        public void example()
            {
            /**
             * We have to ask for permission to use the camera, which may involved interacting
             * with the user and so may take a while. The result of that request will be delivered
             * by calling 'accept(permissionGranted)' on a worker thread in the default thread pool.
             * Alternately, the result could be delivered using a {@link android.os.Handler} instead
             * (that is, on the UI thread, for example) which may be simpler in some situations,
             * depending on application structure.
             */
            Deadline deadline = new Deadline(10, TimeUnit.SECONDS);
            cameraName.asyncRequestCameraPermission(AppUtil.getDefContext(), deadline, Continuation.create(threadPool, new Consumer<Boolean>()
                {
                @Override public void accept(Boolean permissionGranted)
                    {
                    if (permissionGranted)
                        {
                        /* For fun and profit, log a summary of the supported configurations */
                        characteristics = cameraName.getCameraCharacteristics();
                        for (String line : characteristics.toString().split("\\n"))
                            {
                            RobotLog.vv(TAG, line);
                            }

                        /** Open the camera and create a capture session. Flow continues in
                         * the captureStateCallback that we provide. */
                        cameraManager.asyncOpenCameraAssumingPermission(cameraName, Continuation.create(threadPool, new Camera.StateCallbackDefault()
                            {
                            // TODO: error handling could be improved here
                            @Override public void onOpened(@NonNull final Camera camera)
                                {
                                RobotLog.vv(TAG, "camera opened: %s", camera);
                                OneCameraExample.this.camera = camera;

                                if (Misc.contains(characteristics.getAndroidFormats(), imageFormatWanted))
                                    {
                                    sizeWanted = characteristics.getDefaultSize(imageFormatWanted);
                                    try {
                                        camera.createCaptureSession(Continuation.create(threadPool, captureStateCallback));
                                        }
                                    catch (CameraException e)
                                        {
                                        RobotLog.ee(TAG, e, "error creating capture session");
                                        }
                                    }
                                else
                                    {
                                    RobotLog.ee(TAG, "camera doesn't support desired format: 0x%02x", imageFormatWanted);
                                    }
                                }

                            @Override public void onClosed(@NonNull Camera camera)
                                {
                                RobotLog.vv(TAG, "camera reports closed: %s", camera);
                                }
                            }), 10, TimeUnit.SECONDS);
                        }
                    }
                }));
            }

        CameraCaptureSession.StateCallback captureStateCallback = new CameraCaptureSession.StateCallbackDefault()
            {
            @Override public void onConfigured(@NonNull CameraCaptureSession session)
                {
                try {
                    /** Indicate <em>how</em> we want to stream. */
                    final CameraCaptureRequest cameraCaptureRequest = camera.createCaptureRequest(imageFormatWanted, sizeWanted, characteristics.getMaxFramesPerSecond(imageFormatWanted, sizeWanted));

                    /** Start streaming! Flow continues in the captureCallback. */
                    CameraCaptureSequenceId cameraCaptureSequenceId = session.startCapture(cameraCaptureRequest,
                            new WebcamExampleCaptureCallback(cameraCaptureRequest), // callback, not continuation; avoids copying frame
                            Continuation.create(threadPool, new CameraCaptureSession.StatusCallback()
                                {
                                @Override public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, CameraCaptureSequenceId cameraCaptureSequenceId, long lastFrameNumber)
                                    {
                                    RobotLog.vv(TAG, "capture sequence %s reports completed: lastFrame=%d", cameraCaptureSequenceId, lastFrameNumber);
                                    }
                                }));

                    // Put up a dialog, streaming until the user dismisses it or we're not supposed to continue
                    AppUtil.DialogParams params = new AppUtil.DialogParams(UILocation.ONLY_LOCAL, "Streaming Active", "Press OK to stop");
                    try {
                        final CountDownLatch latch = new CountDownLatch(1);
                        AppUtil.DialogContext dialogContext = AppUtil.getInstance().showDialog(params, ThreadPool.getDefault(), new Consumer<AppUtil.DialogContext>()
                            {
                            @Override public void accept(AppUtil.DialogContext dialogContext)
                                {
                                latch.countDown();
                                }
                            });
                        try {
                            for (;;)
                                {
                                if (!exampleShouldContinue.get() || latch.await(100, TimeUnit.MILLISECONDS))
                                    {
                                    RobotLog.vv(TAG, "capture termination requested");
                                    break;
                                    }
                                }
                            }
                        catch (InterruptedException e)
                            {
                            Thread.currentThread().interrupt();
                            }
                        finally
                            {
                            // Ensure the dialog is gone, even in the case where the user didn't dismiss it
                            dialogContext.dismissDialog();
                            }
                        }
                    finally
                        {
                        // Shutdown the camera
                        session.stopCapture();
                        session.close();
                        camera.close();
                        }
                    }
                catch (CameraException e)
                    {
                    RobotLog.ee(TAG, e, "error setting repeat capture request");
                    }
                }

            @Override public void onClosed(@NonNull CameraCaptureSession session)
                {
                RobotLog.vv(TAG, "capture session reports closed: %s", session);
                }
            };

        class WebcamExampleCaptureCallback implements CameraCaptureSession.CaptureCallback
            {
            final @NonNull Bitmap bitmap;
            MovingStatistics nsIntervalRollingAverage = new MovingStatistics(90);
            ElapsedTime timer = null;

            WebcamExampleCaptureCallback(CameraCaptureRequest cameraCaptureRequest)
                {
                bitmap = cameraCaptureRequest.createEmptyBitmap();
                }

            /**
             * The frame data has <em>not</em> been copied automatically for us, and we can only access it
             * for the duration of the callback. If we wish to hold on to it longer than that, we must
             * either make a copy manually or choose not to uze the optimized thread pool.
             * @see CameraFrame#copy()
             */
            @Override public void onNewFrame(@NonNull CameraCaptureSession session, @NonNull CameraCaptureRequest request, @NonNull final CameraFrame cameraFrame)
                {
                /* Report on how we're doing */
                int fps = 0;
                if (timer != null)
                    {
                    long nsInterval = timer.nanoseconds();
                    timer.reset();
                    nsIntervalRollingAverage.add(nsInterval);
                    double secondsPerFrame = nsIntervalRollingAverage.getMean() / (double)ElapsedTime.SECOND_IN_NANO;
                    fps = (int)Math.round(1/secondsPerFrame);
                    }
                else
                    timer = new ElapsedTime();
                //
                RobotLog.vv(TAG, "captured frame#=%d size=%s cb=%d fps=%d", cameraFrame.getFrameNumber(), cameraFrame.getSize(), cameraFrame.getImageSize(), fps);

                /**
                 * Call {@link CameraFrame#copyToBitmap} to retrieve the frame data in a format
                 * in which it's useful to do stuff with in other parts of Android. This is reasonably
                 * fast; a good deal of work has gone into its optimization.
                 *
                 * Note that we reuse the same {@link Bitmap} object for every frame. We do this to avoid
                 * unnecessary memory pressure. But that's only possible because we do all of our
                 * processing during the callback itself; if we posted to a worker queue, for example,
                 * we'd probably have to copy the bitmap (or the underlying {@link CameraFrame}).
                 */
                cameraFrame.copyToBitmap(bitmap);

                /*
                 * One fun thing to do with the data is just save it to a file. In your use, you'd
                 * probably want to do something different. Note that saving to a file is VERY SLOW,
                 * and we miss several frames while we're busy doing tha.
                 */
                saveBitmap(cameraFrame.getFrameNumber(), bitmap, null);
                }
            };

        /**
         * Saves a {@link Bitmap} to a file, either in a compressed format, or as a Windows .bmp file.
         */
        public void saveBitmap(long frameNumber, Bitmap bitmap, @Nullable Bitmap.CompressFormat compressFormat)
            {
            String extension = ".bmp";
            if (compressFormat != null)
                {
                switch (compressFormat)
                    {
                    case JPEG: extension = ".jpg"; break;
                    case PNG: extension  = ".png"; break;
                    case WEBP: extension = ".webp"; break;
                    }
                }

            File file = new File(bitmapOutputDir, Misc.formatInvariant("uvc-%d" + extension, frameNumber));
            try {
                if (compressFormat != null)
                    {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                    bitmap.compress(compressFormat, 100, bufferedOutputStream);
                    bufferedOutputStream.close();
                    fileOutputStream.close();
                    }
                else
                    {
                    BmpFileWriter bmpFileWriter = new BmpFileWriter(bitmap);
                    bmpFileWriter.save(file);
                    }
                }
            catch (IOException e)
                {
                RobotLog.ee(TAG, e, "failed to save bitmap to %s", file);
                }
            }
        }
    }
