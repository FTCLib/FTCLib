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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexFile;

/**
 * Created by bob on 2017-04-08.
 *
 *  http://stackoverflow.com/questions/1771679/difference-between-threads-context-class-loader-and-normal-classloader
 *  https://i.stack.imgur.com/i32nZ.png
 *  https://developer.android.com/reference/java/lang/ClassLoader.html
 *  https://android-developers.googleblog.com/2011/07/custom-class-loading-in-dalvik.html
 *  http://stackoverflow.com/questions/16159473/dynamic-class-reloading-in-dalvik-on-android
 *  http://www.netmite.com/android/mydroid/2.0/dalvik/tests/071-dexfile/src/Main.java
 *  http://stackoverflow.com/questions/2903260/android-using-dexclassloader-to-load-apk-file
 */
@SuppressWarnings("WeakerAccess")
public class OnBotJavaClassLoader extends ClassLoader implements Closeable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = OnBotJavaManager.TAG + ":ClassLoader";

    protected List<File>    jarFiles;
    protected List<DexFile> dexFiles;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public OnBotJavaClassLoader()
        {
        this(OnBotJavaClassLoader.class.getClassLoader(), OnBotJavaManager.getOutputJarFiles());
        }

    public OnBotJavaClassLoader(ClassLoader parentClassLoader, List<File> jarFiles)
        {
        super(parentClassLoader);

        this.jarFiles = new ArrayList<File>();
        this.dexFiles = new ArrayList<DexFile>();
        this.jarFiles.addAll(jarFiles);
        for (File jarFile : this.jarFiles)
            {
            if (jarFile.canRead())  // make sure it really exists
                {
                try {
                    this.dexFiles.add(openDexFile(jarFile));
                    }
                catch (IOException e)
                    {
                    // One reason for the exception is the jar file that results from compiling
                    // zero .java files is (apparently) unopenable
                    RobotLog.ee(TAG, e, "unable to open \"%s\"; ignoring", jarFile.getAbsolutePath());
                    }
                }
            else
                {
                RobotLog.ww(TAG, "unable to read \"%s\"; ignoring", jarFile.getAbsolutePath());
                }
            }
        }

    public void close()
        {
        for (DexFile dexFile : dexFiles)
            {
            closeDexFile(dexFile);
            }
        dexFiles.clear();   // make idempotent
        }

    protected static File getDexCacheDir()
        {
        File dexCache = null;
        // Using getCodeCacheDir() is logically ideal, but we can't use it everywhere since
        // it doesn't exist on KitKat. Having variation increases our test matrix, for relatively
        // little gain (if any? we're careful) so we don't even try.
        /*if (Build.VERSION.SDK_INT >= 21)
            {
            dexCache = AppUtil.getDefContext().getCodeCacheDir();
            }*/
            dexCache = AppUtil.getDefContext().getDir("dexopt", Context.MODE_PRIVATE);
            return dexCache;
        }

    protected File getDexCache(File jarFile)
        {
        // Note: the jar file needs to be uniquely *named* to its contents
        return new File(getDexCacheDir(), jarFile.getAbsolutePath().replace(File.separatorChar, '@') + "@classes.dex");
        }

    public static void fullClean()
        {
        for (File child : AppUtil.getInstance().filesUnder(getDexCacheDir()))
            {
            // RobotLog.vv(TAG, "cleaning up dex file: %s", child);
            AppUtil.getInstance().delete(child);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations & accessing
    //----------------------------------------------------------------------------------------------

    public static boolean isOnBotJava(Class clazz)
        {
        ClassLoader classLoader = clazz.getClassLoader();
        boolean result = classLoader instanceof OnBotJavaClassLoader;
        // RobotLog.vv(TAG, "isOnBotJava: class=%s loader=%s: %s", clazz.getSimpleName(), classLoader.getClass().getSimpleName(), result);
        return result;
        }

    public List<File> getJarFiles()
        {
        return jarFiles;
        }

    public List<DexFile> getDexFiles()
        {
        return dexFiles;
        }

    public DexFile openDexFile(File jarFile) throws IOException
        {
        RobotLog.vv(TAG, "opening DexFile %s", jarFile.getAbsolutePath());

        return DexFile.loadDex(jarFile.getAbsolutePath(), getDexCache(jarFile).getAbsolutePath(), 0);
        }

    public void closeDexFile(DexFile dexFile)
        {
        try {
            dexFile.close();
            }
        catch (IOException e)
            {
            RobotLog.ww(TAG, e, "exception closing DexFile");
            }
        }

    //----------------------------------------------------------------------------------------------
    // ClassLoader interface
    //----------------------------------------------------------------------------------------------

    @Override
    protected Class<?> loadClass(String className, boolean resolveIgnoredOnAndroid) throws ClassNotFoundException
        {
        final Class<?> onBotJavaClass = loadClassFromOnBotJavaJars(className);
        if (onBotJavaClass != null)
        {
            return onBotJavaClass;
        }
        return super.loadClass(className, resolveIgnoredOnAndroid);
        }

    @Override
    @NonNull
    protected Class<?> findClass(String className) throws ClassNotFoundException
        {
        final Class<?> onBotJavaClass = loadClassFromOnBotJavaJars(className);
        if (onBotJavaClass == null)
            {
            throw new ClassNotFoundException(className);
            }
        return onBotJavaClass;
        }

    protected @Nullable Class<?> loadClassFromOnBotJavaJars(String className) throws ClassNotFoundException
        {
        for (DexFile dexFile : dexFiles)
            {
            Class clazz = dexFile.loadClass(className, this);
            if (clazz != null)
                {
                RobotLog.vv(TAG, "From %s loaded: %s", dexFile.getName(), clazz.getName());
                return clazz;
                }
            }

            return null;
        }
    }
