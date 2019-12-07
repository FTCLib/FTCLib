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

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.robot.RobotState;
import com.qualcomm.robotcore.util.TypeConversion;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Hold telemtry data
 */
@SuppressWarnings("unused")
public class TelemetryMessage extends RobocolParsableBase {

  public  static final String       DEFAULT_TAG = "TELEMETRY_DATA";
  private static final Charset      CHARSET     = Charset.forName("UTF-8");

  private final Map<String, String> dataStrings = new LinkedHashMap<String, String>();  // linked so as to preserve addition order as iteration order
  private final Map<String, Float>  dataNumbers = new LinkedHashMap<String, Float>();
  private       String              tag         = "";     // an empty tag is treated as the default tag
  private       long                timestamp   = 0;      // when was this telemetry transmitted (ms)
  private       boolean             isSorted    = true;   // should this telemetry be sorted on the driver station
  private       RobotState          robotState  = RobotState.UNKNOWN;

  public TelemetryMessage() {
    // default constructor
  }

  public TelemetryMessage(byte[] byteArray) throws RobotCoreException {
    fromByteArray(byteArray);
  }

  /**
   * Timestamp this message was sent. Timestamp is in wall time.
   * @return timestamp, or 0 if never sent
   */
  public synchronized long getTimestamp() {
    return timestamp;
  }

  /**
   * Returns whether this telemetry should be sorted by keys on the driver station or not.
   * If not sorted, then data is displayed in the order in which it was added to the telemetry.
   * @return whether the telemetry display should be sorted on the driver station
   * @see #setSorted(boolean)
   */
  public boolean isSorted() {
    return this.isSorted;
  }

  /**
   * Sets whether the telemetry should be sorted by its keys on the driver station or not.
   * @param isSorted whether the telemetry is to be sorted on the driver station
   * @see #isSorted()
   */
  public void setSorted(boolean isSorted) {
    this.isSorted = isSorted;
  }

  public RobotState getRobotState() {
    return this.robotState;
  }

  public void setRobotState(RobotState robotState) {
    this.robotState = robotState;
  }

  /**
   * Set the optional tag value.
   * <p>
   * Setting this to an empty string is equal to setting this to the default value.
   *
   * @see #DEFAULT_TAG
   * @param tag tag this telemetry data
   */
  public synchronized void setTag(String tag) {
    this.tag = tag;
  }

  /**
   * Get the optional tag value
   * @return tag
   */
  public synchronized String getTag() {
    if (tag.length() == 0) return DEFAULT_TAG;

    return tag;
  }

  /**
   * Add a data point
   * <p>
   * All messages will be assumed to be in UTF-8.
   * @param key message key
   * @param msg message
   */
  public synchronized void addData(String key, String msg) {
    dataStrings.put(key, msg);
  }

  /**
   * Add a data point
   * <p>
   * Calls toString() on the given object.
   * @param key message key
   * @param msg message object
   */
  public synchronized void addData(String key, Object msg) {
    dataStrings.put(key, msg.toString());
  }

  /**
   * Add a data point
   * <p>
   * Msg will be down cast to a float.
   * @param key message key
   * @param msg message
   */
  public synchronized void addData(String key, float msg) {
    dataNumbers.put(key, msg);
  }

  /**
   * Add a data point
   * <p>
   * msg will automatically be downcast to a float.
   * @param key message key
   * @param msg message
   */
  public synchronized void addData(String key, double msg) {
    dataNumbers.put(key, (float)(msg));
  }

  /**
   * Get a reference to the map of messages
   * @return reference to the messages
   */
  public synchronized Map<String, String> getDataStrings() {
    return dataStrings;
  }

  public synchronized Map<String, Float> getDataNumbers() {
    return dataNumbers;
  }

  /**
   * Return true if this telemetry object has data added to it
   * @return true if this object has data, otherwise false
   */
  public synchronized boolean hasData() {
    return (dataStrings.isEmpty() == false || dataNumbers.isEmpty() == false);
  }

