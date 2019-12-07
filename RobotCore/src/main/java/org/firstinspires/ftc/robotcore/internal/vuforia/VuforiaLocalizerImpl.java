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
package org.firstinspires.ftc.robotcore.internal.vuforia;

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.vuforia.COORDINATE_SYSTEM_TYPE;
import com.vuforia.CameraCalibration;
import com.vuforia.CameraDevice;
import com.vuforia.Device;
import com.vuforia.Frame;
import com.vuforia.GLTextureUnit;
import com.vuforia.Image;
import com.vuforia.Matrix34F;
import com.vuforia.Matrix44F;
import com.vuforia.Mesh;
import com.vuforia.ObjectTargetResult;
import com.vuforia.ObjectTracker;
import com.vuforia.PIXEL_FORMAT;
import com.vuforia.Renderer;
import com.vuforia.RenderingPrimitives;
import com.vuforia.RotationalDeviceTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.TrackerManager;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.VIEW;
import com.vuforia.Vec2F;
import com.vuforia.Vec2I;
import com.vuforia.VideoBackgroundConfig;
import com.vuforia.VideoMode;
import com.vuforia.Vuforia;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.firstinspires.ftc.robotcore.external.stream.CameraStreamServer;
import org.firstinspires.ftc.robotcore.internal.camera.delegating.SwitchableCameraName;
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue;
import org.firstinspires.ftc.robotcore.internal.opengl.AutoConfigGLSurfaceView;
import org.firstinspires.ftc.robotcore.internal.opengl.Texture;
import org.firstinspires.ftc.robotcore.internal.opengl.models.SavedMeshObject;
import org.firstinspires.ftc.robotcore.internal.opengl.models.SolidCylinder;
import org.firstinspires.ftc.robotcore.internal.opengl.models.Teapot;
import org.firstinspires.ftc.robotcore.internal.opengl.shaders.CubeMeshProgram;
import org.firstinspires.ftc.robotcore.internal.opengl.shaders.SimpleColorProgram;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeManagerImpl;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;
import org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider.VuforiaWebcam;
import org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider.VuforiaWebcamInternal;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * VuforiaLocalizerImpl connects the FTC SDK to the Vuforia engine
 */
