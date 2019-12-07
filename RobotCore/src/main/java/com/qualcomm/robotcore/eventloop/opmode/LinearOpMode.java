package com.qualcomm.robotcore.eventloop.opmode;

import com.qualcomm.robotcore.hardware.TimestampedI2cData;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryInternal;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeManagerImpl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Base class for user defined linear operation modes (op modes).
 * <p>
 * This class derives from OpMode, but you should not override the methods from
 * OpMode.
 */
@SuppressWarnings("unused")
public abstract class LinearOpMode extends OpMode {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  private LinearOpModeHelper helper          = null;
  private ExecutorService    executorService = null;
  private volatile boolean   isStarted       = false;
  private volatile boolean   stopRequested   = false;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public LinearOpMode() {
  }

  //------------------------------------------------------------------------------------------------
  // Operations
  //------------------------------------------------------------------------------------------------

  /**
   * Override this method and place your code here.
   * <p>
   * Please do not swallow the InterruptedException, as it is used in cases
   * where the op mode needs to be terminated early.
   * @throws InterruptedException
   */
  abstract public void runOpMode() throws InterruptedException;

  /**
   * Pauses the Linear Op Mode until start has been pressed or until the current thread
   * is interrupted.
   */
  public synchronized void waitForStart() {
    while (!isStarted()) {
      synchronized (this) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  /**
   * Wait for one full cycle of the hardware
   * <p>
   * Each cycle of the hardware your commands are sent out to the hardware; and
   * the latest data is read back in.
   * <p>
   * This method has a strong guarantee to wait for <strong>at least</strong> one
   * full hardware hardware cycle.
   * @throws InterruptedException
   *
   * @deprecated The need for user code to synchronize with the loop() thread has been
   *             obviated by improvements in the modern motor and servo controller implementations.
   *             Remaining uses of this API are likely unncessarily wasting cycles. If a simple non-zero
   *             delay is required, the {@link Thread#sleep(long) sleep()} method is a better choice.
   *             If one simply wants to allow other threads to run, {@link #idle()} is a good choice.
   *
   * @see Thread#sleep(long)
   * @see #idle()
   * @see #waitForNextHardwareCycle()
   */
  @Deprecated
  public void waitOneFullHardwareCycle() throws InterruptedException {
    // wait for current partial cycle to finish
    waitForNextHardwareCycle();

    // wait for the next hardware cycle to start
    Thread.sleep(1);

    // now wait one full cycle
    waitForNextHardwareCycle();
  }

  /**
   * Wait for the start of the next hardware cycle
   * <p>
   * Each cycle of the hardware your commands are sent out to the hardware; and
   * the latest data is read back in.
   * <p>
   * This method will wait for the current hardware cycle to finish, which is
   * also the start of the next hardware cycle.
   * @throws InterruptedException
   *
   * @deprecated The need for user code to synchronize with the loop() thread has been
   *             obviated by improvements in the modern motor and servo controller implementations.
   *             Remaining uses of this API are likely unncessarily wasting cycles. If a simple non-zero
   *             delay is required, the {@link Thread#sleep(long) sleep()} method is a better choice.
   *             If one simply wants to allow other threads to run, {@link #idle()} is a good choice.
   *
   * @see Thread#sleep(long)
   * @see #idle()
   * @see #waitOneFullHardwareCycle()
   */
  @Deprecated
  public void waitForNextHardwareCycle() throws InterruptedException {
    /*
     * If an InterruptedException is thrown we won't handle it, instead
     * we will pass it up to the calling method to handle.
     *
     * In the case of the linear op mode; this will likely cause the
     * thread to terminate.
     */
    synchronized (this) {
      this.wait();
    }
  }

  /**
   * Puts the current thread to sleep for a bit as it has nothing better to do. This allows other
   * threads in the system to run.
   *
   * <p>One can use this method when you have nothing better to do in your code as you await state
   * managed by other threads to change. Calling idle() is entirely optional: it just helps make
   * the system a little more responsive and a little more efficient.</p>
   *
   * <p>{@link #idle()} is conceptually related to waitOneFullHardwareCycle(), but makes no
   * guarantees as to completing any particular number of hardware cycles, if any.</p>
   *
   * @see #opModeIsActive()
   * @see #waitOneFullHardwareCycle()
   */
  public final void idle() {
    // Otherwise, yield back our thread scheduling quantum and give other threads at
    // our priority level a chance to run
    Thread.yield();
    }

  /**
   * Sleeps for the given amount of milliseconds, or until the thread is interrupted. This is
   * simple shorthand for the operating-system-provided {@link Thread#sleep(long) sleep()} method.
   *
   * @param milliseconds amount of time to sleep, in milliseconds
   * @see Thread#sleep(long)
   */
  public final void sleep(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Answer as to whether this opMode is active and the robot should continue onwards. If the
   * opMode is not active, the OpMode should terminate at its earliest convenience.
   *
   * <p>Note that internally this method calls {@link #idle()}</p>
   *
   * @return whether the OpMode is currently active. If this returns false, you should
   *         break out of the loop in your {@link #runOpMode()} method and return to its caller.
   * @see #runOpMode()
   * @see #isStarted()
   * @see #isStopRequested()
   */
  public final boolean opModeIsActive() {
    boolean isActive = !this.isStopRequested() && this.isStarted();
    if (isActive) {
      idle();
    }
    return isActive;
  }

  /**
   * Has the opMode been started?
   *
   * @return whether this opMode has been started or not
   * @see #opModeIsActive()
   * @see #isStopRequested()
   */
  public final boolean isStarted() {
    return this.isStarted || Thread.currentThread().isInterrupted();
    }

  /**
   * Has the the stopping of the opMode been requested?
   *
   * @return whether stopping opMode has been requested or not
   * @see #opModeIsActive()
   * @see #isStarted()
   */
  public final boolean isStopRequested() {
    return this.stopRequested || Thread.currentThread().isInterrupted();
    }

  /**
   * From the non-linear OpMode; do not override
   */
  @Override
  final public void init() {
    this.executorService = ThreadPool.newSingleThreadExecutor("LinearOpMode");
    this.helper          = new LinearOpModeHelper();
    this.isStarted       = false;
    this.stopRequested   = false;

    this.executorService.execute(helper);
  }

  /**
   * From the non-linear OpMode; do not override
   */
  @Override
  final public void init_loop() {
    handleLoop();
  }

  /**
   * From the non-linear OpMode; do not override
   */
  @Override
  final public void start() {
    stopRequested = false;
    isStarted = true;
    synchronized (this) {
      this.notifyAll();
    }
  }

  /**
   * From the non-linear OpMode; do not override
   */
  @Override
  final public void loop() {
    handleLoop();
  }

  /**
   * From the non-linear OpMode; do not override
   */
  @Override
  final public void stop() {

    // make isStopRequested() return true (and opModeIsActive() return false)
    stopRequested = true;

    if (executorService != null) {  // paranoia
    
      // interrupt the linear opMode and shutdown it's service thread
      executorService.shutdownNow();

      /** Wait, forever, for the OpMode to stop. If this takes too long, then
       * {@link OpModeManagerImpl#callActiveOpModeStop()} will catch that and take action */
      try {
        String serviceName = "user linear op mode";
        ThreadPool.awaitTermination(executorService, 100, TimeUnit.DAYS, serviceName);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  protected void handleLoop() {
    // if there is a runtime exception in user code; throw it so the normal error
    // reporting process can handle it
    if (helper.hasRuntimeException()) {
      throw helper.getRuntimeException();
    }

    synchronized (this) {
      this.notifyAll();
    }
  }

  protected class LinearOpModeHelper implements Runnable {

    protected RuntimeException exception  = null;
    protected boolean          isShutdown = false;

    public LinearOpModeHelper() {
    }

    @Override
    public void run() {
      ThreadPool.logThreadLifeCycle("LinearOpMode main", new Runnable() { @Override public void run() {
        exception = null;
        isShutdown = false;

        try {
          LinearOpMode.this.runOpMode();
          requestOpModeStop();
        } catch (InterruptedException ie) {
          // InterruptedException, shutting down the op mode
          RobotLog.d("LinearOpMode received an InterruptedException; shutting down this linear op mode");
        } catch (CancellationException ie) {
          // In our system, CancellationExceptions are thrown when data was trying to be acquired, but
          // an interrupt occurred, and you're in the unfortunate situation that the data acquisition API
          // involved doesn't allow InterruptedExceptions to be thrown. You can't return (what data would
          // you return?), and so you have to throw a RuntimeException. CancellationException seems the
          // best choice.
          RobotLog.d("LinearOpMode received a CancellationException; shutting down this linear op mode");
        } catch (RuntimeException e) {
          exception = e;
        } finally {
          // If the user has given us a telemetry.update() that hasn't get gone out, then
          // push it out now. However, any NEW device health warning should be suppressed while
          // doing so, since required state might have been cleaned up by now and thus generate errors.
          TimestampedI2cData.suppressNewHealthWarningsWhile(new Runnable() {
            @Override public void run() {
              if (telemetry instanceof TelemetryInternal) {
                telemetry.setMsTransmissionInterval(0); // will be reset the next time the opmode runs
                ((TelemetryInternal) telemetry).tryUpdateIfDirty();
              }
            }
          });
          // Do the necessary bookkeeping
          isShutdown = true;
        }
      }});
    }

    public boolean hasRuntimeException() {
      return (exception != null);
    }

    public RuntimeException getRuntimeException() {
      return exception;
    }
    
    public boolean isShutdown() {
      return isShutdown;
    }
  }

  //----------------------------------------------------------------------------------------------
  // Telemetry management
  //----------------------------------------------------------------------------------------------

  @Override public void internalPostInitLoop() {
    // Do NOT call super, as that updates telemetry unilaterally
    if (telemetry instanceof TelemetryInternal) {
      ((TelemetryInternal)telemetry).tryUpdateIfDirty();
    }
  }

  @Override public void internalPostLoop() {
    // Do NOT call super, as that updates telemetry unilaterally
    if (telemetry instanceof TelemetryInternal) {
      ((TelemetryInternal)telemetry).tryUpdateIfDirty();
    }
  }}
