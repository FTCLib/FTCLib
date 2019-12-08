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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.VuMarkTarget;
import com.vuforia.VuMarkTemplate;

import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.VuMarkInstanceId;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link VuforiaTrackableImpl} is our system implementation of {@link VuforiaTrackable}
 */
@SuppressWarnings("WeakerAccess")
public class VuforiaTrackableImpl implements VuforiaTrackable, VuforiaTrackableNotify, VuforiaTrackableContainer
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected VuforiaTrackable      parent;
    protected Trackable             trackable;
    protected VuforiaTrackablesImpl trackables;
    protected String                name;
    protected Listener              listener;
    protected Object                userData;

    protected final Object          locationLock = new Object();
    protected @NonNull OpenGLMatrix ftcFieldFromTarget;

    protected Class<? extends VuforiaTrackable.Listener> listenerClass;
    protected final Map<VuMarkInstanceId, VuforiaTrackable> vuMarkMap = new HashMap<>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public VuforiaTrackableImpl(VuforiaTrackablesImpl trackables, int index, Class<? extends VuforiaTrackable.Listener> listenerClass)
        {
        this(null, trackables, trackables.dataSet.getTrackable(index), listenerClass);
        }
    public VuforiaTrackableImpl(VuforiaTrackable parent, VuforiaTrackablesImpl trackables, Trackable trackable, Class<? extends VuforiaTrackable.Listener> listenerClass)
        {
        this.parent = parent;
        this.trackable = trackable;
        this.trackables = trackables;
        this.userData = null;
        this.ftcFieldFromTarget = OpenGLMatrix.identityMatrix();
        this.name = null;
        this.listenerClass = listenerClass;
        try {
            Constructor<? extends VuforiaTrackable.Listener> ctor=listenerClass.getConstructor(VuforiaTrackable.class);
            try {
                this.listener = ctor.newInstance(this);
                }
            catch (InstantiationException|IllegalAccessException|InvocationTargetException e)
                {
                throw new RuntimeException("unable to instantiate " + listenerClass.getSimpleName(), e);
                }
            }
        catch (NoSuchMethodException e)
            {
            throw new RuntimeException("class " + listenerClass.getSimpleName() + " missing VuforiaTrackable ctor");
            }
        this.trackable.setUserData(this);
        }

    static VuforiaTrackable from(com.vuforia.Trackable trackable)
        {
        /**
         * If we have a VuMark target, then we need to dyn-create our wrapper from the template.
         */
        if (trackable.isOfType(VuMarkTarget.getClassType()))
            {
            VuMarkTarget vuMarkTarget = (VuMarkTarget)trackable;
            VuMarkTemplate vuMarkTemplate = vuMarkTarget.getTemplate();
            VuforiaTrackableContainer vuforiaTrackableContainer = (VuforiaTrackableContainer)from(vuMarkTemplate);
            return vuforiaTrackableContainer.getChild(vuMarkTarget);
            }
        else
            {
            return (VuforiaTrackable)trackable.getUserData();
            }
        }

    public static VuforiaTrackable from(TrackableResult trackableResult)
        {
        return from(trackableResult.getTrackable());
        }

    //----------------------------------------------------------------------------------------------
    // VuforiaTrackableContainer
    //----------------------------------------------------------------------------------------------

    @Override
    public VuforiaTrackable getChild(VuMarkTarget vuMarkTarget)
        {
        synchronized (vuMarkMap)
            {
            VuMarkInstanceId instanceId = new VuMarkInstanceId(vuMarkTarget.getInstanceId());
            VuforiaTrackable result = vuMarkMap.get(instanceId);
            if (null == result)
                {
                result = new VuforiaTrackableImpl(this, trackables, vuMarkTarget, listenerClass);
                vuMarkMap.put(instanceId, result);
                }
            return result;
            }
        }

    @Override
    public List<VuforiaTrackable> children()
        {
        synchronized (vuMarkMap)
            {
            return new ArrayList<>(vuMarkMap.values());
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    @Override public synchronized void setListener(Listener listener)
        {
        // We *always* have a listener
        this.listener = listener==null ? new VuforiaTrackableDefaultListener(this) : listener;

        // Make sure they know they're listening to us
        this.listener.addTrackable(this);
        }

    @Override public synchronized Listener getListener()
        {
        return this.listener;
        }

    @Override public synchronized void setUserData(Object object)
        {
        this.userData = object;
        }

    @Override public synchronized Object getUserData()
        {
        return this.userData;
        }

    @Override public VuforiaTrackables getTrackables()
        {
        return trackables;
        }

    @Override public void setLocationFtcFieldFromTarget(@NonNull OpenGLMatrix ftcFieldFromTarget)
        {
        /** Separate lock so as to accommodate upcalls from {@link VuforiaTrackableDefaultListener} */
        synchronized (this.locationLock)
            {
            this.ftcFieldFromTarget = ftcFieldFromTarget;
            }
        }

    @Override public void setLocation(@NonNull OpenGLMatrix location)
        {
        setLocationFtcFieldFromTarget(location);
        }

    @Override public @NonNull OpenGLMatrix getFtcFieldFromTarget()
        {
        synchronized (this.locationLock)
            {
            return this.ftcFieldFromTarget;
            }
        }

    @Override public @NonNull OpenGLMatrix getLocation()
        {
        return getFtcFieldFromTarget();
        }


    @Override public String getName()
        {
        return this.name;
        }

    @Override public void setName(String name)
        {
        this.name = name;
        }

    public Trackable getTrackable()
        {
        return trackable;
        }

    @Override public VuforiaTrackable getParent()
        {
        return parent;
        }

    @Override public synchronized void noteNotTracked()
        {
        this.getListener().onNotTracked();

        // We do NOT notify our parent, as in general there may be several children
        // with the same parent, and only one of them visible at a time. Taking care
        // of this relationship is thus the responsibility of our caller.
        }

    @Override public synchronized void noteTracked(TrackableResult trackableResult, CameraName cameraName, @Nullable Camera camera)
        {
        this.getListener().onTracked(trackableResult, cameraName, camera, null);
        if (parent instanceof VuforiaTrackableNotify)
            {
            parent.getListener().onTracked(trackableResult, cameraName, camera, this);
            }
        }
    }
