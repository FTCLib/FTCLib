package org.firstinspires.ftc.robotcore.internal.tfod;

import android.util.Log;
import java.util.concurrent.TimeUnit;

/**
 * Class to simplify periodic execution.
 *
 * After specifying a rate (in Hz), successive calls to sleep() or checkedSleep() will prevent
 * execution from proceeding past the point of the sleep function faster than the requested rate.
 * This is accomplished with a simple Thread.sleep().
 */
public class Rate {

  private static final String TAG = "Rate";

  private final long minSleepTimeNanos;
  private long lastExecutionTimeNanos;

  /**
   * @param rate How fast this Rate should operate, in Hz.
   */
  public Rate(double rate) {
    minSleepTimeNanos = (long) ((1.0 / rate) * 1e9);
  }

  /** Figure out how long we need to sleep for, and update the lastExecutionTimeNanos. */
  private long getNextSleepTimeMs() {
    final long currentTimeNanos = System.nanoTime();
    final long diffTimeNanos = currentTimeNanos - lastExecutionTimeNanos;
    final long sleepTimeNanos = minSleepTimeNanos - diffTimeNanos;

    return TimeUnit.MILLISECONDS.convert(sleepTimeNanos, TimeUnit.NANOSECONDS);
  }

  /** Sleep for the required amount of time to maintain the rate. */
  public void checkedSleep() throws InterruptedException {
    final long sleepTimeMs = getNextSleepTimeMs();

    if (sleepTimeMs > 0) {
      //Log.v(TAG, "Sleeping for ms: " + sleepTimeMs);
      Thread.sleep(sleepTimeMs);
    } else {
      //Log.d(TAG, "Not sleeping. This might indicate that something is too slow!");
    }

    lastExecutionTimeNanos = System.nanoTime();
  }

  /** Convenience method to catch InterruptedException and interrupt current thread again. */
  public void sleep() {
    try {
      checkedSleep();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      // This doesn't get done if checkedSleep() throws, so we do it again here.
      lastExecutionTimeNanos = System.nanoTime();
    }
  }
}
