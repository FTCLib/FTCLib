/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.robotcore.util;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Calculate a rolling average from a stream of numbers. Only the last 'size' elements will be
 * considered.
 */
public class RollingAverage {

  public static final int DEFAULT_SIZE = 100;

  private final Queue<Integer> queue = new LinkedList<Integer>();
  private long total;
  private int size;

  /**
   * Constructor, with default size
   */
  public RollingAverage() {
    resize(DEFAULT_SIZE);
  }

  /**
   * Constructor, with given size
   * @param size
   */
  public RollingAverage(int size) {
    resize(size);
  }

  /**
   * Get the size
   * @return Size
   */
  public int size() {
    return size;
  }

  /**
   * Resize the rolling average
   * <p>
   * Side Effect: the rolling average is reset
   * @param size
   */
  public void resize(int size) {
    this.size = size;
    queue.clear();
  }

  /**
   * Add a number to the rolling average
   * @param number
   */
  public void addNumber(int number) {
    if (queue.size() >= size) {
      int last = queue.remove();
      total -= last;
    }

    queue.add(number);
    total += number;
  }

  /**
   * Get the rolling average
   * @return rolling average, if available; otherwise 0
   */
  public int getAverage() {
    if (queue.isEmpty()) return 0;
    return (int) (total / queue.size());
  }

  /**
   * Reset the rolling average
   */
  public void reset() {
    queue.clear();
  }
}
