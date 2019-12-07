// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.qualcomm.ftccommon.SoundPlayer;
import org.firstinspires.ftc.robotcore.external.android.AndroidSoundPool;

/**
 * A class that provides JavaScript access to the Android SoundPool.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class AndroidSoundPoolAccess extends Access {
  private final AndroidSoundPool androidSoundPool;

  AndroidSoundPoolAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "AndroidSoundPool");
    androidSoundPool = new AndroidSoundPool();
  }

  // Access methods

  @Override
  void close() {
    androidSoundPool.close();
  }

  // Javascript methods

  @SuppressWarnings({"unused", "deprecation"})
  @JavascriptInterface
  public void initialize() {
    startBlockExecution(BlockType.FUNCTION, ".initialize");
    androidSoundPool.initialize(SoundPlayer.getInstance());
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public boolean preloadSound(String soundName) {
    startBlockExecution(BlockType.FUNCTION, ".preloadSound");
    try {
      if (androidSoundPool.preloadSound(soundName))  {
        return true;
      }
      reportWarning("Failed to preload " + soundName);
    } catch (IllegalStateException e) {
      reportWarning(e.getMessage());
    }
    return false;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void play(String soundName) {
    startBlockExecution(BlockType.FUNCTION, ".play");
    try {
      if (!androidSoundPool.play(soundName))  {
        reportWarning("Failed to load " + soundName);
      }
    } catch (IllegalStateException e) {
      reportWarning(e.getMessage());
    }
  }

  @SuppressWarnings({"unused"})
  @JavascriptInterface
  public void stop() {
    startBlockExecution(BlockType.FUNCTION, ".stop");
    androidSoundPool.stop();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getVolume() {
    startBlockExecution(BlockType.GETTER, ".Volume");
    return androidSoundPool.getVolume();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setVolume(float volume) {
    startBlockExecution(BlockType.SETTER, ".Volume");
    if (volume >= 0.0f && volume <= 1.0f) {
      androidSoundPool.setVolume(volume);
    } else {
      reportInvalidArg("", "a number between 0.0 and 1.0");
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getRate() {
    startBlockExecution(BlockType.GETTER, ".Rate");
    return androidSoundPool.getRate();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setRate(float rate) {
    startBlockExecution(BlockType.SETTER, ".Rate");
    if (rate >= 0.5f && rate <= 2.0f) {
      androidSoundPool.setRate(rate);
    } else {
      reportInvalidArg("", "a number between 0.5 and 2.0");
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int getLoop() {
    startBlockExecution(BlockType.GETTER, ".Loop");
    return androidSoundPool.getLoop();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setLoop(int loop) {
    startBlockExecution(BlockType.SETTER, ".Loop");
    if (loop >= -1) {
      androidSoundPool.setLoop(loop);
    } else {
      reportInvalidArg("", "a number greater than or equal to -1");
    }
  }
}
