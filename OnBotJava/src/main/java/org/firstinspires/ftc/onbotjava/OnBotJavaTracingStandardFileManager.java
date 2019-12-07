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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * {@link OnBotJavaTracingStandardFileManager} provides tracing to {@link StandardJavaFileManager} methods.
 */
@SuppressWarnings("WeakerAccess")
public class OnBotJavaTracingStandardFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> implements StandardJavaFileManager
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = OnBotJavaManager.TAG + ":FileManager";

    protected StandardJavaFileManager delegate;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public OnBotJavaTracingStandardFileManager(StandardJavaFileManager delegate)
        {
        super(delegate);
        this.delegate = delegate;
        }

    //---------------------------------------------------------------------------------------------
    // JavaFileManager
    //----------------------------------------------------------------------------------------------

    @Override public void close() throws IOException
        {
        RobotLog.dd(TAG, "close()");
        super.close();
        }

    @Override public void flush() throws IOException
        {
        RobotLog.dd(TAG, "flush()");
        super.flush();
        }

    @Override
    public ClassLoader getClassLoader(Location location)
        {
        RobotLog.dd(TAG, "getClassLoader(%s)", location);
        return super.getClassLoader(location);
        }

    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException
        {
        FileObject result = super.getFileForInput(location, packageName, relativeName);
        RobotLog.dd(TAG, "getFileForInput(%s, %s, %s) -> %s", location, packageName, relativeName, result);
        return result;
        }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException
        {
        FileObject result = super.getFileForOutput(location, packageName, relativeName, sibling);
        RobotLog.dd(TAG, "getFileForOutput(%s, %s, %s, %s) -> %s", location, packageName, relativeName, sibling, result);
        return result;
        }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException
        {
        JavaFileObject result = super.getJavaFileForInput(location, className, kind);
        RobotLog.dd(TAG, "getJavaFileForInput(%s, %s, %s) -> %s", location, className, kind, result);
        return result;
        }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException
        {
        JavaFileObject result = super.getJavaFileForOutput(location, className, kind, sibling);
        RobotLog.dd(TAG, "getJavaFileForOutput(%s, %s, %s, %s) -> %s", location, className, kind, sibling, result);
        return result;
        }

    @Override public boolean handleOption(String current, Iterator<String> remaining)
        {
        RobotLog.dd(TAG, "handleOption(%s, %s)", current, remaining);
        return super.handleOption(current, remaining);
        }

    @Override public boolean hasLocation(Location location)
        {
        boolean result = super.hasLocation(location);
        RobotLog.dd(TAG, "hasLocation(%s) -> %s", location, result);
        return result;
        }

    @Override public String inferBinaryName(Location location, JavaFileObject file)
        {
        String result = super.inferBinaryName(location, file);
        RobotLog.dd(TAG, "inferBinaryName(%s, %s) -> %s", location, file, result);
        return result;
        }

    @Override public boolean isSameFile(FileObject a, FileObject b)
        {
        boolean result = super.isSameFile(a, b);
        RobotLog.dd(TAG, "isSameFile(%s, %s) -> %s", a,b, result);
        return result;
        }

    @Override public int isSupportedOption(String option)
        {
        int result = super.isSupportedOption(option);
        RobotLog.dd(TAG, "isSupportedOption(%s) -> %d", option, result);
        return result;
        }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException
        {
        RobotLog.dd(TAG, "list(%s, %s, %s, %s)", location, packageName, kinds, recurse);
        return super.list(location, packageName, kinds, recurse);
        }

    //---------------------------------------------------------------------------------------------
    // StandardJavaFileManager
    //----------------------------------------------------------------------------------------------

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files)
        {
        Iterable<? extends JavaFileObject> result = delegate.getJavaFileObjectsFromFiles(files);
        RobotLog.dd(TAG, "getJavaFileObjectsFromFiles(%s) -> %s", files, result);
        return result;
        }

    @Override public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files)
        {
        Iterable<? extends JavaFileObject> result = delegate.getJavaFileObjects(files);
        RobotLog.dd(TAG, "getJavaFileObjects(%s) -> %s", files, result);
        return result;
        }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names)
        {
        Iterable<? extends JavaFileObject> result = delegate.getJavaFileObjectsFromStrings(names);
        RobotLog.dd(TAG, "getJavaFileObjectsFromStrings(%s) -> %s", names, result);
        return result;
        }

    @Override public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names)
        {
        Iterable<? extends JavaFileObject> result = delegate.getJavaFileObjects(names);
        RobotLog.dd(TAG, "getJavaFileObjects(%s) -> %s", names, result);
        return result;
        }

    @Override
    public void setLocation(Location location, Iterable<? extends File> path) throws IOException
        {
        RobotLog.dd(TAG, "setLocation(%s, %s)", location, path);
        delegate.setLocation(location, path);
        }

    @Override public Iterable<? extends File> getLocation(Location location)
        {
        Iterable<? extends File> result = delegate.getLocation(location);
        RobotLog.dd(TAG, "getLocation(%s) -> %s", location, result);
        return result;
        }
    }