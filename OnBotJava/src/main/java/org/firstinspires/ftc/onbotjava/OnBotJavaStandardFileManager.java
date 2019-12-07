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
package org.firstinspires.ftc.onbotjava;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

/**
 * {@link OnBotJavaStandardFileManager} manages access to the on-bot java files on behalf
 * of the java compiler. We extend the simple delegation of {@link OnBotJavaDelegatingStandardFileManager}
 * by adding the configuration information specific to the On-Bot Java environment
 */
@SuppressWarnings("WeakerAccess")
public class OnBotJavaStandardFileManager extends OnBotJavaDelegatingStandardFileManager
    {
    //----------------------------------------------------------------------------------------------
    // Static State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = OnBotJavaManager.TAG + ":FileManager";

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public OnBotJavaStandardFileManager(StandardJavaFileManager delegate)
        {
        super(delegate);

        try {
            // classpath includes all the robot controller libraries, and anything
            // the user has put there too, for that matter.
            List<File> classPath = new ArrayList<File>();
            classPath.addAll(AppUtil.getInstance().filesIn(OnBotJavaManager.libDir, ".jar"));

            delegate.setLocation(StandardLocation.CLASS_OUTPUT,        Collections.singletonList(OnBotJavaManager.classesOutputDir));
            delegate.setLocation(StandardLocation.SOURCE_OUTPUT,       Collections.singletonList(OnBotJavaManager.sourceOutputDir));
            delegate.setLocation(StandardLocation.CLASS_PATH,          classPath);
            delegate.setLocation(StandardLocation.SOURCE_PATH,         Collections.singletonList(OnBotJavaManager.srcDir)); // will likely get overridden
            delegate.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, Collections.<File>emptyList());                   // we have no external annotation processors
            delegate.setLocation(StandardLocation.PLATFORM_CLASS_PATH, Collections.singletonList(new File(OnBotJavaManager.libDir, OnBotJavaManager.platformClassPathName)));
            }
        catch (IOException e)
            {
            throw AppUtil.getInstance().unreachable(OnBotJavaManager.TAG, e);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public void setSourcePath(Iterable<? extends File> srcPath)
        {
        try {
            delegate.setLocation(StandardLocation.SOURCE_PATH, srcPath);
            }
        catch (IOException e)
            {
            throw AppUtil.getInstance().unreachable(OnBotJavaManager.TAG, e);
            }
        }
    }