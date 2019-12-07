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

package com.qualcomm.robotcore.robocol;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.exception.RobotProtocolException;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class PeerDiscovery extends RobocolParsableBase {

  public static final String TAG = "PeerDiscovery";

  //------------------------------------------------------------------------------------------------
  // Types
  //------------------------------------------------------------------------------------------------

  /**
   * Peer type
   */
  public enum PeerType {
    /*
     * NOTE: when adding new message types, do not change existing message
     * type values or you will break backwards capability.
     */
    NOT_SET(0),
    PEER(1),
    GROUP_OWNER(2);

    private static final PeerType[] VALUES_CACHE = PeerType.values();
    private int type;

    /**
     * Create a PeerType from a byte
     * @param b
     * @return PeerType
     */
    public static PeerType fromByte(byte b) {
      PeerType p = NOT_SET;
      try {
        p = VALUES_CACHE[b];
      } catch (ArrayIndexOutOfBoundsException e) {
        RobotLog.w(String.format("Cannot convert %d to Peer: %s", b, e.toString()));
      }
      return p;
    }

    private PeerType(int type) {
      this.type = type;
    }

    /**
     * Return this peer type as a byte
     * @return peer type as byte
     */
    public byte asByte() {
      return (byte)(type);
    }
  }

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  private PeerType peerType;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public static PeerDiscovery forReceive() {
    return new PeerDiscovery(PeerType.NOT_SET);
  }

  public PeerDiscovery(PeerDiscovery.PeerType peerType) {
    this.peerType = peerType;
  }

  //------------------------------------------------------------------------------------------------
  // Operations
  //------------------------------------------------------------------------------------------------

  /**
   * @return the peer type
   */
  public PeerType getPeerType() {
    return peerType;
  }

  @Override
  public MsgType getRobocolMsgType() {
    return RobocolParsable.MsgType.PEER_DISCOVERY;
  }

  // Historically, PeerDiscovery had the following serialization format:
  //
  //  1 byte    message type
  //  2 bytes   payload size (big endian)
  //  1 byte    ROBOCOL_VERSION (==1)
  //  1 byte    peer type (parsed iff version is 1, but not actually used)
  //  8 bytes   unused payload (ignored on reception)
  //
  // That's 13 bytes total. On reception, a check was made that the incoming packet had at least 13 bytes.
  // We aim to preserve compatibility with this format, even though that means that the header
  // structure of a PeerDiscovery is now different than all other RobocolParsable's.
  //
  // The current serialization format is this:
  //
  //  1 byte    message type
  //  2 bytes   payload size (big endian)
  //  1 byte    ROBOCOL_VERSION
  //  1 byte    peer type
  //  2 bytes   sequence number (big endian)
  //  6 bytes   unused payload (ignored on reception)

  static final int cbBufferHistorical  = 13;
  static final int cbPayloadHistorical = 10;

  @Override
  public byte[] toByteArray() throws RobotCoreException {
    ByteBuffer buffer = allocateWholeWriteBuffer(cbBufferHistorical);
    try {

      buffer.put(getRobocolMsgType().asByte());
      buffer.putShort((short) cbPayloadHistorical);
      buffer.put(RobocolConfig.ROBOCOL_VERSION);
      buffer.put(peerType.asByte());
      buffer.putShort((short)this.sequenceNumber);

    } catch (BufferOverflowException e) {
      RobotLog.logStacktrace(e);
    }
    return buffer.array();
  }

  @Override
  public void fromByteArray(byte[] byteArray) throws RobotCoreException, RobotProtocolException {
    if (byteArray.length < cbBufferHistorical) {
      throw new RobotCoreException("Expected buffer of at least %d bytes, received %d", cbBufferHistorical, byteArray.length);
    }

    ByteBuffer byteBuffer = getWholeReadBuffer(byteArray);

    byte  peerMessageType    = byteBuffer.get();
    short peerCbPayload      = byteBuffer.getShort();
    byte  peerRobocolVersion = byteBuffer.get();
    byte  peerType           = byteBuffer.get();
    short peerSeqNum         = byteBuffer.getShort();

    // We insist on both ends having the same understanding of the protocol. Something fancier
    // we could do in the future is the usual major.minor version management, but that doesn't
    // seem worthwhile yet
    if (peerRobocolVersion != RobocolConfig.ROBOCOL_VERSION) {
        RobotLog.ee(TAG, "Incompatible robocol versions, remote: %d, local: %d", peerRobocolVersion, RobocolConfig.ROBOCOL_VERSION);
        throw new RobotProtocolException(AppUtil.getDefContext().getString(R.string.incompatibleAppsError),
                AppUtil.getInstance().getAppName(), RobocolConfig.ROBOCOL_VERSION,
                AppUtil.getInstance().getRemoteAppName(), peerRobocolVersion);
    }

    // ALL robocol versions have the peer type
    this.peerType = PeerType.fromByte(peerType);

    // All but the first have the sequence number
    if (peerRobocolVersion > 1) {
      this.setSequenceNumber(peerSeqNum);
    }
  }

  @Override
  public String toString() {
    return String.format("Peer Discovery - peer type: %s", peerType.name());
  }
}
