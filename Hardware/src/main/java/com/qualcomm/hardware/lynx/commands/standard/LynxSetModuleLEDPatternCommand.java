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
package com.qualcomm.hardware.lynx.commands.standard;

import android.graphics.Color;
import androidx.annotation.ColorInt;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.commands.LynxDatagram;
import com.qualcomm.robotcore.hardware.Blinker;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by bob on 2016-05-07.
 */
public class LynxSetModuleLEDPatternCommand extends LynxStandardCommand<LynxAck>
    {
    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    public static class Steps implements Iterable<Blinker.Step>
        {
        ArrayList<Blinker.Step> steps = new ArrayList<>(maxStepCount);

        public void add(Blinker.Step step)
            {
            if (steps.size() < maxStepCount)
                {
                this.steps.add(step);
                }
            }
        public void add(int index, Blinker.Step step)
            {
            if (index < maxStepCount)
                {
                this.steps.add(index, step);
                }
            }
        public Iterator<Blinker.Step> iterator()
            {
            return this.steps.iterator();
            }
        public int size()
            {
            return steps.size();
            }
        public int cbSerialize()
            {
            if (this.size() == maxStepCount)
                {
                return this.steps.size() * cbSerializeStep();
                }
            else
                {
                return (this.steps.size()+1) * cbSerializeStep();
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    Steps steps = new Steps();

    public static final int maxStepCount = 16;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxSetModuleLEDPatternCommand(LynxModule module)
        {
        super(module);
        }

    public LynxSetModuleLEDPatternCommand(LynxModule module, Steps steps)
        {
        this(module);
        this.steps = steps;
        }

    //----------------------------------------------------------------------------------------------
    // Step Serialization
    //----------------------------------------------------------------------------------------------

    public static void serializeStep(Blinker.Step step, ByteBuffer buffer)
        {
        int msDuration = step.getDurationMs();
        int tenthsDuration = (int)Math.round(msDuration / 100.);

        @ColorInt int color = step.getColor();
        buffer.put((byte)Math.min(255, tenthsDuration));
        buffer.put((byte)Color.blue(color));
        buffer.put((byte)Color.green(color));
        buffer.put((byte)Color.red(color));
        }

    public static void deserializeStep(Blinker.Step step, ByteBuffer buffer)
        {
        int tenths = buffer.get();
        int ms     = tenths * 100;
        step.setDuration(ms, TimeUnit.MILLISECONDS);

        byte b = buffer.get();
        byte g = buffer.get();
        byte r = buffer.get();
        step.setColor(Color.rgb(r, g, b));
        }

    public static int cbSerializeStep()
        {
        return 4;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static int getStandardCommandNumber()
        {
        return COMMAND_NUMBER_SET_MODULE_LED_PATTERN;
        }

    @Override
    public boolean isResponseExpected()
        {
        return false;
        }

    @Override
    public int getCommandNumber()
        {
        return getStandardCommandNumber();
        }

    @Override
    public byte[] toPayloadByteArray()
        {
        int cbPayload = this.steps.cbSerialize();
        ByteBuffer buffer = ByteBuffer.allocate(cbPayload).order(LynxDatagram.LYNX_ENDIAN);
        for (Blinker.Step step : this.steps)
            {
            serializeStep(step, buffer);
            }
        if (this.steps.size() < maxStepCount)
            {
            serializeStep(Blinker.Step.nullStep(), buffer);
            }

        return buffer.array();
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        ByteBuffer buffer = ByteBuffer.wrap(rgb).order(LynxDatagram.LYNX_ENDIAN);
        this.steps = new Steps();
        while (buffer.remaining() >= cbSerializeStep())
            {
            Blinker.Step step = new Blinker.Step();
            deserializeStep(step, buffer);
            this.steps.add(step);
            }
        }
    }
