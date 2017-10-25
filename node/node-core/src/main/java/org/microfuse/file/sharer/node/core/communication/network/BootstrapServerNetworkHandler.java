package org.microfuse.file.sharer.node.core.communication.network;

import com.google.common.io.Closeables;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * A UDP Socket based network handler used for communicating with the bootstrap server
 * <p>
 * Uses UDP sockets to communicate with the server. Waits for a reply with a timeout after sending a message.
 */
public class BootstrapServerNetworkHandler extends NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapServerNetworkHandler.class);

    private Thread networkHandlerSendTimeoutThread;

    public BootstrapServerNetworkHandler(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    @Override
    public String getName() {
        return "Bootstrap Server Network Handler";
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

            // Starting a timeout to mark as failed
            DatagramSocket finalSocket = socket;
            networkHandlerSendTimeoutThread = new Thread(() -> {
                try {
                    Thread.sleep(serviceHolder.getConfiguration().getBootstrapServerReplyWaitTimeout());
                } catch (InterruptedException ignored) {
                }
                if (networkHandlerSendTimeoutThread != null) {
                    try {
                        Closeables.close(finalSocket, true);
                    } catch (IOException ignored) {
                    }
                    networkHandlerSendTimeoutThread = null;
                }
            });
            networkHandlerSendTimeoutThread.start();

            logger.debug("Waiting for reply from node " + ip + ":" + port);
            byte[] buffer = new byte[65536];
            DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(incomingPacket);

            byte[] data = incomingPacket.getData();
            String replyMessageString =
                    new String(data, 0, incomingPacket.getLength(), Constants.DEFAULT_CHARSET);

            logger.debug("Reply to message " + messageString + " received from node " + ip + ":" + port);
            runTasksOnMessageReceived(ip, port, Message.parse(replyMessageString));
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

    @Override
    public void startListening() {
        logger.debug("Ignoring request to start listening since the bootstrap server does not initiate communication");
    }

    @Override
    public void restart() {
        super.restart();
        logger.debug("Ignoring request to restart listening since no ports will be used by this network handler");
    }

    @Override
    public void shutdown() {
        logger.debug("Cancelling waits for replies");
        if (networkHandlerSendTimeoutThread != null) {
            Thread thread = networkHandlerSendTimeoutThread;
            networkHandlerSendTimeoutThread = null;
            thread.start();
        }
    }
}
