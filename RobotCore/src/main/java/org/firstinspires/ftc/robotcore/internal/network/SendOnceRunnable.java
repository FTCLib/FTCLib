package org.firstinspires.ftc.robotcore.internal.network;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.Heartbeat;
import com.qualcomm.robotcore.robocol.KeepAlive;
import com.qualcomm.robotcore.robocol.RobocolDatagram;
import com.qualcomm.robotcore.robocol.RobocolDatagramSocket;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.ui.RobotCoreGamepadManager;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles data transmission to the remote device.
 *
 * Creating a new instance implies that a network connection has already been established.
 */
@SuppressWarnings("WeakerAccess")
public class SendOnceRunnable implements Runnable {

    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    public interface DisconnectionCallback {
        /**
         * Will be called periodically while the peer is disconnected
         */
        void disconnected();
    }

    public static class Parameters {
        public boolean                          disconnectOnTimeout = true;
        public boolean                          originateHeartbeats = AppUtil.getInstance().isDriverStation();
        public boolean                          originateKeepAlives = false;
        public volatile RobotCoreGamepadManager gamepadManager      = null;

        public Parameters() { }
    }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String          TAG = RobocolDatagram.TAG;
    public static       boolean         DEBUG = false;

    public static final double          ASSUME_DISCONNECT_TIMER = 2.0; // in seconds
    public static final int             MAX_COMMAND_ATTEMPTS = 10;
    public static final long            GAMEPAD_UPDATE_THRESHOLD = 1000; // in milliseconds
    public static final int             MS_HEARTBEAT_TRANSMISSION_INTERVAL = 100;
    public static final int             MS_KEEPALIVE_TRANSMISSION_INTERVAL = 20;


    @NonNull protected ElapsedTime                      lastRecvPacket;
    @NonNull protected volatile List<Command>           pendingCommands = new CopyOnWriteArrayList<Command>();
    @NonNull protected Heartbeat                        heartbeatSend = new Heartbeat();
    @NonNull protected KeepAlive                        keepAliveSend = new KeepAlive();
    @NonNull protected volatile RobocolDatagramSocket   socket;
    @NonNull protected DisconnectionCallback            disconnectionCallback;
    @NonNull protected final Parameters                 parameters;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public SendOnceRunnable(@NonNull  DisconnectionCallback disconnectionCallback,
                            @NonNull  ElapsedTime lastRecvPacket) {
        this.disconnectionCallback  = disconnectionCallback;
        this.lastRecvPacket         = lastRecvPacket;
        this.parameters             = new Parameters();

        RobotLog.vv(TAG, "SendOnceRunnable created");
    }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public void updateSocket(RobocolDatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        AppUtil appUtil = AppUtil.getInstance();
        boolean sentPacket;
        try {
            // skip if we haven't received a packet in a while. The RC is the center
            // of the world and never disconnects from anyone.
            double seconds = lastRecvPacket.seconds();
            if (parameters.disconnectOnTimeout && seconds > ASSUME_DISCONNECT_TIMER) {
                disconnectionCallback.disconnected();
                return;
            }

            /*
             * If we are on the DriverStation then send heartbeats at a specific rate.  Heartbeats
             * originate on the DriverStation and are echoed by the RobotController.
             *
             * If we have fresh GamePad data from the DriverStation, then send it.
             *
             * If, through this invocation of SendOnceRunnable we sent neither GamePad data, nor
             * a Heartbeat, then send a KeepAlive if configured to do so.  This ensures a minimum
             * packet rate for devices for which this is necessary to prevent disconnects.
             */
            sentPacket = false;
            if (parameters.originateHeartbeats && heartbeatSend.getElapsedSeconds() > 0.001 * MS_HEARTBEAT_TRANSMISSION_INTERVAL) {
                // generate a new heartbeat packet and send it
                heartbeatSend = Heartbeat.createWithTimeStamp();
                // Add the timezone in there too!
                heartbeatSend.setTimeZoneId(TimeZone.getDefault().getID());
                // keep the next three lines as close together in time as possible in order to improve the quality of time synchronization
                heartbeatSend.t0 = appUtil.getWallClockTime();
                RobocolDatagram packetHeartbeat = new RobocolDatagram(heartbeatSend);
                send(packetHeartbeat);
                sentPacket = true;
                // Do any logging after the transmission so as to minimize disruption of timing calculation
            }

            if (parameters.gamepadManager != null) {
                long now = SystemClock.uptimeMillis();

                for (Gamepad gamepad : parameters.gamepadManager.getGamepadsForTransmission()) {

                    // don't send stale gamepads
                    if (now - gamepad.timestamp > GAMEPAD_UPDATE_THRESHOLD && gamepad.atRest())
                        continue;

                    gamepad.setSequenceNumber();
                    RobocolDatagram packetGamepad = new RobocolDatagram(gamepad);
                    send(packetGamepad);
                    sentPacket = true;
                }
            }

            if ((!sentPacket) && (parameters.originateKeepAlives) && (keepAliveSend.getElapsedSeconds() > 0.001 * MS_KEEPALIVE_TRANSMISSION_INTERVAL)) {
                keepAliveSend = KeepAlive.createWithTimeStamp();
                RobocolDatagram packetKeepAlive = new RobocolDatagram(keepAliveSend);
                send(packetKeepAlive);
            }


            long nanotimeNow = System.nanoTime();

            // send commands
            List<Command> commandsToRemove = new ArrayList<Command>();
            for (Command command : pendingCommands) {

                // if this command has exceeded max attempts or is no longer worth transmitting, give up
                if (command.getAttempts() > MAX_COMMAND_ATTEMPTS || command.hasExpired()) {
                    String msg = String.format(AppUtil.getDefContext().getString(R.string.configGivingUpOnCommand), command.getName(), command.getSequenceNumber(), command.getAttempts());
                    RobotLog.vv(TAG, msg);
                    commandsToRemove.add(command);
                    continue;
                }

                // Commands that we originate we only send out every once in a while so as to give ack's a chance to get back to us
                if (command.isAcknowledged() || command.shouldTransmit(nanotimeNow)) {
                    // log commands we initiated, ack the ones we didn't
                    if (!command.isAcknowledged()) {
                        RobotLog.vv(TAG, "sending %s(%d), attempt: %d", command.getName(), command.getSequenceNumber(), command.getAttempts());
                    } else if (DEBUG) {
                        RobotLog.vv(TAG, "acking %s(%d)", command.getName(), command.getSequenceNumber());
                    }

                    // send the command
                    RobocolDatagram packetCommand = new RobocolDatagram(command);
                    send(packetCommand);

                    // if this is a command we handled, remove it
                    if (command.isAcknowledged()) commandsToRemove.add(command);
                }
            }
            pendingCommands.removeAll(commandsToRemove);
        }
        // For robustness and attempted ongoing liveness of the app, we catch
        // *all* types of exception. This will help minimize disruption to the sendLoopService.
        // With (a huge amount of) luck, the next time we're run, things might work better. Though
        // that's unlikely, it seems better than outright killing the app here and now.
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send(RobocolDatagram datagram) {
        if (socket.getInetAddress() != null) {
            socket.send(datagram);
        } else {
            RobotLog.ww(TAG, "Sending a datagram to a null address");
        }
    }

    public void sendCommand(Command cmd) {
        pendingCommands.add(cmd);
    }

    public boolean removeCommand(Command cmd) {
        return pendingCommands.remove(cmd);
    }

    public void clearCommands() {
        pendingCommands.clear();
    }
}
