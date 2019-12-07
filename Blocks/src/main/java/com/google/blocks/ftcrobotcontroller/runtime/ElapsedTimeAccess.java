// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.ElapsedTime.Resolution;

/**
 * A class that provides JavaScript access to {@link ElapsedTime}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class ElapsedTimeAccess extends Access {

  ElapsedTimeAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "ElapsedTime");
  }

  private ElapsedTime checkElapsedTime(Object elapsedTimeArg) {
    return checkArg(elapsedTimeArg, ElapsedTime.class, "timer");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public ElapsedTime create() {
    startBlockExecution(BlockType.CREATE, "");
    return new ElapsedTime();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public ElapsedTime create_withStartTime(long startTime) {
    startBlockExecution(BlockType.CREATE, "");
    return new ElapsedTime(startTime);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public ElapsedTime create_withResolution(String resolutionString) {
    startBlockExecution(BlockType.CREATE, "");
    Resolution resolution = checkArg(resolutionString, Resolution.class, "resolution");
    if (resolution != null) {
      return new ElapsedTime(resolution);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getStartTime(Object elapsedTimeArg) {
    startBlockExecution(BlockType.GETTER, ".StartTime");
    ElapsedTime elapsedTime = checkElapsedTime(elapsedTimeArg);
    if (elapsedTime != null) {
      return elapsedTime.startTime();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getTime(Object elapsedTimeArg) {
    startBlockExecution(BlockType.GETTER, ".Time");
    ElapsedTime elapsedTime = checkElapsedTime(elapsedTimeArg);
    if (elapsedTime != null) {
      return elapsedTime.time();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getSeconds(Object elapsedTimeArg) {
    startBlockExecution(BlockType.GETTER, ".Seconds");
    ElapsedTime elapsedTime = checkElapsedTime(elapsedTimeArg);
    if (elapsedTime != null) {
      return elapsedTime.seconds();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getMilliseconds(Object elapsedTimeArg) {
    startBlockExecution(BlockType.GETTER, ".Milliseconds");
    ElapsedTime elapsedTime = checkElapsedTime(elapsedTimeArg);
    if (elapsedTime != null) {
      return elapsedTime.milliseconds();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getResolution(Object elapsedTimeArg) {
    startBlockExecution(BlockType.GETTER, ".Resolution");
    ElapsedTime elapsedTime = checkElapsedTime(elapsedTimeArg);
    if (elapsedTime != null) {
      Resolution resolution = elapsedTime.getResolution();
      if (resolution != null) {
        return resolution.toString();
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getAsText(Object elapsedTimeArg) {
    startBlockExecution(BlockType.GETTER, ".AsText");
    ElapsedTime elapsedTime = checkElapsedTime(elapsedTimeArg);
    if (elapsedTime != null) {
      return elapsedTime.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void reset(Object elapsedTimeArg) {
    startBlockExecution(BlockType.FUNCTION, ".reset");
    ElapsedTime elapsedTime = checkElapsedTime(elapsedTimeArg);
    if (elapsedTime != null) {
      elapsedTime.reset();
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void log(Object elapsedTimeArg, String label) {
    startBlockExecution(BlockType.FUNCTION, ".log");
    ElapsedTime elapsedTime = checkElapsedTime(elapsedTimeArg);
    if (elapsedTime != null) {
      elapsedTime.log(label);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String toText(Object elapsedTimeArg) {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    ElapsedTime elapsedTime = checkElapsedTime(elapsedTimeArg);
    if (elapsedTime != null) {
      return elapsedTime.toString();
    }
    return "";
  }
}