  /**
   * Clear all messages
   * <p>
   * Clear all messages, reset the timestamp to 0
   */
  public synchronized void clearData() {
    timestamp = 0;
    dataStrings.clear();
    dataNumbers.clear();
  }

  @Override
  public MsgType getRobocolMsgType() {
    return MsgType.TELEMETRY;
  }

  @Override
  public synchronized byte[] toByteArray() throws RobotCoreException {
  // See countMessageBytes(...) for information about data format

    timestamp = System.currentTimeMillis();

    if (dataStrings.size() > cCountMax) {
      throw new RobotCoreException("Cannot have more than %d string data points", cCountMax);
    }

    if (dataNumbers.size() > cCountMax) {
      throw new RobotCoreException("Cannot have more than %d number data points", cCountMax);
    }

    int payloadSize = countMessageBytes();
    int totalSize   = RobocolParsable.HEADER_LENGTH + payloadSize;

    ByteBuffer buffer = getWriteBuffer(payloadSize);

    // timestamp
    buffer.putLong(timestamp);

    // sorted
    buffer.put((byte)(isSorted ? 1 : 0));

    // robot state
    buffer.put(robotState.asByte());

    // tag
    if (tag.length() == 0) {
      putTagLen(buffer, 0);
    } else {
      byte tagBytes[] = tag.getBytes(CHARSET);

      if (tagBytes.length > cbTagMax) {
        throw new RobotCoreException(String.format("Telemetry tag cannot exceed %d bytes [%s]", cbTagMax, tag));
      }

      putTagLen(buffer, tagBytes.length);
      buffer.put(tagBytes);
    }

    // data strings
    putCount(buffer, dataStrings.size());
    for (Entry<String, String> entry : dataStrings.entrySet()) {
      byte[] key = entry.getKey().getBytes(CHARSET);
      byte[] value = entry.getValue().getBytes(CHARSET);

      if (key.length > cbKeyMax)
        throw new RobotCoreException("telemetry key '%s' too long: %d bytes; max %d bytes", entry.getKey(), key.length, cbKeyMax);
      if (value.length > cbValueMax)
        throw new RobotCoreException("telemetry value '%s' too long: %d bytes; max %d bytes", entry.getValue(), value.length, cbValueMax);

      putKeyLen(buffer, key.length);
      buffer.put(key);
      putValueLen(buffer, value.length);
      buffer.put(value);
    }

    // data numbers
    putCount(buffer, dataNumbers.size());
    for (Entry<String, Float> entry : dataNumbers.entrySet()) {
      byte[] key = entry.getKey().getBytes(CHARSET);
      float val = entry.getValue();

      if (key.length > cbKeyMax)
        throw new RobotCoreException("telemetry key '%s' too long: %d bytes; max %d bytes", entry.getKey(), key.length, cbKeyMax);

      putKeyLen(buffer, key.length);
      buffer.put(key);
      buffer.putFloat(val);
    }

    // done
    return buffer.array();
  }

  @Override
  public synchronized void fromByteArray(byte[] byteArray) throws RobotCoreException {

    clearData();

    ByteBuffer buffer = getReadBuffer(byteArray);

    // timestamp
    timestamp = buffer.getLong();

    // sorted
    isSorted = buffer.get() != 0;

    // robot state
    robotState = RobotState.fromByte(buffer.get());

    // tag
    int tagLength = getTagLen(buffer);
    if (tagLength == 0) {
      tag = "";
    } else {
      byte[] tagBytes = new byte[tagLength];
      buffer.get(tagBytes);
      tag = new String(tagBytes , CHARSET);
    }

    // data strings
    int stringDataPoints = getCount(buffer);
    for (int i = 0; i < stringDataPoints; i++) {
      int keyLength = getKeyLen(buffer);
      byte[] keyBytes = new byte[keyLength];
      buffer.get(keyBytes);

      int valLength = getValueLen(buffer);
      byte[] valBytes = new byte[valLength];
      buffer.get(valBytes);

      String key = new String(keyBytes, CHARSET);
      String val = new String(valBytes, CHARSET);

      dataStrings.put(key, val);
    }

    // data numbers
    int numberDataPoints = getCount(buffer);
    for (int i = 0; i < numberDataPoints; i++) {
      int keyLength = getKeyLen(buffer);
      byte[] keyBytes = new byte[keyLength];
      buffer.get(keyBytes);
      String key = new String(keyBytes, CHARSET);
      float val = buffer.getFloat();

      dataNumbers.put(key, val);
    }
  }

