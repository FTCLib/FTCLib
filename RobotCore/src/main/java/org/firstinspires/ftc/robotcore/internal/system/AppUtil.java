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
package org.firstinspires.ftc.robotcore.internal.system;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ResultReceiver;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;
import com.qualcomm.robotcore.util.WeakReferenceSet;

import org.firstinspires.ftc.robotcore.external.Predicate;
import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.files.MediaTransferProtocolMonitor;
import org.firstinspires.ftc.robotcore.internal.network.CallbackLooper;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;
import org.firstinspires.ftc.robotcore.internal.ui.ProgressParameters;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link AppUtil} contains a few utilities related to application and activity management.
 */
@SuppressWarnings({"WeakerAccess", "JavaDoc"})
public class AppUtil
    {
    //----------------------------------------------------------------------------------------------
    // Directory management
    // See also OnBotJavaManager.
    //----------------------------------------------------------------------------------------------

    /** The root of all external storage that we use */
    public static final File ROOT_FOLDER = Environment.getExternalStorageDirectory();

    /** {@link #FIRST_FOLDER} is the root of the tree we use in non-volatile storage. Robot configurations
     * are stored in this folder. */
    public static final File FIRST_FOLDER = new File(ROOT_FOLDER + "/FIRST/");

    /** Where to place logs */
    public static final File LOG_FOLDER = ROOT_FOLDER;
    public static final File MATCH_LOG_FOLDER = new File(FIRST_FOLDER + "/matchlogs/");
    public static final int MAX_MATCH_LOGS_TO_KEEP = 5;

    /** Dirctory in which .xml robot configurations should live */
    public static final File CONFIG_FILES_DIR = FIRST_FOLDER;

    /** Opmodes generated by the blocks programming tool are stored in {@link #BLOCK_OPMODES_DIR} */
    public static final File BLOCK_OPMODES_DIR = new File(FIRST_FOLDER, "/blocks/");
    public static final String BLOCKS_BLK_EXT = ".blk";
    public static final String BLOCKS_JS_EXT = ".js";
    public static final File BLOCKS_SOUNDS_DIR = new File(BLOCK_OPMODES_DIR, "/sounds/");

    /** {@link #ROBOT_SETTINGS} is a folder in which it's convenient to store team-generated settings
     * associated with their robot */
    public static final File ROBOT_SETTINGS = new File(FIRST_FOLDER, "/settings/");

    /** {@link #ROBOT_DATA_DIR} is a convenient place in which to put persistent data created by your opmode */
    public static final File ROBOT_DATA_DIR = new File(FIRST_FOLDER, "/data/");

    /** {@link #UPDATES_DIR} is a folder used to manage updates to firmware, installed APKs, and other components */
    public static final File UPDATES_DIR = new File(FIRST_FOLDER, "/updates/");
    public static final File RC_APP_UPDATE_DIR = new File(UPDATES_DIR, "/Robot Controller Application/");
    public static final File LYNX_FIRMWARE_UPDATE_DIR = new File(UPDATES_DIR, "/Expansion Hub Firmware/");
    public static final File OTA_UPDATE_DIR = new File(Environment.getExternalStorageDirectory(), "/OTA-Updates");

    /** {@link #SOUNDS_DIR} is used by the SoundPlayer. {@link #SOUNDS_CACHE} is a cache of remoted sounds */
    public static final File SOUNDS_DIR = new File(FIRST_FOLDER, "sounds");
    public static final File SOUNDS_CACHE = new File(SOUNDS_DIR, "cache");

    /** Where to place webcam calibration files */
    public static final File WEBCAM_CALIBRATIONS_DIR = new File(FIRST_FOLDER + "/webcamcalibrations/");

    //----------------------------------------------------------------------------------------------
    // Static State
    //----------------------------------------------------------------------------------------------

    public static final String TAG= "AppUtil";

    private static class InstanceHolder
        {
        public static AppUtil theInstance = new AppUtil();
        }

    public static AppUtil getInstance()
        {
        return InstanceHolder.theInstance;
        }

    public static Context getDefContext()
        {
        return getInstance().getApplication();
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private final Object        usbfsRootLock = new Object();
    private final Object        dialogLock = new Object();
    private @NonNull Application application;
    private LifeCycleMonitor    lifeCycleMonitor;
    private Activity            rootActivity;
    private Activity            currentActivity;
    private ProgressDialog      currentProgressDialog;
    private final Lock          requestPermissionLock;
    private Random              random;
    private @Nullable String    usbFileSystemRoot; // never transitions from non-null to null
    private final WeakReferenceSet<UsbFileSystemRootListener> usbfsListeners = new WeakReferenceSet<>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public static void onApplicationStart(@NonNull Application application)
        {
        getInstance().initialize(application);
        }

    protected AppUtil()
        {
        this.requestPermissionLock = new ReentrantLock();
        }

    protected void initialize(@NonNull Application application)
        {
        lifeCycleMonitor = new LifeCycleMonitor();
        rootActivity     = null;
        currentActivity  = null;
        currentProgressDialog = null;
        random = new Random();

        application.registerActivityLifecycleCallbacks(lifeCycleMonitor);

        // REVIEW: Why do this AFTER registering?
        this.application = application;

        RobotLog.vv(TAG, "initializing: getExternalStorageDirectory()=%s", Environment.getExternalStorageDirectory());

        usbFileSystemRoot = null;
        getUsbFileSystemRoot();
        }

    //----------------------------------------------------------------------------------------------
    // File and Directory Management
    //----------------------------------------------------------------------------------------------

    /**
     * Given a root File and a child underneath same returns the path from the former to the latter
     */
    public File getRelativePath(File root, File child)
        {
        File result = new File("");
        while (!root.equals(child))
            {
            File parent = child.getParentFile();
            result = new File(new File(child.getName()), result.getPath());
            if (parent == null) break;
            child = parent;
            }
        return result;
        }

    /**
     * Make sure all the components of the path exist, notifying MTP if necessary for any creations
     */
    public void ensureDirectoryExists(final File directory)
        {
        ensureDirectoryExists(directory, true);
        }
    public void ensureDirectoryExists(final File directory, boolean notify)
        {
        if (!directory.isDirectory())
            {
            directory.delete(); // might be a file; get rid of same

            File parent = directory.getParentFile();
            if (parent != null)
                {
                ensureDirectoryExists(parent, notify);
                }

            if (directory.mkdir())
                {
                // Successfully newly created the dir. Notify MTP. However, MTP doesn't like to be
                // notified of directories. So we make a temp file, notify on that, then delete same
                // once the scan has completed.
                if (notify)
                    {
                    MediaTransferProtocolMonitor.makeIndicatorFile(directory);
                    }
                }
            else
                {
                // already existed, or error; latter logged & ignored. Try to clean up any indicator files.
                if (directory.isDirectory())
                    {
                    // all is well
                    }
                else
                    {
                    RobotLog.ee(TAG, "failed to create directory %s", directory);
                    }
                if (notify)
                    {
                    MediaTransferProtocolMonitor.renoticeIndicatorFiles(directory);
                    }
                }
            }
        }

    public void deleteChildren(File file)
        {
        File[] children = file.listFiles();
        if (children != null)
            {
            for (File child : children)
                {
                delete(child);
                }
            }
        }

    /** Delete the indicated file or directory, clearing directories as needed */
    public void delete(File file)
        {
        deleteChildren(file);
        if (!file.delete())
            {
            RobotLog.ee(TAG, "failed to delete '%s'", file.getAbsolutePath());
            }
        }

    public List<File> filesUnder(File directory)
        {
        return filesUnder(directory, (Predicate<File>)null);
        }

    /** Return all the files (ie: not directories) under root for which predicate returns true */
    public List<File> filesUnder(File parent, @Nullable Predicate<File> predicate)
        {
        ArrayList<File> result = new ArrayList<>();
        if (parent.isDirectory())
            {
            for (File child : parent.listFiles())
                {
                result.addAll(filesUnder(child, predicate));
                }
            }
        else if (parent.exists())
            {
            if (predicate==null || predicate.test(parent))
                {
                result.add(parent.getAbsoluteFile());
                }
            }
        return result;
        }

    public List<File> filesUnder(File parent, @NonNull final String extension)
        {
        return filesUnder(parent, new Predicate<File>()
            {
            @Override public boolean test(File file)
                {
                return file.getName().endsWith(extension);
                }
            });
        }

    public List<File> filesIn(File directory)
        {
        return filesIn(directory, (Predicate<File>)null);
        }

    public List<File> filesIn(File directory, @Nullable Predicate<File> predicate)
        {
        ArrayList<File> result = new ArrayList<>();
        File[] children = directory.listFiles();
        if (children != null)
            {
            for (File child : children)
                {
                if (predicate==null || predicate.test(child))
                    {
                    result.add(child.getAbsoluteFile());
                    }
                }
            }
        return result;
        }

    public List<File> filesIn(File directory, @NonNull final String extension)
        {
        return filesIn(directory, new Predicate<File>()
            {
            @Override public boolean test(File file)
                {
                return file.getName().endsWith(extension);
                }
            });
        }

    public File getSettingsFile(String filename)
        {
        File file = new File(filename);
        if (!file.isAbsolute())
            {
            ensureDirectoryExists(ROBOT_SETTINGS);
            file = new File(ROBOT_SETTINGS, filename);
            }
        return file;
        }

    public void copyFile(File fromFile, File toFile) throws IOException
        {
        InputStream inputStream = new FileInputStream(fromFile);
        try {
            copyStream(inputStream, toFile);
            }
        finally
            {
            inputStream.close();
            }
        }

    public void copyStream(InputStream inputStream, File toFile) throws IOException
        {
        OutputStream outputStream = new FileOutputStream(toFile);
        try
            {
            copyStream(inputStream, outputStream);
            }
        finally
            {
            outputStream.close();
            }
        }

    public void copyStream(File inputFile, OutputStream outputStream) throws IOException
        {
        InputStream inputStream = new FileInputStream(inputFile);
        try {
            copyStream(inputStream, outputStream);
            }
        finally
            {
            inputStream.close();
            }
        }

    public void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException
        {
        int cbBuffer = Math.min(4096, inputStream.available());
        byte[] buffer = new byte[cbBuffer];
        for (;;)
            {
            int cbRead = inputStream.read(buffer);
            if (cbRead <= 0) break;
            outputStream.write(buffer, 0, cbRead);
            }
        }

    public File createTempFile(@NonNull String prefix, @Nullable String suffix, @Nullable File directory) throws IOException
        {
        return File.createTempFile(prefix, suffix, directory);
        }

    public File createTempDirectory(@NonNull String prefix, @Nullable String suffix, @Nullable File directory) throws IOException
        {
        /** @see File#createTempFile */
        if (prefix.length() < 3)
            {
            throw new IllegalArgumentException("prefix must be at least 3 characters");
            }
        if (suffix == null)
            {
            suffix = ".tmp";
            }
        File tmpDirFile = directory;
        if (tmpDirFile == null)
            {
            String tmpDir = System.getProperty("java.io.tmpdir", ".");
            tmpDirFile = new File(tmpDir);
            }
        File result;
        do
            {
            result = new File(tmpDirFile, prefix + random.nextInt() + suffix);
            }
        while (!result.mkdir()); // mkdir returns false failure or if the directory already existed.
        return result;
        }

    /**
     * Find the path in the Linux file system to the USB root. This is needed, ultimately,
     * by LibUsb, and on Android L (?) and later, we can't just enumerate the Linux file system
     * ourselves (in native code, of course), due to security considerations.
     *
     * If there currently is a USB device attached, we should be able to do this. But if there
     * isn't, then we're probably out of luck until some device attaches.
     */
    public @Nullable String getUsbFileSystemRoot()
        {
        if (usbFileSystemRoot == null)
            {
            synchronized (usbfsRootLock)
                {
                UsbManager usbManager = (UsbManager) AppUtil.getDefContext().getSystemService(Context.USB_SERVICE);
                for (String usbDeviceName : usbManager.getDeviceList().keySet())
                    {
                    String path = usbFileSystemRootFromDeviceName(usbDeviceName);
                    if (path != null)
                        {
                        setUsbFileSystemRoot(path);
                        break;
                        }
                    }
                }
            }
        return usbFileSystemRoot;
        }

    /** Similar to {@link #getUsbFileSystemRoot()}, but returns a default value if we don't have a real one */
    public @NonNull String getNonNullUsbFileSystemRoot()
        {
        String result = getUsbFileSystemRoot();
        if (result == null)
            {
            result = "/dev/bus/usb"; // as good a guess as any
            }
        return result;
        }

    public void setUsbFileSystemRoot(UsbDevice usbDevice)
        {
        setUsbFileSystemRoot(usbFileSystemRootFromDeviceName(usbDevice.getDeviceName()));
        }

    protected static @Nullable String usbFileSystemRootFromDeviceName(String usbDeviceName)
        {
        // Example: "/dev/bus/usb/001/002"
        final String[] nameParts = TextUtils.isEmpty(usbDeviceName) ? null : usbDeviceName.split("/");
        if (nameParts != null && nameParts.length > 2)
            {
            // Example: { "", "dev", "bus", "usb", "001", "002" }
            final StringBuilder builder = new StringBuilder(nameParts[0]);
            for (int i = 1; i < nameParts.length - 2; i++)
                {
                builder.append("/").append(nameParts[i]);
                }
            // Example: "/dev/bus/usb"
            return builder.toString();
            }
        return null;
        }

    protected void setUsbFileSystemRoot(@Nullable String usbFileSystemRoot)
        {
        if (usbFileSystemRoot != null)
            {
            if (this.usbFileSystemRoot == null) // test outside for efficiency
                {
                synchronized (usbfsRootLock)
                    {
                    if (this.usbFileSystemRoot == null) // test inside for correctness
                        {
                        RobotLog.ii(TAG, "found usbFileSystemRoot: %s", usbFileSystemRoot);
                        this.usbFileSystemRoot = usbFileSystemRoot;
                        notifyUsbListeners(usbFileSystemRoot);
                        }
                    }
                }
            }
        }

    protected void notifyUsbListeners(String usbFileSystemRoot)
        {
        Collection<UsbFileSystemRootListener> listeners;
        synchronized (usbfsListeners)
            {
            listeners = new ArrayList<>(usbfsListeners);
            }

        // Notification is an up-call: avoid locks as much as possible so as to avoid deadlocks
        // caused by lock-acquisition inversion.
        for (UsbFileSystemRootListener listener : listeners)
            {
            listener.onUsbFileSystemRootChanged(usbFileSystemRoot);
            }
        }

    public void addUsbfsListener(UsbFileSystemRootListener listener)
        {
        synchronized (usbfsListeners)
            {
            usbfsListeners.add(listener);
            }
        }

    public void removeUsbfsListener(UsbFileSystemRootListener listener)
        {
        synchronized (usbfsListeners)
            {
            usbfsListeners.remove(listener);
            }
        }

    public interface UsbFileSystemRootListener
        {
        void onUsbFileSystemRootChanged(String usbFileSystemRoot);
        }

    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    /**
     * Restarts the current application
     * @param exitCode the exit code to return from the current app run
     */
    public void restartApp(int exitCode)
        {
        // See http://stackoverflow.com/questions/2681499/android-how-to-auto-restart-application-after-its-been-force-closed
        RobotLog.vv(TAG, "restarting app");

        @SuppressWarnings("WrongConstant") PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplication().getBaseContext(),
                0,
                new Intent(rootActivity.getIntent()),
                rootActivity.getIntent().getFlags());

        // Carry out the restart by having the AlarmManager (re)issue our intent after a delay
        // that's long enough for us to get out of the way in the first place.
        int msRestartDelay = 1500;
        AlarmManager alarmManager = (AlarmManager) rootActivity.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + msRestartDelay, pendingIntent);
        System.exit(exitCode);
        }

    public void finishRootActivityAndExitApp()
        {
        synchronousRunOnUiThread(new Runnable()
            {
            @Override public void run()
                {
                RobotLog.vv(TAG, "finishRootActivityAndExitApp()");
                if (Build.VERSION.SDK_INT >= 21)
                    {
                    rootActivity.finishAndRemoveTask();
                    }
                else
                    {
                    rootActivity.finish();
                    }
                exitApplication();
                }
            });
        }

    public void exitApplication(int resultCode)
        {
        RobotLog.vv(TAG, "exitApplication(%d)", resultCode);
        System.exit(resultCode);
        }

    public void exitApplication()
        {
        exitApplication(0);
        }

    //----------------------------------------------------------------------------------------------
    // Application
    //----------------------------------------------------------------------------------------------

    /**
     * Returns the contextually running {@link Application}
     * @return the contextually running {@link Application}
     */
    public @NonNull Application getApplication()
        {
        return this.application;
        }

    /**
     * Returns the application id of this current application, the identifier
     * that distinguishes it from all other applications. This is the 'packageName' in
     * the ultimately-shipped manifest, but that differs from the 'packageName' attribute
     * in the source AndroidManifest.xml.
     *
     * @see <a href="https://developer.android.com/studio/build/application-id.html">Application Id</a>
     */
    public String getApplicationId()
        {
        return getApplication().getPackageName();
        }

    public boolean isRobotController()
        {
        return getApplicationId().equals(getDefContext().getString(R.string.packageNameRobotController));
        }

    public boolean isDriverStation()
        {
        return getApplicationId().equals(getDefContext().getString(R.string.packageNameDriverStation));
        }

    public String getAppName()
        {
        if (isRobotController())
            return getDefContext().getString(R.string.appNameRobotController);
        else if (isDriverStation())
            return getDefContext().getString(R.string.appNameDriverStation);
        else
            return getDefContext().getString(R.string.appNameUnknown);
        }

    public String getRemoteAppName()
        {
        if (isRobotController())
            return getDefContext().getString(R.string.appNameDriverStation);
        else if (isDriverStation())
            return getDefContext().getString(R.string.appNameRobotController);
        else
            return getDefContext().getString(R.string.appNameUnknown);
        }

    //----------------------------------------------------------------------------------------------
    // General UI interaction
    //----------------------------------------------------------------------------------------------

    public static @ColorInt int getColor(int id)
        {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
            return getDefContext().getColor(id);
            }
        else
            {
            return getDefContext().getResources().getColor(id);
            }
        }

    /**
     * A drop-in replacement for {@link Activity#runOnUiThread(Runnable) runonUiThread()} which doesn't
     * return until the UI action is complete.
     * @param action the action to perform on the UI thread
     */
    public void synchronousRunOnUiThread(final Runnable action)
        {
        synchronousRunOnUiThread(getActivity(), action);
        }

    public void synchronousRunOnUiThread(Activity activity, final Runnable action)
        {
        try {
            final CountDownLatch uiDone = new CountDownLatch(1);
            activity.runOnUiThread(new Runnable()
                {
                @Override public void run()
                    {
                    action.run();
                    uiDone.countDown();
                    }
                });
            uiDone.await();
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        }

    /**
     * A simple helper so that callers have syntactically similar forms for both synchronous and non.
     */
    public void runOnUiThread(final Runnable action)
        {
        runOnUiThread(getActivity(), action);
        }

    public void runOnUiThread(Activity activity, final Runnable action)
        {
        activity.runOnUiThread(action);
        }

    public void showWaitCursor(@NonNull final String message, @NonNull final Runnable runnable)
        {
        showWaitCursor(message, runnable, null);
        }

    public void showWaitCursor(@NonNull final String message, @NonNull final Runnable backgroundWorker, @Nullable final Runnable runPostOnUIThread)
        {
        this.runOnUiThread(new Runnable()
            {
            /* 'leakage' not of significance here */@SuppressLint("StaticFieldLeak") @Override public void run()
                {
                new AsyncTask<Object,Void,Void>()
                    {
                    ProgressDialog dialog;

                    @Override protected void onPreExecute()
                        {
                        dialog = new ProgressDialog(getActivity());
                        dialog.setMessage(message);
                        dialog.setIndeterminate(true);
                        dialog.setCancelable(false);
                        dialog.show();
                        }

                    @Override protected Void doInBackground(Object... params)
                        {
                        backgroundWorker.run();
                        return null;
                        }

                    @Override protected void onPostExecute(Void aVoid)
                        {
                        dialog.dismiss();
                        if (runPostOnUIThread != null)
                            {
                            runPostOnUIThread.run();
                            }
                        }
                    }.execute();
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // Progress Dialog remoting
    //----------------------------------------------------------------------------------------------

    public void showProgress(UILocation uiLocation, final String message, final double fractionComplete)
        {
        showProgress(uiLocation, message, ProgressParameters.fromFraction(fractionComplete));
        }
    public void showProgress(UILocation uiLocation, final String message, final double fractionComplete, int max)
        {
        showProgress(uiLocation, message, ProgressParameters.fromFraction(fractionComplete, max));
        }
    public void showProgress(UILocation uiLocation, final String message, ProgressParameters progressParameters)
        {
        showProgress(uiLocation, this.getActivity(), message, progressParameters);
        }

    public void showProgress(UILocation uiLocation, final Activity activity, final String message, final ProgressParameters progressParameters)
        {
        final int maxMax = 10000;   // per ProgressBar.MAX_LEVEL
        final int cappedMax = Math.min(progressParameters.max, maxMax);

        this.runOnUiThread(new Runnable()
            {
            @Override public void run()
                {
                if (currentProgressDialog == null)
                    {
                    currentProgressDialog = new ProgressDialog(activity);
                    currentProgressDialog.setMessage(message);
                    currentProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    currentProgressDialog.setMax(cappedMax);
                    currentProgressDialog.setProgress(0);
                    currentProgressDialog.setCanceledOnTouchOutside(false);
                    currentProgressDialog.show();
                    }
                currentProgressDialog.setProgress(progressParameters.cur);
                }
            });

        if (uiLocation == UILocation.BOTH)
            {
            RobotCoreCommandList.ShowProgress showProgress = new RobotCoreCommandList.ShowProgress();
            showProgress.message = message;
            showProgress.cur = progressParameters.cur;
            showProgress.max = progressParameters.max;
            NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_SHOW_PROGRESS, showProgress.serialize()));
            }
        }

    public void dismissProgress(UILocation uiLocation)
        {
        this.runOnUiThread(new Runnable()
            {
            @Override public void run()
                {
                if (currentProgressDialog != null)
                    {
                    currentProgressDialog.dismiss();
                    currentProgressDialog = null;
                    }
                }
            });

        if (uiLocation == UILocation.BOTH)
            {
            NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_DISMISS_PROGRESS));
            }
        }

    //----------------------------------------------------------------------------------------------
    // USB Permissions Management
    //----------------------------------------------------------------------------------------------

    /**
     * Requests permission to access a USB device if same is not currently available.
     * The result of the request is delivered asynchronously.
     *
     * @param tag           application tag with which to do any logging
     * @param modalContext  the context used to scope modal dialogs that might need be shown
     * @param usbDevice     the device for whom permission is requested
     * @param deadline      the deadline within which the request must complete. If the deadline expires,
     *                      or is cancelled, before the user consents to the use of the device, then
     *                      false is returned
     * @param handler       the mechanism through which resultReport is provided
     * @param resultReport  invoked once the permission result is known. This may be invoked either
     *                      BEFORE or AFTER asyncRequestUsbPermission() has returned. The invocation is made
     *                      on the thread associated with the indicated handler, or the handler of
     *                      the current thread (if that is null), or the main thread (if THAT is null).
     */
    public void asyncRequestUsbPermission(String tag, Context modalContext, UsbDevice usbDevice, Deadline deadline, @Nullable Handler handler, Consumer<Boolean> resultReport)
        {
        asyncRequestUsbPermission(tag, modalContext, usbDevice, deadline, Continuation.create(handler, resultReport));
        }

    /**
     * Requests permission to access a USB device if same is not currently available.
     * The result of the request is delivered asynchronously.
     *
     * @param tag           application tag with which to do any logging
     * @param modalContext  the context used to scope modal dialogs that might need be shown
     * @param usbDevice     the device for whom permission is requested
     * @param deadline      the deadline within which the request must complete. If the deadline expires,
     *                      or is cancelled, before the user consents to the use of the device, then
     *                      false is returned
     * @param threadPool    the mechanism through which resultReport is provided
     * @param resultReport  invoked once the permission result is known. This may be invoked either
     *                      BEFORE or AFTER asyncRequestUsbPermission() has returned. The invocation is made
     *                      on a thread of the indicated thread pool.
     */
    public void asyncRequestUsbPermission(String tag, Context modalContext, UsbDevice usbDevice, Deadline deadline, @NonNull ExecutorService threadPool, Consumer<Boolean> resultReport)
        {
        asyncRequestUsbPermission(tag, modalContext, usbDevice, deadline, Continuation.create(threadPool, resultReport));
        }

    /**
     * Requests permission to access a USB device if same is not currently available.
     * The result of the request is delivered asynchronously.
     *
     * @param tag           application tag with which to do any logging
     * @param modalContext  the context used to scope modal dialogs that might need be shown
     * @param usbDevice     the device for whom permission is requested
     * @param deadline      the deadline within which the request must complete. If the deadline expires,
     *                      or is cancelled, before the user consents to the use of the device, then
     *                      false is returned
     * @param continuation  invoked once the permission result is known. This may be invoked either
     *                      BEFORE or AFTER asyncRequestUsbPermission() has returned
     */
    public void asyncRequestUsbPermission(
            final String tag,
            final Context modalContext,
            final UsbDevice usbDevice,
            final Deadline deadline,
            final Continuation<? extends Consumer<Boolean>> continuation)
        {
        RobotLog.vv(tag,"asyncRequestUsbPermission()...");
        try {
            Assert.assertFalse(CallbackLooper.isLooperThread());

            // First check to see if we've already got permission
            final UsbManager usbManager = (UsbManager) modalContext.getSystemService(Context.USB_SERVICE);
            if (usbManager.hasPermission(usbDevice))
                {
                RobotLog.dd(tag, "permission already available for %s", usbDevice.getDeviceName());
                continuation.dispatch(new ContinuationResult<Consumer<Boolean>>()
                    {
                    @Override public void handle(Consumer<Boolean> resultReport)
                        {
                        resultReport.accept(true);
                        }
                    });
                }
            else
                {
                final MutableReference<Boolean> result = new MutableReference<>(false);
                final Runnable runnable = new Runnable()
                    {
                    @Override public void run()
                        {
                        // We don't have permission. We're going to have to talk to the user. But to avoid confusion,
                        // we only allow one such dialog to appear at any given time. TODO: Review how well that really works.
                        try {
                            if (deadline.tryLock(requestPermissionLock))
                                {
                                try {
                                    // Use an ACTION that specific to the device in question, just in case there's other requests
                                    // floating around. If, somehow, there is more than one request for the same device, they'll
                                    // all be treated as equivalent.
                                    final String intentPrefix = "org.firstinspires.ftc.USB_PERMISSION_REQUEST:";
                                    final String actionUsbPermissionRequest = intentPrefix + usbDevice.getDeviceName();
                                    final CountDownLatch permissionResultAvailable = new CountDownLatch(1);
                                    final BroadcastReceiver receiver = new BroadcastReceiver()
                                        {
                                        @Override public void onReceive(Context context, Intent intent)
                                            {
                                            if (intent.getAction().equals(actionUsbPermissionRequest))
                                                {
                                                UsbDevice notifiedDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                                                if (notifiedDevice!=null && notifiedDevice.getDeviceName().equals(usbDevice.getDeviceName()))
                                                    {
                                                    result.setValue(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));
                                                    if (!result.getValue())
                                                        {
                                                        RobotLog.vv(tag, "requestPermission(%s): user declined permission", notifiedDevice.getDeviceName());
                                                        }
                                                    RobotLog.vv(tag, "releasing permissionResultAvailable latch");
                                                    permissionResultAvailable.countDown();
                                                    }
                                                else
                                                    RobotLog.ee(tag, "unexpected permission request response");
                                                }
                                            }
                                        };

                                    // https://developer.android.com/guide/topics/connectivity/usb/host.html
                                    final IntentFilter intentFilter = new IntentFilter(actionUsbPermissionRequest);

                                    // Note the threading: in order minimize interactions with the application threads, particularly the
                                    // application main thread, we use a special purpose dedicated 'CallbackLooper' thread whose whole purpose
                                    // in life is to serve this role. No one's code should ever block that thread for any significant time.
                                    modalContext.registerReceiver(receiver, intentFilter, null, CallbackLooper.getDefault().getHandler());
                                    try {
                                        PendingIntent pendingIntent = PendingIntent.getBroadcast(modalContext, 0, new Intent(actionUsbPermissionRequest), PendingIntent.FLAG_ONE_SHOT);
                                        //
                                        // Post the dialog that asks for the user's permission
                                        //
                                        usbManager.requestPermission(usbDevice, pendingIntent);
                                        //
                                        // Wait until the user interacts with the dialog, the indicated timeout occurs,
                                        // or user code indicates explicitly that the wait should be cancelled. Note that
                                        // in the latter two cases, the dialog will unfortunately stay on the screen as
                                        // a non-functional zombie since we have no way for it to be removed.
                                        //
                                        if (deadline.await(permissionResultAvailable))
                                            {
                                            RobotLog.vv(tag, "permissionResultAvailable latch awaited");
                                            }
                                        else
                                            {
                                            RobotLog.vv(tag, "requestPermission(): cancelled or timed out waiting for user response");
                                            pendingIntent.cancel(); // seems like good housekeeping
                                            }
                                        }
                                    catch (InterruptedException e)
                                        {
                                        Thread.currentThread().interrupt();
                                        }
                                    finally
                                        {
                                        modalContext.unregisterReceiver(receiver);
                                        }
                                    }
                                finally
                                    {
                                    requestPermissionLock.unlock();
                                    }
                                }
                            else
                                {
                                RobotLog.vv(tag, "requestPermission(): requestPermissionLock.tryLock() returned false");
                                }
                            }
                        catch (InterruptedException e)
                            {
                            Thread.currentThread().interrupt();
                            }
                        finally
                            {
                            RobotLog.vv(tag, "USB permission request for %s: result=%s", usbDevice.getDeviceName(), result.getValue());
                            }
                        }
                    };

                if (continuation.isHandler())
                    {
                    // Run the request on a worker thread, then thunk the result to the handler
                    ThreadPool.getDefault().submit(new Runnable()
                        {
                        @Override public void run()
                            {
                            runnable.run();
                            continuation.dispatch(new ContinuationResult<Consumer<Boolean>>()
                                {
                                @Override public void handle(Consumer<Boolean> resultReport)
                                    {
                                    resultReport.accept(result.getValue());
                                    }
                                });
                            }
                        });

                    }
                else
                    {
                    // Run the whole thing in on a one worker thread in the same thread pool
                    // that the continuation is targeted to
                    continuation.<Void>createForNewTarget(null).dispatch(new ContinuationResult<Void>()
                        {
                        @Override public void handle(Void aVoid)
                            {
                            runnable.run();
                            continuation.dispatchHere(new ContinuationResult<Consumer<Boolean>>()
                                {
                                @Override public void handle(Consumer<Boolean> resultReport)
                                    {
                                    resultReport.accept(result.getValue());
                                    }
                                });
                            }
                        });
                    }
                }
            }
        finally
            {
            RobotLog.vv(tag,"...asyncRequestUsbPermission()");
            }
        }

    //----------------------------------------------------------------------------------------------
    // Alert / Confirm / Prompt Dialog
    // https://developer.android.com/guide/topics/ui/dialogs.html
    //----------------------------------------------------------------------------------------------

    public enum DialogFlavor { ALERT, CONFIRM, PROMPT }

    private Map<String, DialogContext> dialogContextMap = new ConcurrentHashMap<>();

    public static class DialogContext
        {
        public enum Outcome { UNKNOWN, CANCELLED, CONFIRMED }

        public final CountDownLatch dismissed = new CountDownLatch(1);

        protected final String         uuidString;
        protected AlertDialog          dialog;
        protected boolean              isArmed = true;
        protected Outcome              outcome = Outcome.UNKNOWN;
        protected CharSequence         textResult = null;
        protected EditText             input = null;

        public DialogContext(String uuidString)
            {
            this.uuidString = uuidString;
            }

        public Outcome getOutcome()
            {
            return outcome;
            }

        public CharSequence getText()
            {
            return textResult;
            }

        // Dismiss the dialog if it hasn't been dismissed already
        public void dismissDialog()
            {
            AppUtil.getInstance().runOnUiThread(new Runnable()
                {
                @Override public void run()
                    {
                    Dialog dialog = DialogContext.this.dialog;
                    if (dialog != null)
                        {
                        dialog.dismiss();
                        }
                    }
                });
            }
        }

    public static class DialogParams extends MemberwiseCloneable<DialogParams>
        {
        public UILocation       uiLocation;
        public String           title;
        public String           message;
        public DialogFlavor     flavor         = DialogFlavor.ALERT;
        public Activity         activity       = AppUtil.getInstance().getActivity();
        public @Nullable String defaultValue   = null;
        public @Nullable String uuidString     = null;

        public DialogParams(UILocation uiLocation, String title, String message)
            {
            this.uiLocation = uiLocation;
            this.title = title;
            this.message = message;
            }

        public DialogParams copy()
            {
            return super.memberwiseClone();
            }
        }

    public DialogContext showAlertDialog(UILocation uiLocation, String title, String message)
        {
        return showDialog(new DialogParams(uiLocation, title, message));
        }

    public DialogContext showDialog(@NonNull DialogParams params)
        {
        return showDialog(params, (Continuation<Consumer<DialogContext>>)null);
        }
    public DialogContext showDialog(@NonNull DialogParams params, @Nullable Consumer<DialogContext> runOnDismiss)
        {
        return showDialog(params, (Handler)null, runOnDismiss);
        }
    public DialogContext showDialog(@NonNull DialogParams params, @Nullable Handler handler, @Nullable Consumer<DialogContext> runOnDismiss)
        {
        return showDialog(params, runOnDismiss != null ? Continuation.create(handler, runOnDismiss) : null);
        }
    public DialogContext showDialog(@NonNull DialogParams params, @NonNull Executor threadPool, @Nullable Consumer<DialogContext> runOnDismiss)
        {
        return showDialog(params, runOnDismiss != null ? Continuation.create(threadPool, runOnDismiss) : null);
        }

    public DialogContext showDialog(@NonNull DialogParams params, @Nullable final Continuation<? extends Consumer<DialogContext>> runOnDismiss)
        {
        synchronized (dialogLock)
            {
            // Capture so as to decouple from possible user shenanigans
            final DialogParams paramsCopy = params.copy();

            final RobotCoreCommandList.ShowDialog showDialog = new RobotCoreCommandList.ShowDialog();
            showDialog.title = paramsCopy.title;
            showDialog.message = paramsCopy.message;
            showDialog.uuidString = paramsCopy.uuidString != null ? paramsCopy.uuidString : UUID.randomUUID().toString();

            final MutableReference<DialogContext> result = new MutableReference<>();
            this.synchronousRunOnUiThread(new Runnable() // note the synchronicity
                {
                @Override public void run()
                    {
                    // Whenever the dialog goes away, for whatever reason, we need to (a) fire the
                    // CountDownLatch to unstick whomever might be awaiting, and (b) make sure to dismiss
                    // from the other side, too, if appropriate.
                    final DialogContext dialogContext = new DialogContext(showDialog.uuidString);

                    AlertDialog.Builder builder = new AlertDialog.Builder(paramsCopy.activity);
                    builder.setTitle(paramsCopy.title);
                    builder.setMessage(paramsCopy.message);
                    switch (paramsCopy.flavor)
                        {
                        case ALERT:
                            builder.setNeutralButton(R.string.buttonNameOK, new DialogInterface.OnClickListener()
                                {
                                @Override public void onClick(DialogInterface dialog, int which)
                                    {
                                    dialogContext.outcome = DialogContext.Outcome.CONFIRMED;
                                    }
                                });
                            break;
                        case PROMPT:
                            // https://stackoverflow.com/questions/10903754/input-text-dialog-android
                            dialogContext.input = new EditText(paramsCopy.activity);
                            dialogContext.input.setInputType(InputType.TYPE_CLASS_TEXT);
                            if (paramsCopy.defaultValue != null) dialogContext.input.setText(paramsCopy.defaultValue);
                            builder.setView(dialogContext.input);
                            // fall through
                        case CONFIRM:
                            if (paramsCopy.uiLocation != UILocation.ONLY_LOCAL) throw new IllegalArgumentException("remote confirmation dialogs not yet supported");
                            builder.setPositiveButton(R.string.buttonNameOK, new DialogInterface.OnClickListener()
                                {
                                @Override public void onClick(DialogInterface dialog, int which)
                                    {
                                    RobotLog.vv(TAG, "dialog OK clicked: uuid=%s", dialogContext.uuidString);
                                    dialogContext.outcome = DialogContext.Outcome.CONFIRMED;
                                    // Capture the text while we know we're on a good thread
                                    if (dialogContext.input != null)
                                        {
                                        dialogContext.textResult = dialogContext.input.getText();
                                        }
                                    dialog.dismiss();
                                    }
                                });
                            builder.setNegativeButton(R.string.buttonNameCancel, new DialogInterface.OnClickListener()
                                {
                                @Override public void onClick(DialogInterface dialog, int which)
                                    {
                                    RobotLog.vv(TAG, "dialog cancel clicked: uuid=%s", dialogContext.uuidString);
                                    dialog.cancel();
                                    }
                                });
                            break;
                        }

                    dialogContext.dialog = builder.create();
                    dialogContext.dialog.setOnShowListener(new DialogInterface.OnShowListener()
                        {
                        @Override public void onShow(DialogInterface dialog)
                            {
                            RobotLog.vv(TAG, "dialog shown: uuid=%s", dialogContext.uuidString);
                            }
                        });
                    dialogContext.dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                        {
                        @Override public void onCancel(DialogInterface dialog)
                            {
                            RobotLog.vv(TAG, "dialog cancelled: uuid=%s", dialogContext.uuidString);
                            dialogContext.outcome = DialogContext.Outcome.CANCELLED;
                            }
                        });
                    dialogContext.dialog.setOnDismissListener(new DialogInterface.OnDismissListener()
                        {
                        @Override public void onDismiss(DialogInterface dialog)
                            {
                            RobotLog.vv(TAG, "dialog dismissed: uuid=%s", dialogContext.uuidString);
                            dialogContext.dismissed.countDown();
                            if (runOnDismiss != null)
                                {
                                runOnDismiss.dispatch(new ContinuationResult<Consumer<DialogContext>>()
                                    {
                                    @Override public void handle(Consumer<DialogContext> dialogContextConsumer)
                                        {
                                        dialogContextConsumer.accept(dialogContext);
                                        }
                                    });
                                }
                            if (dialogContext.isArmed)
                                {
                                // Actively dismissing on the DS should also dismiss on the RC, and visa versa
                                RobotCoreCommandList.DismissDialog dismissDialog = new RobotCoreCommandList.DismissDialog(showDialog.uuidString);
                                dismissDialog(UILocation.BOTH, dismissDialog);
                                }
                            }
                        });

                    dialogContextMap.put(dialogContext.uuidString, dialogContext);
                    result.setValue(dialogContext);
                    dialogContext.dialog.show();
                    }
                                      });

            Assert.assertNotNull(result.getValue());

            if (paramsCopy.uiLocation==UILocation.BOTH)
                {
                NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_SHOW_DIALOG, showDialog.serialize()));
                }

            return result.getValue();
            }
        }

    public void dismissDialog(UILocation uiLocation, RobotCoreCommandList.DismissDialog dismissDialog)
        {
        dismissDialog(dismissDialog.uuidString);
        if (uiLocation==UILocation.BOTH)
            {
            NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_DISMISS_DIALOG, dismissDialog.serialize()));
            }
        }

    protected void dismissDialog(final String uuidString)
        {
        this.runOnUiThread(new Runnable()
            {
            @Override public void run()
                {
                DialogContext dialogContext = dialogContextMap.remove(uuidString);
                if (dialogContext != null)
                    {
                    dialogContext.isArmed = false;
                    dialogContext.dialog.dismiss();
                    }
                }
            });
        }

    public void dismissAllDialogs(UILocation uiLocation)
        {
        List<String> uuidStrings = new ArrayList<>(dialogContextMap.keySet());
        for (String uuidString : uuidStrings)
            {
            dismissDialog(uuidString);
            }
        if (uiLocation == UILocation.BOTH)
            {
            NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_DISMISS_ALL_DIALOGS));
            }
        }

    //----------------------------------------------------------------------------------------------
    // Toast
    //----------------------------------------------------------------------------------------------

    /**
     * Displays a toast message to the user. May be called from any thread.
     */
    public void showToast(UILocation uiLocation, String msg)
        {
        showToast(uiLocation, msg, Toast.LENGTH_SHORT);
        }
    public void showToast(final UILocation uiLocation, final String msg, final int duration)
        {
        new Handler(Looper.getMainLooper()).post(new Runnable()
            {
            @Override public void run()
                {
                Toast toast = Toast.makeText(getDefContext(), msg, duration);
                TextView message = (TextView) toast.getView().findViewById(android.R.id.message);
                message.setTextColor(getColor(R.color.text_toast));
                message.setTextSize(18);
                toast.show();
                }
            });

        if (uiLocation==UILocation.BOTH)
            {
            RobotCoreCommandList.ShowToast showToast = new RobotCoreCommandList.ShowToast();
            showToast.message = msg;
            showToast.duration = duration;
            NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_SHOW_TOAST, showToast.serialize()));
            }
        }

    // We always ignore any provided activity and context values. It's safest to always use the
    // application context, and to access the main thread without interacting with the lifecycle
    // of a particular activity.
    @Deprecated
    public void showToast(UILocation uiLocation, Context context, String msg )
        {
        showToast(uiLocation, msg);
        }
    @Deprecated
    public void showToast(UILocation uiLocation, final Activity activity, Context context, String msg)
        {
        showToast(uiLocation, msg);
        }
    @Deprecated
    public void showToast(UILocation uiLocation, final Activity activity, final Context context, final String msg, final int duration)
        {
        showToast(uiLocation, msg, duration);
        }

    //----------------------------------------------------------------------------------------------
    // Activities
    //----------------------------------------------------------------------------------------------

    /**
     * Returns the contextually running {@link Activity}. This should RARELY be null, but might
     * possibly be if we currently are only running services
     * @return the contextually running {@link Activity}
     */
    public @Nullable Activity getActivity()
        {
        return currentActivity;
        }

    /**
     * Returns an appropriate context against which to launch modal dialogs, etc.
     * @return
     */
    public @NonNull Context getModalContext()
        {
        return currentActivity != null ? currentActivity : getApplication();
        }

    /**
     * Returns the root activity of the current application
     * @return the root activity of the current application
     */
    public Activity getRootActivity()
        {
        return rootActivity;
        }

    private void initializeRootActivityIfNecessary()
        {
        if (rootActivity == null)
            {
            rootActivity = currentActivity;
            RobotLog.vv(TAG, "rootActivity=%s", rootActivity.getClass().getSimpleName());
            }
        }

    /**
     * {@link LifeCycleMonitor} is a class that allows us to keep track of the currently active Activity.
     */
    private class LifeCycleMonitor implements Application.ActivityLifecycleCallbacks
        {
        @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState)
            {
            currentActivity = activity;
            initializeRootActivityIfNecessary();
            }

        @Override public void onActivityStarted(Activity activity)
            {
            currentActivity = activity;
            initializeRootActivityIfNecessary();
            }

        @Override public void onActivityResumed(Activity activity)
            {
            currentActivity = activity;
            initializeRootActivityIfNecessary();
            }

        @Override public void onActivityPaused(Activity activity)
            {
            }

        @Override public void onActivityStopped(Activity activity)
            {
            }

        @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState)
            {
            }

        @Override public void onActivityDestroyed(Activity activity)
            {
            if (activity == rootActivity && rootActivity != null)
                {
                RobotLog.vv(TAG, "rootActivity=%s destroyed", rootActivity.getClass().getSimpleName());
                rootActivity = null;

                /*
                 * Don't leave us without a root activity.
                 */
                initializeRootActivityIfNecessary();
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Date and time
    //----------------------------------------------------------------------------------------------

    public SimpleDateFormat getIso8601DateFormat()
        {
        // From https://en.wikipedia.org/wiki/ISO_8601
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter;
        }

    /**
     * Returns the current system wall clock time.
     */
    public long getWallClockTime()
        {
        return System.currentTimeMillis();
        }

    /**
     * Attempts to set the clock returned by {@link #getWallClockTime()}. W/o a modified Android
     * kernel, this will be unsuccessful, due to a check in kernel/security/commoncap.c. On the Control
     * Hub, that has been disabled. Note that no error is reported on failure.
     *
     * Also attempts to set the current time zone for the system, not just this process.
     */
    public void setWallClockTime(long millis)
        {
        nativeSetCurrentTimeMillis(millis);
        }

    /**
     * Set the system timezone by whatever means necessary :-)
     */
    public void setTimeZone(String timeZone)
        {
        TimeZone before = TimeZone.getDefault();
        AlarmManager alarmManager = (AlarmManager)AppUtil.getDefContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.setTimeZone(timeZone);
        TimeZone.setDefault(null); TimeZone after = TimeZone.getDefault();
        RobotLog.vv(TAG, "attempted to set timezone: before=%s after=%s", before.getID(), after.getID());
        }

    /**
     * Is the indicated time one that could reasonably exist for a robot controller or a driver station?
     * What we're really trying to do here is to detect systems that have no battery-backed-up
     * clock on board. Those, on boot, will start counting from the Unix epoch, which is in 1970.
     */
    public boolean isSaneWalkClockTime(long millis)
        {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(millis);
        return calendar.get(GregorianCalendar.YEAR) > 1975;
        }

    //----------------------------------------------------------------------------------------------
    // System
    //----------------------------------------------------------------------------------------------

    /** Warning: calling this method can be very expensive, due to the
     * need to create stack traces. Do not use in production code. */
    public String findCaller(String message, int frameOffset)
        {
        StackTraceElement element = (new RuntimeException()).getStackTrace()[2+frameOffset]; // 2 is one for RuntimeException, one for our own caller.
        String fullClassName = element.getClassName();
        String callingClass = fullClassName.substring(fullClassName.lastIndexOf('.')+1);
        String fileName = new File(element.getFileName()).getName();
        int line = element.getLineNumber();
        return Misc.formatInvariant("%s caller=[%s:%d] %s", message, fileName, line, callingClass);
        }

    public RuntimeException unreachable()
        {
        return unreachable(TAG);
        }

    public RuntimeException unreachable(Throwable throwable)
        {
        return unreachable(TAG, throwable);
        }

    public RuntimeException unreachable(String tag)
        {
        return failFast(tag, "internal error: this code is unreachable");
        }

    public RuntimeException unreachable(String tag, Throwable throwable)
        {
        return failFast(tag, throwable, "internal error: this code is unreachable");
        }

    public RuntimeException failFast(String tag, String format, Object... args)
        {
        String message = String.format(format, args);
        return failFast(tag, message);
        }

    public RuntimeException failFast(String tag, String message)
        {
        RobotLog.ee(tag, message);
        exitApplication(-1);
        return new RuntimeException("keep compiler happy");
        }

    public RuntimeException failFast(String tag, Throwable throwable, String format, Object... args)
        {
        String message = String.format(format, args);
        return failFast(tag, throwable, message);
        }

    public RuntimeException failFast(String tag, Throwable throwable, String message)
        {
        RobotLog.ee(tag, throwable, message);
        exitApplication(-1);
        return new RuntimeException("keep compiler happy", throwable);
        }

    private native boolean nativeSetCurrentTimeMillis(long millis);

// MODIFIED FOR OPENFTC
// RobotCore depends on having the Vuforia lib loaded, so we'll load this
// when we load the real Vuforia lib
//    static
//        {
//        System.loadLibrary("RobotCore");
//        }

    //----------------------------------------------------------------------------------------------
    // Inter-Process Communication
    //----------------------------------------------------------------------------------------------

    // https://stackoverflow.com/a/12183036
    public static ResultReceiver wrapResultReceiverForIpc(ResultReceiver actualReceiver)
        {
        Parcel parcel = Parcel.obtain();
        actualReceiver.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ResultReceiver receiverForSending = ResultReceiver.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return receiverForSending;
        }

    }