@SuppressWarnings("WeakerAccess")
public class VuforiaLocalizerImpl implements VuforiaLocalizer
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "Vuforia";
    public static boolean TRACE = true;
    protected   Tracer                  tracer              = Tracer.create(TAG, TRACE);

    protected   LifeCycleCallbacks      lifeCycleCallbacks  = new LifeCycleCallbacks();
    protected   OpModeManagerImpl       opModeManager       = null;
    protected   OpModeNotifications     opModeNotifications = new OpModeNotifications();
    protected   VuforiaCallback         vuforiaCallback     = new VuforiaCallback();
    protected   GLSurfaceViewRenderer   glSurfaceViewRenderer = new GLSurfaceViewRenderer();

    protected   AppUtil                 appUtil             = AppUtil.getInstance();
    protected   Parameters              parameters          = null;
    protected   final Activity          activity;
    protected   int                     vuforiaFlags        = 0;
    protected   boolean                 wantCamera          = false;
    protected   boolean                 isCameraInited      = false;
    protected   boolean                 isCameraStarted     = false;
    protected   boolean                 isCameraRunning     = false;
    protected   CameraCalibration       camCal              = null;

    protected   final VuforiaWebcamInternal vuforiaWebcam;
    protected   VuforiaInitPhase        vuforiaInitPhase    = VuforiaInitPhase.Nascent;

    protected   ViewGroup               glSurfaceParent     = null;
    protected   int                     glSurfaceParentPreviousVisibility = 0;
    protected   AutoConfigGLSurfaceView glSurface           = null;
    protected   boolean                 fillSurfaceParent   = false;     // vs show full camera image
    protected   boolean                 isPortrait          = true;
    protected   Parameters.CameraMonitorFeedback cameraCameraMonitorFeedback = null;

    protected   RelativeLayout          loadingIndicatorOverlay = null;
    protected   View                    loadingIndicator        = null;
    protected   Renderer                renderer                = null;
    protected   boolean                 rendererIsActive        = false;
    protected   final Object            renderingPrimitivesMutex = new Object();
    protected   CloseableRenderingPrimitives renderingPrimitives = null;

    // An object used for synchronizing Vuforia initialization, dataset loading
    // and the Android onDestroy() life cycle event. If the application is
    // destroyed while a data set is still being loaded, then we wait for the
    // loading operation to finish before shutting down Vuforia:
    protected   final Object            startStopLock           = new Object();
    public static class ViewPort
        {
        public Point   lowerLeft = new Point();
        public Point   extent    = new Point();
        public @Override String toString() { return Misc.formatForUser("[(%d,%d)-(%d,%d)]", lowerLeft.x, lowerLeft.y, extent.x, extent.y); }
        }
    protected   volatile ViewPort       viewport                    = null;
    protected   Matrix44F               projectionMatrix            = null;

    protected   CubeMeshProgram         cubeMeshProgram             = null;
    protected   SimpleColorProgram      simpleColorProgram          = null;
    protected   List<Texture>           textures                    = null;
    protected   Texture                 teapotTexture               = null;
    protected   Teapot                  teapot                      = null;
    protected   float                   teapotScale                 = 3.0f;
    protected   CoordinateAxes          coordinateAxes              = new CoordinateAxes();
    protected   Texture                 buildingsTexture            = null;
    protected   SavedMeshObject         buildingsModel              = null;
    protected   float                   buildingsScale              = 12.0f;
    protected   final Object            updateCallbackLock          = new Object();
    protected   final List<VuforiaTrackablesImpl> loadedTrackableSets = new LinkedList<VuforiaTrackablesImpl>();
    protected   boolean                 isExtendedTrackingActive    = false;
    protected   final Object            frameQueueLock              = new Object();
    protected   BlockingQueue<CloseableFrame> frameQueue;
    protected   int                     frameQueueCapacity;
    protected   Continuation<? extends Consumer<Frame>> getFrameOnce = null;

    protected   final Object            bitmapFrameLock             = new Object();
    protected   Continuation<? extends Consumer<Bitmap>> bitmapContinuation;

    // Some simple statistics, perhaps useful during debugging
    protected   int                     renderCount = 0;
    protected   int                     callbackCount = 0;

    protected enum VuforiaInitPhase
        {
        Nascent(0),
        PreInit(1),
        Init(2),
        PostInit(3);
        public int value = 0;
        VuforiaInitPhase(int value) { this.value = value; }

        public VuforiaInitPhase from(int value)
            {
            for (VuforiaInitPhase phase : values())
                {
                if (phase.value == value) return phase;
                }
            return null;
            }
        public VuforiaInitPhase prev()
            {
            return from(this.value-1);
            }
        public VuforiaInitPhase next()
            {
            return from(this.value+1);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public VuforiaLocalizerImpl(VuforiaLocalizer.Parameters parameters)
        {
        this.parameters = parameters;
        activity = parameters.activity == null ? appUtil.getActivity() : parameters.activity;
        isExtendedTrackingActive = parameters.useExtendedTracking;
        cameraCameraMonitorFeedback = parameters.cameraMonitorFeedback;
        if (cameraCameraMonitorFeedback == null)
            {
            cameraCameraMonitorFeedback = isExtendedTrackingActive ? Parameters.CameraMonitorFeedback.BUILDINGS : Parameters.CameraMonitorFeedback.AXES;
            }
        vuforiaWebcam = createVuforiaWebcam();

        fillSurfaceParent = parameters.fillCameraMonitorViewParent;
        setFrameQueueCapacity(0);
        registerLifeCycleCallbacks();
        if (parameters.cameraMonitorViewParent != null)
            {
            setMonitorViewParent(parameters.cameraMonitorViewParent);
            }
        else
            {
            setMonitorViewParent(parameters.cameraMonitorViewIdParent);
            }
        makeLoadingIndicator();
        loadTextures();
        startAR();

        CameraStreamServer.getInstance().setSource(this);
        }

    /**
     * Once this method is complete, either a non-null {@link VuforiaWebcamInternal} object is
     * returned, indicating we are in the 'new' world, or a null such object is returned, indicating
     * that we are to use built-in phone cameras just as we always used to.
     */
    protected VuforiaWebcamInternal createVuforiaWebcam()
        {
        VuforiaWebcam result = null;
        if (parameters.camera != null)
            {
            // user specified the use of an explicit camera; use it!
            tracer.trace("using camera by instance: %s", parameters.camera.getCameraName());
            result = new VuforiaWebcam(parameters.webcamCalibrationResources, parameters.webcamCalibrationFiles, parameters.minWebcamAspectRatio, parameters.maxWebcamAspectRatio, parameters.secondsUsbPermissionTimeout, parameters.camera);
            }
        else
            {
            if (parameters.cameraName != null)
                {
                if (parameters.cameraName.isWebcam() || (parameters.cameraName.isSwitchable() && ((SwitchableCameraName)parameters.cameraName).allMembersAreWebcams()))
                    {
                    // user specified the use of a webcam or webcams; use it!
                    tracer.trace("using camera by name: %s", parameters.cameraName);
                    result = new VuforiaWebcam(parameters.webcamCalibrationResources, parameters.webcamCalibrationFiles, parameters.minWebcamAspectRatio, parameters.maxWebcamAspectRatio, parameters.secondsUsbPermissionTimeout, parameters.cameraName);
                    }
                else if (parameters.cameraName.isUnknown())
                    {
                    tracer.trace("using camera by direction (classic): %s", parameters.cameraDirection);
                    // user hasn't changed cameraName from the initialized value; use cameraDirection as given
                    }
                else if (parameters.cameraName.isCameraDirection())
                    {
                    // user gave us a camera direction in a new way; use it
                    parameters.cameraDirection = ((BuiltinCameraName)(parameters.cameraName)).getCameraDirection();
                    tracer.trace("using camera by direction (wrapped): %s", parameters.cameraDirection);
                    }
                else
                    {
                    throw Misc.illegalArgumentException("parameters.cameraName=%s is not supported", parameters.cameraName);
                    }
                }
            else
                {
                throw Misc.illegalArgumentException("parameters.cameraName is null; is your camera attached to the robot controller?");
                }
            }
        return result;
        }

    @Override public @NonNull CameraName getCameraName()
        {
        if (vuforiaWebcam != null)
            {
            return vuforiaWebcam.getCameraName();
            }
        else
            {
            return ClassFactory.getInstance().getCameraManager().nameFromCameraDirection(parameters.cameraDirection);
            }
        }

    @Override public @Nullable Camera getCamera()
        {
        if (vuforiaWebcam != null)
            {
            return vuforiaWebcam.getCamera();
            }
        else
            {
            return null;
            }
        }

    protected void close()
    // Must be idempotent. Callable from ANY thread
        {
        stopAR();
        removeLoadingIndicator();
        unregisterLifeCycleCallbacks();
        }

    //----------------------------------------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------------------------------------

    protected void setMonitorViewParent(@IdRes int resourceId)
        {
        View view = this.activity.findViewById(resourceId); // may return null if resourceId is, e.g., zero
        setMonitorViewParent((ViewGroup)view);
        }

    protected void setMonitorViewParent(@Nullable ViewGroup viewParent)
        {
        this.glSurfaceParent = viewParent;
        if (glSurfaceParent != null)
            {
            glSurfaceParentPreviousVisibility = glSurfaceParent.getVisibility();
            }
        }

    @Override public VuforiaTrackables loadTrackablesFromAsset(String assetName)
        {
        return loadTrackablesFromAsset(assetName, VuforiaTrackableDefaultListener.class);
        }

    public VuforiaTrackables loadTrackablesFromAsset(String assetName, Class<? extends VuforiaTrackable.Listener> listenerClass)
        {
        return loadDataSet(assetName, STORAGE_TYPE.STORAGE_APPRESOURCE, listenerClass);
        }

    @Override public VuforiaTrackables loadTrackablesFromFile(String absoluteFileName)
        {
        return loadTrackablesFromFile(absoluteFileName, VuforiaTrackableDefaultListener.class);
        }

    public VuforiaTrackables loadTrackablesFromFile(String absoluteFileName, Class<? extends VuforiaTrackable.Listener> listenerClass)
        {
        return loadDataSet(absoluteFileName, STORAGE_TYPE.STORAGE_ABSOLUTE, listenerClass);
        }

    protected VuforiaTrackables loadDataSet(String name, int type, Class<? extends VuforiaTrackable.Listener> listenerClass)
        {
        showLoadingIndicator(View.VISIBLE);
        try {
            ObjectTracker tracker = getObjectTracker();
            com.vuforia.DataSet dataSet = tracker.createDataSet();
            tracer.trace("loading data set '%s'...", name);
            try {
                throwIfFail(dataSet.load(name + ".xml", type));
                }
            finally
                {
                tracer.trace("... done loading data set '%s'", name);
                }
            VuforiaTrackablesImpl result = new VuforiaTrackablesImpl(this, dataSet, listenerClass);
            result.setName(name);

            synchronized (this.loadedTrackableSets)
                {
                this.loadedTrackableSets.add(result);
                }
            return result;
            }
        finally
            {
            showLoadingIndicator(View.INVISIBLE);
            }
        }

    protected void showLoadingIndicator(final int visibility)
        {
        appUtil.runOnUiThread(new Runnable()
            {
            @Override public void run()
                {
                if (loadingIndicator != null)
                    {
                    loadingIndicator.setVisibility(visibility);
                    }
                }
            });
        }

    protected void makeLoadingIndicator()
        {
        removeLoadingIndicator();
        appUtil.synchronousRunOnUiThread(new Runnable()
            {
            @Override public void run()
                {
                loadingIndicatorOverlay = (RelativeLayout)View.inflate(activity, R.layout.loading_indicator_overlay, null);
                loadingIndicator = loadingIndicatorOverlay.findViewById(R.id.loadingIndicator);
                loadingIndicator.setVisibility(View.INVISIBLE);
                activity.addContentView(loadingIndicatorOverlay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                }
            });
        }

    protected void removeLoadingIndicator()
        {
        if (loadingIndicatorOverlay != null)
            {
            appUtil.synchronousRunOnUiThread(new Runnable()
                {
                @Override public void run()
                    {
                    if (loadingIndicatorOverlay != null)
                        {
                        ((ViewGroup)loadingIndicatorOverlay.getParent()).removeView(loadingIndicatorOverlay);
                        loadingIndicatorOverlay = null;
                        }
                    }
                });
            }
        }

    protected void adjustExtendedTracking()
        {
        synchronized (this.loadedTrackableSets)
            {
            for (VuforiaTrackablesImpl set : this.loadedTrackableSets)
                {
                set.adjustExtendedTracking(this.isExtendedTrackingActive);
                }
            }
        }

    protected void destroyTrackables()
        {
        synchronized (this.loadedTrackableSets)
            {
            for (VuforiaTrackablesImpl trackable : this.loadedTrackableSets)
                {
                trackable.destroy();
                }
            this.loadedTrackableSets.clear();
            }
        }

    public int getRenderCount()
        {
        return this.renderCount;
        }

    public int getCallbackCount()
        {
        return this.callbackCount;
        }


    //----------------------------------------------------------------------------------------------
    // Activity life cycles
    //----------------------------------------------------------------------------------------------

    protected void registerLifeCycleCallbacks()
        {
        appUtil.getApplication().registerActivityLifecycleCallbacks(this.lifeCycleCallbacks);
        this.opModeManager = OpModeManagerImpl.getOpModeManagerOfActivity(this.activity);
        if (this.opModeManager != null)
            {
            this.opModeManager.registerListener(this.opModeNotifications);
            }
        }

    protected void unregisterLifeCycleCallbacks()
        {
        if (this.opModeManager != null)
            {
            this.opModeManager.unregisterListener(this.opModeNotifications);
            }
        appUtil.getApplication().unregisterActivityLifecycleCallbacks(this.lifeCycleCallbacks);
        }

    protected class OpModeNotifications implements OpModeManagerNotifier.Notifications
        {
        @Override public void onOpModePreInit(OpMode opMode)
            {
            }

        @Override public void onOpModePreStart(OpMode opMode)
            {
            }

        @Override public void onOpModePostStop(OpMode opMode)
            {
            /** We automatically shut down after the opmode (in which we are started) stops.  */
            close();
            }
        }

    protected class LifeCycleCallbacks implements Application.ActivityLifecycleCallbacks
        {
        @Override
        public void onActivityCreated(Activity activity, Bundle bundle)
            {
            }

        @Override
        public void onActivityStarted(Activity activity)
            {
            }

        @Override
        public void onActivityResumed(Activity activity)
            {
            if (activity == VuforiaLocalizerImpl.this.activity)
                {
                resumeAR();
                if (glSurface != null)
                    {
                    glSurface.setVisibility(View.VISIBLE);
                    glSurface.onResume();
                    }
                }
            }

        @Override
        public void onActivityPaused(Activity activity)
            {
            if (activity == VuforiaLocalizerImpl.this.activity)
                {
                if (glSurface != null)
                    {
                    glSurface.setVisibility(View.INVISIBLE);
                    glSurface.onPause();
                    }
                pauseAR();
                }
            }

        @Override
        public void onActivityStopped(Activity activity)
            {
            }

        @Override
        public void onActivityDestroyed(Activity activity)
            {
            if (activity == VuforiaLocalizerImpl.this.activity)
                {
                close();
                }
            }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState)
            {
            }
        }

    //----------------------------------------------------------------------------------------------
    // AR life cycle
    //----------------------------------------------------------------------------------------------

    protected boolean advanceInitPhase(Supplier<Boolean> supplier)
        {
        boolean success = false;
        try {
            success = supplier.get();
            }
        catch (VuforiaException e)
            {
            tracer.traceError(e, "exception in vuforia initialization: phase:%s", vuforiaInitPhase);
            throw e;
            }
        catch (Exception e)
            {
            tracer.traceError(e, "exception in vuforia initialization: phase:%s", vuforiaInitPhase);
            throw e;
            }
        finally
            {
            if (success) vuforiaInitPhase = vuforiaInitPhase.next();
            }
        return success;
        }
    protected void retreatInitPhase(Runnable runnable)
        {
        try {
            runnable.run();
            }
        catch (Exception e)
            {
            tracer.traceError(e, "exception in vuforia deinitialization: phase:%s", vuforiaInitPhase);
            throw e;
            }
        finally
            {
            vuforiaInitPhase = vuforiaInitPhase.prev();
            }
        }
    protected void expectPhase(VuforiaInitPhase phase)
        {
        Assert.assertTrue(vuforiaInitPhase == phase);
        }

    protected boolean startAR()
        {
        boolean success = true;
        synchronized (startStopLock)
            {
            vuforiaInitPhase = VuforiaInitPhase.Nascent;
            showLoadingIndicator(View.VISIBLE);
            try {
                updateActivityOrientation();
                this.vuforiaFlags = Vuforia.GL_20;
                Vuforia.setInitParameters(activity, vuforiaFlags, parameters.vuforiaLicenseKey);

                if (success)
                    {
                    success = advanceInitPhase(new Supplier<Boolean>()
                        {
                        @Override public Boolean get()
                            {
                            boolean ret = vuforiaWebcam == null || vuforiaWebcam.preVuforiaInit();
                            if (!ret)
                                {
                                throwFailure(getInitializationErrorString(Vuforia.INIT_NO_CAMERA_ACCESS));
                                }
                            return ret;
                            }
                        });
                    }

                if (success)
                    {
                    expectPhase(VuforiaInitPhase.PreInit);
                    success = advanceInitPhase(new Supplier<Boolean>()
                        {
                        @Override public Boolean get()
                            {
                            int initProgress;
                            do  {
                            initProgress = tracer.trace("Vuforia.init()", new Supplier<Integer>()
                                {
                                @Override public Integer get()
                                    {
                                    return Vuforia.init();
                                    }
                                });
                            }
                            while (initProgress >= 0 && initProgress < 100);
                            if (initProgress < 0) {
                                throwFailure(getInitializationErrorString(initProgress));
                            }
                            return true;
                            }
                        });
                    }

                if (success)
                    {
                    expectPhase(VuforiaInitPhase.Init);
                    success = advanceInitPhase(new Supplier<Boolean>()
                        {
                        @Override public Boolean get()
                            {
                            if (vuforiaWebcam != null)
                                {
                                vuforiaWebcam.postVuforiaInit();
                                }
                            return true;
                            }
                        });
                    }

                if (success)
                    {
                    expectPhase(VuforiaInitPhase.PostInit);
                    initTracker();
                    Vuforia.registerCallback(VuforiaLocalizerImpl.this.vuforiaCallback);
                    makeGlSurface();
                    wantCamera = true;
                    if (startCamera())
                        {
                        updateRenderingPrimitives(); // TODO: can we move this BEFORE startCamera()?
                        // Try to turn on auto-focus; ignore if not supported
                        CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);
                        rendererIsActive = true;
                        }
                    }
                }
            finally
                {
                showLoadingIndicator(View.INVISIBLE);
                }
            }
        if (!success)
            {
            throwFailure("Vuforia failed to start: phase=%s", vuforiaInitPhase);
            }
        return success;
        }

    protected void makeGlSurface()
        {
        // Create OpenGL ES view:
        final int depthSize = 16;
        final int stencilSize = 0;
        final boolean translucent = Vuforia.requiresAlpha();

        if (glSurfaceParent != null)
            {
            appUtil.synchronousRunOnUiThread(new Runnable() { @Override public void run()
                {
                ViewGroup parent = glSurfaceParent;
                if (parent != null)
                    {
                    AutoConfigGLSurfaceView surface = glSurface = new AutoConfigGLSurfaceView(activity);
                    surface.init(translucent, depthSize, stencilSize);

                    surface.setRenderer(VuforiaLocalizerImpl.this.glSurfaceViewRenderer);

                    // Now add the GL surface view. It is important that the OpenGL ES surface view gets added
                    // BEFORE the camera is started and video background is configured.
                    surface.setVisibility(View.INVISIBLE);    // invisible until we know if we have to resize it or not
                    parent.addView(surface);
                    parent.setVisibility(View.VISIBLE);
                    }
                }});
            }
        }

    protected void removeGlSurface()
        {
        if (this.glSurfaceParent != null)
            {
            appUtil.runOnUiThread(new Runnable()
                {
                @Override public void run()
                    {
                    View surface = glSurface;
                    if (surface != null)
                        {
                        surface.setVisibility(View.INVISIBLE); // avoids seeing a flash of naked background in below
                        }
                    ViewGroup parent = glSurfaceParent;
                    if (parent != null)
                        {
                        parent.removeAllViews();
                        parent.getOverlay().clear();
                        glSurface = null;
                        parent.setVisibility(glSurfaceParentPreviousVisibility);
                        }
                    }
                });
            }
        }

    protected String getInitializationErrorString(int code)
        {
        switch (code)
            {
            case Vuforia.INIT_DEVICE_NOT_SUPPORTED:                 return activity.getString(R.string.VUFORIA_INIT_ERROR_DEVICE_NOT_SUPPORTED);
            case Vuforia.INIT_NO_CAMERA_ACCESS:                     return activity.getString(R.string.VUFORIA_INIT_ERROR_NO_CAMERA_ACCESS);
            case Vuforia.INIT_LICENSE_ERROR_MISSING_KEY:            return activity.getString(R.string.VUFORIA_INIT_LICENSE_ERROR_MISSING_KEY);
            case Vuforia.INIT_LICENSE_ERROR_INVALID_KEY:            return activity.getString(R.string.VUFORIA_INIT_LICENSE_ERROR_INVALID_KEY);
            case Vuforia.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT:   return activity.getString(R.string.VUFORIA_INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT);
            case Vuforia.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT:   return activity.getString(R.string.VUFORIA_INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT);
            case Vuforia.INIT_LICENSE_ERROR_CANCELED_KEY:           return activity.getString(R.string.VUFORIA_INIT_LICENSE_ERROR_CANCELED_KEY);
            case Vuforia.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH:  return activity.getString(R.string.VUFORIA_INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH);
            default:                                                return activity.getString(R.string.VUFORIA_INIT_LICENSE_ERROR_UNKNOWN_ERROR);
            }
        }

    protected void stopAR()
        {
        synchronized (startStopLock)
            {
            wantCamera = false;
            rendererIsActive = false;
            stopCamera();
            destroyTrackables();
            deinitTracker();
            removeGlSurface();

            if (vuforiaInitPhase.value >= VuforiaInitPhase.PostInit.value)
                {
                retreatInitPhase(new Runnable()
                    {
                    @Override public void run()
                        {
                        if (vuforiaWebcam != null)
                            {
                            vuforiaWebcam.preVuforiaDeinit();
                            }
                        }
                    });
                }
            if (vuforiaInitPhase.value >= VuforiaInitPhase.Init.value)
                {
                retreatInitPhase(new Runnable()
                    {
                    @Override public void run()
                        {
                        tracer.trace("Vuforia.deinit()", new Runnable()
                            {
                            @Override public void run()
                                {
                                Vuforia.deinit();
                                }
                            });
                        }
                    });
                }
            if (vuforiaInitPhase.value >= VuforiaInitPhase.PreInit.value)
                {
                retreatInitPhase(new Runnable()
                    {
                    @Override public void run()
                        {
                        if (vuforiaWebcam != null)
                            {
                            vuforiaWebcam.postVuforiaDeinit();
                            }
                        }
                    });
                }
            expectPhase(VuforiaInitPhase.Nascent);
            }
        }

    protected void resumeAR()
        {
        Vuforia.onResume();
        if (wantCamera)
            {
            startCamera();
            }
        }

    protected void pauseAR()
        {
        stopCamera();
        Vuforia.onPause();
        }

    //----------------------------------------------------------------------------------------------
    // Tracker management. Currently, we only use the ObjectTracker flavor, but it's
    // conceivable that in future we might add additional flavors.
    //----------------------------------------------------------------------------------------------

    protected static ObjectTracker getObjectTracker()
        {
        return (ObjectTracker) TrackerManager.getInstance().getTracker(ObjectTracker.getClassType());
        }

    protected void initTracker()
        {
        TrackerManager.getInstance().initTracker(ObjectTracker.getClassType());
        }

    protected boolean startTracker()
        {
        return getObjectTracker().start();
        }

    protected boolean isObjectTargetTrackableResult(TrackableResult trackableResult)
        {
        return trackableResult.isOfType(ObjectTargetResult.getClassType());
        }

    protected void stopTracker()
        {
        getObjectTracker().stop();
        }

    protected void deinitTracker()
        {
        TrackerManager.getInstance().deinitTracker(ObjectTracker.getClassType());
        }

    // -----

    protected static RotationalDeviceTracker getRotationalDeviceTracker()
        {
        return (RotationalDeviceTracker) TrackerManager.getInstance().getTracker(RotationalDeviceTracker.getClassType());
        }

    //----------------------------------------------------------------------------------------------
    // Camera management
    //----------------------------------------------------------------------------------------------

    @Override
    public synchronized CameraCalibration getCameraCalibration()
        {
        return this.camCal;
        }

    protected synchronized boolean startCamera()
        {
        boolean success = false;
        throwIfFail(!isCameraRunning, "camera already running");

        int cameraIndex = vuforiaWebcam == null
                ? parameters.cameraDirection.getDirection()
                : CameraDirection.BACK.getDirection(); // will be ignored by Vuforia if we're using an external camera

        if (CameraDevice.getInstance().init(cameraIndex)) // calls VuforiaWebcamImpl.open (for webcams)
            {
            isCameraInited = true;

            if(CameraDevice.getInstance().selectVideoMode(CameraDevice.MODE.MODE_DEFAULT))
                {
                configureVideoBackground();

                if (CameraDevice.getInstance().start())
                    {
                    isCameraStarted = true;

                    camCal = CameraDevice.getInstance().getCameraCalibration();
                    projectionMatrix = Tool.getProjectionGL(camCal, 10.0f, 5000.0f);

                    if (startTracker())
                        {
                        if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO))
                            {
                            if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO))
                                {
                                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
                                }
                            }

                        isCameraRunning = true;
                        success = true;
                        }
                    else
                        tracer.traceError("camera tracker failed to start on camera %s", getCameraName());
                    }
                else
                    tracer.traceError("unable to select video mode on camera %s", getCameraName());
                }
            else
                tracer.traceError("camera %s failed to start ", getCameraName());
            }
        else
            tracer.traceError("CameraDevice.getInstance.init() failed");

        return success;
        }

    protected synchronized void stopCamera()
        {
        if (isCameraRunning)
            {
            stopTracker();
            if (isCameraStarted) CameraDevice.getInstance().stop();
            if (isCameraInited) CameraDevice.getInstance().deinit();
            isCameraInited  = false;
            isCameraStarted = false;
            isCameraRunning = false;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Miscellaneous
    //----------------------------------------------------------------------------------------------

    protected void configureVideoBackground()
        {
        if (glSurface == null)
            return;

        // The setLayoutParams() and the setVisibility() calls below MUST be done on the UI thread.
        // Doing *all* the work there makes the sequencing of those and of getWidth()/getHeight()
        // always work in the right relative order. The downside is that 'viewport' won't be set
        // robustly on completion of configureVideoBackground().
        appUtil.runOnUiThread(new Runnable()
            {
            @Override public void run()
                {
                if (glSurface == null)
                  return;

                // What screen real estate do we have to draw on?
                PointF view = new PointF(glSurface.getWidth(), glSurface.getHeight());

                // What's the incoming camera video stream dimensions?
                CameraDevice cameraDevice = CameraDevice.getInstance();
                VideoMode videoMode = cameraDevice.getVideoMode(CameraDevice.MODE.MODE_DEFAULT);
                PointF video = new PointF(videoMode.getWidth(), videoMode.getHeight());

                // Do our math here with a local variable until it's finished and internally consistent
                ViewPort viewport = new ViewPort();
                if (isPortrait)
                    {
                    viewport.extent.x = (int) (video.y * view.y / video.x);
                    viewport.extent.y = (int) view.y;

                    if (viewport.extent.x < view.x)
                        {
                        if (fillSurfaceParent)
                            {
                            viewport.extent.x = (int) view.x;
                            viewport.extent.y = (int) (view.x * video.x / video.y);
                            }
                        else
                            {
                            // See the full camera view, but (a tradeoff) don't fill up the whole surface parent view
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(viewport.extent.x, glSurface.getLayoutParams().height);
                            params.gravity = Gravity.CENTER_HORIZONTAL;
                            glSurface.setLayoutParams(params);
                            }
                        }
                    }
                else
                    {
                    viewport.extent.x = (int) view.x;
                    viewport.extent.y = (int) (video.y * view.x / video.x);

                    if (viewport.extent.y < view.y)
                        {
                        if (fillSurfaceParent)
                            {
                            viewport.extent.x = (int) (view.y * video.x / video.y);
                            viewport.extent.y = (int) view.y;
                            }
                        else
                            {
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(glSurface.getLayoutParams().width, viewport.extent.y);
                            params.gravity = Gravity.CENTER_VERTICAL;
                            glSurface.setLayoutParams(params);
                            }
                        }
                    }

                // Update in case we changed things. And we know the size, so we can show the surface w/o flashing naked background
                view = new PointF(glSurface.getWidth(), glSurface.getHeight());
                glSurface.setVisibility(View.VISIBLE);

                VideoBackgroundConfig videoBackgroundConfig = new VideoBackgroundConfig();
                videoBackgroundConfig.setEnabled(true);

                // The center of the background should be in the center of the viewport
                videoBackgroundConfig.setPosition(new Vec2I(0, 0));
                videoBackgroundConfig.setSize(new Vec2I(viewport.extent.x, viewport.extent.y));

                // The Vuforia VideoBackgroundConfig takes the position relative to the
                // centre of the screen, where as the OpenGL glViewport call takes the
                // position relative to the lower left corner
                viewport.lowerLeft.x = (int) ((view.x - viewport.extent.x) / 2) + videoBackgroundConfig.getPosition().getData()[0];
                viewport.lowerLeft.y = (int) ((view.y - viewport.extent.y) / 2) + videoBackgroundConfig.getPosition().getData()[1];

                // Adjust (only needed in non-fillSurfaceParent case) to keep viewport inside glSurface
                viewport.lowerLeft.x = Math.min(0, viewport.lowerLeft.x);
                viewport.lowerLeft.y = Math.min(0, viewport.lowerLeft.y);

                Renderer.getInstance().setVideoBackgroundConfig(videoBackgroundConfig);

                // Set the viewport that others will see
                VuforiaLocalizerImpl.this.viewport = viewport;
                }
            });
        }

    // Stores the orientation depending on the current resources configuration
    protected void updateActivityOrientation()
        {
        Configuration config = activity.getResources().getConfiguration();
        switch (config.orientation)
            {
            case Configuration.ORIENTATION_LANDSCAPE:
                this.isPortrait = false;
                break;
            case Configuration.ORIENTATION_UNDEFINED:
            case Configuration.ORIENTATION_PORTRAIT:
            default:
                this.isPortrait = true;
                break;
            }
        }

    protected void initRendering()
        {
        this.renderer = Renderer.getInstance();

        // Specify the red, green, blue, and alpha values used when the color buffers are cleared
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);

        for (Texture t : textures)
            {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, t.mWidth, t.mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, t.mData);
            }

        cubeMeshProgram = new CubeMeshProgram(this.activity);
        simpleColorProgram = new SimpleColorProgram(this.activity);

        if (teapotRequired())
            {
            teapot = new Teapot();
            }

        if (buildingsRequired())
            {
            buildingsModel = new SavedMeshObject();
            try {
                buildingsModel.loadModel(activity.getResources().getAssets(), "Buildings.txt");
                }
            catch (IOException e)
                {
                throwFailure(e);
                }
            }
        }

    protected void updateRendering(int width, int height)
        {
        configureVideoBackground();
        }

    /**
     * Previously, we simply called Renderer.drawVideoBackground(). But that method has been deprecated and
     * removed in Vuforia 7. So, we do it ourselves, adapting code from the SampleAppRenderer provided in
     * the Vuforia 7 SDK.
     */
    protected void renderVideoBackground()
        {
        // Use texture unit 0 for the video background - this will hold the camera frame and we want to reuse for all views
        // So need to use a different texture unit for the augmentation
        final int vbVideoTextureUnit = 0;

        // Bind the video bg texture and get the Texture ID from Vuforia
        // Please note that if you want to use a specific GL texture handle for the video background, you should configure it
        // from the initRendering() method and use the Renderer::setVideoBackgroundTexture( GLTextureData ) method.
        // Please refer to API Guide for more information.
        GLTextureUnit vbTexture = new GLTextureUnit(vbVideoTextureUnit);
        if (renderer.updateVideoBackgroundTexture(vbTexture))
            {
            Matrix34F matrix34F = renderingPrimitives.getVideoBackgroundProjectionMatrix(VIEW.VIEW_SINGULAR, COORDINATE_SYSTEM_TYPE.COORDINATE_SYSTEM_CAMERA);
            Matrix44F vbProjectionMatrix = Tool.convert2GLMatrix(matrix34F);

            // Apply the scene scale on video see-through eyewear, to scale the video background and augmentation
            // so that the display lines up with the real world
            // This should not be applied on optical see-through devices, as there is no video background,
            // and the calibration ensures that the augmentation matches the real world
            if (Device.getInstance().isViewerActive())
                {
                float sceneScaleFactor = getSceneScaleFactor();
                scalePoseMatrix(sceneScaleFactor, sceneScaleFactor, 1.0f, vbProjectionMatrix);
                }

            // Disable certain GL booleans
            boolean[] params = new boolean[3];
            GLES20.glGetBooleanv(GLES20.GL_DEPTH_TEST, params, 0);
            GLES20.glGetBooleanv(GLES20.GL_CULL_FACE, params, 1);
            GLES20.glGetBooleanv(GLES20.GL_SCISSOR_TEST, params, 2);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

            Mesh vbMesh = renderingPrimitives.getVideoBackgroundMesh(VIEW.VIEW_SINGULAR);

            // Load the shader and upload the vertex/texcoord/index data
            cubeMeshProgram.useProgram();
            cubeMeshProgram.vertex.setCoordinates(vbMesh);
            cubeMeshProgram.fragment.setTextureUnit(vbTexture);

            // Pass the projection matrix to OpenGL
            cubeMeshProgram.vertex.setModelViewProjectionMatrix(vbProjectionMatrix.getData());

            // Then, we issue the render call
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, vbMesh.getNumTriangles() * 3, GLES20.GL_UNSIGNED_SHORT, vbMesh.getTriangles());

            // Finally, we disable the vertex arrays
            cubeMeshProgram.vertex.disableAttributes();

            // Restore disabled GL booleans
            if (params[0]) GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            if (params[1]) GLES20.glEnable(GLES20.GL_CULL_FACE);
            if (params[2]) GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            }
        else
            {
            // Early in the launch process, we seem to get a couple of renderFrame() calls that
            // lead to failure in updateVideoBackgroundTexture(). As that clears up very quickly
            // (a frame or two), probably simply because of sequencing of initialization, there
            // seems little point in bothering people in the log about it.
            // tracer.traceError("updateVideoBackgroundTexture() failed");
            }
        }

    protected float getSceneScaleFactor()
        {
        final double VIRTUAL_FOV_Y_DEGS = 85;

        // Get the y-dimension of the physical camera field of view
        Vec2F fovVector = CameraDevice.getInstance().getCameraCalibration().getFieldOfViewRads();
        float cameraFovYRads = fovVector.getData()[1];

        // Get the y-dimension of the virtual camera field of view
        double virtualFovYRads = VIRTUAL_FOV_Y_DEGS * Math.PI / 180;

        // The scene-scale factor represents the proportion of the viewport that is filled by
        // the video background when projected onto the same plane.
        // In order to calculate this, let 'd' be the distance between the cameras and the plane.
        // The height of the projected image 'h' on this plane can then be calculated:
        //   tan(fov/2) = h/2d
        // which rearranges to:
        //   2d = h/tan(fov/2)
        // Since 'd' is the same for both cameras, we can combine the equations for the two cameras:
        //   hPhysical/tan(fovPhysical/2) = hVirtual/tan(fovVirtual/2)
        // Which rearranges to:
        //   hPhysical/hVirtual = tan(fovPhysical/2)/tan(fovVirtual/2)
        // ... which is the scene-scale factor
        double result = Math.tan(cameraFovYRads / 2) / Math.tan(virtualFovYRads / 2);
        return (float)result;
        }

    protected void scalePoseMatrix(float x, float y, float z, Matrix44F matrix)
        {
        float[] matrixData = matrix.getData();

        matrixData[0]  *= x;
        matrixData[1]  *= x;
        matrixData[2]  *= x;
        matrixData[3]  *= x;

        matrixData[4]  *= y;
        matrixData[5]  *= y;
        matrixData[6]  *= y;
        matrixData[7]  *= y;

        matrixData[8]  *= z;
        matrixData[9]  *= z;
        matrixData[10] *= z;
        matrixData[11] *= z;

        matrix.setData(matrixData);
        }

    /**
     * A variant of RenderingPrimitives that provides for deterministic cleanup
     */
    protected static class CloseableRenderingPrimitives extends RenderingPrimitives
        {
        public CloseableRenderingPrimitives(RenderingPrimitives primitives)
            {
            super(primitives);
            }
        public void close()
            {
            super.delete();
            }
        }

    protected void updateRenderingPrimitives()
        {
        synchronized (renderingPrimitivesMutex)
            {
            if (renderingPrimitives != null)
                {
                renderingPrimitives.close();
                renderingPrimitives = null;
                }
            renderingPrimitives = new CloseableRenderingPrimitives(Device.getInstance().getRenderingPrimitives());
            }
        }

    protected boolean buildingsRequired()
        {
        return this.parameters.cameraMonitorFeedback == Parameters.CameraMonitorFeedback.BUILDINGS;
        }

    protected boolean teapotRequired()
        {
        return this.parameters.cameraMonitorFeedback == Parameters.CameraMonitorFeedback.TEAPOT;
        }

    //----------------------------------------------------------------------------------------------
    // Rendering
    //----------------------------------------------------------------------------------------------

    protected void loadTextures()
        {
        this.buildingsTexture = Texture.loadTextureFromApk("Buildings.jpeg", this.activity.getAssets());
        this.teapotTexture    = Texture.loadTextureFromApk("TextureTeapotBrass.png", this.activity.getAssets());
        this.textures = new LinkedList<Texture>();
        this.textures.add(buildingsTexture);
        this.textures.add(teapotTexture);
        }

    protected class GLSurfaceViewRenderer implements GLSurfaceView.Renderer
        {
        public void onSurfaceCreated(GL10 gl, EGLConfig config)
            {
            initRendering();

            // Call Vuforia function to (re)initialize rendering after first use
            // or after OpenGL ES context was lost (e.g. after onPause/onResume):
            Vuforia.onSurfaceCreated();
            }

        @Override public void onSurfaceChanged(GL10 gl, int width, int height)
            {
            updateRendering(width, height);
            Vuforia.onSurfaceChanged(width, height);
            updateRenderingPrimitives();
            }

        @Override public void onDrawFrame(GL10 gl)
            {
            if (rendererIsActive)
                {
                renderFrame();
                }
            }
        }

    /**
     * onRenderFrame()
     *
     * Noop.  Intended as an entry point for user defined overlaying of the SDK's built in camera rendering.
     */
    public void onRenderFrame()
        {
        }

    protected void renderFrame()
        {
        if (glSurface == null)
            {
            return;
            }

        synchronized (renderingPrimitivesMutex)
            {
            renderCount++;

            // Clear color and depth buffers
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


            if (viewport != null) // can't do the rest if configureVideoBackground hasn't finished
                {
                State state = renderer.begin();
                renderVideoBackground();

                GLES20.glEnable(GLES20.GL_DEPTH_TEST);

                // Set the viewport
                GLES20.glViewport(viewport.lowerLeft.x, viewport.lowerLeft.y, viewport.extent.x, viewport.extent.y);

                // Handle face culling, we need to detect if we are using reflection to determine the direction of the culling
                GLES20.glEnable(GLES20.GL_CULL_FACE);
                GLES20.glCullFace(GLES20.GL_BACK);
                if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
                    {
                    GLES20.glFrontFace(GLES20.GL_CW); // Front camera
                    }
                else
                    {
                    GLES20.glFrontFace(GLES20.GL_CCW); // Back camera
                    }

                // did we find any trackables this frame?
                for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
                    {
                    TrackableResult trackableResult = state.getTrackableResult(tIdx);
                    if (isObjectTargetTrackableResult(trackableResult))
                        {
                        Matrix44F poseMatrixOpenGl = Tool.convertPose2GLMatrix(trackableResult.getPose());
                        float[] poseMatrix = poseMatrixOpenGl.getData();

                        switch (this.cameraCameraMonitorFeedback)
                            {
                            case NONE:                                 break;
                            case BUILDINGS: drawBuildings(poseMatrix); break;
                            case TEAPOT:    drawTeapot(poseMatrix);    break;
                            case AXES:      drawAxes(poseMatrix);      break;
                            }
                        }
                    }

                // Let the user do something interesting with this frame.
                onRenderFrame();

                // Grab the contents of glSurface
                synchronized (bitmapFrameLock)
                    {
                    if (bitmapContinuation != null)
                        {
                        final Bitmap bitmap = glSurfaceToBitmap();
                        if (bitmap != null)
                            {
                            bitmapContinuation.dispatch(new ContinuationResult<Consumer<Bitmap>>()
                                {
                                @Override
                                public void handle(Consumer<Bitmap> bitmapConsumer)
                                    {
                                    bitmapConsumer.accept(bitmap);
                                    bitmap.recycle();
                                    }
                                });
                            bitmapContinuation = null;
                            }
                        }
                    }

                GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                renderer.end();
                }
            }
        }

    protected void drawAxes(float[] poseMatrix)
        {
        this.coordinateAxes.draw(poseMatrix);
        }

    class CoordinateAxes
        {
        private SolidCylinder axis;

        public CoordinateAxes()
            {
            float radius = 0.05f;
            float height = 1.0f;
            this.axis = new SolidCylinder(radius, height, 32);
            }

        public void draw(float[] poseMatrixIn)
            {
            // Axis is, by default, sitting along the y axis, with its center at the origin
            drawAxis(poseMatrixIn, axis, Color.RED,       0,0,-1, 0,axis.height/2,0);
            drawAxis(poseMatrixIn, axis, Color.BLUE,      -1,0,0, 0,-axis.height/2,0);
            drawAxis(poseMatrixIn, axis, Color.GREEN,     0,0,0,  0,axis.height/2,0);
            }

        private void drawAxis(float[] poseMatrixIn, SolidCylinder axis, int color,
                              float rx, float ry, float rz,
                              float dx, float dy, float dz)
            {
            float axesScale = 100f;

            float[] poseMatrix = Arrays.copyOf(poseMatrixIn, poseMatrixIn.length);

            // Yes, it's odd that we scale, then translate, and only finally rotate. But
            // this works (finally) so we're not incented to change it.
            if (rx!=0 || ry!=0 || rz!=0) Matrix.rotateM(poseMatrix, 0, 90f, rx, ry, rz);
            Matrix.translateM(poseMatrix, 0, dx * axesScale, dy * axesScale, dz * axesScale);

            Matrix.scaleM(poseMatrix, 0, axesScale, axesScale, axesScale);

            float[] modelViewProjectionMatrix = new float[16];
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix.getData(), 0, poseMatrix, 0);

            simpleColorProgram.useProgram();
            simpleColorProgram.fragment.setColor(color);

            axis.bindData(simpleColorProgram.vertex);

            simpleColorProgram.vertex.setModelViewProjectionMatrix(modelViewProjectionMatrix);
            axis.draw();
            simpleColorProgram.vertex.disableAttributes();
            }
        }

    protected void drawTeapot(float[] poseMatrix)
        {
        Assert.assertTrue(teapotRequired());

        // Rotate / scale / position the teapot
        Matrix.scaleM(poseMatrix, 0, teapotScale, teapotScale, teapotScale);

        float[] modelViewProjectionMatrix = new float[16];
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix.getData(), 0, poseMatrix, 0);

        cubeMeshProgram.useProgram();
        cubeMeshProgram.vertex.setCoordinates(teapot);
        cubeMeshProgram.fragment.setTexture(teapotTexture);
        cubeMeshProgram.vertex.setModelViewProjectionMatrix(modelViewProjectionMatrix);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, teapot.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT, teapot.getIndices());

        cubeMeshProgram.vertex.disableAttributes();
        }

    protected void drawBuildings(float[] poseMatrix)
        {
        Assert.assertTrue(buildingsRequired());

        // Rotate / scale / position the buildings
        Matrix.rotateM(poseMatrix, 0, 90.0f, 1.0f, 0, 0);
        Matrix.scaleM(poseMatrix, 0, buildingsScale, buildingsScale, buildingsScale);

        float[] modelViewProjectionMatrix = new float[16];
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix.getData(), 0, poseMatrix, 0);

        // activate the shader program and bind the vertex/normal/tex coords
        cubeMeshProgram.useProgram();

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        cubeMeshProgram.vertex.setCoordinates(buildingsModel);
        cubeMeshProgram.fragment.setTexture(buildingsTexture);
        cubeMeshProgram.vertex.setModelViewProjectionMatrix(modelViewProjectionMatrix);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, buildingsModel.getNumObjectVertex());

        cubeMeshProgram.vertex.disableAttributes();
        }

    // https://stackoverflow.com/questions/5514149/capture-screen-of-glsurfaceview-to-bitmap
    protected @Nullable Bitmap glSurfaceToBitmap()
        {
        int w = viewport.extent.x;
        int h = viewport.extent.y;
        int x = viewport.lowerLeft.x;
        int y = viewport.lowerLeft.y;

        int[] bitmapBuffer = new int[w * h];
        int[] bitmapSource = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try
            {
            GLES20.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++)
                {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++)
                    {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                    }
                }
            }
        catch (GLException e)
            {
            return null;
            }

        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
        }

    //----------------------------------------------------------------------------------------------
    // Frame Queue management
    //----------------------------------------------------------------------------------------------

    @Override public BlockingQueue<CloseableFrame> getFrameQueue()
        {
        synchronized (this.frameQueueLock)
            {
            return frameQueue;
            }
        }

    @Override public void setFrameQueueCapacity(int capacity)
        {
        synchronized (this.frameQueueLock)
            {
            this.frameQueueCapacity = Math.max(0, capacity);
            if (capacity <= 0)
                {
                this.frameQueue = new ArrayBlockingQueue<CloseableFrame>(1); // dummy, never actually written to
                }
            else
                {
                EvictingBlockingQueue<CloseableFrame> queue = new EvictingBlockingQueue<CloseableFrame>(new ArrayBlockingQueue<CloseableFrame>(capacity));
                // Proactively close frames that the user doesn't consume to help reduce memory pressure
                queue.setEvictAction(new Consumer<CloseableFrame>()
                    {
                    @Override public void accept(CloseableFrame frame)
                        {
                        frame.close();
                        }
                    });
                this.frameQueue = queue;
                }
            }
        }

    @Override public int getFrameQueueCapacity()
        {
        synchronized (this.frameQueueLock)
            {
            return this.frameQueueCapacity;
            }
        }


    @Override
    public void getFrameBitmap(Continuation<? extends Consumer<Bitmap>> continuation)
        {
        synchronized (bitmapFrameLock)
            {
            bitmapContinuation = continuation;
            }
        }

    @Override public void getFrameOnce(Continuation<? extends Consumer<Frame>> getFrameOnce)
        {
        synchronized (frameQueueLock)
            {
            this.getFrameOnce = getFrameOnce;
            }
        }

    @Override public void enableConvertFrameToBitmap()
        {
        enableConvertFrameToFormat(PIXEL_FORMAT.RGB565); // RGBA8888 seems to always fail
        }

    @Override public boolean[] enableConvertFrameToFormat(int... pixelFormats)
        {
        boolean[] results = new boolean[pixelFormats.length];
        int i = 0;
        for (int pixelFormat : pixelFormats)
            {
            if (Vuforia.setFrameFormat(pixelFormat, true))
                {
                results[i] = true;
                }
            else
                {
                results[i] = false;
                tracer.traceError("enableConvertFrameToBitmap(): internal error: setFrameFormat(%d) failed", pixelFormat);
                }
            i++;
            }
        return results;
        }

    @Override public Bitmap convertFrameToBitmap(Frame frame)
        {
        int[] pixelFormats = new int[] { PIXEL_FORMAT.RGB565, PIXEL_FORMAT.RGBA8888 };

        for (int pixelFormat : pixelFormats)
            {
            for (int i = 0; i < frame.getNumImages(); i++)
                {
                Image image = frame.getImage(i);
                if (image.getFormat() == pixelFormat)
                    {
                    Bitmap.Config config;
                    switch (pixelFormat)
                        {
                        case PIXEL_FORMAT.RGB565: config = Bitmap.Config.RGB_565; break;
                        case PIXEL_FORMAT.RGBA8888: config = Bitmap.Config.ARGB_8888; break;
                        default:
                            continue;
                        }

                    Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), config);
                    bitmap.copyPixelsFromBuffer(image.getPixels());
                    return bitmap;
                    }
                }
            }

        return null;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    protected class VuforiaCallback implements  Vuforia.UpdateCallbackInterface
        {
        @Override public void Vuforia_onUpdate(State state)
            {
            synchronized (updateCallbackLock)
                {
                callbackCount++;

                // If the user wants to see frames, then give him the new one. Convert it to
                // a CloseableFrame so as to (a) make it liveable beyond the callback lifetime, and
                // (b) expose a close() method that can be used to proactively reclaim memory.
                Continuation<? extends Consumer<Frame>> capturedContinuation = null;
                synchronized (frameQueueLock)
                    {
                    if (frameQueueCapacity > 0)
                        {
                        CloseableFrame closeableFrame = new CloseableFrame(state.getFrame());
                        frameQueue.add(closeableFrame);
                        }
                    capturedContinuation = getFrameOnce;
                    getFrameOnce = null;
                    }
                if (capturedContinuation != null)
                    {
                    final CloseableFrame closeableFrame = new CloseableFrame(state.getFrame());
                    capturedContinuation.dispatch(new ContinuationResult<Consumer<Frame>>()
                        {
                        @Override public void handle(Consumer<Frame> frameConsumer)
                            {
                            frameConsumer.accept(closeableFrame);
                            closeableFrame.close();
                            }
                        });
                    }

                // Figure out which of our trackables are visible and which are not. Let each
                // one know its status.

                Set<VuforiaTrackable> notVisible = new HashSet<>();

                synchronized (loadedTrackableSets)
                    {
                    for (VuforiaTrackablesImpl trackables : loadedTrackableSets)
                        {
                        for (VuforiaTrackable vuforiaTrackable : trackables)
                            {
                            // Add the trackable itself
                            notVisible.add(vuforiaTrackable);

                            // Robustly take care of any parent / child relationships.
                            if (vuforiaTrackable instanceof VuforiaTrackableContainer)
                                {
                                notVisible.addAll(((VuforiaTrackableContainer)vuforiaTrackable).children());
                                }
                            VuforiaTrackable parent = vuforiaTrackable.getParent();
                            if (parent != null)
                                {
                                notVisible.add(parent);
                                }
                            }
                        }
                    }

                int numTrackables = state.getNumTrackableResults();
                for (int i = 0; i < numTrackables; i++)
                    {
                    TrackableResult trackableResult = state.getTrackableResult(i);
                    if (isObjectTargetTrackableResult(trackableResult))
                        {
                        Trackable trackable = trackableResult.getTrackable();
                        if (trackable != null)
                            {
                            VuforiaTrackable vuforiaTrackable = VuforiaTrackableImpl.from(trackable);
                            if (vuforiaTrackable != null)
                                {
                                notVisible.remove(vuforiaTrackable);
                                VuforiaTrackable parent = vuforiaTrackable.getParent();
                                if (parent != null)
                                    {
                                    notVisible.remove(parent);
                                    }
                                if (vuforiaTrackable instanceof VuforiaTrackableNotify)
                                    {
                                    ((VuforiaTrackableNotify)vuforiaTrackable).noteTracked(trackableResult, getCameraName(), getCamera());
                                    }
                                }
                            else
                                tracer.trace("vuforiaTrackable unexpectedly null: %s", trackableResult.getClass().getSimpleName());
                            }
                        else
                            tracer.trace("trackable unexpectedly null: %s", trackableResult.getClass().getSimpleName());
                        }
                    else
                        tracer.trace("unexpected TrackableResult: %s", trackableResult.getClass().getSimpleName());
                    }

                for (VuforiaTrackable vuforiaTrackable : notVisible)
                    {
                    if (vuforiaTrackable instanceof VuforiaTrackableNotify)
                        {
                        ((VuforiaTrackableNotify)vuforiaTrackable).noteNotTracked();
                        }
                    }
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected static void throwIfFail(boolean success)
        {
        if (!success)
            {
            throwFailure();
            }
        }

    protected static void throwIfFail(boolean success, String format, Object... args)
        {
        if (!success)
            {
            throwFailure(format, args);
            }
        }

    protected static void throwFailure()
        {
        throwFailure("Vuforia operation failed");
        }

    protected static void throwFailure(String format, Object... args)
        {
        throw new VuforiaException(format, args);
        }

    protected static void throwFailure(Exception nested)
        {
        throw new VuforiaException(nested, "Vuforia operation failed");
        }
    }
