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
package org.firstinspires.ftc.robotcore.internal.ui;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link LocalByRefRequestCodeHolder} is intended to allow context to be passed
 * from startActivityForResult() to it's subsequent onActivityResult() by way of information
 * keyed by its (dynamic) request code.
 */
@SuppressWarnings("WeakerAccess")
public class LocalByRefRequestCodeHolder<T>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = LocalByRefRequestCodeHolder.class.getSimpleName();

    protected final static AtomicInteger requestCodeGenerator = new AtomicInteger(1000000);
    private static final Map<Integer, LocalByRefRequestCodeHolder> mapRequestCodeToHolder = new ConcurrentHashMap<>();

    protected int actualRequestCode;
    protected int userRequestCode;
    protected T target;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LocalByRefRequestCodeHolder(int userRequestCode, T target)
        {
        this.actualRequestCode = requestCodeGenerator.getAndIncrement();
        this.userRequestCode = userRequestCode;
        this.target = target;
        mapRequestCodeToHolder.put(this.actualRequestCode, this);
        }

    public int getActualRequestCode()
        {
        return this.actualRequestCode;
        }

    public int getUserRequestCode()
        {
        return this.userRequestCode;
        }

    public T getTargetAndForget()
        {
        mapRequestCodeToHolder.remove(this.actualRequestCode);
        return target;
        }

    public static @Nullable LocalByRefRequestCodeHolder from(int actualRequestCode)
        {
        return mapRequestCodeToHolder.get(actualRequestCode);
        }

    }
