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

package com.qualcomm.robotcore.eventloop;

import com.qualcomm.robotcore.hardware.usb.RobotUsbModule;

/**
 * {@link SyncdDevice} is for a device that wants to be in sync with the event loop. If there is sync'd
 * device registered with the event loop manager then the event loop manager will run the event
 * loop in this manner:
 * <ol>
 *   <li>wait until all sync'd device have returned from blockUtilReady()</li>
 *   <li>run EventLoop.loop()</li>
 *   <li>call startBlockingWork() on all sync'd device</li>
 * </ol>
 * Sync'd devices need to register themselves with the event loop manager
 *
 * Note: the original actual need for {@link SyncdDevice} per se now lies in the dustbin of time.
 * However, this self-same object and its associated registration mechanism is also used as part
 * of logic used to deal with abnormal device shutdown (e.g.: USB disconnects) and the processing
 * thereof. The interface thus probably now deserves a better name.
 */
public interface SyncdDevice {

  interface Manager {
    void registerSyncdDevice(SyncdDevice device);
    void unregisterSyncdDevice(SyncdDevice device);
  }

  interface Syncable {
    void setSyncDeviceManager(Manager manager);
  }

  /**
   * Has this device shutdown abnormally? Note that even if this method returns true that
   * a close() will still be necessary to fully clean up associated resources.
   *
   * @return whether the device has experienced an abnormal shutdown
   */
  ShutdownReason getShutdownReason();

  /**
   * {@link ShutdownReason} indicates the health of the shutdown of the device.
   */
  enum ShutdownReason {
    /** The device shutdown normally */
    NORMAL,
    /** The device shutdown abnormally, and we don't know anything about when it might
     * be useful to try to communicate with it again. Its USB cable might have been pulled,
     * for example, and we don't know when it will be plugged back in, if ever. */
    ABNORMAL,
    /** The device shutdown abnormally, but due to a temporary circumstance (such as ESD)
     * that we believe might be recoverable from after a brief interval
     * @see #msAbnormalReopenInterval */
    ABNORMAL_ATTEMPT_REOPEN
  }

  /** When a device shuts down with {@link ShutdownReason#ABNORMAL_ATTEMPT_REOPEN}, this is
   * the recommended duration of time to wait before attempting reopen. It was only heuristically
   * determined, and might thus perhaps be shortened */
  int msAbnormalReopenInterval = 250;

  /**
  * Records the owning module of this sync'd device. The owner of the device is the party
  * that is responsible for the device's lifetime management, and thus who should be involved
  * if the device experiences problems and needs to be shutdown or restarted.
  *
  * @see #getOwner()
  */
  void setOwner(RobotUsbModule owner);

  /**
   * Retrieves the owning module of this sync'd device.
   *
   * @return the {@link RobotUsbModule} which is the owner of this device
   * @see #setOwner(RobotUsbModule)
   */
  RobotUsbModule getOwner();
}
