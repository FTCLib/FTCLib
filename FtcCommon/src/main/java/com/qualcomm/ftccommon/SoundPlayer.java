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
package com.qualcomm.ftccommon;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;

import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.internal.android.SoundPoolIntf;
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.files.FileBasedLock;
import org.firstinspires.ftc.robotcore.internal.network.CallbackLooper;
import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.RefCounted;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link SoundPlayer} is a simple utility class that plays sounds on the phone. The class
 * is used through its singleton instance.
 *
 * @see SoundPlayer#startPlaying
 */
@SuppressWarnings({"javadoc", "WeakerAccess"})
public class SoundPlayer implements SoundPool.OnLoadCompleteListener, SoundPoolIntf
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "SoundPlayer";
    public static boolean TRACE = true;
    protected Tracer tracer = Tracer.create(TAG, TRACE);

    protected static class InstanceHolder
        {
        public static SoundPlayer theInstance = new SoundPlayer(3, 6); // param choices are wet-finger-in-wind
        }
    public static SoundPlayer getInstance()
        {
        return InstanceHolder.theInstance;
        }

    public static final int msSoundTransmissionFreshness = 400;

    protected final Object         lock = new Object();
    protected final boolean        isRobotController = AppUtil.getInstance().isRobotController();
    protected SoundPool            soundPool;
    protected CountDownLatch       currentlyLoadingLatch = null;
    protected SoundInfo            currentlyLoadingInfo = null;
    protected LoadedSoundCache     loadedSounds;
    protected ExecutorService      threadPool;
    protected ScheduledExecutorService scheduledThreadPool;
    protected SharedPreferences    sharedPreferences;
    protected float                soundOnVolume = 1.0f;
    protected float                soundOffVolume = 0.0f;
    protected float                masterVolume = 1.0f;
    protected MediaPlayer          mediaSizer;

    protected static class CurrentlyPlaying
        {
        protected long msFinish = Long.MAX_VALUE;
        protected int streamId = 0;
        protected int loopControl = 0;
        protected @Nullable Runnable runWhenFinished = null;
        protected boolean isLooping()
            {
            return loopControl == -1;
            }
        }
    protected Set<CurrentlyPlaying> currentlyPlayingSounds;

    protected enum StopWhat { All, Loops }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    /**
     * Instantiates a new sound player.
     *
     * @param simultaneousStreams the number of sounds that can simultaneously play from this player.
     *                            If one, then playing any new sound interrupts the playing of a
     *                            a previous sound
     * @param cacheSize           the maximum size of the cache of loaded sounds.
     */
    public SoundPlayer(int simultaneousStreams, int cacheSize)
        {
        mediaSizer = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= 21)
            {
            AudioAttributes.Builder audioAttributesBuilder = new AudioAttributes.Builder();
            audioAttributesBuilder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
            AudioAttributes audioAttributes = audioAttributesBuilder.build();

            SoundPool.Builder soundPoolBuilder = new SoundPool.Builder();
            soundPoolBuilder.setAudioAttributes(audioAttributes);
            soundPoolBuilder.setMaxStreams(simultaneousStreams);
            soundPool = soundPoolBuilder.build();

            mediaSizer.setAudioAttributes(audioAttributes);
            AudioManager audioManager = (AudioManager)(AppUtil.getDefContext().getSystemService(Context.AUDIO_SERVICE));
            int audioSessionId = audioManager.generateAudioSessionId();
            mediaSizer.setAudioSessionId(audioSessionId);
            }
        else
            {
            /** {@link AudioManager#STREAM_NOTIFICATION} might have been a better choice, but we use STREAM_MUSIC because we've always done so; not worth changing */
            soundPool = new SoundPool(simultaneousStreams, AudioManager.STREAM_MUSIC, /*quality*/0); // can't use SoundPool.Builder on KitKat
            mediaSizer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
        loadedSounds = new LoadedSoundCache(cacheSize);
        currentlyPlayingSounds = new HashSet<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(AppUtil.getDefContext());
        //
        final CountDownLatch interlock = new CountDownLatch(1);
        threadPool = ThreadPool.newFixedThreadPool(1, "SoundPlayer");
        scheduledThreadPool = ThreadPool.newScheduledExecutor(1, "SoundPlayerScheduler");
        CallbackLooper.getDefault().post(new Runnable()
            {
            @Override public void run()
                {
                // Must call setOnLoadCompleteListener() on a looper thread, the one on which
                // we want to get completion callbacks to run. 'Bit of an odd API decision, but
                // there you go!
                soundPool.setOnLoadCompleteListener(SoundPlayer.this);
                interlock.countDown();
                }
            });
        try { interlock.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

    @Override
    public void close()
        {
        if (threadPool != null)
            {
            threadPool.shutdownNow();
            ThreadPool.awaitTerminationOrExitApplication(threadPool, 5, TimeUnit.SECONDS, "SoundPool", "internal error");
            threadPool = null;
            }
        if (scheduledThreadPool != null)
            {
            scheduledThreadPool.shutdownNow();
            ThreadPool.awaitTerminationOrExitApplication(scheduledThreadPool, 3, TimeUnit.SECONDS, "SoundPool", "internal error");
            }
        if (mediaSizer != null)
            {
            mediaSizer.release();
            }
        }

    /**
     * Ensures that these local sounds are also in the local cache
     */
    public void prefillSoundCache(@RawRes int... resourceIds)
        {
        for (@RawRes final int resId : resourceIds)
            {
            threadPool.submit(new Runnable()
                {
                @Override public void run()
                    {
                    ensureCached(AppUtil.getDefContext(), resId);
                    }
                });
            }
        }

    //----------------------------------------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------------------------------------

    public static class PlaySoundParams
        {
        /** an additional volume scaling that will be applied to this particular play action */
        public float volume = 1.0f;

        /** whether to wait for any currently-playing non-looping sound to finish before playing */
        public boolean waitForNonLoopingSoundsToFinish = true;

        /** -1 means playing loops forever, 0 is play once, 1 is play twice, etc */
        public int loopControl = 0;

        /** playback rate (1.0 = normal playback, range 0.5 to 2.0) */
        public float rate = 1.0f;

        //--------------

        public PlaySoundParams() { }

        public PlaySoundParams(boolean wait) { this.waitForNonLoopingSoundsToFinish = wait; }

        public PlaySoundParams(PlaySoundParams them)
            {
            this.volume = them.volume;
            this.waitForNonLoopingSoundsToFinish = them.waitForNonLoopingSoundsToFinish;
            this.loopControl = them.loopControl;
            this.rate = them.rate;
            }

        public boolean isLooping()
            {
            return loopControl == -1;
            }
        }

    /**
     * Asynchronously loads the indicated sound from its resource (if not already loaded), then
     * initiates its play once any current non-looping sound is finished playing.
     *
     * @param context   the context in which resId is to be interpreted
     * @param resId     the resource id of the raw resource containing the sound.
     */
    public void startPlaying(final Context context, @RawRes final int resId)
        {
        startPlaying(context, resId, new PlaySoundParams(true), null,null);
        }
    public void startPlaying(final Context context, File file)
        {
        startPlaying(context, file, new PlaySoundParams(true), null,null);
        }

    /**
     * Asynchronously loads the indicated sound from its resource (if not already loaded), then
     * initiates its play, optionally waiting for any currently non-looping playing sounds to finish first.
     *
     * @param context   the context in which resId is to be interpreted
     * @param resId     the resource id of the raw resource containing the sound.
     * @param params    controls how the playback proceeds
     * @param runWhenStarted   executed when the stream starts to play
     * @param runWhenFinished  executed when the stream finishes playing
     */
    public void startPlaying(final Context context, @RawRes final int resId, final PlaySoundParams params, @Nullable final Consumer<Integer> runWhenStarted,  @Nullable final Runnable runWhenFinished)
        {
        threadPool.execute(new Runnable()
            {
            @Override public void run()
                {
                loadAndStartPlaying(context, resId, params, runWhenStarted, runWhenFinished);
                }
            });
        }

    public void startPlaying(final Context context, final File file, final PlaySoundParams params, @Nullable final Consumer<Integer> runWhenStarted, @Nullable final Runnable runWhenFinished)
        {
        if (file==null) return;
        threadPool.execute(new Runnable()
            {
            @Override public void run()
                {
                loadAndStartPlaying(context, file, params, runWhenStarted, runWhenFinished);
                }
            });
        }

    /**
     * Stops playing all sounds that are currently playing
     */
    @Override
    public void stopPlayingAll()
        {
        internalStopPlaying(StopWhat.All);
        }

    /**
     * Stops playing all sounds that are currently playing in a loop
     */
    public void stopPlayingLoops()
        {
        internalStopPlaying(StopWhat.Loops);
        }

    protected void internalStopPlaying(StopWhat stopWhat)
        {
        synchronized (lock)
            {
            for (CurrentlyPlaying currentlyPlaying : currentlyPlayingSounds)
                {
                if (stopWhat==StopWhat.All || currentlyPlaying.isLooping() && stopWhat==StopWhat.Loops)
                    {
                    currentlyPlaying.msFinish = Long.MIN_VALUE;
                    }
                }

            checkForFinishedSounds();

            if (isRobotController)
                {
                // Tell the driver station too
                CommandList.CmdStopPlayingSounds cmdStopPlayingSounds = new CommandList.CmdStopPlayingSounds(stopWhat);
                Command command = new Command(CommandList.CmdPlaySound.Command, cmdStopPlayingSounds.serialize());
                NetworkConnectionHandler.getInstance().sendCommand(command);
                }
            }
        }


    /**
     * Preloads the sound so as to to reduce delays if the sound is subsequently played.
     */
    @Override
    public boolean preload(Context context, @RawRes int resourceId)
        {
        boolean result = false;
        synchronized (lock)
            {
            SoundInfo soundInfo = ensureLoaded(context, resourceId);
            if (soundInfo != null)
                {
                result = true;
                releaseRef(soundInfo);
                }
            }
        return result;
        }

    /**
     * Preloads the sound so as to to reduce delays if the sound is subsequently played.
     */
    @Override
    public boolean preload(Context context, File file)
        {
        boolean result = false;
        synchronized (lock)
            {
            SoundInfo soundInfo = ensureLoaded(context, file);
            if (soundInfo != null)
                {
                result = true;
                releaseRef(soundInfo);
                }
            }
        return result;
        }

    /**
     * Sets the master volume control that is applied to all played sounds
     * @see #getMasterVolume()
     */
    public void setMasterVolume(float masterVolume)
        {
        synchronized (lock)
            {
            this.masterVolume = masterVolume;
            }
        }

    /**
     * Returns the master volume control that is applied to all played sounds
     * @see #setMasterVolume(float)
     */
    public float getMasterVolume()
        {
        return this.masterVolume;
        }


    /** @deprecated use {@link #startPlaying(Context, int)} instead */
    @Deprecated
    public void play(final Context context, @RawRes final int resId)
        {
        startPlaying(context, resId);
        }

    /** @deprecated use {@link #startPlaying(Context, int, PlaySoundParams, Consumer, Runnable)} instead */
    @Deprecated
    public void play(final Context context, @RawRes final int resId, final boolean waitForCompletion)
        {
        startPlaying(context, resId, new PlaySoundParams(waitForCompletion), null, null);
        }

    //----------------------------------------------------------------------------------------------
    // Internal operations
    //----------------------------------------------------------------------------------------------

    protected void loadAndStartPlaying(Context context, @RawRes int resourceId, PlaySoundParams params, @Nullable final Consumer<Integer> runWhenStarted, @Nullable Runnable runWhenFinished)
        {
        synchronized (lock)
            {
            SoundInfo soundInfo = ensureLoaded(context, resourceId);
            if (soundInfo != null)
                {
                startPlayingLoadedSound(soundInfo, params, runWhenStarted, runWhenFinished);
                releaseRef(soundInfo);
                }
            }
        }

    protected void loadAndStartPlaying(Context context, File file, PlaySoundParams params, @Nullable final Consumer<Integer> runWhenStarted, @Nullable Runnable runWhenFinished)
        {
        synchronized (lock)
            {
            SoundInfo soundInfo = ensureLoaded(context, file);
            if (soundInfo != null)
                {
                startPlayingLoadedSound(soundInfo, params, runWhenStarted, runWhenFinished);
                releaseRef(soundInfo);
                }
            }
        }

    protected SoundInfo ensureLoaded(Context context, @RawRes int resourceId) // returns a ref
        {
        synchronized (lock)
            {
            SoundInfo result = loadedSounds.getResource(resourceId);
            if (result == null)
                {
                int msDuration = getMsDuration(context, resourceId);
                currentlyLoadingLatch = new CountDownLatch(1);
                currentlyLoadingInfo = result = new SoundInfo(context, resourceId, msDuration);
                int sampleId = soundPool.load(context, resourceId, 1);
                if (sampleId != 0)
                    {
                    result.initialize(sampleId);
                    loadedSounds.putResource(resourceId, result);
                    waitForLoadCompletion();
                    }
                else
                    tracer.traceError("unable to load sound resource 0x%08x", resourceId);
                }
            return result;
            }
        }

    protected SoundInfo ensureLoaded(Context context, File file) // returns a ref
        {
        synchronized (lock)
            {
            SoundInfo result = loadedSounds.getFile(file);
            if (result == null)
                {
                int msDuration = getMsDuration(context, file);
                currentlyLoadingLatch = new CountDownLatch(1);
                currentlyLoadingInfo = result = new SoundInfo(file, msDuration);
                int sampleId = soundPool.load(file.getAbsolutePath(), 1);
                if (sampleId != 0)
                    {
                    result.initialize(sampleId);
                    loadedSounds.putFile(file, result);
                    waitForLoadCompletion();
                    }
                else
                    tracer.traceError("unable to load sound %s", file);
                }
            return result;
            }
        }

    public boolean isLocalSoundOn()
        {
        return sharedPreferences.getBoolean(AppUtil.getDefContext().getString(R.string.pref_sound_on_off), true)
                && sharedPreferences.getBoolean(AppUtil.getDefContext().getString(R.string.pref_has_speaker), true);
        }

    void checkForFinishedSounds()
        {
        synchronized (lock)
            {
            long msNow = getMsNow();
            for (CurrentlyPlaying currentlyPlaying : new ArrayList<>(currentlyPlayingSounds)) // copy so we can remove while iterating
                {
                if (currentlyPlaying.msFinish <= msNow)
                    {
                    soundPool.stop(currentlyPlaying.streamId);
                    if (currentlyPlaying.runWhenFinished != null)
                        {
                        threadPool.execute(currentlyPlaying.runWhenFinished);
                        }
                    currentlyPlayingSounds.remove(currentlyPlaying);
                    }
                }
            }
        }

    // Play it for me, Sam.
    protected void startPlayingLoadedSound(final SoundInfo soundInfo, @Nullable PlaySoundParams paramsIn, @Nullable final Consumer<Integer> runWhenStarted, final @Nullable Runnable runWhenFinished)
        {
        // Get a writeable copy of the parameters
        final PlaySoundParams params = paramsIn==null ? new PlaySoundParams() : new PlaySoundParams(paramsIn);

        // Scale the volume by the master
        params.volume *= masterVolume;

        if (soundInfo != null)
            {
            synchronized (lock)
                {
                addRef(soundInfo);
                loadedSounds.noteSoundUsage(soundInfo);

                boolean soundOn = isLocalSoundOn();
                final float volume = (soundOn ? soundOnVolume : soundOffVolume) * params.volume;

                checkForFinishedSounds();

                long msNow = getMsNow();
                long msFinishNonLoopers = Long.MIN_VALUE;
                for (CurrentlyPlaying currentlyPlaying : currentlyPlayingSounds)
                    {
                    if (!currentlyPlaying.isLooping())
                        {
                        msFinishNonLoopers = Math.max(msFinishNonLoopers, currentlyPlaying.msFinish);
                        }
                    }
                final long msPresentation = params.waitForNonLoopingSoundsToFinish
                    ? Math.max(msNow, msFinishNonLoopers)
                    : msNow;
                long msDelay = msPresentation - msNow;

                Runnable playSound = new Runnable()
                    {
                    @Override public void run()
                        {
                        synchronized (lock)
                            {
                            long msStart = getMsNow();
                            final int streamId = soundPool.play(soundInfo.sampleId, /*leftVol*/volume, /*rightVol*/volume, /*priority*/1, params.loopControl, params.rate);
                            boolean result = 0 != streamId;
                            if (result)
                                {
                                long msDuration = soundInfo.msDuration * (params.isLooping() ? 1/*don't care*/ : (params.loopControl+1));

                                CurrentlyPlaying currentlyPlaying = new CurrentlyPlaying();
                                currentlyPlaying.streamId = streamId;
                                currentlyPlaying.loopControl = params.loopControl;
                                currentlyPlaying.msFinish = params.isLooping() ? Long.MAX_VALUE : msStart + msDuration;
                                currentlyPlaying.runWhenFinished = runWhenFinished;
                                currentlyPlayingSounds.add(currentlyPlaying);

                                if (runWhenFinished != null && !params.isLooping())
                                    {
                                    scheduledThreadPool.schedule(new Runnable()
                                        {
                                        @Override public void run()
                                            {
                                            checkForFinishedSounds();
                                            }
                                        }, msDuration + 5*(params.loopControl+1) /*slop so it'll definitely be done by the time we check*/, TimeUnit.MILLISECONDS);
                                    }

                                tracer.trace("playing volume=%f %s", volume, soundInfo);
                                soundInfo.msLastPlay = msStart;
                                }
                            else
                                {
                                tracer.traceError("unable to play %s", soundInfo);
                                }
                            releaseRef(soundInfo);

                            if (runWhenStarted != null)
                                {
                                threadPool.execute(new Runnable()
                                    {
                                    @Override public void run()
                                        {
                                        runWhenStarted.accept(streamId);
                                        }
                                    });
                                }
                            }
                        if (isRobotController)
                            {
                            // Tell the driver station too!
                            CommandList.CmdPlaySound cmdPlaySound = new CommandList.CmdPlaySound(msPresentation, soundInfo.hashString, params);
                            Command command = new Command(CommandList.CmdPlaySound.Command, cmdPlaySound.serialize());
                            command.setTransmissionDeadline(new Deadline(msSoundTransmissionFreshness, TimeUnit.MILLISECONDS));
                            NetworkConnectionHandler.getInstance().sendCommand(command);
                            }
                        }
                    };

                if (msDelay > 0)
                    {
                    // Wait for any current sound to finish playing.
                    scheduledThreadPool.schedule(playSound, msDelay, TimeUnit.MILLISECONDS);
                    }
                else
                    playSound.run();

                }
            }
        }

    protected int getMsDuration(Context context, @RawRes int resourceId)
        {
        int msDuration = 0;
        synchronized (lock)
            {
            try {
                mediaSizer.reset();
                AssetFileDescriptor afd = context.getResources().openRawResourceFd(resourceId);
                mediaSizer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mediaSizer.prepare();
                msDuration = mediaSizer.getDuration();
                }
            catch (IOException e)
                {
                tracer.traceError(e,"exception preparing media sizer; media duration taken to be zero");;
                }
            }
        return msDuration < 0 ? 0 : msDuration;
        }

    protected int getMsDuration(Context context, File file)
        {
        Uri uri = Uri.fromFile(file);
        int msDuration = 0;
        synchronized (lock)
            {
            try {
                mediaSizer.reset();
                mediaSizer.setDataSource(context, uri);
                mediaSizer.prepare();
                msDuration = mediaSizer.getDuration();
                }
            catch (IOException e)
                {
                tracer.traceError(e,"exception preparing media sizer; media duration taken to be zero");;
                }
            }
        return msDuration < 0 ? 0 : msDuration;
        }

    protected void waitForLoadCompletion()
        {
        // Wait for the load to finish. Note that our lock is held when this is called.
        try {
            currentlyLoadingLatch.await();
            currentlyLoadingLatch = null;
            currentlyLoadingInfo = null;
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        }

    @Override public void onLoadComplete(SoundPool soundPool, int sampleId, int status)
        {
        tracer.trace("onLoadComplete(%s, samp=%d)=%d", currentlyLoadingInfo, sampleId, status);
        currentlyLoadingLatch.countDown();
        }

    protected long getMsNow()
        {
        return AppUtil.getInstance().getWallClockTime();
        }

    //----------------------------------------------------------------------------------------------
    // Remoting
    //----------------------------------------------------------------------------------------------

    protected interface SoundFromFile
        {
        SoundInfo apply(File file);
        }

    /** returns a new ref on the returned {@link SoundInfo}; caller must releaseRef() */
    protected @CheckResult SoundInfo ensureLoaded(final String hashString, final SoundFromFile ifAbsent)
        {
        SoundInfo soundInfo = loadedSounds.getHash(hashString);
        if (soundInfo != null)
            {
            return soundInfo;
            }
        else
            {
            return ensureCached(hashString, ifAbsent);
            }
        }

    /** Ensures this local sound is also in the local cache. */
    protected void ensureCached(Context context, @RawRes int resId)
        {
        final SoundInfo soundInfo = ensureLoaded(context, resId);
        if (soundInfo != null)
            {
            String hashString = soundInfo.hashString;
            SoundInfo cachedSoundInfo = ensureCached(hashString, new SoundFromFile()
                {
                @Override public SoundInfo apply(File file)
                    {
                    InputStream inputStream = soundInfo.getInputStream();
                    OutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(file);
                        copy(inputStream, outputStream, soundInfo.cbSize);
                        }
                    catch (IOException e)
                        {
                        tracer.traceError(e, "exception caching file: %s", file);
                        }
                    finally
                        {
                        safeClose(outputStream);
                        safeClose(inputStream);
                        }
                    return null; // we don't need the actual sound
                    }
                });
            if (cachedSoundInfo != null)
                {
                releaseRef(cachedSoundInfo);
                }
            }
        }

    /** returns a new ref on the returned {@link SoundInfo}; caller must releaseRef() */
    protected @CheckResult SoundInfo ensureCached(final String hashString, final SoundFromFile ifAbsent)
        {
        final MutableReference<SoundInfo> result = new MutableReference<>(null);
        AppUtil.getInstance().ensureDirectoryExists(AppUtil.SOUNDS_CACHE, false);

        // Only one of these at a time, please
        FileBasedLock fileBasedLock = new FileBasedLock(AppUtil.SOUNDS_CACHE);

        try {
            fileBasedLock.lockWhile(new Runnable()
                {
                @Override public void run()
                    {
                    // It's not loaded. Do we have a cache?
                    boolean success = false;
                    File file = new File(AppUtil.SOUNDS_CACHE, hashString + ".sound"); // we don't know the actual format, so can't guess an extension
                    if (file.exists())
                        {
                        SoundInfo soundInfo = ensureLoaded(AppUtil.getDefContext(), file);
                        if (soundInfo != null)
                            {
                            result.setValue(soundInfo);
                            success = true;
                            }
                        }
                    if (!success)
                        {
                        result.setValue(ifAbsent.apply(file));
                        }
                    }
                });
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }

        return result.getValue();
        }

    public CallbackResult handleCommandPlaySound(String extra)
        {
        CallbackResult callbackResult = CallbackResult.HANDLED; // we may not be *successful*, but not worth having others try
        final CommandList.CmdPlaySound cmdPlaySound = CommandList.CmdPlaySound.deserialize(extra);

        SoundInfo soundInfo = ensureLoaded(cmdPlaySound.hashString, new SoundFromFile()
            {
            @Override public SoundInfo apply(File file)
                {
                return requestRemoteSound(file, cmdPlaySound.hashString);
                }
            });

        if (soundInfo != null)
            {
            long msPresentation = RobotLog.getLocalTime(cmdPlaySound.msPresentationTime);
            long msNow = getMsNow();
            long msDelay = msPresentation - msNow;

            // Ideally, if msDelay is positive, and if we know we're in good time synch, we'd wait until
            // the presentation time to actually play the sound. But we're a little queasy about relying
            // on the time synch, especially as we reduce heartbeats to save network traffic, so we omit
            // that, for now at least. As a consequence, it only makes sense for the RC to sends us stuff
            // it wants to play immediately, which is all that it presently ever sends, so we're OK.
            // See also the command.hasExpired() check in SendOnceRunnable.

            startPlayingLoadedSound(soundInfo, cmdPlaySound.getParams(), null, null);
            releaseRef(soundInfo);
            }

        return callbackResult;
        }

    public CallbackResult handleCommandStopPlayingSounds(Command stopPlayingSoundsCommand)
        {
        String extra = stopPlayingSoundsCommand.getExtra();
        CallbackResult callbackResult = CallbackResult.HANDLED; // we may not be *successful*, but not worth having others try
        CommandList.CmdStopPlayingSounds cmdStopPlayingSounds = CommandList.CmdStopPlayingSounds.deserialize(extra);
        tracer.trace("handleCommandStopPlayingSounds(): what=%s", cmdStopPlayingSounds.stopWhat);
        internalStopPlaying(cmdStopPlayingSounds.stopWhat);
        return callbackResult;
        }

    SoundInfo requestRemoteSound(File file, String hashString)
        {
        SoundInfo result = null;

        // We need to get it from the other side
        OutputStream outputStream = null;
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        InputStream inputStream = null;
        boolean deleteFile = true;
        try {
            // Open the file so we're ready for data
            outputStream = new FileOutputStream(file);

            // Start listening on an ephemeral local port
            serverSocket = new ServerSocket(0); // 0 == port assigned by the OS

            // Ask the other guy to send us that sound
            CommandList.CmdRequestSound cmdRequestSound = new CommandList.CmdRequestSound(hashString, serverSocket.getLocalPort());
            tracer.trace("handleCommandPlaySound(): requesting: port=%d hash=%s", cmdRequestSound.port, cmdRequestSound.hashString);
            NetworkConnectionHandler.getInstance().sendCommand(new Command(CommandList.CmdRequestSound.Command, cmdRequestSound.serialize()));

            final int msTimeout = 1 * 1000;
            serverSocket.setSoTimeout(msTimeout);
            try {
                clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(msTimeout);

                // Ok, he's sending. Suck it all in and write it to the file
                inputStream = clientSocket.getInputStream();

                // How much data is he going to send us?
                byte[] buffer = new byte[4];
                if (buffer.length == inputStream.read(buffer))
                    {
                    int cbToRead = TypeConversion.byteArrayToInt(buffer);
                    if (cbToRead > 0)
                        {
                        copy(inputStream, outputStream, cbToRead);
                        safeClose(outputStream); outputStream = null;
                        deleteFile = false;
                        tracer.trace("handleCommandPlaySound(): received: hash=%s", hashString);

                        result = ensureLoaded(AppUtil.getDefContext(), file);
                        }
                    else
                        tracer.traceError("handleCommandPlaySound(): client couldn't send sound");
                    }
                else
                    throw new IOException("framing error");
                }
            catch (SocketTimeoutException e)
                {
                tracer.traceError("timed out awaiting sound file");
                }
            }
        catch (IOException|RuntimeException e)
            {
            tracer.traceError(e, "handleCommandPlaySound(): exception thrown");
            }
        finally
            {
            safeClose(inputStream);
            safeClose(clientSocket);
            safeClose(serverSocket);
            safeClose(outputStream);
            if (deleteFile)
                {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                }
            }

        return result;
        }

    protected static void copy(InputStream inputStream, OutputStream outputStream, int cbToCopy) throws IOException
        {
        if (cbToCopy > 0)
            {
            byte[] buffer = new byte[256];
            for (;;)
                {
                int cbRead = inputStream.read(buffer);
                if (cbRead < 0)
                    throw new IOException("insufficient data");
                outputStream.write(buffer, 0, cbRead);
                cbToCopy -= cbRead;
                if (cbToCopy <= 0)
                    break;
                }
            }
        }

    public CallbackResult handleCommandRequestSound(Command requestSoundCommand)
        {
        String extra = requestSoundCommand.getExtra();
        CallbackResult callbackResult = CallbackResult.HANDLED; // we may not be *successful*, but not worth having others try
        CommandList.CmdRequestSound cmdRequestSound = CommandList.CmdRequestSound.deserialize(extra);
        tracer.trace("handleCommandRequestSound(): hash=%s", cmdRequestSound.hashString);
        //
        Socket socket = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            // He told us what port to use, but the host involved is just whomever sent us the command
            socket = new Socket(requestSoundCommand.getSender().getAddress(), cmdRequestSound.port);
            outputStream = socket.getOutputStream();

            SoundInfo soundInfo = loadedSounds.getHash(cmdRequestSound.hashString);
            if (soundInfo != null)
                {
                inputStream = soundInfo.getInputStream();
                }
            else
                tracer.traceError("handleCommandRequestSound(): can't find hash=%s", cmdRequestSound.hashString);

            if (inputStream != null)
                {
                // Write framing
                outputStream.write(TypeConversion.intToByteArray(soundInfo.cbSize));
                // Write data
                byte[] buffer = new byte[256];
                int cbWritten = 0;
                for (;;)
                    {
                    int cbRead = inputStream.read(buffer);
                    if (cbRead < 0)
                        break;
                    outputStream.write(buffer, 0, cbRead);
                    cbWritten += cbRead;
                    }
                tracer.trace("handleCommandRequestSound(): finished: %s cbSize=%d cbWritten=%d", soundInfo, soundInfo.cbSize, cbWritten);
                }
            else
                {
                // Write error framing to unblock caller
                outputStream.write(TypeConversion.intToByteArray(0));
                }

            releaseRef(soundInfo);
            }
        catch (IOException|RuntimeException e)
            {
            tracer.traceError(e, "handleCommandRequestSound(): exception thrown");
            }
        finally
            {
            safeClose(inputStream);
            safeClose(outputStream);
            safeClose(socket);
            }

        return callbackResult;
        }

    protected void safeClose(Object closeable)
        {
        if (closeable != null)
            {
            try {
                if (closeable instanceof Flushable)
                    {
                    try {
                        ((Flushable)closeable).flush();
                        }
                    catch (IOException e)
                        {
                        tracer.traceError(e, "exception while flushing");
                        }
                    }

                if (closeable instanceof Closeable)
                    {
                    ((Closeable) closeable).close();
                    }
                else
                    {
                    throw new IllegalArgumentException("Unknown object to close");
                    }
                }
            catch (IOException e)
                {
                tracer.traceError(e, "exception while closing");
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    protected class SoundInfo extends RefCounted
        {
        public final Context     context;
        public final @RawRes int resourceId;
        public final File        file;
        public final long        msDuration;
        public       int         sampleId;
        public       String      hashString; // String form of hash of the contents of the sound
        public       int         cbSize;
        public       long        msLastPlay = 0;

        @Override public String toString()
            {
            return Misc.formatInvariant("samp=%d|ms=%d", sampleId, msDuration);
            }

        public SoundInfo(Context context, @RawRes int resourceId, int msDuration)
            {
            this.context = context;
            this.resourceId = resourceId;
            this.file = null;
            this.msDuration = msDuration;
            this.hashString = computeHash();
            }

        public SoundInfo(File file, int msDuration)
            {
            this.context = null;
            this.resourceId = 0;
            this.file = file;
            this.msDuration = msDuration;
            this.hashString = computeHash();
            }

        public void initialize(int sampleId)
            {
            this.sampleId = sampleId;
            this.hashString = computeHash(); // also sets cbSize
            }

        @Override protected void destructor()
            {
            tracer.trace("unloading sound %s", this);
            soundPool.unload(sampleId);
            super.destructor();
            }

        public @Nullable InputStream getInputStream()
            {
            try {
                if (resourceId != 0)
                    {
                    return context.getResources().openRawResource(resourceId);
                    }
                else
                    {
                    return new FileInputStream(file);
                    }
                }
            catch (IOException e)
                {
                return null;
                }
            }

        protected String computeHash()
            {
            InputStream inputStream = getInputStream();
            if (inputStream != null)
                {
                try {
                    MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
                    byte[] buffer = new byte[256];
                    cbSize = 0;
                    for (;;)
                        {
                        int cbRead = inputStream.read(buffer);
                        if (cbRead < 0)
                            break;
                        cbSize += cbRead;
                        digest.update(buffer, 0, cbRead);
                        }
                    byte[] hash = digest.digest();
                    StringBuilder result = new StringBuilder();
                    for (int ib = 0; ib < hash.length; ib++)
                        {
                        result.append(String.format(Locale.ROOT, "%02x", hash[ib]));
                        }
                    return result.toString();
                    }
                catch (NoSuchAlgorithmException|IOException e)
                    {
                    tracer.traceError(e, "exception computing hash");
                    }
                finally
                    {
                    safeClose(inputStream);
                    }
                }
            throw Misc.illegalStateException("internal error: unable to compute hash of %s", this); // likely a bug; this will help us find
            }

        public Object getKey()
            {
            return resourceId==0 ? file : resourceId;
            }
        }

    public static SoundInfo addRef(SoundInfo soundInfo)
        {
        if (soundInfo != null)
            {
            soundInfo.addRef();
            }
        return soundInfo;
        }

    public static void releaseRef(SoundInfo soundInfo)
        {
        if (soundInfo != null)
            {
            soundInfo.releaseRef();
            }
        }

    /**
     * {@link LoadedSoundCache} keeps track of loaded sounds, mapping sound resource id to loaded
     * sound id. It keeps track of which sounds have been recently used, and unloads neglected
     * songs when a configured capacity of loaded sounds has been reached.
     */
    protected class LoadedSoundCache
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        private final Object lock = new Object();
        private final int capacity;         // max number of cached sounds
        private boolean unloadOnRemove;     // whether we should unload a sound when it's removed
        private final Map<Object, SoundInfo> keyMap;
        private final Map<String, SoundInfo> hashMap;

        class SoundInfoMap<K> extends LinkedHashMap<K, SoundInfo>
            {
            private static final float loadFactor = 0.75f; // allow extra headroom. worth it?

            public SoundInfoMap(int capacity)
                {
                super((int)Math.ceil(capacity / loadFactor) + 1, loadFactor, true);
                }

            @Override protected boolean removeEldestEntry(Entry<K, SoundInfo> eldest)
                {
                return size() > capacity;
                }

            @Override public SoundInfo remove(Object key)
                {
                SoundInfo removed = super.remove(key);
                if (unloadOnRemove)
                    {
                    if (removed != null)
                        {
                        releaseRef(removed);
                        }
                    }
                return removed;
                }
            };

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        LoadedSoundCache(int capacity)
            {
            this.keyMap = new SoundInfoMap<>(capacity);
            this.hashMap = new SoundInfoMap<>(capacity);
            this.capacity = capacity;
            this.unloadOnRemove = true;
            }

        //------------------------------------------------------------------------------------------
        // Accessing
        //------------------------------------------------------------------------------------------

        public @CheckResult SoundInfo getResource(@RawRes int resourceId)
            {
            synchronized (lock)
                {
                return addRef(keyMap.get(resourceId));
                }
            }

        public @CheckResult SoundInfo getFile(File file)
            {
            synchronized (lock)
                {
                return addRef(keyMap.get(file.getAbsoluteFile()));
                }
            }

        public @CheckResult SoundInfo getHash(String hashString)
            {
            synchronized (lock)
                {
                return addRef(hashMap.get(hashString));
                }
            }

        public void putResource(@RawRes int resourceId, SoundInfo info)
            {
            synchronized (lock)
                {
                keyMap.put(resourceId, addRef(info));
                hashMap.put(info.hashString, addRef(info));
                }
            }

        public void putFile(File file, SoundInfo info)
            {
            synchronized (lock)
                {
                keyMap.put(file.getAbsoluteFile(), addRef(info));
                hashMap.put(info.hashString, addRef(info));
                }
            }

        /** update the fact that this sound has been just used, again */
        public void noteSoundUsage(SoundInfo info)
            {
            synchronized (lock)
                {
                // We're updating the MRU, we don't want to unload the sound during the remove() below.
                unloadOnRemove = false;
                try {
                    // Make this key most recently used
                    Object key = info.getKey();
                    keyMap.remove(key);
                    keyMap.put(key, info);

                    hashMap.remove(info.hashString);
                    hashMap.put(info.hashString, info);
                    }
                finally
                    {
                    unloadOnRemove = true;
                    }
                }
            }
        }

    @Override
    public void play(Context context, @RawRes int resourceId, float volume, int loop, float rate)
        {
        PlaySoundParams params = new PlaySoundParams(false);
        params.volume = volume;
        params.loopControl = loop;
        params.rate = rate;
        startPlaying(context, resourceId, params, null, null);
        }

    @Override
    public void play(Context context, File file, float volume, int loop, float rate)
        {
        PlaySoundParams params = new PlaySoundParams(false);
        params.volume = volume;
        params.loopControl = loop;
        params.rate = rate;
        startPlaying(context, file, params, null, null);
        }
    }
