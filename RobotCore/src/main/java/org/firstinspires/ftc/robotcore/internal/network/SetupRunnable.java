package org.firstinspires.ftc.robotcore.internal.network;

import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.PeerDiscoveryManager;
import com.qualcomm.robotcore.robocol.RobocolDatagramSocket;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;
import com.qualcomm.robotcore.wifi.NetworkConnection;

import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class SetupRunnable implements Runnable
    {
    public static final String TAG = "SetupRunnable";

    protected RobocolDatagramSocket             socket;
    protected NetworkConnection                 networkConnection;
    protected PeerDiscoveryManager              peerDiscoveryManager;
    protected ExecutorService                   recvLoopService;
    protected volatile RecvLoopRunnable         recvLoopRunnable;
    protected RecvLoopRunnable.RecvLoopCallback recvLoopCallback;
    protected ElapsedTime                       lastRecvPacket;
    protected CountDownLatch                    countDownLatch;
    protected SocketConnect socketConnect;

    public SetupRunnable(
            RecvLoopRunnable.RecvLoopCallback recvLoopCallback,
            NetworkConnection networkConnection,
            ElapsedTime lastRecvPacket,
            SocketConnect socketConnect
            ) {
        this.recvLoopCallback   = recvLoopCallback;
        this.networkConnection  = networkConnection;
        this.lastRecvPacket     = lastRecvPacket;
        this.countDownLatch     = new CountDownLatch(1);
        this.socketConnect      = socketConnect;
    }

    @Override
    public void run() {
        ThreadPool.logThreadLifeCycle("SetupRunnable.run()", new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket != null) socket.close();
                    socket = new RobocolDatagramSocket();
                    socket.listenUsingDestination(networkConnection.getConnectionOwnerAddress());
                    /*
                     * The driver station knows who it's peer is, so we go ahead and 'connect' this UDP
                     * socket to that peer.   The robot controller will wait for the peer discovery packet
                     * as it's can't statically determine the ip address of the driver station.
                     */
                    if (socketConnect==SocketConnect.CONNECTION_OWNER) {
                        socket.connect(networkConnection.getConnectionOwnerAddress());
                    }
                } catch (SocketException e) {
                    RobotLog.e("Failed to open socket: " + e.toString());
                }

                // start the new event loops
                recvLoopService = ThreadPool.newFixedThreadPool(2, "ReceiveLoopService");
                recvLoopRunnable = new RecvLoopRunnable(recvLoopCallback, socket, lastRecvPacket);
                RecvLoopRunnable.CommandProcessor commandProcessor = recvLoopRunnable.new CommandProcessor();
                NetworkConnectionHandler.getInstance().setRecvLoopRunnable(recvLoopRunnable);
                recvLoopService.execute(commandProcessor);
                recvLoopService.execute(recvLoopRunnable);

                // start peer discovery service. do this after we set up listener so as not to miss anything
                if (peerDiscoveryManager != null) {
                    peerDiscoveryManager.stop();
                }
                peerDiscoveryManager = new PeerDiscoveryManager(socket, networkConnection.getConnectionOwnerAddress());

                // allow shutdown to proceed
                countDownLatch.countDown();

                // send loop will be started after peer discovery
                RobotLog.v("Setup complete");
            }
        });
    }

    public RobocolDatagramSocket getSocket() {
        return socket;
    }
    
    public void injectReceivedCommand(Command cmd) {
        RecvLoopRunnable recvLoopRunnable = this.recvLoopRunnable;
        if (recvLoopRunnable != null) {
            recvLoopRunnable.injectReceivedCommand(cmd);
        } else {
            RobotLog.vv(TAG, "injectReceivedCommand(): recvLoopRunnable==null; command ignored");
        }
    }

    public void stopPeerDiscovery() {
        if (peerDiscoveryManager != null) {
            peerDiscoveryManager.stop();
            peerDiscoveryManager = null;
        }
    }

    protected void closeSocket() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    public void shutdown() {
        RobotLog.ii(TAG, "Shutting down setup and receive loop");
        try {
            // wait for startup to get to a safe point where we can shut it down
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (recvLoopService != null) {
            recvLoopService.shutdownNow();
            ThreadPool.awaitTerminationOrExitApplication(recvLoopService, 5, TimeUnit.SECONDS, "ReceiveLoopService", "internal error");
            recvLoopService = null;
            recvLoopRunnable = null;
        }

        stopPeerDiscovery();
        closeSocket();
    }

    public long getRxDataCount() {
        return socket.getRxDataCount();
    }

    public long getTxDataCount() {
        return socket.getTxDataCount();
    }

    public long getBytesPerSecond() {
        if (recvLoopRunnable != null) {
            return recvLoopRunnable.getBytesPerSecond();
        } else {
            return 0;
        }
    }
}
