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

import com.qualcomm.robotcore.eventloop.SyncdDevice;

import java.util.concurrent.ExecutorService;

@SuppressWarnings("unused")
public interface ReadWriteRunnable extends Runnable, SyncdDevice {

  enum BlockingState { BLOCKING, WAITING }

  int MAX_BUFFER_SIZE = 256;

  void setCallback(Callback callback);

  boolean writeNeeded();

  void resetWriteNeeded();

  void write(int address, byte[] data);

  void setAcceptingWrites(boolean acceptingWrites);

  boolean getAcceptingWrites();

  void drainPendingWrites();

  void suppressReads(boolean suppress);

  byte[] readFromWriteCache(int address, int size);

  byte[] read(int address, int size);

  void close();

  ReadWriteRunnableSegment createSegment(int key, int address, int size);

  void destroySegment(int key);

  ReadWriteRunnableSegment getSegment(int key);

  void queueSegmentRead(int key);
  void queueSegmentWrite(int key);

  void executeUsing(ExecutorService service);

  @Override
  void run();

  interface Callback {
    void startupComplete() throws InterruptedException;
    void readComplete() throws InterruptedException;
    void writeComplete() throws InterruptedException;
    void shutdownComplete() throws  InterruptedException;
  }

  class EmptyCallback implements Callback {

    @Override
    public void startupComplete() throws InterruptedException {
      // take no action
      }

    @Override
    public void readComplete() throws InterruptedException {
      // take no action
    }

    @Override
    public void writeComplete() throws InterruptedException {
      // take no action
    }

    @Override
    public void shutdownComplete() throws InterruptedException  {
      // take no action
      }
  }
}
