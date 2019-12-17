package com.qualcomm.robotcore.robocol;

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.util.RobotLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Multi-threaded datagram socket with non-blocking IO.
 */
public class RobocolDatagramSocket {

  public static final String TAG = RobocolDatagram.TAG;

  private static final boolean DEBUG = false;
  private static final boolean VERBOSE_DEBUG = false;

  public enum State {
    LISTENING,  /// Socket is ready
    CLOSED,     /// Socket is not ready
    ERROR       /// Socket is in error state
  }

  private       DatagramSocket  socket;
  private       int             receiveBufferSize;
  private       int             sendBufferSize;
  private       int             msReceiveTimeout;
  volatile private State        state;
  private final Object          recvLock = new Object(); // only one recv() at a time
  private final Object          sendLock = new Object(); // only one send() at a time
  private final Object          bindCloseLock = new Object(); // serializes bind() vs close()
  private       boolean         sendErrorReported = false;
  private       boolean         recvErrorReported = false;
  private       long            rxDataTotal = 0;
  private       long            txDataTotal = 0;
  private       long            rxDataSample = 0;
  private       long            txDataSample = 0;
  private       boolean         trafficDataCollection = false;

  public RobocolDatagramSocket() {
    state = State.CLOSED;
  }

  public void listenUsingDestination(InetAddress destAddress) throws SocketException {
    bind(new InetSocketAddress(RobocolConfig.determineBindAddress(destAddress), RobocolConfig.PORT_NUMBER));
  }

  @SuppressLint("DefaultLocale") public void bind(InetSocketAddress bindAddress) throws SocketException {
    synchronized (this.bindCloseLock) {
      if (state != State.CLOSED) {
        close();
      }
      state = State.LISTENING;

      // start up the socket
      socket = new DatagramSocket(bindAddress);
      sendErrorReported = false;
      recvErrorReported = false;

      // use a non-infinite timeout to cycle back to RecvLoopRunnable reasonably often
      socket.setSoTimeout(RobocolConfig.MS_RECEIVE_TIMEOUT);

      // limit the receive byte[]'s we use in to avoid pointless memory usage
      receiveBufferSize = Math.min(RobocolConfig.MAX_MAX_PACKET_SIZE, socket.getReceiveBufferSize());
      sendBufferSize    = socket.getSendBufferSize();
      msReceiveTimeout  = socket.getSoTimeout();

      RobotLog.dd(TAG, String.format("RobocolDatagramSocket listening addr=%s cbRec=%d cbSend=%d msRecTO=%d", bindAddress.toString(), receiveBufferSize, sendBufferSize, msReceiveTimeout));
    }
  }

  public void connect(InetAddress connectAddress) throws SocketException {
    InetSocketAddress addr = new InetSocketAddress(connectAddress, RobocolConfig.PORT_NUMBER);
    RobotLog.dd(TAG, "RobocolDatagramSocket connected to " + addr.toString());
    socket.connect(addr);
  }

  public void close() {
    synchronized (this.bindCloseLock) {
      state = State.CLOSED;

      if (socket != null) socket.close();

      RobotLog.dd(TAG, "RobocolDatagramSocket is closed");
    }
  }

  @SuppressLint("DefaultLocale") public void send(RobocolDatagram message) {
    // We're not certain whether socket.send() is thread-safe or not, so we are conservative. And,
    // besides, this makes sure that any logging isn't interlaced.
    synchronized (this.sendLock) {
      try {
        if (message.getLength() > sendBufferSize) {
          throw new RuntimeException(String.format("send packet too large: size=%d max=%d", message.getLength(), sendBufferSize));
        }
        if (VERBOSE_DEBUG) RobotLog.vv(TAG, "calling socket.send()");
        socket.send(message.getPacket());
        if (DEBUG) RobotLog.vv(TAG, String.format("sent packet to=%s len=%d", message.getPacket().getAddress().toString(), message.getPayloadLength()));
        if (trafficDataCollection) txDataSample += message.getPayloadLength();

      } catch (RuntimeException e) {
        RobotLog.logExceptionHeader(TAG, e, "exception sending datagram");

      } catch (IOException e) {
        if (!sendErrorReported) {
          sendErrorReported = !DEBUG;
          RobotLog.logExceptionHeader(TAG, e, "exception sending datagram");
        }
      }
    }
  }

  /**
   * Receive a RobocolDatagram packet
   * @return packet; or null if error
   */
  @SuppressLint("DefaultLocale") public @Nullable RobocolDatagram recv() {
    // This locking may ultimately now be unnecessary. But it's harmless, so we'll keep it for now
    synchronized (this.recvLock) {
      RobocolDatagram result = RobocolDatagram.forReceive(receiveBufferSize);
      DatagramPacket packetRecv = result.getPacket();

      try {
        // We have seen rare situations where recv() is called before the socket is bound.
        // Thus guards against same.
        if (socket == null) return null;

        // Block until a packet is received or a timeout occurs
        if (VERBOSE_DEBUG) RobotLog.vv(TAG, "calling socket.receive()");
        socket.receive(packetRecv);
        if (DEBUG) RobotLog.vv(TAG, String.format("received packet from=%s len=%d", packetRecv.getAddress().toString(), result.getPayloadLength()));
        if (trafficDataCollection) rxDataSample += result.getPayloadLength();

      } catch (SocketException|SocketTimeoutException e) {
        if (!recvErrorReported) {
          recvErrorReported = !DEBUG;
          RobotLog.logExceptionHeader(TAG, e, "no packet received");
        }
        return null;

      } catch (IOException|RuntimeException e) {
        RobotLog.logExceptionHeader(TAG, e, "no packet received");
        return null;
      }

      return result;
    }
  }

  public void gatherTrafficData(boolean enable) {
    trafficDataCollection = enable;
  }

  public long getRxDataSample() {
    return rxDataSample;
  }

  public long getTxDataSample() {
    return txDataSample;
  }

  public long getRxDataCount() {
    return rxDataTotal;
  }

  public long getTxDataCount() {
    return txDataTotal;
  }

  public void resetDataSample() {
    rxDataTotal += rxDataSample;
    txDataTotal += txDataSample;
    rxDataSample = 0;
    txDataSample = 0;
  }

  public State getState() {
    return state;
  }

  public InetAddress getInetAddress() {
    if (socket == null) return null;

    return socket.getInetAddress();
  }

  public InetAddress getLocalAddress() {
    if (socket == null) return null;

    return socket.getLocalAddress();
  }

  public boolean isRunning() {
    return (state == State.LISTENING);
  }

  public boolean isClosed() {
    return (state == State.CLOSED);
  }
}
