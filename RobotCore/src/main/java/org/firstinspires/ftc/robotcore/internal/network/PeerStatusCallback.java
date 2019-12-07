package org.firstinspires.ftc.robotcore.internal.network;

public interface PeerStatusCallback {
    /**
     * Notifies that a peer is newly connected (including if the peer just changed or the robot was restarted).
     */
    void onPeerConnected();

    /**
     * Notifies that the peer is newly disconnected.
     */
    void onPeerDisconnected();
}
