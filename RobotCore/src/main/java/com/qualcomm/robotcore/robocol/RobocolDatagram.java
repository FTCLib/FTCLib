package com.qualcomm.robotcore.robocol;

import com.qualcomm.robotcore.exception.RobotCoreException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * RobocolDatagram
 *
 * Used by RobocolServer and RobocolClient to pass messages.
 */
public class RobocolDatagram {

   //-----------------------------------------------------------------------------------------------
   // State
   //-----------------------------------------------------------------------------------------------

   public static final String TAG = "Robocol";

   /** the system-level packet over which we are a wrapper */
   private DatagramPacket packet;

   /** If non-null, then this buffer should be salvaged on finalization */
   private byte[]         receiveBuffer = null;

   /** the place we put old receive buffers */
   static Queue<byte[]>   receiveBuffers = new ConcurrentLinkedQueue<byte[]>();

   //-----------------------------------------------------------------------------------------------
   // Construction
   //-----------------------------------------------------------------------------------------------

   /**
    * Construct a RobocolDatagram from a RobocolParsable
    * @param message
    */
   public RobocolDatagram(RobocolParsable message) throws RobotCoreException {
      setData(message.toByteArrayForTransmission());
   }

   /**
    * Construct a RobocolDatagram from a byte array
    * @param message
    */
   public RobocolDatagram(byte[] message) {
      setData(message);
   }

   /**
    * Returns a RobocolDatagram suitable for use in socket receives. We pay particular attention
    * here to avoiding allocating too many buffers, fearing an impact on the GC, as the buffers
    * are relatively large-ish.
    *
    * @return a new datagram suitable for socket receiving
    */
   public static RobocolDatagram forReceive(int receiveBufferSize) {
      byte[] buffer = receiveBuffers.poll();
      if (buffer == null || buffer.length != receiveBufferSize) {
         buffer = new byte[receiveBufferSize];
      }
      //
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      //
      RobocolDatagram result = new RobocolDatagram();
      result.packet        = packet;
      result.receiveBuffer = buffer;
      return result;
   }

   protected RobocolDatagram() {
      this.packet = null;
   }

   //-----------------------------------------------------------------------------------------------
   // Teardown
   //-----------------------------------------------------------------------------------------------

   /** Clients are done with this message. If it has a socket receive buffer, then scavenge that. */
   public void close() {
      if (this.receiveBuffer != null) {
         receiveBuffers.add(this.receiveBuffer);
         this.receiveBuffer = null;
      }
      this.packet = null;
   }

   //-----------------------------------------------------------------------------------------------
   // Operations
   //-----------------------------------------------------------------------------------------------

   /**
    * Get the message type
    * @return message type
    */
   public RobocolParsable.MsgType getMsgType() {
      return RobocolParsable.MsgType.fromByte(packet.getData()[0]);
   }

   /**
    * Get the size of this RobocolDatagram, in bytes
    * @return size of this RobocolDatagram, in bytes
    */
   public int getLength() {
      return packet.getLength();
   }

   /**
    * Get the size of the payload, in bytes
    * @return size of payload, in bytes
    */
   public int getPayloadLength() {
      return packet.getLength() - RobocolParsable.HEADER_LENGTH;
   }

   /**
    * Gets the payload of this datagram packet
    * @return byte[] data
    */
   public byte[] getData() {
      return packet.getData();
   }

   public void setData(byte[] data) {
      packet = new DatagramPacket(data, data.length);
   }

   public InetAddress getAddress() {
      return packet.getAddress();
   }
   public int getPort() {
      return packet.getPort();
   }

   public void setAddress(InetAddress address) {
      packet.setAddress(address);
   }

   public String toString() {
      int size = 0;
      String type = "NONE";
      String addr = null;

      if (packet != null && packet.getAddress() != null && packet.getLength() > 0) {
         type = RobocolParsable.MsgType.fromByte(packet.getData()[0]).name();
         size = packet.getLength();
         addr = packet.getAddress().getHostAddress();
      }

      return String.format("RobocolDatagram - type:%s, addr:%s, size:%d", type, addr, size);
   }

   protected DatagramPacket getPacket() {
      return packet;
   }

   protected void setPacket(DatagramPacket packet) {
      this.packet = packet;
   }
}
