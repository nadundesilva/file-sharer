package org.microfuse.file.sharer.node.core.communication.network;

import com.google.common.io.Closeables;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * A Socket based network handler.
 * <p>
 * Uses TCP sockets to communicate with other nodes.
 */
public class UDPSocketNetworkHandler extends NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(UDPSocketNetworkHandler.class);

    private DatagramSocket udpSocket;

    @Override
    public String getName() {
        return NetworkHandlerType.TCP_SOCKET.getValue();
    }

    @Override
    public void startListening() {
        super.startListening();
        new Thread(() -> {
            while (running) {
                int portNumber = ServiceHolder.getConfiguration().getPeerListeningPort();
                try {
                    udpSocket = new DatagramSocket(portNumber);
                    logger.debug("Started listening at " + portNumber + ".");
                    while (running && !restartRequired) {
                        byte[] buffer = new byte[65536];
                        DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                        udpSocket.receive(incomingPacket);

                        byte[] data = incomingPacket.getData();
                        String messageString =
                                new String(data, 0, incomingPacket.getLength(), Constants.DEFAULT_CHARSET);
                        logger.debug("Message received from " + incomingPacket.getAddress().getHostAddress()
                                + ":" + incomingPacket.getPort() + " : " + messageString);

                        onMessageReceived(
                                incomingPacket.getAddress().getHostAddress(),
                                incomingPacket.getPort(),
                                Message.parse(messageString)
                        );
                    }
                    logger.debug("Restarted socket");
                } catch (IOException e) {
                    logger.error("Failed to establish socket connection", e);
                } finally {
                    closeSocket();
                }
            }
        }).start();
    }

    @Override
    public void restart() {
        super.restart();
        restartRequired = true;
        closeSocket();
        restartRequired = false;
    }

    @Override
    public void shutdown() {
        running = false;
        closeSocket();
    }

    @Override
    public void sendMessage(String ip, int port, Message message) {
        try {
            String messageString = message.toString();
            DatagramSocket udpSendSocket = new DatagramSocket();

            try {
                DatagramPacket datagramPacket = new DatagramPacket(
                        messageString.getBytes(Constants.DEFAULT_CHARSET),
                        messageString.getBytes(Constants.DEFAULT_CHARSET).length,
                        InetAddress.getByName(ip), port);
                udpSendSocket.send(datagramPacket);
            } catch (IOException e) {
                logger.debug("Message sent to " + ip + ":" + port + " : " + message, e);
            }
        } catch (SocketException e) {
            logger.error("Failed to send message to " + ip + ":" + port, e);
            onMessageSendFailed(ip, port, message);
        }
    }

    /**
     * Close the UDP socket.
     */
    private void closeSocket() {
        try {
            Closeables.close(udpSocket, true);
        } catch (IOException e) {
            logger.warn("Failed to shutdown socket", e);
        }
    }
}
