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

package com.qualcomm.robotcore.robocol;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.Deadline;

import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Comparator;

/**
 * Class used to send and receive commands
 * <p>
 * These commands should be acknowledged by the receiver. The sender may resend the command
 * repeatedly until it receives and acknowledgment from the receiver. The receiver should not
 * reprocess repeated commands.
 */
@SuppressWarnings("WeakerAccess")
public class Command extends RobocolParsableBase implements Comparable<Command>, Comparator<Command> {

  // space for the timestamp (8 bytes), ack byte (1 byte)
  private static final short cbStringLength = 2;
  private static final short cbPayloadBase = 8 + 1;

  String  mName;
  String  mExtra;
  long    mTimestamp;
  boolean mAcknowledged = false;
  byte    mAttempts = 0;
  boolean mIsInjected = false; // not transmitted over network
  @Nullable Deadline mTransmissionDeadline = null;
  InetSocketAddress mSender; // only on reception

  /**
   * Constructs a {@link Command} for transmission.
   */
  public Command(String name) {
    this(name, "");
  }

  /**
   * Constructs a {@link Command} for transmission.
   */
  public Command(String name, String extra) {
    mName       = name;
    mExtra      = extra;
    mTimestamp  = generateTimestamp();
  }

  /**
   * Constructs a Command from a received {@link RobocolDatagram}.
   */
  public Command(RobocolDatagram packet) throws RobotCoreException {
    fromByteArray(packet.getData());
    mSender = new InetSocketAddress(packet.getAddress(), packet.getPort());
  }

  /**
   * The receiver should call this method before sending this command back to the sender
   */
  public void acknowledge() {
    mAcknowledged = true;
  }

  /**
   * Check if this command has been acknowledged
   * @return true if acknowledged, otherwise false
   */
  public boolean isAcknowledged() {
    return mAcknowledged;
  }

  /**
   * Get the command name as a string
   * @return command name
   */
  public String getName() {
    return mName;
  }

  /**
   * Get the extra data as a string
   * @return extra string
   */
  public String getExtra() { return mExtra; }

  /**
   * Number of times this command was packaged into a byte array
   * <p>
   * After Byte.MAX_VALUE is reached, this will stop counting and remain at Byte.MAX_VALUE.
   *
   * @return number of times this command was packaged into a byte array
   */
  public byte getAttempts() {
    return mAttempts;
  }

  public boolean hasExpired() {
    return mTransmissionDeadline != null && mTransmissionDeadline.hasExpired();
  }

  /*
   * (non-Javadoc)
   * @see com.qualcomm.robotcore.robocol.RobocolParsable#getRobocolMsgType()
   */
  @Override
  public MsgType getRobocolMsgType() {
    return RobocolParsable.MsgType.COMMAND;
  }

  public boolean isInjected() {
    return mIsInjected;
  }

  public void setIsInjected(boolean isInjected) {
    this.mIsInjected = isInjected;
  }

  public void setTransmissionDeadline(Deadline deadline) {
    mTransmissionDeadline = deadline;
  }

  public InetSocketAddress getSender() {
    return mSender;
  }

  /*
   * (non-Javadoc)
   * @see com.qualcomm.robotcore.robocol.RobocolParsable#toByteArray()
   */
  @Override
  public byte[] toByteArray() throws RobotCoreException {

    if (mAttempts != Byte.MAX_VALUE) mAttempts += 1;

    byte[] nameBytes  = TypeConversion.stringToUtf8(mName);
    byte[] extraBytes = TypeConversion.stringToUtf8(mExtra);

    short cbPayload = (short)getPayloadSize(nameBytes.length, extraBytes.length);
    if (cbPayload > Short.MAX_VALUE)
      throw new IllegalArgumentException(String.format("command payload is too large: %d", cbPayload));

    ByteBuffer buffer = getWriteBuffer(cbPayload);
    try {
      buffer.putLong(mTimestamp);
      buffer.put((byte)(mAcknowledged ? 1 : 0));
      buffer.putShort((short) nameBytes.length);
      buffer.put(nameBytes);

      // If we are just an ack, then we don't transmit the body in order to save net bandwidth
      if (!mAcknowledged) {
        buffer.putShort((short) extraBytes.length);
        buffer.put(extraBytes);
      }
    } catch (BufferOverflowException e) {
      RobotLog.logStacktrace(e);
    }
    return buffer.array();
  }

  int getPayloadSize(int nameBytesLength, int extraBytesLength) {
    if (mAcknowledged) {
      return cbPayloadBase + cbStringLength + nameBytesLength;
    } else {
      return cbPayloadBase + cbStringLength + nameBytesLength + cbStringLength + extraBytesLength;
    }
  }

  /*
   * (non-Javadoc)
   * @see com.qualcomm.robotcore.robocol.RobocolParsable#fromByteArray(byte[])
   */
  @Override
  public void fromByteArray(byte[] byteArray) throws RobotCoreException {
    ByteBuffer buffer = getReadBuffer(byteArray);

    mTimestamp = buffer.getLong();
    mAcknowledged = (buffer.get() != 0);

    int cbName = TypeConversion.unsignedShortToInt(buffer.getShort());
    byte[] nameBytes = new byte[cbName];
    buffer.get(nameBytes);
    mName = TypeConversion.utf8ToString(nameBytes);

    if (!mAcknowledged) {
      int cbExtra = TypeConversion.unsignedShortToInt(buffer.getShort());
      byte[] extraBytes = new byte[cbExtra];
      buffer.get(extraBytes);
      mExtra = TypeConversion.utf8ToString(extraBytes);
    }
  }

  @Override
  public String toString() {
    return String.format("command: %20d %5s %s", mTimestamp, mAcknowledged, mName);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Command) {
      Command c = (Command) o;
      if (this.mName.equals(c.mName) && this.mTimestamp == c.mTimestamp) return true;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return (mName.hashCode() ^ (int)mTimestamp); // xor preserves entropy
  }

  @Override
  public int compareTo(Command another) {
    int diff = mName.compareTo(another.mName);

    if (diff != 0) return diff;

    if (mTimestamp < another.mTimestamp) return -1;
    if (mTimestamp > another.mTimestamp) return  1;

    return 0;
  }

  @Override
  public int compare(Command c1, Command c2) {
    return c1.compareTo(c2);
  }

  public static long generateTimestamp() {
    return System.nanoTime();
  }
}