  //------------------------------------------------------------------------------------------------
  // Sizing
  //------------------------------------------------------------------------------------------------

  static final int cbTimestamp = 8;
  static final int cbSorted   = 1;
  static final int cbRobotState = 1;
  static final int cbTagLen   = 1;
  static final int cbCountLen = 1;
  static final int cbKeyLen   = 2;
  static final int cbValueLen = 2;
  static final int cbFloat    = 4;

  public final static int cbTagMax   = (1 << (cbTagLen*8))   - 1;
  public final static int cCountMax  = (1 << (cbCountLen*8)) - 1;
  public final static int cbKeyMax   = (1 << (cbKeyLen*8))   - 1;
  public final static int cbValueMax = (1 << (cbValueLen*8)) - 1;

  static void putCount(ByteBuffer buffer, int count) {
    buffer.put((byte)count);
  }
  static int getCount(ByteBuffer buffer) {
    return TypeConversion.unsignedByteToInt(buffer.get());
  }

  static void putTagLen(ByteBuffer buffer, int cbTag) {
    buffer.put((byte)cbTag);
  }
  static int getTagLen(ByteBuffer buffer) {
    return TypeConversion.unsignedByteToInt(buffer.get());
  }

  static void putKeyLen(ByteBuffer buffer, int cbKey) {
    buffer.putShort((short)cbKey);
  }
  static int getKeyLen(ByteBuffer buffer) {
    return TypeConversion.unsignedShortToInt(buffer.getShort());
  }

  static void putValueLen(ByteBuffer buffer, int cbValue) { putKeyLen(buffer, cbValue); }
  static int  getValueLen(ByteBuffer buffer)              { return getKeyLen(buffer);   }

  private int countMessageBytes() {

    /*
     * Data format
     *
     * bytes    | format | value
     * ---------|--------|---------------------------------
     *  8       | int64  | timestamp
     *  1       | uint8  | isSorted
     *  1       | uint8  | robotState
     *  1       | uint8  | length of tag (may be zero)
     *  varies  | UTF-8  | value of tag
     *  1       | uint8  | count of string data points
     *  varies  | varies | string data points
     *  1       | uint8  | count of number data points
     *  varies  | varies | number data points
     *
     *
     * String Data Points (repeating)
     *
     * bytes    | format | value
     * ---------|--------|---------------------------------
     *  2       | uint16 | length of key
     *  varies  | UTF-8  | key
     *  2       | uint16 | length of value
     *  varies  | UTF-8  | value
     *
     * Number Data Points (repeating)
     *
     * bytes    | format | value
     * ---------|--------|---------------------------------
     *  2       | uint16 | length of key
     *  varies  | UTF-8  | key
     *  4       | float  | value
     */

    int count = cbTimestamp + cbSorted + cbRobotState;

    // count the length of the tag
    count += cbTagLen + tag.getBytes(CHARSET).length;

    // count the string data
    count += cbCountLen;
    for(Entry<String, String> entry : dataStrings.entrySet()) {
      count += cbKeyLen + entry.getKey().getBytes(CHARSET).length;
      count += cbValueLen + entry.getValue().getBytes(CHARSET).length;
    }

    // count the number data
    count += cbCountLen;
    for (Entry<String, Float> entry: dataNumbers.entrySet()) {
      count += cbKeyLen + entry.getKey().getBytes(CHARSET).length;
      count += cbFloat;
    }

    return count;
  }

}
