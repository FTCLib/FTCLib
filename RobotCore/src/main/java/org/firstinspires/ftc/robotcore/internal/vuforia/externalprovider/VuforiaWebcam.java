/*
Copyright (c) 2018 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.ImageFormat;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.XmlRes;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.external.function.ThrowingSupplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSequenceId;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraFrame;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.FocusControl;
import org.firstinspires.ftc.robotcore.internal.camera.CameraFrameInternal;
import org.firstinspires.ftc.robotcore.internal.camera.CameraInternal;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerInternal;
import org.firstinspires.ftc.robotcore.internal.camera.ImageFormatMapper;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationIdentity;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationManager;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiCameraFrame;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiExposureControl;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiFocusControl;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.constants.UvcFrameFormat;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.VuforiaExternalProviderCameraFrame;
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.ContinuationSynchronizer;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.firstinspires.ftc.robotcore.internal.system.NativeObject;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;
import org.firstinspires.ftc.robotcore.internal.vuforia.VuforiaException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * {@link VuforiaWebcam} is the java side of the vuforia external camera support. Instances of
 * this class exist one-to-one with instances of {@link NativeVuforiaWebcam}
 */
@SuppressWarnings("WeakerAccess")
public class VuforiaWebcam implements VuforiaWebcamInternal, VuforiaWebcamNativeCallbacks
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "VuforiaWebcam";
    public static boolean TRACE = true;
    public static boolean TRACE_VERBOSE = false;
    protected Tracer tracer = Tracer.create(TAG, TRACE);
    protected Tracer verboseTracer = Tracer.create(TAG, TRACE_VERBOSE);

    protected final Object                          lock = new Object();
    protected final CameraManagerInternal           cameraManager;
    protected final Executor                        serialThreadPool;
    protected final CameraCalibrationManager        calibrationManager;
    protected final double                          minAspectRatio;
    protected final double                          maxAspectRatio;
    protected final String                          externalCameraLib = "libRobotCore.so";
    protected NativeVuforiaWebcam                   nativeVuforiaWebcam = null;
    protected Continuation<? extends Consumer<CameraFrame>> getFrameOnce = null;
    protected final int                             secondsPermissionTimeout;

    protected final CameraName                      cameraName;
    protected Camera                                cameraTemplate;
    protected CameraCharacteristics                 cameraCharacteristics = null;
    protected List<CameraMode>                      cameraModesForVuforia = new ArrayList<>();

    protected Camera                                camera = null;
    protected CameraCalibration                     cameraCalibrationCache = null;
    protected CameraCaptureSession                  cameraCaptureSession = null;
    protected long                                  nsFrameExposureCache = ExposureControl.unknownExposure;
    protected long                                  msFrameExposureCacheRefresh = 750;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected VuforiaWebcam(@XmlRes int[] webcamCalibrationResources, File[] webcamCalibrationFiles, double minRatio, double maxRatio, int secondsPermissionTimeout, @Nullable Camera camera, @Nullable CameraName cameraName)
        {
        this.secondsPermissionTimeout = secondsPermissionTimeout;
        this.cameraManager = (CameraManagerInternal)ClassFactory.getInstance().getCameraManager();
        this.serialThreadPool = cameraManager.getSerialThreadPool();
        this.cameraTemplate = camera==null ? null : camera.dup();
        this.minAspectRatio = minRatio;
        this.maxAspectRatio = maxRatio;
        this.cameraName = cameraName;

        List<XmlPullParser> parsers = new ArrayList<>();
        List<FileInputStream> inputStreams = new ArrayList<>();
        try {
            // Get XmlPullParsers for everything
            for (@XmlRes int calibrationsResource : webcamCalibrationResources)
                {
                Resources resources = AppUtil.getDefContext().getResources();
                try {
                    XmlResourceParser parser = resources.getXml(calibrationsResource);
                    parsers.add(parser);
                    }
                catch (RuntimeException e)
                    {
                    RobotLog.ee(TAG, e, "exception opening XML resource %d; ignoring", calibrationsResource);
                    }
                }
            for (File file : webcamCalibrationFiles)
                {
                try {
                    FileInputStream inputStream = new FileInputStream(file);
                    inputStreams.add(inputStream);
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(inputStream, null);
                    parsers.add(parser);
                    }
                catch (FileNotFoundException|XmlPullParserException|RuntimeException e)
                    {
                    RobotLog.ee(TAG, e, "exception opening XML file %s; ignoring", file);
                    }
                }

            // Build calibrations using the parsers
            this.calibrationManager = new CameraCalibrationManager(parsers);
            }
        finally
            {
            // Close down all the parsers
            for (XmlPullParser parser : parsers)
                {
                if (parser instanceof XmlResourceParser)
                    {
                    ((XmlResourceParser)parser).close();
                    }
                }
            for (FileInputStream fileInputStream : inputStreams)
                {
                try {fileInputStream.close(); } catch (IOException e) {/*ignore*/}
                }
            }
        }

    public VuforiaWebcam(@XmlRes int[] webcamCalibrationResources, File[] webcamCalibrationFiles, double minRatio, double maxRatio, int secondsPermissionTimeout, @NonNull Camera camera)
        {
        this(webcamCalibrationResources, webcamCalibrationFiles, minRatio, maxRatio, secondsPermissionTimeout, camera, null);
        }
    public VuforiaWebcam(@XmlRes int[] webcamCalibrationResources, File[] webcamCalibrationFiles, double minRatio, double maxRatio, int secondsPermissionTimeout, @NonNull CameraName cameraName)
        {
        this(webcamCalibrationResources, webcamCalibrationFiles, minRatio, maxRatio, secondsPermissionTimeout, null, cameraName);
        }

    //----------------------------------------------------------------------------------------------
    // Vuforia life cycle
    //----------------------------------------------------------------------------------------------

    @Override public boolean preVuforiaInit()
        {
        boolean success = createNativeVuforiaWebcam();
        if (success && cameraName != null)
            {
            /**
             * There's something that goes on inside of Vuforia.init() such that even though a
             * permissions dialog might get accepted quickly, the response doesn't get delivered
             * (and allow {@link AppUtil#asyncRequestUsbPermission} to convey that) until many,
             * many seconds later. We don't exactly know what's going on, how exactly something
             * in the init could be blocking things, but it's easier here for now just to ask on the
             * <em>outside</em> before we do the init, and all seems to be well.
             * TODO: Get to the bottom of what's going on.
             */
            success = cameraName.requestCameraPermission(new Deadline(secondsPermissionTimeout, TimeUnit.SECONDS));
            }
        return success;
        }

    protected boolean isExternalCamera()
        {
        return cameraName != null || cameraTemplate != null;
        }

    public boolean createNativeVuforiaWebcam()
        {
        boolean success = true;
        long pointer = nativePreVuforiaInit(isExternalCamera() ? externalCameraLib : null);
        if (pointer != 0)
            {
            nativeVuforiaWebcam = new NativeVuforiaWebcam(pointer); // ASDKDFKLE
            }
        else
            {
            RobotLog.ee(TAG, "nativeCreateNativeVuforiaWebcam() failed");
            success = false;
            }
        return success;
        }

    @Override public void postVuforiaInit()
        {
        synchronized (lock)
            {
            if (nativeVuforiaWebcam != null)
                {
                nativeVuforiaWebcam.postVuforiaInit();
                }
            }
        }

    @Override public void preVuforiaDeinit()
        {
        synchronized (lock)
            {
            if (nativeVuforiaWebcam != null)
                {
                nativeVuforiaWebcam.preVuforiaDeinit();
                }
            }
        }

    @Override public void postVuforiaDeinit()
        {
        synchronized (lock)
            {
            nativePostVuforiaDeinit();
            if (nativeVuforiaWebcam != null)
                {
                nativeVuforiaWebcam.releaseRef(); // ASDKDFKLE
                nativeVuforiaWebcam = null;
                }
            if (cameraTemplate != null)
                {
                cameraTemplate.close();
                cameraTemplate = null;
                }
            }
        }

    @Override public CameraName getCameraName()
        {
        synchronized (lock)
            {
            if (camera != null)
                {
                return camera.getCameraName();
                }
            if (cameraTemplate != null)
                {
                return cameraTemplate.getCameraName();
                }
            return cameraName;
            }
        }

    @Override public void getFrameOnce(Continuation<? extends Consumer<CameraFrame>> getFrameOnce)
        {
        synchronized (lock)
            {
            this.getFrameOnce = getFrameOnce;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Opening and closing
    //----------------------------------------------------------------------------------------------

    public @Nullable Camera getCamera()
        {
        synchronized (lock)
            {
            return camera;
            }
        }

    protected boolean openCamera() throws CameraException
        {
        return tracer.trace("openCamera()", new ThrowingSupplier<Boolean, CameraException>()
            {
            @Override public Boolean get() throws CameraException
                {
                boolean success = false;
                synchronized (lock)
                    {
                    try {
                        if (cameraTemplate != null)
                            {
                            camera = cameraTemplate.dup();
                            }
                        else
                            {
                            camera = cameraManager.requestPermissionAndOpenCamera(new Deadline(secondsPermissionTimeout, TimeUnit.SECONDS), cameraName, null);
                            }
                        if (camera != null)
                            {
                            cameraCharacteristics = camera.getCameraName().getCameraCharacteristics();
                            computeCameraModesForVuforia();
                            success = true;
                            }
                        else
                            {
                            cameraCharacteristics = cameraName.getCameraCharacteristics();
                            cameraModesForVuforia = new ArrayList<>();
                            }
                        }
                    catch (RuntimeException e)
                        {
                        camera = null;
                        throw e;
                        }
                    return success;
                    }
                }
            });
        }

    protected void computeCameraModesForVuforia()
        {
        Assert.assertNotNull(camera);
        List<CameraMode> hasSupportedFormatList = filterSupportedFormats();

        List<CameraMode> result = new ArrayList<>();
        if (result.isEmpty()) result = filterAspectRatios(filterHasCalibration(hasSupportedFormatList));
        if (result.isEmpty()) result = filterAspectRatios(hasSupportedFormatList);
        if (result.isEmpty()) result = filterHasCalibration(hasSupportedFormatList);
        if (result.isEmpty()) result = hasSupportedFormatList;

        cameraModesForVuforia = result;
        }

    protected List<CameraMode> filterAspectRatios(List<CameraMode> cameraModes)
        {
        List<CameraMode> result = new ArrayList<>();
        for (CameraMode cameraMode : cameraModes)
            {
            if (isLegalAspectRatio(cameraMode.getSize()))
                {
                result.add(cameraMode);
                }
            }
        return result;
        }

    protected boolean isLegalAspectRatio(Size size)
        {
        double aspectRatio = (double)size.getWidth() / (double)size.getHeight();
        return minAspectRatio <= aspectRatio && aspectRatio <= maxAspectRatio;
        }

    protected List<CameraMode> filterHasCalibration(List<CameraMode> cameraModes)
        {
        List<CameraMode> result = new ArrayList<>();
        for (CameraMode cameraMode : cameraModes)
            {
            if (((CameraInternal)camera).hasCalibration(calibrationManager, cameraMode.getSize()))
                {
                result.add(cameraMode);
                }
            }
        return result;
        }

    protected List<CameraMode> filterSupportedFormats()
        {
        ArrayList<CameraMode> hasSupportedFormatList = new ArrayList<>();
        for (CameraCharacteristics.CameraMode mode : cameraCharacteristics.getAllCameraModes())
            {
            verboseTracer.trace("supported cameraMode: %s", mode);

            // Vuforia external cameras currently (2018.05.05) support only a very limited set of formats
            // (i.e.: just one). If the mode from the camera isn't one of those, we ignore it.
            switch (mode.androidFormat)
                {
                case ImageFormat.YUY2:
                    CameraMode externalMode = new CameraMode(mode.size.getWidth(), mode.size.getHeight(), mode.fps, ImageFormatMapper.vuforiaWebcamFromAndroid(mode.androidFormat));
                    hasSupportedFormatList.add(externalMode);
                    break;
                default:
                    // not supported
                    break;
                }
            }
        return hasSupportedFormatList;
        }

    public boolean closeCamera()
        {
        return tracer.trace("closeCamera()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    stopCamera();
                    Camera cameraToClose = camera;
                    camera = null;
                    if (cameraToClose != null)
                        {
                        cameraToClose.close();
                        }
                    return true;
                    }
                }
            });
        }

    protected CameraCalibration getCalibrationOfCurrentCamera(final CameraMode cameraMode) // call with lock held
        {
        CameraCalibrationIdentity currentIdentity = camera==null ? null : ((CameraInternal)camera).getCalibrationIdentity();
        if (camera != null)
            {
            if (currentIdentity != null)
                {
                if (cameraCalibrationCache==null || !(currentIdentity.equals(cameraCalibrationCache.getIdentity()) && cameraMode.getSize().equals(cameraCalibrationCache.getSize())))
                    {
                    cameraCalibrationCache = ((CameraInternal)camera).getCalibration(calibrationManager, cameraMode.getSize());
                    }
                }
            else
                cameraCalibrationCache = null;
            }
        if (cameraCalibrationCache==null)
            {
            // If we don't have a calibration on record, then just use a generic one that indicates to Vuforia that no calibration data is available.
            cameraCalibrationCache = CameraCalibration.forUnavailable(currentIdentity, cameraMode.getSize());
            }
        return cameraCalibrationCache;
        }

    protected Boolean startCamera(final CameraMode cameraMode, final CameraCallback vuforiaCameraCallbackParam)
        {
        return tracer.trace("start(" + cameraMode + ")", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    stopCamera();

                    final ContinuationSynchronizer<CameraCaptureSession> synchronizer = new ContinuationSynchronizer<>(secondsPermissionTimeout, TimeUnit.SECONDS, TRACE);

                    try {
                        camera.createCaptureSession(Continuation.create(serialThreadPool, new CameraCaptureSession.StateCallback()
                            {
                            // Hold the callback we use to get back to Vuforia for as long as the session is open
                            CameraCallback vuforiaCameraCallback = null;

                            @Override public void onConfigured(@NonNull CameraCaptureSession session)
                                {
                                tracer.trace("capture session %s reports configured", session);
                                // We'll set the session into the synchronizer at the end of this method if
                                // we don't encounter any intervening failures.
                                this.vuforiaCameraCallback = vuforiaCameraCallbackParam;
                                this.vuforiaCameraCallback.addRef(); // alaksdjfasdkl

                                try {
                                    /** Indicate <em>how</em> we want to stream. */
                                    final CameraCaptureRequest cameraCaptureRequest = camera.createCaptureRequest(
                                            cameraMode.getAndroidFormat(),
                                            cameraMode.getSize(),
                                            cameraMode.getFramesPerSecond());

                                    /** Start streaming! */
                                    CameraCaptureSequenceId cameraCaptureSequenceId = session.startCapture(cameraCaptureRequest,
                                            new CameraCaptureSession.CaptureCallback() // raw callback, so will avoid copying frame
                                                {
                                                @Override public void onNewFrame(@NonNull CameraCaptureSession session, @NonNull CameraCaptureRequest request, @NonNull CameraFrame cameraFrame)
                                                    {
                                                    // tracer.trace("captured frame#=%d size=%s cb=%d fps=%d", capturedFrame.getFrameNumber(), capturedFrame.getSize(), capturedFrame.getImageSize(), fps);
                                                    if (nativeVuforiaWebcam != null)
                                                        {
                                                        CameraCalibration calibration = getCalibrationOfCurrentCamera(cameraMode);
                                                        nativeVuforiaWebcam.deliverFrameToVuforia(vuforiaCameraCallback, cameraFrame, calibration.toArray());
                                                        }

                                                    Continuation<? extends Consumer<CameraFrame>> capturedOneShot = null;
                                                    synchronized (lock)
                                                        {
                                                        capturedOneShot = getFrameOnce;
                                                        getFrameOnce = null;
                                                        }
                                                    if (capturedOneShot != null)
                                                        {
                                                        final CameraFrame copiedFrame = cameraFrame.copy(); // klasdjfalkjasdf
                                                        capturedOneShot.dispatch(new ContinuationResult<Consumer<CameraFrame>>()
                                                            {
                                                            @Override public void handle(Consumer<CameraFrame> cameraFrameConsumer)
                                                                {
                                                                cameraFrameConsumer.accept(copiedFrame);
                                                                copiedFrame.releaseRef(); // klasdjfalkjasdf
                                                                }
                                                            });
                                                        }
                                                    }
                                                },
                                            Continuation.create(serialThreadPool, new CameraCaptureSession.StatusCallback()
                                                {
                                                @Override public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, CameraCaptureSequenceId cameraCaptureSequenceId, long lastFrameNumber)
                                                    {
                                                    tracer.trace("capture sequence %s reports completed: lastFrame=%d", cameraCaptureSequenceId, lastFrameNumber);
                                                    }
                                                }));
                                    }
                                catch (CameraException|RuntimeException e)
                                    {
                                    RobotLog.ee(TAG, e, "exception setting repeat capture request: closing session: %s", session);
                                    session.close();
                                    session = null;
                                    }

                                synchronizer.finish("onConfigured", session);
                                }

                            @Override public void onClosed(@NonNull CameraCaptureSession session)
                                {
                                tracer.trace("capture session reports closed: %s", session);
                                if (vuforiaCameraCallback != null)
                                    {
                                    vuforiaCameraCallback.releaseRef(); // alaksdjfasdkl
                                    vuforiaCameraCallback = null;
                                    }
                                }
                            }));
                        }
                    catch (CameraException|RuntimeException e)
                        {
                        tracer.traceError(e, "exception starting capture: %s", camera);
                        synchronizer.finish("exception starting capture", null);
                        }

                    // Wait for the above to complete
                    try {
                        synchronizer.await("camera start");
                        }
                    catch (InterruptedException e)
                        {
                        Thread.currentThread().interrupt();
                        }

                    cameraCaptureSession = synchronizer.getValue();
                    return cameraCaptureSession != null;
                    }
                }
            });
        }

    public Boolean stopCamera()
        {
        synchronized (lock)
            {
            final CameraCaptureSession cameraCaptureSession = VuforiaWebcam.this.cameraCaptureSession;
            VuforiaWebcam.this.cameraCaptureSession = null;

            boolean success = false;
            if (cameraCaptureSession != null)
                {
                tracer.trace("stop()", new Runnable()
                    {
                    @Override public void run()
                        {
                        cameraCaptureSession.stopCapture();
                        cameraCaptureSession.close();
                        }
                    });
                success = true;
                }
            return success;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected static void throwFailure(String format, Object... args)
        {
        throw new VuforiaException(format, args);
        }

    //------------------------------------------------------------------------------------------
    // Callbacks from native code (see NativeVuforiaWebcam.cpp)
    //------------------------------------------------------------------------------------------

    /**
     * @return success or failure
     */
    @Override public boolean nativeCallbackOpen()
        {
        return tracer.trace("nativeCallbackOpen()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    try {
                        return openCamera();
                        }
                    catch (CameraException|RuntimeException e)
                        {
                        tracer.traceError(e, "exception in nativeCallbackOpen(); ignored");
                        }
                    }
                return false;
                }
            });
        }

    /**
     * @return the number of camera modes supported that are useful to report to vuforia
     * @see #nativeCallbackGetSupportedCameraMode(int)
     */
    @Override public int nativeCallbackGetNumSupportedCameraModes()
        {
        return verboseTracer.trace("nativeCallbackGetNumSupportedCameraModes", new Supplier<Integer>()
            {
            @Override public Integer get()
                {
                synchronized (lock)
                    {
                    return cameraModesForVuforia.size();
                    }
                }
            });
        }

    /**
     * @return the data of the index'th supported camera mode
     * @see #nativeCallbackGetNumSupportedCameraModes()
     */
    @Override public int[] nativeCallbackGetSupportedCameraMode(int index)
        {
        synchronized (lock)
            {
            CameraMode cameraMode = cameraModesForVuforia.get(index);
            // tracer.trace("getSupportedCameraMode(%d)=%s", index, cameraMode);
            return cameraMode.toArray();
            }
        }

    @Override public boolean nativeCallbackStart(final int[] cameraModeData, final long pointerVuforiaEngineFrameCallback)
        {
        return tracer.trace("nativeCallbackStart()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    if (camera != null)
                        {
                        CameraMode cameraMode = new CameraMode(cameraModeData);
                        CameraCallback cameraCallback = new CameraCallback(pointerVuforiaEngineFrameCallback); // adklajasdf
                        try {
                            return startCamera(cameraMode, cameraCallback);
                            }
                        finally
                            {
                            cameraCallback.releaseRef(); // adklajasdf
                            }
                        }
                    return false;
                    }
                }
            });
        }

    @Override public boolean nativeCallbackStop()
        {
        return tracer.trace("nativeCallbackStop()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    return stopCamera();
                    }
                }
            });
        }

    @Override public boolean nativeCallbackClose()
        {
        return tracer.trace("nativeCallbackClose()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    return closeCamera();
                    }
                }
            });
        }

    @Override public boolean nativeCallbackIsFocusModeSupported(final int vuforiaFocusMode)
        {
        return verboseTracer.trace("nativeCallbackIsFocusModeSupported()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    FocusControl focusControl = camera.getControl(FocusControl.class);
                    if (focusControl != null)
                        {
                        return focusControl.isModeSupported(UvcApiFocusControl.fromVuforia(vuforiaFocusMode));
                        }
                    return false;
                    }
                }
            });
        }

    @Override public int nativeCallbackGetFocusMode()
        {
        return verboseTracer.trace("nativeCallbackGetFocusMode()", new Supplier<Integer>()
            {
            @Override public Integer get()
                {
                synchronized (lock)
                    {
                    FocusControl focusControl = camera.getControl(FocusControl.class);
                    FocusMode result = FocusMode.UNKNOWN;
                    if (focusControl != null)
                        {
                        result = UvcApiFocusControl.toVuforia(focusControl.getMode());
                        }
                    return result.ordinal();
                    }
                }
            });
        }

    @Override public boolean nativeCallbackSetFocusMode(final int vuforiaFocusMode)
        {
        return verboseTracer.trace("nativeCallbackSetFocusMode", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    FocusControl focusControl = camera.getControl(FocusControl.class);
                    if (focusControl != null)
                        {
                        FocusControl.Mode mode = UvcApiFocusControl.fromVuforia(vuforiaFocusMode);
                        return focusControl.setMode(mode);
                        }
                    return false;
                    }
                }
            });
        }

    @Override public boolean nativeCallbackIsFocusLengthSupported()
        {
        return verboseTracer.trace("nativeCallbackIsFocusLengthSupported()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    FocusControl focusControl = camera.getControl(FocusControl.class);
                    if (focusControl != null)
                        {
                        return focusControl.isFocusLengthSupported();
                        }
                    return false;
                    }
                }
            });
        }

    @Override public double nativeCallbackGetMinFocusLength()
        {
        return verboseTracer.trace("nativeCallbackGetMinFocusLength()", new Supplier<Double>()
            {
            @Override public Double get()
                {
                synchronized (lock)
                    {
                    FocusControl focusControl = camera.getControl(FocusControl.class);
                    if (focusControl != null)
                        {
                        return focusControl.getMinFocusLength();
                        }
                    return FocusControl.unknownFocusLength;
                    }
                }
            });
        }

    @Override public double nativeCallbackGetMaxFocusLength()
        {
        return verboseTracer.trace("nativeCallbackGetMaxFocusLength()", new Supplier<Double>()
            {
            @Override public Double get()
                {
                synchronized (lock)
                    {
                    FocusControl focusControl = camera.getControl(FocusControl.class);
                    if (focusControl != null)
                        {
                        return focusControl.getMaxFocusLength();
                        }
                    return FocusControl.unknownFocusLength;
                    }
                }
            });
        }

    @Override public double nativeCallbackGetFocusLength()
        {
        return verboseTracer.trace("nativeCallbackGetFocusLength()", new Supplier<Double>()
            {
            @Override public Double get()
                {
                synchronized (lock)
                    {
                    FocusControl focusControl = camera.getControl(FocusControl.class);
                    if (focusControl != null)
                        {
                        return focusControl.getFocusLength();
                        }
                    return FocusControl.unknownFocusLength;
                    }
                }
            });
        }

    @Override public boolean nativeCallbackSetFocusLength(final double focusLength)
        {
        return verboseTracer.trace("nativeCallbackSetFocusLength()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    FocusControl focusControl = camera.getControl(FocusControl.class);
                    if (focusControl != null)
                        {
                        return focusControl.setFocusLength(focusLength);
                        }
                    return false;
                    }
                }
            });
        }

    //----------------------------------------------------------------------------------------------

    @Override public boolean nativeCallbackIsExposureModeSupported(final int vuforiaMode)
        {
        return verboseTracer.traceResult("nativeCallbackIsExposureModeSupported()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    ExposureControl exposureControl = camera.getControl(ExposureControl.class);
                    if (exposureControl != null)
                        {
                        ExposureControl.Mode mode = UvcApiExposureControl.fromVuforia(vuforiaMode);
                        boolean result = exposureControl.isModeSupported(mode);
                        if (!result && mode == ExposureControl.Mode.Auto)
                            {
                            // Vuforia likes to combine these two modes. We keep them separate until the last minute
                            // PTC: "Both usb exposure mode AUTO and APERTURE PRIORITY are essentially continuous auto exposure"
                            result = exposureControl.isModeSupported(ExposureControl.Mode.AperturePriority);
                            }
                        return result;
                        }
                    return false;
                    }
                }
            });
        }

    @Override public int nativeCallbackGetExposureMode()
        {
        return verboseTracer.traceResult("nativeCallbackGetExposureMode()", new Supplier<Integer>()
            {
            @Override public Integer get()
                {
                synchronized (lock)
                    {
                    ExposureControl exposureControl = camera.getControl(ExposureControl.class);
                    if (exposureControl != null)
                        {
                        ExposureControl.Mode mode = exposureControl.getMode();
                        if (mode == ExposureControl.Mode.AperturePriority)
                            {
                            mode = ExposureControl.Mode.Auto;
                            }
                        return UvcApiExposureControl.toVuforia(mode).ordinal();
                        }
                    return ExtendedExposureMode.UNKNOWN.ordinal();
                    }
                }
            });
        }

    @Override public boolean nativeCallbackSetExposureMode(final int vuforiaMode)
        {
        return verboseTracer.traceResult("nativeCallbackSetExposureMode", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    ExposureControl exposureControl = camera.getControl(ExposureControl.class);
                    if (exposureControl != null)
                        {
                        ExposureControl.Mode mode = UvcApiExposureControl.fromVuforia(vuforiaMode);
                        boolean result = exposureControl.setMode(mode);
                        if (!result && mode == ExposureControl.Mode.Auto)
                            {
                            // Vuforia likes to combine these two modes. We keep them separate until the last minute
                            // PTC: "Since both usb exposure mode AUTO and APERTURE PRIORITY are essentially continuous auto exposure
                            // we try both of them and see which one is going through"
                            result = exposureControl.setMode(ExposureControl.Mode.AperturePriority);
                            }
                        return result;
                        }
                    return false;
                    }
                }
            });
        }

    @Override public long nativeCallbackGetMinExposure()
        {
        return verboseTracer.trace("nativeCallbackGetMinExposure()", new Supplier<Long>()
            {
            @Override public Long get()
                {
                synchronized (lock)
                    {
                    ExposureControl exposureControl = camera.getControl(ExposureControl.class);
                    if (exposureControl != null)
                        {
                        return exposureControl.getMinExposure(TimeUnit.NANOSECONDS);
                        }
                    return ExposureControl.unknownExposure;
                    }
                }
            });
        }

    @Override public long nativeCallbackGetMaxExposure()
        {
        return verboseTracer.trace("nativeCallbackGetMinExposure()", new Supplier<Long>()
            {
            @Override public Long get()
                {
                synchronized (lock)
                    {
                    ExposureControl exposureControl = camera.getControl(ExposureControl.class);
                    if (exposureControl != null)
                        {
                        return exposureControl.getMaxExposure(TimeUnit.NANOSECONDS);
                        }
                    return ExposureControl.unknownExposure;
                    }
                }
            });
        }

    @Override public long nativeCallbackGetExposure()
        {
        return verboseTracer.trace("nativeCallbackGetMinExposure()", new Supplier<Long>()
            {
            @Override public Long get()
                {
                synchronized (lock)
                    {
                    ExposureControl exposureControl = camera.getControl(ExposureControl.class);
                    if (exposureControl != null)
                        {
                        return exposureControl.getExposure(TimeUnit.NANOSECONDS);
                        }
                    return ExposureControl.unknownExposure;
                    }
                }
            });
        }

    @Override public boolean nativeCallbackSetExposure(final long nsExposure)
        {
        return verboseTracer.trace("nativeCallbackSetExposure()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    ExposureControl exposureControl = camera.getControl(ExposureControl.class);
                    if (exposureControl != null)
                        {
                        return exposureControl.setExposure(nsExposure, TimeUnit.NANOSECONDS);
                        }
                    return false;
                    }
                }
            });
        }

    @Override public boolean nativeCallbackIsExposureSupported()
        {
        return verboseTracer.trace("nativeCallbackIsExposureSupported()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    ExposureControl exposureControl = camera.getControl(ExposureControl.class);
                    if (exposureControl != null)
                        {
                        return exposureControl.isExposureSupported();
                        }
                    return false;
                    }
                }
            });
        }


    //----------------------------------------------------------------------------------------------
    // NativeVuforiaWebcam
    //----------------------------------------------------------------------------------------------

    /**
     * {@link NativeVuforiaWebcam} is the very thin java-side front end to the C++ NativeVuforiaWebcam
     * found in NativeVuforiaWebcam.h. Our primary reason for being here is to get robust calls to
     * nativeReleaseVuforiaWebcam when the instance is closed or finalized.
     */
    @SuppressWarnings("WeakerAccess")
    class NativeVuforiaWebcam extends NativeObject
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        protected final Map<UvcFrameFormat, FrameFormat> formatMap = new HashMap<>();

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        public NativeVuforiaWebcam(long pointerNativeVuforiaWebcam)
            {
            super(pointerNativeVuforiaWebcam, MemoryAllocator.EXTERNAL);
            for (UvcFrameFormat uvcFrameFormat : UvcFrameFormat.values())
                {
                if (uvcFrameFormat == UvcFrameFormat.UNKNOWN) continue;;
                FrameFormat vuforiaFormat = ImageFormatMapper.vuforiaWebcamFromUvc(uvcFrameFormat);
                formatMap.put(uvcFrameFormat, vuforiaFormat);
                }
            }

        @Override protected void destructor()
            {
            if (pointer != 0)
                {
                nativeReleaseVuforiaWebcam(pointer);
                clearPointer();
                }
            super.destructor();
            }

        //------------------------------------------------------------------------------------------
        // Accessing
        //------------------------------------------------------------------------------------------

        public void postVuforiaInit()
            {
            if (nativePostVuforiaInit(pointer, VuforiaWebcam.this))
                {
                for (ImageFormatMapper.Format format : ImageFormatMapper.all())
                    {
                    nativeNoteAndroidVuforiaExternalFormatMapping(pointer, format.uvc.value, format.vuforiaWebcam.ordinal());
                    }
                }
            else
                throwFailure("nativePostVuforiaInit() failed");
            }

        public void preVuforiaDeinit()
            {
            if (pointer != 0)
                {
                nativePreVuforiaDeinit(pointer);
                }
            }

        /**
         * Return our best estimate of the exposure time used with this camera frame. The only way right now
         * we have to return something useful is to poll the camera, which we'd really like to avoid doing
         * each and every frame, so we poll less frequently and cache the result. This seems reasonable
         * if we don't expect auto-exposure to adjust/change too terribly quickly.
         *
         *  "From: Dobrev, Niki <ndobrev@ptc.com>
         *  Sent: Monday, June 18, 2018 9:38 AM
         *
         *  The exposure value is used internally in some parts Vuforia, but for the Vuforia 7.4 that you
         *  are currently using it's not strictly needed. In case ignoring it brings you any benefit you
         *  can safely set it to zero without any difference from Vuforia side. But please be aware that
         *  this might change in future versions. Future version here meaning Vuforia > 7.4: we won't
         *  change this in any 7.4.x version. Basically if/when this changes in the future, not providing
         *  the exposure time won't break anything, but having it would improve the Vuforia functionalities."
         */
        protected long getFrameExposureTime(CameraFrame cameraFrame)
            {
            ExposureControl exposureControl = camera.getControl(ExposureControl.class);
            if (exposureControl != null)
                {
                nsFrameExposureCache = exposureControl.getCachedExposure(TimeUnit.NANOSECONDS, new MutableReference<Boolean>(), msFrameExposureCacheRefresh, TimeUnit.MILLISECONDS);
                }
            return nsFrameExposureCache;
            }

        public void deliverFrameToVuforia(CameraCallback cameraCallback, CameraFrame cameraFrame, float[] cameraInstrinsicsArray)
            {
            final boolean enableOptimization = true;
            if (enableOptimization && cameraFrame instanceof CameraFrameInternal)
                {
                // This optimization pierces the CameraFrame abstraction to assume that there's a
                // UVC implementation underneath. Ok, so far as it goes, but not what we want to
                // always have to do. The reality is that this optimization might not really be worth
                // it: it was originally coded as the *only* code path, then just not removed.
                UvcApiCameraFrame uvcApiCameraFrame = ((CameraFrameInternal)cameraFrame).getUvcApiCameraFrame();
                if (uvcApiCameraFrame != null)
                    {
                    nativeDeliverFrameToVuforiaUvc(this.pointer, cameraCallback.getPointer(), uvcApiCameraFrame.getPointer(), getFrameExposureTime(cameraFrame), cameraInstrinsicsArray);
                    }
                else
                    tracer.traceError("getUvcApiCameraFrame() failed");
                }
            else
                {
                VuforiaExternalProviderCameraFrame vuforiaFrame = new VuforiaExternalProviderCameraFrame();
                try {
                    // Compare to NativeVuforiaWebcam::deliverFrameToVuforia()
                    Size size = cameraFrame.getSize();
                    vuforiaFrame.setFrameIndex((int)cameraFrame.getFrameNumber());
                    vuforiaFrame.setWidth(size.getWidth());
                    vuforiaFrame.setHeight(size.getHeight());
                    vuforiaFrame.setFormat(formatMap.get(cameraFrame.getUvcFrameFormat()).ordinal());
                    vuforiaFrame.setStride(cameraFrame.getStride());
                    vuforiaFrame.setBuffer(cameraFrame.getImageBuffer());
                    vuforiaFrame.setBufferSize(cameraFrame.getImageSize());
                    vuforiaFrame.setTimestamp(cameraFrame.getCaptureTime());
                    vuforiaFrame.setExposureTime(getFrameExposureTime(cameraFrame));
                    // intrinsics will be filled in in native code (just an easier, existing code path)
                    nativeDeliverFrameToVuforiaVuforia(this.pointer, cameraCallback.getPointer(), vuforiaFrame.getPointer(), cameraInstrinsicsArray);
                    }
                finally
                    {
                    vuforiaFrame.releaseRef();
                    }
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Native APIs
    //----------------------------------------------------------------------------------------------

    protected native long nativePreVuforiaInit(String libraryName); // 0/null returned on failure
    protected native void nativeReleaseVuforiaWebcam(long pointer);
    protected native boolean nativePostVuforiaInit(long pointer, VuforiaWebcamNativeCallbacks vuforiaWebcamNativeCallback);
    protected native boolean nativePreVuforiaDeinit(long pointer);
    protected native void nativePostVuforiaDeinit();

    protected native void nativeNoteAndroidVuforiaExternalFormatMapping(long pointer, int uvcFrameFormat, int vuforiaExternal);
    protected native void nativeNoteCameraIntrinsics(long pointer, float[] intrinsicsData);
    protected native void nativeDeliverFrameToVuforiaUvc(long pointer, long pointerCameraCallback, long pointerUvcFrame, long exposureTime, float[] cameraInstrinsicsArray);
    protected native void nativeDeliverFrameToVuforiaVuforia(long pointer, long pointerCameraCallback, long pointerVuforiaFrame, float[] cameraInstrinsicsArray);
    }
