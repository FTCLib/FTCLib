package org.firstinspires.ftc.robotcore.internal.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.RobocolDatagram;
import com.qualcomm.robotcore.robocol.RobocolDatagramSocket;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.concurrent.LinkedBlockingDeque;

@SuppressWarnings("WeakerAccess")
public class RecvLoopRunnable implements Runnable {

    public static final String TAG = RobocolDatagram.TAG;
    public static       boolean DEBUG = false;

    /*
     * To turn on traffic stats on the inspection activities, set this
     * and InspectionActivity.SHOW_TRAFFIC_STATS to true.
     */
    private static       boolean DO_TRAFFIC_DATA = false;

    private static ElapsedTime bandwidthSampleTimer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
    private final static int BANDWIDTH_SAMPLE_PERIOD = 500;
    private double bytesPerMilli = 0.0;

    public interface RecvLoopCallback {
        CallbackResult packetReceived(RobocolDatagram packet) throws RobotCoreException;

        CallbackResult peerDiscoveryEvent(RobocolDatagram packet) throws RobotCoreException;
        CallbackResult heartbeatEvent(RobocolDatagram packet, long tReceived) throws RobotCoreException;
        CallbackResult commandEvent(Command command) throws RobotCoreException;
        CallbackResult telemetryEvent(RobocolDatagram packet) throws RobotCoreException;
        CallbackResult gamepadEvent(RobocolDatagram packet) throws RobotCoreException;
        CallbackResult emptyEvent(RobocolDatagram packet) throws RobotCoreException;
        CallbackResult reportGlobalError(String error, boolean recoverable);
    }

    /** A degenerate implementation so that individual callbacks need not themselves implement a bunch of trivial methods */
    public static class DegenerateCallback implements RecvLoopCallback {
        @Override public CallbackResult packetReceived(RobocolDatagram packet) throws RobotCoreException {
            return CallbackResult.NOT_HANDLED;
        }
        @Override public CallbackResult peerDiscoveryEvent(RobocolDatagram packet) throws RobotCoreException {
            return CallbackResult.NOT_HANDLED;
        }
        @Override public CallbackResult heartbeatEvent(RobocolDatagram packet, long tReceived) throws RobotCoreException {
            return CallbackResult.NOT_HANDLED;
        }
        @Override public CallbackResult commandEvent(Command command) throws RobotCoreException {
            return CallbackResult.NOT_HANDLED;
        }
        @Override public CallbackResult telemetryEvent(RobocolDatagram packet) throws RobotCoreException {
            return CallbackResult.NOT_HANDLED;
        }
        @Override public CallbackResult gamepadEvent(RobocolDatagram packet) throws RobotCoreException {
            return CallbackResult.NOT_HANDLED;
        }
        @Override public CallbackResult emptyEvent(RobocolDatagram packet) throws RobotCoreException {
            return CallbackResult.NOT_HANDLED;
        }
        @Override public CallbackResult reportGlobalError(String error, boolean recoverable) {
            return CallbackResult.NOT_HANDLED;
        }
    }

    protected ElapsedTime lastRecvPacket;
    protected ElapsedTime packetProcessingTimer;
    protected ElapsedTime commandProcessingTimer;
    protected double sProcessingTimerReportingThreshold;
    protected RobocolDatagramSocket socket;
    protected RecvLoopCallback callback;
    protected LinkedBlockingDeque<Command> commandsToProcess = new LinkedBlockingDeque<Command>();

    public RecvLoopRunnable(RecvLoopCallback callback, @NonNull RobocolDatagramSocket socket, @Nullable ElapsedTime lastRecvPacket ) {
        this.callback = callback;
        this.socket = socket;
        this.lastRecvPacket = lastRecvPacket;
        this.packetProcessingTimer = new ElapsedTime();
        this.commandProcessingTimer = new ElapsedTime();
        this.sProcessingTimerReportingThreshold = 0.5;
        this.socket.gatherTrafficData(DO_TRAFFIC_DATA);
        RobotLog.vv(TAG, "RecvLoopRunnable created");
    }

    public void setCallback(RecvLoopCallback callback) {
        this.callback = callback;
    }

