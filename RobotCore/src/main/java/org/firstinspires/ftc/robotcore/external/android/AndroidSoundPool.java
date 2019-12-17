/*
Copyright 2018 Google LLC.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.firstinspires.ftc.robotcore.external.android;

import static org.firstinspires.ftc.robotcore.internal.system.AppUtil.BLOCKS_SOUNDS_DIR;

import android.content.Context;
import android.support.annotation.RawRes;
import org.firstinspires.ftc.robotcore.internal.android.SoundPoolIntf;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that provides access to the Android SoundPool.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class AndroidSoundPool {
  private static final String SOUNDS_DIR = "sounds";
  public static final String RAW_RES_PREFIX = "RawRes:";
  private final Map<String, File> soundFileMap = new HashMap<>();
  private final Map<String, Integer> soundResIdMap = new HashMap<>();
  private volatile SoundPoolIntf soundPool;
  private volatile float volume = 1.0f;
  private volatile float rate = 1.0f;
  private volatile int loop = 0;

  private File getSoundFile(String soundName) {
    File preloadedSoundFile = soundFileMap.get(soundName);
    if (preloadedSoundFile != null) {
      return preloadedSoundFile;
    }
    File soundFile = new File(BLOCKS_SOUNDS_DIR, soundName);
    if (soundPool.preload(getContext(), soundFile)) {
      soundFileMap.put(soundName, soundFile);
      return soundFile;
    }
    return null;
  }

  private @RawRes Integer getSoundResId(String soundName) {
    @RawRes Integer preloadedSoundResId = soundResIdMap.get(soundName);
    if (preloadedSoundResId != null) {
      return preloadedSoundResId;
    }

    Context context = getContext();
    @RawRes int soundResId = context.getResources().getIdentifier(
        soundName, "raw", context.getPackageName());
    if (soundResId != 0) {
      if (soundPool.preload(context, soundResId)) {
        soundResIdMap.put(soundName, soundResId);
        return soundResId;
      }
    }
    return null;
  }

  private Context getContext() {
    return AppUtil.getInstance().getRootActivity();
  }

  // public methods

  /**
   * Initializes the SoundPool.
   */
  @SuppressWarnings("deprecation")
  public void initialize(SoundPoolIntf soundPool) {
    this.soundPool = soundPool;
  }

  /**
   * Preloads the sound with the given name.
   *
   * @return true if sound is successfully preloaded, false otherwise
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public boolean preloadSound(String soundName) {
    if (soundPool == null) {
      throw new IllegalStateException("You forgot to call AndroidSoundPool.initialize!");
    }

    if (soundName.startsWith(RAW_RES_PREFIX)) {
      @RawRes Integer soundResId = getSoundResId(soundName.substring(RAW_RES_PREFIX.length()));
      return soundResId != null;
    }

    File soundFile = getSoundFile(soundName);
    return soundFile != null;
  }

  /**
   * Plays the sound with the given name.
   *
   * @return true if sound is successfully loaded, false otherwise
   * throws IllegalStateException if initialized has not been called yet.
   */
  public boolean play(String soundName) {
    if (soundPool == null) {
      throw new IllegalStateException("You forgot to call AndroidSoundPool.initialize!");
    }

    if (soundName.startsWith(RAW_RES_PREFIX)) {
      @RawRes Integer soundResId = getSoundResId(soundName.substring(RAW_RES_PREFIX.length()));
      if (soundResId != null) {
        soundPool.play(getContext(), soundResId, volume, loop, rate);
        return true;
      }
      return false;
    }

    File soundFile = getSoundFile(soundName);
    if (soundFile != null) {
      soundPool.play(getContext(), soundFile, volume, loop, rate);
      return true;
    }
    return false;
  }

  /**
   * Stops the playback.
   */
  public void stop() {
    if (soundPool != null) {
      soundPool.stopPlayingAll();
    }
  }

  /**
   * Returns the current volume.
   */
  public float getVolume() {
    return volume;
  }

  /**
   * Sets the volume. Volume range is 0.0 to 1.0.
   */
  public void setVolume(float volume) {
    if (volume >= 0.0f && volume <= 1.0f) {
      this.volume = volume;
    }
  }

  /**
   * Returns the playback rate.
   */
  public float getRate() {
    return rate;
  }

  /**
   * Sets the playback rate. Rate range is 0.5 to 2.0. Normal rate is 1.0.
   */
  public void setRate(float rate) {
    if (rate >= 0.5f && rate <= 2.0f) {
      this.rate = rate;
    }
  }

  /**
   * Returns the number of repeats.
   */
  public int getLoop() {
    return loop;
  }

  /**
   * Sets the number of repeats. Loop 0 means no repeats. Loop -1 means repeat indefinitely.
   */
  public void setLoop(int loop) {
    if (loop >= -1) {
      this.loop = loop;
    }
  }

  /**
   * Unloads all the sounds and releases the SoundPool.
   */
  public void close() {
    if (soundPool != null) {
      soundPool.stopPlayingAll();

      soundFileMap.clear();
      soundResIdMap.clear();

      // We don't call soundPool.close() here because we didn't create the soundPool instance.
      soundPool = null;
    }
  }
}
