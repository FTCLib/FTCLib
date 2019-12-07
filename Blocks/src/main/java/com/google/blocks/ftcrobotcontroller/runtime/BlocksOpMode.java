/*
Copyright 2016 Google LLC.

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

package com.google.blocks.ftcrobotcontroller.runtime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItemMap;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareType;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareUtil;
import com.google.blocks.ftcrobotcontroller.util.Identifier;
import com.google.blocks.ftcrobotcontroller.util.ProjectsUtil;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.opmode.InstanceOpModeManager;
import org.firstinspires.ftc.robotcore.internal.opmode.InstanceOpModeRegistrar;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMeta;
import org.firstinspires.ftc.robotcore.internal.opmode.RegisteredOpModes;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A subclass of {@link LinearOpMode} that loads JavaScript from a file and executes it.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public final class BlocksOpMode extends LinearOpMode {
  private static final String BLOCK_EXECUTION_ERROR = "Error: Error calling method on NPObject.";
  private static final String LOG_PREFIX = "BlocksOpMode - ";

  private static final AtomicReference<RuntimeException> fatalExceptionHolder = new AtomicReference<RuntimeException>();
  private static final AtomicReference<String> fatalErrorMessageHolder = new AtomicReference<String>();

  @SuppressLint("StaticFieldLeak")
  private static Activity activity;
  @SuppressLint("StaticFieldLeak")
  private static WebView webView;
  private static final AtomicReference<String> nameOfOpModeLoadedIntoWebView = new AtomicReference<String>();
  // Visible for testing.
  static final Map<String, Access> javascriptInterfaces = new ConcurrentHashMap<String, Access>();
  private final String project;
  private final String logPrefix;
  private final AtomicLong interruptedTime = new AtomicLong();

  private volatile BlockType currentBlockType;
  private volatile String currentBlockFirstName;
  private volatile String currentBlockLastName;

  /**
   * Instantiates a BlocksOpMode that loads JavaScript from a file and executes it when the op mode
   * is run.
   *
   * @param project the name of the project.
   */
  // Visible for testing
  BlocksOpMode(String project) {
    super();
    this.project = project;
    logPrefix = LOG_PREFIX + "\"" + project + "\" - ";
  }

  private String getLogPrefix() {
    Thread thread = Thread.currentThread();
    return logPrefix + thread.getThreadGroup().getName() + "/" + thread.getName() + " - ";
  }

  void startBlockExecution(BlockType blockType, String blockFirstName, String blockLastName) {
    currentBlockType = blockType;
    currentBlockFirstName = blockFirstName;
    currentBlockLastName = blockLastName;
    checkIfStopRequested();
  }

  String getFullBlockLabel() {
    switch (currentBlockType) {
      default:
        return "to runOpmode";
      case SPECIAL:
        return currentBlockFirstName + currentBlockLastName;
      case EVENT:
        return "to " +  currentBlockFirstName + currentBlockLastName;
      case CREATE:
        return "new " + currentBlockFirstName;
      case SETTER:
        return "set " + currentBlockFirstName + currentBlockLastName + " to";
      case GETTER:
        return currentBlockFirstName + currentBlockLastName;
      case FUNCTION:
        return "call " + currentBlockFirstName + currentBlockLastName;
    }
  }

  // TODO(lizlooney): Consider changeing existing code in *Access.java that catches exception to
  // call throwException instead of reportWarning.
  void throwException(Exception e) {
    String errorMessage = e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : "");
    RuntimeException re = new RuntimeException(
        "Fatal error occurred while executing the block labeled \"" + getFullBlockLabel() + "\". " +
        errorMessage, e);
    fatalExceptionHolder.set(re);
    throw re; // This will cause the opmode to stop.
  }

  private void checkIfStopRequested() {
    if (interruptedTime.get() != 0L &&
        isStopRequested() &&
        System.currentTimeMillis() - interruptedTime.get() >= msStuckDetectStop) {
      RobotLog.i(getLogPrefix() + "checkIfStopRequested - about to stop opmode by throwing RuntimeException");
      throw new RuntimeException("Stopping opmode " + project + " by force.");
    }
  }

  void waitForStartForBlocks() {
    // Because this method is executed on the Java Bridge thread, it is not interrupted when stop
    // is called. To fix this, we repeatedly wait 100ms and check isStarted.
    RobotLog.i(getLogPrefix() + "waitForStartForBlocks - start");
    try {
      while (!isStartedForBlocks()) {
        synchronized (this) {
          try {
            this.wait(100);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }
    } finally {
      RobotLog.i(getLogPrefix() + "waitForStartForBlocks - end");
    }
  }

  void sleepForBlocks(long millis) {
    // Because this method is executed on the Java Bridge thread, it is not interrupted when stop
    // is called. To fix this, we repeatedly sleep 100ms and check isInterrupted.
    RobotLog.i(getLogPrefix() + "sleepForBlocks - start");
    try {
      long endTime = System.currentTimeMillis() + millis;
      while (!isInterrupted()) {
        long chunk = Math.min(100L, endTime - System.currentTimeMillis());
        if (chunk <= 0) {
          break;
        }
        sleep(chunk);
      }
    } finally {
      RobotLog.i(getLogPrefix() + "sleepForBlocks - end");
    }
  }

  private boolean isInterrupted() {
    return interruptedTime.get() != 0L;
  }

  boolean isStartedForBlocks() {
    return super.isStarted() || isInterrupted();
  }

  boolean isStopRequestedForBlocks() {
    return super.isStopRequested() || isInterrupted();
  }

  @Override
  public void runOpMode() {
    RobotLog.i(getLogPrefix() + "runOpMode - start");
    cleanUpPreviousBlocksOpMode();
    try {
      fatalExceptionHolder.set(null);
      fatalErrorMessageHolder.set(null);

      currentBlockType = BlockType.EVENT;
      currentBlockFirstName = "";
      currentBlockLastName = "runOpMode";

      boolean interrupted = false;
      interruptedTime.set(0L);

      final AtomicBoolean scriptFinished = new AtomicBoolean();
      final Object scriptFinishedLock = new Object();

      final BlocksOpModeAccess blocksOpModeAccess = new BlocksOpModeAccess(
          Identifier.BLOCKS_OP_MODE.identifierForJavaScript, scriptFinishedLock, scriptFinished);

      javascriptInterfaces.put(
          Identifier.BLOCKS_OP_MODE.identifierForJavaScript, blocksOpModeAccess);

      // Start running the user's op mode blocks by calling loadScript on the UI thread.
      // Execution of the script is done by the WebView component, which uses the Java Bridge
      // thread to call into java.

      AppUtil appUtil = AppUtil.getInstance();

      synchronized (scriptFinishedLock) {
        appUtil.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            try {
              RobotLog.i(getLogPrefix() + "run1 - before loadScript");
              loadScript();
              RobotLog.i(getLogPrefix() + "run1 - after loadScript");
            } catch (Exception e) {
              RobotLog.e(getLogPrefix() + "run1 - caught " + e);
              // The exception may not have a stacktrace, so we check before calling
              // RobotLog.logStackTrace.
              if (e.getStackTrace() != null) {
                RobotLog.logStackTrace(e);
              }
            }
          }
        });

        // This thread (the thread executing BlocksOpMode.runOpMode) waits for the script to finish
        // When the script finishes, it calls BlocksOpModeAccess.scriptFinished() (on the Java
        // Bridge thread), which will set scriptFinished to true and call
        // scriptFinishedLock.notifyAll(). At that point, the scriptFinished.wait() call below
        // finish, allowing this thread to continue running.

        // If the stop button is pressed, the scriptFinished.wait() call below will be interrrupted
        // and this thread will catch InterruptedException. The script will continue to run and
        // this thread will continue to wait until scriptFinished is set. However, all calls from
        // javascript into java call Access.stopRunawayTrain. Access.stopRunawayTrain will throw a
        // RuntimeException if the elapsed time since catching the InterruptedException exceeds 20
        // seconds. The RuntimeException will cause the script to stop immediately, set
        // scriptFinished to true and call scriptFinished.notifyAll().

        RobotLog.i(getLogPrefix() + "runOpMode - before while !scriptFinished loop");
        while (!scriptFinished.get()) {
          try {
            scriptFinishedLock.wait();
          } catch (InterruptedException e) {
            RobotLog.e(getLogPrefix() + "runOpMode - caught InterruptedException during scriptFinishedLock.wait");
            interrupted = true;
            interruptedTime.set(System.currentTimeMillis());
          }
        }
        RobotLog.i(getLogPrefix() + "runOpMode - after while !scriptFinished loop");
      }

      // Clean up the WebView component by calling clearScript on the UI thread.
      appUtil.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          try {
            RobotLog.i(getLogPrefix() + "run2 - before clearScript");
            clearScript();
            RobotLog.i(getLogPrefix() + "run2 - after clearScript");
          } catch (Exception e) {
            RobotLog.e(getLogPrefix() + "run2 - caught " + e);
            // The exception may not have a stacktrace, so we check before calling
            // RobotLog.logStackTrace.
            if (e.getStackTrace() != null) {
              RobotLog.logStackTrace(e);
            }
          }
        }
      });

      // If an InterruptedException was caught, call Thread.currentThread().interrupt() to set
      // the interrupted status.

      if (interrupted) {
        Thread.currentThread().interrupt();
      }

      // If there was an exception, throw it now.
      RuntimeException fatalException = fatalExceptionHolder.getAndSet(null);
      if (fatalException != null) {
        throw fatalException;
      }

      // If there was a fatal error in the WebView component, set the global error message.
      String fatalErrorMessage = fatalErrorMessageHolder.getAndSet(null);
      if (fatalErrorMessage != null) {
        RobotLog.setGlobalErrorMsg(fatalErrorMessage);
      }
    } finally {
      long interruptedTime = this.interruptedTime.get();
      if (interruptedTime != 0L) {
        RobotLog.i(getLogPrefix() + "runOpMode - end - " +
            (System.currentTimeMillis() - interruptedTime) + "ms after InterruptedException");
      } else {
        RobotLog.i(getLogPrefix() + "runOpMode - end - no InterruptedException");
      }
    }
  }

  private void cleanUpPreviousBlocksOpMode() {
    String name = nameOfOpModeLoadedIntoWebView.get();
    if (name != null) {
      RobotLog.w(getLogPrefix() + "cleanUpPreviousBlocksOpMode - Warning: The Blocks runtime system is still loaded " +
          "with the Blocks op mode named " + name + ".");
      RobotLog.w(getLogPrefix() + "cleanUpPreviousBlocksOpMode - Trying to clean up now.");
      AppUtil.getInstance().synchronousRunOnUiThread(new Runnable() {
        @Override
        public void run() {
          try {
            RobotLog.w(getLogPrefix() + "cleanUpPreviousBlocksOpMode run - before clearScript");
            clearScript();
            RobotLog.w(getLogPrefix() + "cleanUpPreviousBlocksOpMode run - after clearScript");
          } catch (Exception e) {
            RobotLog.e(getLogPrefix() + "cleanUpPreviousBlocksOpMode run - caught " + e);
            // The exception may not have a stacktrace, so we check before calling
            // RobotLog.logStackTrace.
            if (e.getStackTrace() != null) {
              RobotLog.logStackTrace(e);
            }
          }
        }
      });
      if (nameOfOpModeLoadedIntoWebView.get() != null) {
        RobotLog.w(getLogPrefix() + "cleanUpPreviousBlocksOpMode - Clean up was successful.");
      } else {
        RobotLog.e(getLogPrefix() + "cleanUpPreviousBlocksOpMode - Error: Clean up failed.");
        throw new RuntimeException(
            "Unable to start running the Blocks op mode named " + project + ". The Blocks runtime " +
            "system is still loaded with the previous Blocks op mode named " + name + ". " +
            "Please restart the Robot Controller app.");
      }
    }
  }

  @SuppressLint("JavascriptInterface")
  private void addJavascriptInterfaces(HardwareItemMap hardwareItemMap) {
    addJavascriptInterfacesForIdentifiers();
    addJavascriptInterfacesForHardware(hardwareItemMap);

    for (Map.Entry<String, Access> entry : javascriptInterfaces.entrySet()) {
      String identifier = entry.getKey();
      Access access = entry.getValue();
      webView.addJavascriptInterface(access, identifier);
    }
  }

  // Visible for testing.
  void addJavascriptInterfacesForIdentifiers() {
    javascriptInterfaces.put(Identifier.ACCELERATION.identifierForJavaScript,
        new AccelerationAccess(this, Identifier.ACCELERATION.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.ANDROID_ACCELEROMETER.identifierForJavaScript,
        new AndroidAccelerometerAccess(this, Identifier.ANDROID_ACCELEROMETER.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.ANDROID_GYROSCOPE.identifierForJavaScript,
        new AndroidGyroscopeAccess(this, Identifier.ANDROID_GYROSCOPE.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.ANDROID_ORIENTATION.identifierForJavaScript,
        new AndroidOrientationAccess(this, Identifier.ANDROID_ORIENTATION.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.ANDROID_SOUND_POOL.identifierForJavaScript,
        new AndroidSoundPoolAccess(this, Identifier.ANDROID_SOUND_POOL.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.ANDROID_TEXT_TO_SPEECH.identifierForJavaScript,
        new AndroidTextToSpeechAccess(this, Identifier.ANDROID_TEXT_TO_SPEECH.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.ANGULAR_VELOCITY.identifierForJavaScript,
        new AngularVelocityAccess(this, Identifier.ANGULAR_VELOCITY.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.BLINKIN_PATTERN.identifierForJavaScript,
        new BlinkinPatternAccess(this, Identifier.BLINKIN_PATTERN.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.BNO055IMU_PARAMETERS.identifierForJavaScript,
        new BNO055IMUParametersAccess(this, Identifier.BNO055IMU_PARAMETERS.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.COLOR.identifierForJavaScript,
        new ColorAccess(this, Identifier.COLOR.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.DBG_LOG.identifierForJavaScript,
        new DbgLogAccess(this, Identifier.DBG_LOG.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.ELAPSED_TIME.identifierForJavaScript,
        new ElapsedTimeAccess(this, Identifier.ELAPSED_TIME.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.GAMEPAD_1.identifierForJavaScript,
        new GamepadAccess(this, Identifier.GAMEPAD_1.identifierForJavaScript, gamepad1));
    javascriptInterfaces.put(Identifier.GAMEPAD_2.identifierForJavaScript,
        new GamepadAccess(this, Identifier.GAMEPAD_2.identifierForJavaScript, gamepad2));
    javascriptInterfaces.put(Identifier.LINEAR_OP_MODE.identifierForJavaScript,
        new LinearOpModeAccess(this, Identifier.LINEAR_OP_MODE.identifierForJavaScript, project));
    javascriptInterfaces.put(Identifier.MAGNETIC_FLUX.identifierForJavaScript,
        new MagneticFluxAccess(this, Identifier.MAGNETIC_FLUX.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.MATRIX_F.identifierForJavaScript,
        new MatrixFAccess(this, Identifier.MATRIX_F.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.MISC.identifierForJavaScript,
        new MiscAccess(this, Identifier.MISC.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.NAVIGATION.identifierForJavaScript,
        new NavigationAccess(this, Identifier.NAVIGATION.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.OPEN_GL_MATRIX.identifierForJavaScript,
        new OpenGLMatrixAccess(this, Identifier.OPEN_GL_MATRIX.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.ORIENTATION.identifierForJavaScript,
        new OrientationAccess(this, Identifier.ORIENTATION.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.PIDF_COEFFICIENTS.identifierForJavaScript,
        new PIDFCoefficientsAccess(this, Identifier.PIDF_COEFFICIENTS.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.POSITION.identifierForJavaScript,
        new PositionAccess(this, Identifier.POSITION.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.QUATERNION.identifierForJavaScript,
        new QuaternionAccess(this, Identifier.QUATERNION.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.RANGE.identifierForJavaScript,
        new RangeAccess(this, Identifier.RANGE.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.SYSTEM.identifierForJavaScript,
        new SystemAccess(this, Identifier.SYSTEM.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.TELEMETRY.identifierForJavaScript,
        new TelemetryAccess(this, Identifier.TELEMETRY.identifierForJavaScript, telemetry));
    javascriptInterfaces.put(Identifier.TEMPERATURE.identifierForJavaScript,
        new TemperatureAccess(this, Identifier.TEMPERATURE.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.TFOD_ROVER_RUCKUS.identifierForJavaScript,
        new TfodRoverRuckusAccess(this, Identifier.TFOD_ROVER_RUCKUS.identifierForJavaScript, hardwareMap));
    javascriptInterfaces.put(Identifier.TFOD_SKY_STONE.identifierForJavaScript,
        new TfodSkyStoneAccess(this, Identifier.TFOD_SKY_STONE.identifierForJavaScript, hardwareMap));
    javascriptInterfaces.put(Identifier.VECTOR_F.identifierForJavaScript,
        new VectorFAccess(this, Identifier.VECTOR_F.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.VELOCITY.identifierForJavaScript,
        new VelocityAccess(this, Identifier.VELOCITY.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.VUFORIA_RELIC_RECOVERY.identifierForJavaScript,
        new VuforiaRelicRecoveryAccess(this, Identifier.VUFORIA_RELIC_RECOVERY.identifierForJavaScript, hardwareMap));
    javascriptInterfaces.put(Identifier.VUFORIA_ROVER_RUCKUS.identifierForJavaScript,
        new VuforiaRoverRuckusAccess(this, Identifier.VUFORIA_ROVER_RUCKUS.identifierForJavaScript, hardwareMap));
    javascriptInterfaces.put(Identifier.VUFORIA_SKY_STONE.identifierForJavaScript,
        new VuforiaSkyStoneAccess(this, Identifier.VUFORIA_SKY_STONE.identifierForJavaScript, hardwareMap));
    javascriptInterfaces.put(Identifier.VUFORIA_LOCALIZER.identifierForJavaScript,
        new VuforiaLocalizerAccess(this, Identifier.VUFORIA_LOCALIZER.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.VUFORIA_LOCALIZER_PARAMETERS.identifierForJavaScript,
        new VuforiaLocalizerParametersAccess(this, Identifier.VUFORIA_LOCALIZER_PARAMETERS.identifierForJavaScript, activity, hardwareMap));
    javascriptInterfaces.put(Identifier.VUFORIA_TRACKABLE.identifierForJavaScript,
        new VuforiaTrackableAccess(this, Identifier.VUFORIA_TRACKABLE.identifierForJavaScript));
    javascriptInterfaces.put(Identifier.VUFORIA_TRACKABLE_DEFAULT_LISTENER.identifierForJavaScript,
        new VuforiaTrackableDefaultListenerAccess(this, Identifier.VUFORIA_TRACKABLE_DEFAULT_LISTENER.identifierForJavaScript, hardwareMap));
    javascriptInterfaces.put(Identifier.VUFORIA_TRACKABLES.identifierForJavaScript,
        new VuforiaTrackablesAccess(this, Identifier.VUFORIA_TRACKABLES.identifierForJavaScript));
  }

  private void addJavascriptInterfacesForHardware(HardwareItemMap hardwareItemMap) {
    for (HardwareType hardwareType : HardwareType.values()) {
      if (hardwareItemMap.contains(hardwareType)) {
        for (HardwareItem hardwareItem : hardwareItemMap.getHardwareItems(hardwareType)) {
          if (javascriptInterfaces.containsKey(hardwareItem.identifier)) {
            RobotLog.w(getLogPrefix() + "There is already a JavascriptInterface for identifier \"" +
                hardwareItem.identifier + "\". Ignoring hardware type " + hardwareType + ".");
            continue;
          }
          Access access =
              HardwareAccess.newHardwareAccess(this, hardwareType, hardwareMap, hardwareItem);
          if (access != null) {
            javascriptInterfaces.put(hardwareItem.identifier, access);
          }
        }
      }
    }
  }

  private void removeJavascriptInterfaces() {
    Iterator<Map.Entry<String, Access>> it = javascriptInterfaces.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Access> entry = it.next();
      String identifier = entry.getKey();
      Access access = entry.getValue();
      webView.removeJavascriptInterface(identifier);
      access.close();
      it.remove();
    }
  }

  private class BlocksOpModeAccess extends Access {
    private final Object scriptFinishedLock;
    private final AtomicBoolean scriptFinished;

    private BlocksOpModeAccess(String identifier, Object scriptFinishedLock, AtomicBoolean scriptFinished) {
      super(BlocksOpMode.this, identifier, "");
      this.scriptFinishedLock = scriptFinishedLock;
      this.scriptFinished = scriptFinished;
    }

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void scriptStarting() {
      RobotLog.i(getLogPrefix() + "scriptStarting");
    }

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void caughtException(String message) {
      if (message != null) {
        // If a hardware device is used in blocks, but has been removed (or renamed) in the
        // configuration, the message is like "ReferenceError: left_drive is not defined".
        if (message.startsWith("ReferenceError: ") && message.endsWith(" is not defined")) {
          String missingHardwareDeviceName = message.substring(16, message.length() - 15);
          fatalErrorMessageHolder.compareAndSet(null,
              "Could not find hardware device: " + missingHardwareDeviceName);
          return;
        }

        // An exception occured while a block was executed. The message varies (depending on the
        // version of Android?) so we don't bother checking it.
        fatalErrorMessageHolder.compareAndSet(null,
            "Fatal error occurred while executing the block labeled \"" + getFullBlockLabel() + "\".");
      }

      RobotLog.e(getLogPrefix() + "caughtException - message is " + message);
    }

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void scriptFinished() {
      RobotLog.i(getLogPrefix() + "scriptFinished");
      synchronized (scriptFinishedLock) {
        scriptFinished.set(true);
        scriptFinishedLock.notifyAll();
      }
    }
  }

  private void loadScript() throws IOException {
    nameOfOpModeLoadedIntoWebView.set(project);
    HardwareItemMap hardwareItemMap = HardwareItemMap.newHardwareItemMap(hardwareMap);

    addJavascriptInterfaces(hardwareItemMap);

    String jsFileContent = ProjectsUtil.fetchJsFileContent(project);
    String jsContent = HardwareUtil.upgradeJs(jsFileContent, hardwareItemMap);

    String html = "<html><body onload='callRunOpMode()'><script type='text/javascript'>\n"
        + "function callRunOpMode() {\n"
        + "  blocksOpMode.scriptStarting();\n"
        + "  try {\n"
        + "    runOpMode();\n" // This calls the runOpMode method in the generated javascript.
        + "  } catch (e) {\n"
        + "    blocksOpMode.caughtException(String(e));\n"
        + "  }\n"
        + "  blocksOpMode.scriptFinished();\n"
        + "}\n"
        + "\n"
        + "function telemetryAddTextData(key, data) {\n"
        + "  switch (typeof data) {\n"
        + "    case 'string':\n"
        + "      telemetry.addTextData(key, data);\n"
        + "      break;\n"
        + "    case 'object':\n"
        + "      if (data instanceof Array) {\n"
        + "        telemetry.addTextData(key, String(data));\n"
        + "      } else {\n"
        + "        telemetry.addObjectData(key, data);\n"
        + "      }\n"
        + "      break;\n"
        + "    default:\n"
        + "      telemetry.addTextData(key, String(data));\n"
        + "      break;\n"
        + "  }\n"
        + "}\n"
        + "\n"
        + jsContent
        + "\n"
        + "</script></body></html>\n";
    webView.loadDataWithBaseURL(
        null /* baseUrl */, html, "text/html", "UTF-8", null /* historyUrl */);
  }

  private void clearScript() {
    removeJavascriptInterfaces();
    if (!javascriptInterfaces.isEmpty()) {
      RobotLog.w(getLogPrefix() + "clearScript - Warning: javascriptInterfaces is not empty.");
    }
    javascriptInterfaces.clear();

    webView.loadDataWithBaseURL(
        null /* baseUrl */, "", "text/html", "UTF-8", null /* historyUrl */);
    nameOfOpModeLoadedIntoWebView.set(null);
  }

  /**
   * Sets the {@link WebView} so that all BlocksOpModes can access it.
   */
  @SuppressLint("setJavaScriptEnabled")
  public static void setActivityAndWebView(Activity a, WebView wv) {
    if (activity == null && webView == null) {
      addOpModeRegistrar();
    }

    activity = a;
    webView = wv;
    webView.getSettings().setJavaScriptEnabled(true);

    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        RobotLog.i(LOG_PREFIX + "consoleMessage.message() " + consoleMessage.message());
        RobotLog.i(LOG_PREFIX + "consoleMessage.lineNumber() " + consoleMessage.lineNumber());
        return false; // continue with console logging.
      }
    });
  }

  private static void addOpModeRegistrar() {
    RegisteredOpModes.getInstance().addInstanceOpModeRegistrar(new InstanceOpModeRegistrar() {
      @Override public void register(InstanceOpModeManager manager) {
        try {
          // fetchEnabledProjectsWithJavaScript is thread-safe wrt concurrent saves from the browswer
          List<OpModeMeta> projects = ProjectsUtil.fetchEnabledProjectsWithJavaScript();
          for (OpModeMeta opModeMeta : projects) {
            manager.register(opModeMeta, new BlocksOpMode(opModeMeta.name));
          }
        } catch (Exception e) {
          RobotLog.logStackTrace(e);
        }
      }
    });
  }

  /**
   * @deprecated functionality now automatically called by the system
   */
  @Deprecated
  public static void registerAll(OpModeManager manager) {
    RobotLog.w(BlocksOpMode.class.getSimpleName(), "registerAll(OpModeManager) is deprecated and will be removed soon, as calling it is unnecessary in this and future API version");
  }
}