    public class CommandProcessor implements Runnable {
      @Override public void run() {
        while (!Thread.currentThread().isInterrupted()) {
          try {
            // Wait for a command to appear, then process it
            Command command = commandsToProcess.takeFirst();
            commandProcessingTimer.reset();
            //
            if (DEBUG) RobotLog.vv(TAG, "command=%s...", command.getName());
            callback.commandEvent(command);
            if (DEBUG) RobotLog.vv(TAG, "...command=%s", command.getName());
            //
            double seconds = commandProcessingTimer.seconds();
            if (seconds > sProcessingTimerReportingThreshold) {
                RobotLog.ee(TAG, "command processing took %.3f s: command=%s", seconds, command.getName());
            }
          } catch (InterruptedException e) {
            // Just get out of here
            return;
          } catch (RobotCoreException|RuntimeException e) {
            // Report the error, but stay alive
            RobotLog.ee(TAG, e, "exception in %s", Thread.currentThread().getName());
            callback.reportGlobalError(e.getMessage(), false);
          }
        }
      }
    }

    public void injectReceivedCommand(Command cmd) {
        commandsToProcess.addLast(cmd);
    }

    public long getBytesPerSecond() {
        return (long)(bytesPerMilli * 1000);
    }

    protected void calculateBytesPerMilli() {
        if (bandwidthSampleTimer.time() >= BANDWIDTH_SAMPLE_PERIOD) {
            bytesPerMilli = (socket.getRxDataSample() + socket.getTxDataSample()) / bandwidthSampleTimer.time();
            bandwidthSampleTimer.reset();
            socket.resetDataSample();
        }
    }

    @Override
    public void run() {
        ThreadPool.logThreadLifeCycle("RecvLoopRunnable.run()", new Runnable() {
            @Override
            public void run() {

                bandwidthSampleTimer.reset();
                AppUtil appUtil = AppUtil.getInstance();
                while (!Thread.currentThread().isInterrupted()) {

                    // Block until a packet is received, a timeout or other error occurs, or the socket is closed.
                    // In the second and third cases, null is returned.
                    RobocolDatagram packet = socket.recv();
                    long tReceived = appUtil.getWallClockTime();

                    // We might have waited for a while in the recv(), and been interrupted in the meantime
                    if (Thread.currentThread().isInterrupted()) {
                      return;
                    }

                    if (packet == null) {
                        if (socket.isClosed()) {
                            RobotLog.vv(TAG, "socket closed; %s returning", Thread.currentThread().getName());
                            return;
                            }
                        Thread.yield();
                        continue;
                    }
                    if (lastRecvPacket != null) lastRecvPacket.reset();

                    try {
                        packetProcessingTimer.reset();
                        if (callback.packetReceived(packet)!=CallbackResult.HANDLED) {

                            switch (packet.getMsgType()) {

                                case PEER_DISCOVERY:
                                    callback.peerDiscoveryEvent(packet);
                                    break;
                                case HEARTBEAT:
                                    callback.heartbeatEvent(packet, tReceived);
                                    break;
                                case COMMAND:
                                    // Handle acks here so they get back to sender quickly, then queue for
                                    // internal processing. The queue allows command processing to take a
                                    // long time w/o adversely affecting network responsiveness, which could
                                    // otherwise lead to apparent disconnects.
                                    Command command = new Command(packet);
                                    CallbackResult result = NetworkConnectionHandler.getInstance().processAcknowledgments(command);
                                    if (!result.isHandled()) {
                                      RobotLog.vv(RobocolDatagram.TAG, "received command: %s(%d) %s", command.getName(), command.getSequenceNumber(), command.getExtra());
                                      commandsToProcess.addLast(command);
                                    }
                                    break;
                                case TELEMETRY:
                                    callback.telemetryEvent(packet);
                                    break;
                                case GAMEPAD:
                                    callback.gamepadEvent(packet);
                                    break;
                                case EMPTY:
                                    callback.emptyEvent(packet);
                                    break;
                                case KEEPALIVE:
                                    /*
                                     * Intentionally swallow.
                                     */
                                    break;
                                default:
                                    RobotLog.ee(TAG, "Unhandled message type: " + packet.getMsgType().name());
                                    break;
                            }
                        }
                        double seconds = packetProcessingTimer.seconds();
                        if (seconds > sProcessingTimerReportingThreshold) {
                            RobotLog.vv(TAG, "packet processing took %.3f s: type=%s", seconds, packet.getMsgType().toString());
                        }
                    } catch (RobotCoreException|RuntimeException e) {
                        RobotLog.ee(TAG, e, "exception in %s", Thread.currentThread().getName());
                        callback.reportGlobalError(e.getMessage(), false);
                    } finally {
                        // proactively reclaim the receive buffer of the message (don't wait for GC)
                        packet.close();
                    }

                    if (DO_TRAFFIC_DATA) calculateBytesPerMilli();
                }
            RobotLog.vv(TAG, "interrupted; %s returning", Thread.currentThread().getName());
            }
        });
    }
}
