/*
 * Copyright (C) 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.firstinspires.ftc.robotcore.internal.tfod;

/**
 * Threadsafe class to maintain a rolling average (in constant time).
 *
 * <p>Clients have the choice of the default value being 0 or a specified value. Calling with a
 * specified value ensures that the first call to get() will return a value that is within reason
 * for the expected values of the rolling average, whereas calling with 0 will mean that it will
 * take until after the first call to add() to get a reasonable value from this class.
 *
 * @author Vasu Agrawal
 */
public class RollingAverage {

  /** Each of the elements, maintained as a ring buffer. */
  private final double[] elements;

  /** How many elements we have stored in the elements array. */
  private int numElements;

  /** The index into elements of the first available slot (oldest element). */
  private int elementsIndex;

  /** The sum for all valid elements inside elements. */
  private double sum;

  /** The actual current average. */
  // Volatile keyword ensures that updates are atomic and seen by all threads.
  private volatile double average;

  /**
   * Construct a new RollingAverage which will start at a specific value, rather than 0.
   *
   * @param bufferSize The number of elements to keep a rolling average over.
   * @param startingValue The value to return from get() if add() has not been called.
   */
  public RollingAverage(int bufferSize, double startingValue) {

    if (bufferSize <= 0) {
      throw new IllegalArgumentException("Need positive buffer size!");
    }

    this.average = startingValue;
    elements = new double[bufferSize];
  }

  /**
   * Construct a new RollingAverage which will start at 0.
   *
   * @param bufferSize The number of elements to keep a rolling average over.
   */
  public RollingAverage(int bufferSize) {
    this(bufferSize, 0); // Default starting value of 0.
  }

  public double get() {
    // Just returning average is fine, as long as there's never a chance of it being in an
    // inconsistent state inside the add() method.
    return average;
  }

  public synchronized void add(double value) {

    // First, see if space needs to be made for the new value.
    numElements++;

    if (numElements > elements.length) {
      double removeValue = elements[elementsIndex];
      numElements = elements.length;
      sum -= removeValue; // Remove the evicted value from the sum.
    }

    // Then, store the value we just got.
    elements[elementsIndex] = value;
    elementsIndex = (elementsIndex + 1) % elements.length;

    // Finally, add the new value into the sum and compute the new average.
    sum += value;
    average = sum / numElements; // Write to volatile double is atomic
  }
}
