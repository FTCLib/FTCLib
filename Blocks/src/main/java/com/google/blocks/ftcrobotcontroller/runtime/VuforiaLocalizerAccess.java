// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import java.util.concurrent.atomic.AtomicReference;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.Parameters;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

/**
 * A class that provides JavaScript access to {@link VuforiaLocalizer}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class VuforiaLocalizerAccess extends Access {

  VuforiaLocalizerAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "VuforiaLocalizer");
  }

  private VuforiaLocalizer checkVuforiaLocalizer(Object vuforiaLocalizerArg) {
    return checkArg(vuforiaLocalizerArg, VuforiaLocalizer.class, "vuforiaLocalizer");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VuforiaLocalizer create(Object vuforiaLocalizerParameters) {
    startBlockExecution(BlockType.CREATE, "");
    final Parameters parameters = checkVuforiaLocalizerParameters(vuforiaLocalizerParameters);
    if (parameters != null) {
      // Because the JavaBridge thread is a looper, but not the main looper, we need to create
      // another thread to call ClassFactory.createVuforiaLocalizer(parameters). Otherwise the
      // Vuforia.UpdateCallbackInterface.Vuforia_onUpdate method is called on the JavaBridge
      // thread and the camera monitor view won't update until after waitForStart is finished.
      final AtomicReference<VuforiaLocalizer> vuforiaLocalizerHolder = new AtomicReference<VuforiaLocalizer>();
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          vuforiaLocalizerHolder.set(ClassFactory.getInstance().createVuforia(parameters));
        }
      });
      thread.start();
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return vuforiaLocalizerHolder.get();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VuforiaTrackables loadTrackablesFromAsset(Object vuforiaLocalizerArg, String assetName) {
    startBlockExecution(BlockType.FUNCTION, ".loadTrackablesFromAsset");
    VuforiaLocalizer vuforiaLocalizer = checkVuforiaLocalizer(vuforiaLocalizerArg);
    if (vuforiaLocalizer != null) {
      return vuforiaLocalizer.loadTrackablesFromAsset(assetName);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VuforiaTrackables loadTrackablesFromFile(Object vuforiaLocalizerArg, String absoluteFileName) {
    startBlockExecution(BlockType.FUNCTION, ".loadTrackablesFromFile");
    VuforiaLocalizer vuforiaLocalizer = checkVuforiaLocalizer(vuforiaLocalizerArg);
    if (vuforiaLocalizer != null) {
      return vuforiaLocalizer.loadTrackablesFromFile(absoluteFileName);
    }
    return null;
  }
}
