package org.firstinspires.ftc.robotcore.internal.webserver.websockets;

import androidx.annotation.NonNull;

import java.net.InetAddress;

public interface FtcWebSocket {
    /**
     * Send a message to this WebSocket
     *
     * @param message The message to send
     * @throws IllegalArgumentException if the message namespace is "system"
     */
    void send(@NonNull FtcWebSocketMessage message);

    /**
     * @return the IP address of the client this WebSocket is connected to
     */
    InetAddress getRemoteIpAddress();

    /**
     * @return the hostname of the client this WebSocket is connected to
     */
    String getRemoteHostname();

    /**
     * @return the server port that this WebSocket is connected to
     */
    int getPort();

    /**
     * @return whether the WebSocket is open for communication
     */
    boolean isOpen();

    /**
     * Close the WebSocket connection
     */
    void close(CloseCode closeCode, String reason);
}
