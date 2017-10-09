package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.utils.Constants;
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

    @Override
    public String getName() {
        return NetworkHandlerType.TCP_SOCKET.getValue();
    }

    @Override
    public void startListening() {
        while (true) {
            int portNumber = ServiceHolder.getConfiguration().getPeerListeningPort();
            DatagramSocket udpSocket;
            String messageString;

            try {
                udpSocket = new DatagramSocket();
                logger.info("Started listening at " + portNumber + ".");

                byte[] buffer = new byte[65536];
                DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(incomingPacket);

                byte[] data = incomingPacket.getData();
                messageString = new String(data, 0, incomingPacket.getLength(), Constants.DEFAULT_CHARSET);
                logger.debug("Message received from " + incomingPacket.getAddress().getHostAddress()
                        + ":" + incomingPacket.getPort() + " : " + messageString);

                onMessageReceived(
                        incomingPacket.getAddress().getHostAddress(),
                        incomingPacket.getPort(),
                        Message.parse(messageString)
                );
            } catch (IOException e) {
                logger.error("Listener error occurred. Restarting listening." + e);
            }
        }
    }

    @Override
    public void sendMessage(String ip, int port, Message message) {
        try {
            String messageString = message.toString();
            DatagramSocket udpSocket = new DatagramSocket();

            try {
                DatagramPacket datagramPacket = new DatagramPacket(
                        messageString.getBytes(Constants.DEFAULT_CHARSET),
                        messageString.getBytes(Constants.DEFAULT_CHARSET).length,
                        InetAddress.getByAddress(ip.getBytes(Constants.DEFAULT_CHARSET)), port);
                udpSocket.send(datagramPacket);
            } catch (IOException e) {
                logger.debug("Message sent to " + ip + ":" + port + " : " + message, e);
            }
        } catch (SocketException e) {
            logger.error("Failed to send message to " + ip + ":" + port, e);
            onMessageSendFailed(ip, port, message);
        }
    }
}
