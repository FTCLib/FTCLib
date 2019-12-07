/*
 * Copyright (c) 2018 Craig MacFarlane
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Craig MacFarlane nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.firstinspires.ftc.onbotjava;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.opmode.ClassManager;
import org.firstinspires.ftc.robotcore.internal.opmode.OnBotJavaHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dalvik.system.DexFile;


public class OnBotJavaHelperImpl implements OnBotJavaHelper {

    private final static String TAG = "OnBotJavaHelperImpl";

    @Override
    public ClassLoader getOnBotJavaClassLoader()
    {
        return new OnBotJavaClassLoader();
    }

    @Override
    public Set<String> getOnBotJavaClassNames()
    {
        Set<String> classNames = new HashSet<String>();
        OnBotJavaClassLoader onBotJavaClassLoader = new OnBotJavaClassLoader();
        try
        {
            for (DexFile dexFile : onBotJavaClassLoader.getDexFiles())
            {
                for (String name : Collections.list(dexFile.entries())) {
                    RobotLog.ii(TAG, dexFile.getName() + ": " + name);
                    classNames.add(name);
                }

            }
            return classNames;
        }
        finally
        {
            onBotJavaClassLoader.close();
        }
    }

    @Override
    public void close(ClassLoader classLoader)
    {
        if (classLoader instanceof OnBotJavaClassLoader) {
            ((OnBotJavaClassLoader) classLoader).close();
        } else {
            RobotLog.ee(TAG, "Attempt to close a non-closable class loader: " + classLoader.getClass().getSimpleName());
        }
    }
}
