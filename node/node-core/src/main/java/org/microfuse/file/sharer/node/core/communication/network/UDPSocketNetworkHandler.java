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

/**
 * A UDP Socket based network handler.
 * <p>
 * Uses TCP sockets to communicate with other nodes.
 */
public class UDPSocketNetworkHandler extends NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(UDPSocketNetworkHandler.class);

    private DatagramSocket serverSocket;

    public UDPSocketNetworkHandler(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    @Override
    public String getName() {
        return NetworkHandlerType.TCP_SOCKET.getValue();
    }

    @Override
    public void startListening() {
        if (!running) {
            super.startListening();
            new Thread(() -> {
                while (running) {
                    int portNumber = serviceHolder.getConfiguration().getPeerListeningPort();
                    try {
                        serverSocket = new DatagramSocket(portNumber);
                        logger.debug("Started listening at " + portNumber + ".");
                        while (running && !restartRequired) {
                            byte[] buffer = new byte[65536];
                            DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                            serverSocket.receive(incomingPacket);

                            byte[] data = incomingPacket.getData();
                            String messageString =
                                    new String(data, 0, incomingPacket.getLength(), Constants.DEFAULT_CHARSET);
                            logger.debug("Message received from " + incomingPacket.getAddress().getHostAddress()
                                    + ":" + incomingPacket.getPort() + " : " + messageString);

                            runTasksOnMessageReceived(
                                    incomingPacket.getAddress().getHostAddress(),
                                    incomingPacket.getPort(),
                                    Message.parse(messageString)
                            );
                        }
                    } catch (IOException e) {
                        logger.debug("Listening stopped", e);
                    } finally {
                        closeSocket();
                    }
                }
            }).start();
        } else {
            logger.warn("The UDP network handler is already listening. Ignored request to start again.");
        }
    }

    @Override
    public void restart() {
        if (running) {
            super.restart();
            restartRequired = true;
            try {
                closeSocket();
            } finally {
                restartRequired = false;
            }
        } else {
            logger.warn("The TCP network handler is not listening. Ignored request to restart.");
        }
    }

    @Override
    public void shutdown() {
        logger.debug("Shutting down UDP network handler");
        running = false;
        closeSocket();
    }

    @Override
    public void sendMessage(String ip, int port, Message message) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            String messageString = message.toString();

            DatagramPacket datagramPacket = new DatagramPacket(
                    messageString.getBytes(Constants.DEFAULT_CHARSET),
                    messageString.getBytes(Constants.DEFAULT_CHARSET).length,
                    InetAddress.getByName(ip), port);
            socket.send(datagramPacket);
            logger.debug("Message " + message.toString() + " sent to node " + ip + ":" + port);
        } catch (IOException e) {
            logger.error("Failed to send message " + message.toString() + " to " + ip + ":" + port, e);
            runTasksOnMessageSendFailed(ip, port, message);
        } finally {
            try {
                Closeables.close(socket, true);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Close the UDP socket.
     */
    private void closeSocket() {
        try {
            Closeables.close(serverSocket, true);
        } catch (IOException ignored) {
        }
    }
}
