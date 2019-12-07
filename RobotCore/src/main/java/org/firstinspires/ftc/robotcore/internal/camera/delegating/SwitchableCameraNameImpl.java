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
package org.firstinspires.ftc.robotcore.internal.camera.delegating;

import android.content.Context;

import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiCameraCharacteristicsBuilder;
import org.firstinspires.ftc.robotcore.internal.camera.names.CameraNameImplBase;
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class SwitchableCameraNameImpl extends CameraNameImplBase implements SwitchableCameraName
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "SwitchableCameraName";
    public static boolean TRACE = true;
    protected final Tracer tracer = Tracer.create(TAG, TRACE);

    protected final CameraName[] members;

    @Override public String toString()
        {
        StringBuilder builder = new StringBuilder();
        builder.append("Switchable(");
        boolean first = true;
        for (CameraName name : members)
            {
            if (!first) builder.append("|");
            builder.append(name.toString());
            first = false;
            }
        builder.append(")");
        return builder.toString();
        }

    //----------------------------------------------------------------------------------------------
    // Construction (all internal)
    //----------------------------------------------------------------------------------------------

    private SwitchableCameraNameImpl(CameraName... members)
        {
        if (members.length==0) throw Misc.illegalArgumentException("the list of CameraNames cannot be empty");

        // Flatten internal switchables (why?)
        Set<CameraName> nameSet = new LinkedHashSet<>();
        for (CameraName name : members)
            {
            add(nameSet, name);
            }

        this.members = nameSet.toArray(new CameraName[nameSet.size()]);
        }

    public static SwitchableCameraName forSwitchable(CameraName... names)
        {
        return new SwitchableCameraNameImpl(names);
        }

    protected static void add(Set<CameraName> set, CameraName name)
        {
        if (name == null)
            {
            throw Misc.illegalArgumentException("a member of a SwitchableCameraName cannot be null");
            }
        else if (name instanceof SwitchableCameraName)
            {
            SwitchableCameraName switchableCameraName = (SwitchableCameraName)name;
            for (CameraName member : switchableCameraName.getMembers())
                {
                add(set, member);
                }
            }
        else
            set.add(name);
        }

    //----------------------------------------------------------------------------------------------
    // Equality
    //----------------------------------------------------------------------------------------------

    @Override public boolean equals(Object o)
        {
        if (o instanceof SwitchableCameraNameImpl)
            {
            SwitchableCameraNameImpl them = (SwitchableCameraNameImpl)o;
            return Arrays.equals(members, them.members);
            }
        return super.equals(o);
        }

    @Override public int hashCode()
        {
        return Arrays.hashCode(members);
        }

    //----------------------------------------------------------------------------------------------
    // CameraName
    //----------------------------------------------------------------------------------------------

    @Override public boolean isSwitchable()
        {
        return true;
        }

    /**
     * We need to get permission from all the member cameras.
     */
    @Override public void asyncRequestCameraPermission(Context context, Deadline deadline, final Continuation<? extends Consumer<Boolean>> continuation)
        {
        final MutableReference<Boolean> goodSoFar = new MutableReference<>(true);
        final MutableReference<Integer> responsesToCome = new MutableReference<>(members.length);
        final MutableReference<Boolean> calledContinuation = new MutableReference<>(false);

        Continuation<? extends Consumer<Boolean>> localContinuation = Continuation.create(ThreadPool.getDefault(), new Consumer<Boolean>()
            {
            @Override public void accept(Boolean value)
                {
                synchronized (goodSoFar) // as good as any to lock on
                    {
                    final boolean stillGood = goodSoFar.getValue() && value;
                    goodSoFar.setValue(stillGood);
                    responsesToCome.setValue(responsesToCome.getValue()-1);

                    if (!stillGood || responsesToCome.getValue()==0)
                        {
                        if (!calledContinuation.getValue())
                            {
                            calledContinuation.setValue(true);
                            continuation.dispatch(new ContinuationResult<Consumer<Boolean>>()
                                {
                                @Override public void handle(Consumer<Boolean> booleanConsumer)
                                    {
                                    booleanConsumer.accept(stillGood);
                                    }
                                });
                            }
                        }
                    }
                }
            });

        for (CameraName cameraName : members)
            {
            cameraName.asyncRequestCameraPermission(context, deadline, localContinuation);
            }
        }

    /**
     * The characteristics we project is the intersection of that of all of our members.
     */
    @Override public CameraCharacteristics getCameraCharacteristics()
        {
        Set<CameraCharacteristics.CameraMode> commonModes = null;
        for (CameraName member : getMembers())
            {
            CameraCharacteristics memberCharacteristics = member.getCameraCharacteristics();
            tracer.trace("memberCharacteristics: %s", memberCharacteristics);
            Set<CameraCharacteristics.CameraMode> memberModes = new HashSet<>(memberCharacteristics.getAllCameraModes());
            if (commonModes == null)
                {
                commonModes = memberModes;
                }
            else
                {
                commonModes = Misc.intersect(commonModes, memberModes);
                }
            }
        CameraCharacteristics result = UvcApiCameraCharacteristicsBuilder.buildFromModes(commonModes);
        tracer.trace("result = %s", result);
        return result;
        }

    //----------------------------------------------------------------------------------------------
    // SwitchableCameraName
    //----------------------------------------------------------------------------------------------

    @Override public CameraName[] getMembers()
        {
        return members;
        }

    @Override public boolean allMembersAreWebcams()
        {
        for (CameraName cameraName : members)
            {
            if (!cameraName.isWebcam())
                {
                return false;
                }
            }
        return true;
        }
    }
