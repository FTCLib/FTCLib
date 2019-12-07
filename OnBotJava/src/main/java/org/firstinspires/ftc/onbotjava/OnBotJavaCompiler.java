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

import com.qualcomm.robotcore.util.RobotLog;
import com.sun.tools.javac.api.JavacTool;

import org.firstinspires.ftc.robotcore.external.Predicate;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

/**
 * {@link OnBotJavaCompiler} guides our access to and usage of the java compiler tool.
 *
 * http://docs.oracle.com/javase/7/docs/technotes/tools/solaris/javac.html
 * https://www.ibm.com/developerworks/library/j-jcomp/
 * http://stackoverflow.com/questions/12173294/compile-code-fully-in-memory-with-javax-tools-javacompiler
 * https://github.com/trung/InMemoryJavaCompiler
 * http://www.java2s.com/Code/Java/JDK-6/CompilingfromMemory.htm
 * http://atamur.blogspot.com/2009/10/using-built-in-javacompiler-with-custom.html
 *
 * http://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file
 */
@SuppressWarnings("WeakerAccess")
public class OnBotJavaCompiler
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = OnBotJavaManager.TAG + ":Compiler";

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public boolean compile(File srcRoot, OnBotJavaDiagnosticsListener diagnosticListener)
        {
        List<File> javaFiles = javaFilesUnder(srcRoot);

        JavacTool javac = JavacTool.create();
        OnBotJavaStandardFileManager fileManager = new OnBotJavaStandardFileManager(javac.getStandardFileManager(diagnosticListener, null, null));
        fileManager.setSourcePath(Collections.singleton(srcRoot));

        Iterable<? extends JavaFileObject> javaFileObjects = fileManager.getJavaFileObjects(javaFiles.toArray(new File[javaFiles.size()]));
        if (javaFileObjects.iterator().hasNext())
            {
            // Our list of options here matches what's used by Android Studio's build process, as empirically determined
            List<String> options = Arrays.asList(
                    "-source", "1.7",
                    "-target", "1.7",
                    "-g",                       // generate all debug info
                    "-encoding", "UTF-8",
                    "-Xlint:unchecked",         // shows details of unchecked or unsafe usage
                    "-Xlint:deprecation",       // shows details of deprecation usage
                    "-XDuseUnsharedTable=true"  // work around javac memory bug (?)
            );
            try {
                JavaCompiler.CompilationTask task = javac.getTask(
                        diagnosticListener.getWriter(),
                        fileManager,
                        diagnosticListener,
                        options,
                        null,   // Iterable<String> classes, names of classes to be processed by annotation processing, null means no class names
                        javaFileObjects);

                return task.call();
                }
            catch (RuntimeException e)
                {
                // https://docs.oracle.com/javase/7/docs/api/index.html?javax/tools/JavaCompiler.html
                // "if an unrecoverable error occurred in a user-supplied component. The cause will be the error in user code."
                //
                // Generally, the error will have already been reported to the user, so we need not
                // do so here.
                //
                RobotLog.logStackTrace(TAG, e);
                return false;
                }
            }
        else
            {
            RobotLog.vv(TAG, "no source files; omitting javac compile");
            return true;
            }
        }

    protected List<File> javaFilesUnder(File src)
        {
        return AppUtil.getInstance().filesUnder(src, new Predicate<File>()
            {
            @Override public boolean test(File file)
                {
                return file.getName().endsWith(".java");
                }
            });
        }

    }