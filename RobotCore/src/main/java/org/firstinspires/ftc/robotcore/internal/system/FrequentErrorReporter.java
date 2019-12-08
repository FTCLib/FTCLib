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
package org.firstinspires.ftc.robotcore.internal.system;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.util.RobotLog;

/**
 * {@link FrequentErrorReporter} is a little utility that suppresses runs of the same
 * error so as to avoid flooding logs
 */
@SuppressWarnings("WeakerAccess")
public class FrequentErrorReporter<T>
    {
    protected T value;

    public FrequentErrorReporter()
        {
        reset();
        }

    public void reset()
        {
        this.value = null;
        }

    public synchronized void aa(@NonNull T value, String tag, String format, Object...args)
        {
        Assert.assertNotNull(value);
        if (this.value == null || !this.value.equals(value))
            {
            this.value = value;
            RobotLog.aa(tag, format, args);
            }
        }

    public synchronized void vv(@NonNull T value, String tag, String format, Object...args)
        {
        Assert.assertNotNull(value);
        if (this.value == null || !this.value.equals(value))
            {
            this.value = value;
            RobotLog.vv(tag, format, args);
            }
        }

    public synchronized void dd(@NonNull T value, String tag, String format, Object...args)
        {
        Assert.assertNotNull(value);
        if (this.value == null || !this.value.equals(value))
            {
            this.value = value;
            RobotLog.dd(tag, format, args);
            }
        }

    public synchronized void ii(@NonNull T value, String tag, String format, Object...args)
        {
        Assert.assertNotNull(value);
        if (this.value == null || !this.value.equals(value))
            {
            this.value = value;
            RobotLog.ii(tag, format, args);
            }
        }

    public synchronized void ww(@NonNull T value, String tag, String format, Object...args)
        {
        Assert.assertNotNull(value);
        if (this.value == null || !this.value.equals(value))
            {
            this.value = value;
            RobotLog.ww(tag, format, args);
            }
        }

    public synchronized void ee(@NonNull T value, String tag, String format, Object...args)
        {
        Assert.assertNotNull(value);
        if (this.value == null || !this.value.equals(value))
            {
            this.value = value;
            RobotLog.ee(tag, format, args);
            }
        }
    }
