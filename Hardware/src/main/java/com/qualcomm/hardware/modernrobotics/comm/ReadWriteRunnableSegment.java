/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.hardware.modernrobotics.comm;

import android.annotation.SuppressLint;

import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("WeakerAccess")
@SuppressLint("DefaultLocale")
public class ReadWriteRunnableSegment {

  private int key;
  private int address;
  private boolean retryOnReadFailure;
  private TimeWindow timeWindow;

  final Lock lockRead;
  final private byte[] bufferRead;

  final Lock lockWrite;
  final private byte[] bufferWrite;

  public ReadWriteRunnableSegment(int key, int address, int size) {
    this.key = key;
    this.address = address;
    this.lockRead = new ReentrantLock();
    this.bufferRead = new byte[size];
    this.lockWrite = new ReentrantLock();
    this.bufferWrite = new byte[size];
    this.retryOnReadFailure = true; // true is the safer default
    this.timeWindow = new TimeWindow();
  }

  public int getKey() {
    return key;
  }

  public int getAddress() {
    return address;
  }

  public void setAddress(int address) {
    this.address = address;
  }

  public Lock getReadLock() {
    return lockRead;
  }

  public byte[] getReadBuffer() {
    return bufferRead;
  }

  public Lock getWriteLock() {
    return lockWrite;
  }

  public byte[] getWriteBuffer() {
    return bufferWrite;
  }

  public void setRetryOnReadFailure(boolean retryOnReadFailure) {
    this.retryOnReadFailure = retryOnReadFailure;
  }

  public boolean getRetryOnReadFailure() {
    return this.retryOnReadFailure;
  }

  public TimeWindow getTimeWindow() {
    return timeWindow;
  }

  public String toString() {
    return String.format("Segment - address:%d read:%d write:%d", address, bufferRead.length, bufferWrite.length);
  }
}
