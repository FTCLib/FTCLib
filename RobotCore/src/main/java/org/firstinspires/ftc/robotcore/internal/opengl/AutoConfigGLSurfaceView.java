/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

package org.firstinspires.ftc.robotcore.internal.opengl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

import com.qualcomm.robotcore.util.RobotLog;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * {@link AutoConfigGLSurfaceView} is responsible for setting up and configuring the OpenGL surface
 * view we use to render localization monitoring.
 */
public class AutoConfigGLSurfaceView extends GLSurfaceView
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private static final String LOGTAG = "LocalizationGLView";

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AutoConfigGLSurfaceView(Context context)
        {
        super(context);
        }

    //----------------------------------------------------------------------------------------------
    // Initialization
    //----------------------------------------------------------------------------------------------

    public void init(boolean translucent, int depth, int stencil)
        {
        // By default GLSurfaceView tries to find a surface that is as close as possible to a
        // 16-bit RGB frame buffer with a 16-bit depth buffer. This function can override the
        // default values and set custom values.

        // By default, GLSurfaceView() creates a RGB_565 opaque surface. If we want a translucent
        // one, we should change the surface's format here, using PixelFormat.TRANSLUCENT for
        // GL Surfaces is interpreted as any 32-bit surface with alpha by SurfaceFlinger.

        // If required, set translucent format to allow camera image to show through in the background
        if (translucent)
            {
            this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            }

        // Setup the context factory for 2.0 rendering
        setEGLContextFactory(new ContextFactory());

        // We need to choose an EGLConfig that matches the format of our surface exactly. This
        // is going to be done in our custom config chooser. See ConfigChooser class definition below.
        setEGLConfigChooser(translucent ? new ConfigChooser(8, 8, 8, 8, depth, stencil) : new ConfigChooser(5, 6, 5, 0, depth, stencil));
        }

    // Creates OpenGL contexts.
    private static class ContextFactory implements EGLContextFactory
        {
        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig)
            {
            EGLContext context;

            checkEglError("Before eglCreateContext", egl);
            int[] attrib_list_gl20 = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
            context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list_gl20);

            checkEglError("After eglCreateContext", egl);
            return context;
            }


        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context)
            {
            egl.eglDestroyContext(display, context);
            }
        }

    // Checks the OpenGL error.
    private static void checkEglError(String prompt, EGL10 egl)
        {
        int error;
        while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS)
            {
            RobotLog.ee(LOGTAG, "%s: EGL error: 0x%x", prompt, error);
            }
        }

    // The config chooser.
    private static class ConfigChooser implements EGLConfigChooser
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
        private int[] mValue = new int[1];

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        public ConfigChooser(int r, int g, int b, int a, int depth, int stencil)
            {
            mRedSize = r;
            mGreenSize = g;
            mBlueSize = b;
            mAlphaSize = a;
            mDepthSize = depth;
            mStencilSize = stencil;
            }

        //------------------------------------------------------------------------------------------
        // Operations
        //------------------------------------------------------------------------------------------

        private EGLConfig getMatchingConfig(EGL10 egl, EGLDisplay display, int[] configAttribs)
            {
            // Get the number of minimally matching EGL configurations
            int[] num_config = new int[1];
            egl.eglChooseConfig(display, configAttribs, null, 0, num_config);

            int numConfigs = num_config[0];
            if (numConfigs <= 0)
                throw new IllegalArgumentException("No matching EGL configs");

            // Allocate then read the array of minimally matching EGL configs
            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, configAttribs, configs, numConfigs, num_config);

            // Now return the "best" one
            return chooseConfig(egl, display, configs);
            }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display)
            {
            // This EGL config specification is used to specify 2.0
            // rendering. We use a minimum size of 4 bits for
            // red/green/blue, but will perform actual matching in
            // chooseConfig() below.
            final int EGL_OPENGL_ES2_BIT = 0x0004;
            final int[] s_configAttribs_gl20 = {EGL10.EGL_RED_SIZE, 4,
                    EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_NONE};

            return getMatchingConfig(egl, display, s_configAttribs_gl20);
            }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs)
            {
            for (EGLConfig config : configs)
                {
                int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);

                // We need at least mDepthSize and mStencilSize bits
                if (d < mDepthSize || s < mStencilSize)
                    continue;

                // We want an *exact* match for red/green/blue/alpha
                int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);

                if (r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize)
                    return config;
                }

            return null;
            }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue)
            {
            if (egl.eglGetConfigAttrib(display, config, attribute, mValue))
                return mValue[0];

            return defaultValue;
            }
        }
    }
