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

import com.vuforia.DataSet;

import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link VuforiaTrackablesImpl} provides an implementation of the {@code VuforiaTrackables}
 * interface.
 */
@SuppressWarnings("WeakerAccess")
public class VuforiaTrackablesImpl extends AbstractList<VuforiaTrackable> implements VuforiaTrackables
    {
    //------------------------------------------------------------------------------------------
    // State
    //------------------------------------------------------------------------------------------

    VuforiaLocalizerImpl vuforiaLocalizer;
    DataSet              dataSet;
    String               name;
    boolean              isActive;
    List<VuforiaTrackableImpl> trackables;

    //------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------

    public VuforiaTrackablesImpl(VuforiaLocalizerImpl vuforiaLocalizer, DataSet dataSet, Class<? extends VuforiaTrackable.Listener> listenerClass)
        {
        this.vuforiaLocalizer = vuforiaLocalizer;
        this.dataSet = dataSet;
        this.isActive = false;
        this.trackables = new ArrayList<>(this.dataSet.getNumTrackables());
        for (int i = 0; i < this.dataSet.getNumTrackables(); i++)
            {
            VuforiaTrackableImpl trackableImpl = new VuforiaTrackableImpl(this, i, listenerClass);
            this.trackables.add(trackableImpl);
            }
        }

    @Override public synchronized void setName(String name)
        {
        this.name = name;
        for (VuforiaTrackableImpl trackable : this.trackables)
            {
            if (trackable.getName() == null)
                {
                trackable.setName(name);
                }
            }
        }

    @Override public synchronized String getName()
        {
        return this.name;
        }

    //------------------------------------------------------------------------------------------
    // Accessing
    //------------------------------------------------------------------------------------------

    @Override public int size()
        {
        return this.trackables.size();
        }

    @Override public VuforiaTrackable get(int index)
        {
        return this.trackables.get(index);
        }

    @Override public VuforiaLocalizer getLocalizer()
        {
        return this.vuforiaLocalizer;
        }

    //------------------------------------------------------------------------------------------
    // Life-cycle
    //------------------------------------------------------------------------------------------

    @Override synchronized public void activate()
        {
        if (!isActive)
            {
            VuforiaLocalizerImpl.throwIfFail(VuforiaLocalizerImpl.getObjectTracker().activateDataSet(this.dataSet));
            isActive = true;

            adjustExtendedTracking(vuforiaLocalizer.isExtendedTrackingActive);
            }
        }

    @Override synchronized public void deactivate()
        {
        if (isActive)
            {
            VuforiaLocalizerImpl.throwIfFail(VuforiaLocalizerImpl.getObjectTracker().deactivateDataSet(this.dataSet));
            isActive = false;
            }
        }

    public void adjustExtendedTracking(boolean isExtendedTrackingActive)
        {
        if (isActive)
            {
            for (VuforiaTrackableImpl trackable : this.trackables)
                {
                if (isExtendedTrackingActive)
                    trackable.getTrackable().startExtendedTracking();
                else
                    trackable.getTrackable().stopExtendedTracking();
                }
            }
        }

    public void destroy()
        {
        deactivate();
        VuforiaLocalizerImpl.getObjectTracker().destroyDataSet(this.dataSet);
        this.dataSet = null;
        }
    }
