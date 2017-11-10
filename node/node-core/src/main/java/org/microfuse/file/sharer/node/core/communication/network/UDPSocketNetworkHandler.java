package org.microfuse.file.sharer.node.core.communication.network;

import com.google.common.io.Closeables;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.messaging.UDPMessageType;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.messaging.UDPMessage;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A UDP Socket based network handler.
 * <p>
 * Uses TCP sockets to communicate with other nodes.
 */
public class UDPSocketNetworkHandler extends NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(UDPSocketNetworkHandler.class);

    private DatagramSocket serverSocket;
    private long sequenceNumber;
    private Thread retryThread;

    private Map<Node, Set<UDPMessage>> messagesPendingDataAck;
    private Map<Node, Long> dataSequenceNumbers;
    private Set<Long> missingDataSequenceNumbers;

    public UDPSocketNetworkHandler(ServiceHolder serviceHolder) {
        super(serviceHolder);
        sequenceNumber = 0;
        messagesPendingDataAck = new HashMap<>();
        dataSequenceNumbers = new HashMap<>();
        missingDataSequenceNumbers = new HashSet<>();
    }

    @Override
    public String getName() {
        return NetworkHandlerType.TCP_SOCKET.getValue();
    }

    @Override
    public void startListening() {
        if (!running) {
            super.startListening();
            startRetryThread();
            Thread thread = new Thread(() -> {
                while (running) {
                    int portNumber = serviceHolder.getConfiguration().getPeerListeningPort();
                    try {
                        serverSocket = new DatagramSocket(portNumber);
                        logger.debug("Starting listening at " + portNumber + ".");
                        while (running && !restartRequired) {
                            byte[] buffer = new byte[65536];
                            DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                            serverSocket.receive(incomingPacket);

                            byte[] data = incomingPacket.getData();
                            String messageString =
                                    new String(data, 0, incomingPacket.getLength(), Constants.DEFAULT_CHARSET);
                            logger.info("Message received from " + incomingPacket.getAddress().getHostAddress()
                                    + ":" + incomingPacket.getPort() + " : " + messageString);

                            UDPMessage udpMessage = UDPMessage.parse(messageString);
                            String fromIP = udpMessage.getSourceIP();
                            int fromPort = udpMessage.getSourcePort();

                            synchronized (this) {
                                if (udpMessage.getType() == UDPMessageType.DATA) {
                                    Node fromNode = new Node(fromIP, fromPort);
                                    long lastSequenceNumber = dataSequenceNumbers.computeIfAbsent(fromNode, k -> -1L);
                                    long currentSequenceNumber = udpMessage.getSequenceNumber();

                                    boolean isRelevant = false;
                                    if (currentSequenceNumber > lastSequenceNumber) {
                                        for (long i = lastSequenceNumber + 1; i < currentSequenceNumber; i++) {
                                            missingDataSequenceNumbers.add(i);
                                        }
                                        isRelevant = true;
                                    } else if (missingDataSequenceNumbers.contains(currentSequenceNumber)) {
                                        missingDataSequenceNumbers.remove(currentSequenceNumber);
                                        isRelevant = true;
                                    } else {
                                        logger.info("Dropped repeated message " + udpMessage.toString() + " from node "
                                                + fromIP + ";" + fromPort);
                                    }

                                    UDPMessage ackMessage = new UDPMessage();
                                    ackMessage.setType(UDPMessageType.DATA_ACK);
                                    ackMessage.setSourceIP(serviceHolder.getConfiguration().getIp());
                                    ackMessage.setSourcePort(serviceHolder.getConfiguration().getPeerListeningPort());
                                    ackMessage.setSequenceNumber(udpMessage.getSequenceNumber());

                                    sendMessage(fromIP, fromPort, ackMessage);

                                    if (isRelevant) {
                                        dataSequenceNumbers.put(fromNode, currentSequenceNumber);
                                        runTasksOnMessageReceived(fromIP, fromPort, udpMessage.getMessage());
                                    }
                                } else if (udpMessage.getType() == UDPMessageType.DATA_ACK) {
                                    removeMessagePendingDataAck(
                                            incomingPacket.getAddress().getHostAddress(),
                                            incomingPacket.getPort(), udpMessage);
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.info("Listening stopped", e);
                    } finally {
                        closeSocket();
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
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
        logger.info("Shutting down UDP network handler");
        running = false;
        closeSocket();
        closeRetryThread();
    }

    @Override
    public void sendMessage(String ip, int port, Message message) {
        UDPMessage udpMessage = new UDPMessage();
        udpMessage.setType(UDPMessageType.DATA);
        udpMessage.setSourceIP(serviceHolder.getConfiguration().getIp());
        udpMessage.setSourcePort(serviceHolder.getConfiguration().getPeerListeningPort());
        udpMessage.setSequenceNumber(sequenceNumber++);
        udpMessage.setMessage(message);

        Set<UDPMessage> udpMessages = messagesPendingDataAck.computeIfAbsent(new Node(ip, port), k -> new HashSet<>());
        udpMessages.add(udpMessage);

        sendMessage(ip, port, udpMessage);
    }

    /**
     * Start the thread which handles retrying.
     */
    private void startRetryThread() {
        retryThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(serviceHolder.getConfiguration().getUdpNetworkHandlerRetryInterval());
                } catch (InterruptedException ignored) {
                }
                retryMessages();
            }
            retryThread = null;
        });
        retryThread.setPriority(Thread.NORM_PRIORITY);
        retryThread.setDaemon(true);
        retryThread.start();
    }

    /**
     * Stop the thread which handles retrying.
     */
    private void closeRetryThread() {
        if (retryThread != null) {
            try {
                retryThread.join();
            } catch (InterruptedException e) {
                logger.warn("Failed to wait for the retry thread to stop", e);
            }
            retryThread = null;
        }
    }

    /**
     * Send a UDP message to the specified node.
     *
     * @param ip         The ip address to which the UDP message should be sent
     * @param port       The port to which the UDP message should be sent
     * @param udpMessage The UDP message to be sent
     */
    private void sendMessage(String ip, int port, UDPMessage udpMessage) {
        String messageString = udpMessage.toString();
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();

            DatagramPacket datagramPacket = new DatagramPacket(
                    messageString.getBytes(Constants.DEFAULT_CHARSET),
                    messageString.getBytes(Constants.DEFAULT_CHARSET).length,
                    InetAddress.getByName(ip), port);
            socket.send(datagramPacket);
            logger.info("Message " + messageString + " sent to node " + ip + ":" + port);
        } catch (IOException e) {
            logger.error("Failed to send message " + messageString + " to " + ip + ":" + port, e);
        } finally {
            try {
                Closeables.close(socket, true);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Retry to send the messages not delivered.
     */
    private void retryMessages() {
        if (running) {
            Map<Node, Set<UDPMessage>> messagesToBeRemoved = new HashMap<>();
            messagesPendingDataAck.entrySet().stream().parallel().forEach(entry -> {
                Node node = entry.getKey();
                entry.getValue().stream().parallel().forEach(udpMessage -> {
                    if (udpMessage.getUsedRetriesCount() == -1) {
                        udpMessage.setUsedRetriesCount(0);
                    } else if (udpMessage.getUsedRetriesCount()
                            < serviceHolder.getConfiguration().getUdpNetworkHandlerRetryCount()) {
                        udpMessage.setUsedRetriesCount(udpMessage.getUsedRetriesCount() + 1);
                        sendMessage(node.getIp(), node.getPort(), udpMessage);
                        logger.info("Retrying to send message " + udpMessage.toString());
                    } else {
                        logger.info("Failed to send message " + udpMessage.toString());
                        runTasksOnMessageSendFailed(node.getIp(), node.getPort(), udpMessage.getMessage());

                        Set<UDPMessage> udpMessages = messagesToBeRemoved.computeIfAbsent(node, k -> new HashSet<>());
                        udpMessages.add(udpMessage);
                    }
                });
            });

            messagesToBeRemoved.entrySet().stream().sequential().forEach(entry -> {
                Node node = entry.getKey();
                entry.getValue().stream().sequential().forEach(udpMessage ->
                    removeMessagePendingDataAck(node.getIp(), node.getPort(), udpMessage));
            });
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

    /**
     * Remove messages from the messages set which need to be retried.
     *
     * @param toIP       The IP to which the message needed to be sent
     * @param toPort     The port to which the messages needed to be sent
     * @param udpMessage The message to be sent
     */
    private void removeMessagePendingDataAck(String toIP, int toPort, UDPMessage udpMessage) {
        Node ackNode = new Node(toIP, toPort);
        Set<UDPMessage> udpMessages = messagesPendingDataAck.get(ackNode);
        if (udpMessages != null) {
            udpMessages.remove(udpMessage);
            if (udpMessages.size() == 0) {
                messagesPendingDataAck.remove(ackNode);
            }
        }
    }
}
