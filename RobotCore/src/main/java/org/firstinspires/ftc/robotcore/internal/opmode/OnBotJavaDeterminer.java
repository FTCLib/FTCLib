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
package org.firstinspires.ftc.robotcore.internal.opmode;

import com.qualcomm.robotcore.util.RobotLog;

import java.lang.reflect.Constructor;

public class OnBotJavaDeterminer {

    private static String TAG = "OnBotJavaDeterminer";
    private static String onBotJavaConstructorName = "org.firstinspires.ftc.onbotjava.OnBotJavaClassLoader";

    public static boolean isOnBotJava(Class clazz)
    {
        ClassLoader classLoader = clazz.getClassLoader();
        Class targetClazz = classLoader.getClass();
        boolean result;

        try {
            Constructor ctor = targetClazz.getConstructor();
            String classLoaderName = ctor.getName();
            if (classLoaderName.equals(onBotJavaConstructorName)) {
                result = true;
            } else {
                result = false;
            }
        } catch (NoSuchMethodException e) {
            RobotLog.vv(TAG, "isOnBotJava: class=%s loader=%s: %s", clazz.getSimpleName(), classLoader.getClass().getSimpleName(), false);
            return false;
        }

        RobotLog.vv(TAG, "isOnBotJava: class=%s loader=%s: %s", clazz.getSimpleName(), classLoader.getClass().getSimpleName(), result);
        return result;
    }

}
