package org.microfuse.file.sharer.node.core.tracing.stats;

import org.microfuse.file.sharer.node.commons.communication.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.tracing.TraceableNode;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.tracing.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps the history of the network.
 */
public class History {
    private static final Logger logger = LoggerFactory.getLogger(History.class);

    private Network network;

    private long startUpTimeStamp;
    private Map<TraceableNode, Map<Long, SerMessage>> nodeSerMessages;
    private Map<TraceableNode, Map<Long, SerSuperPeerMessage>> nodeSerSuperPeerMessages;
    private long bootstrappingMessageCount;
    private long maintenanceMessageCount;

    public History(Network network) {
        this.network = network;

        startUpTimeStamp = System.currentTimeMillis();
        nodeSerMessages = new HashMap<>();
        nodeSerSuperPeerMessages = new HashMap<>();
        bootstrappingMessageCount = 0;
        maintenanceMessageCount = 0;
    }

    /**
     * Add a new message to the history.
     *
     * @param timeStamp The timestamp at which the message was sent
     * @param ip        The ip of the node which sent the message
     * @param port      The port of the node which sent the message
     * @param message   The message that was used.
     */
    public void notifyMessageSend(long timeStamp, String ip, int port, Message message) {
        logger.info("Received notification of message " + message.toString());
        switch (message.getType()) {
            case REG:
            case REG_OK:
            case JOIN:
            case JOIN_OK:
            case JOIN_SUPER_PEER:
            case JOIN_SUPER_PEER_OK:
            case LEAVE:
            case LEAVE_OK:
            case UNREG:
            case UNREG_OK:
                addBootstrappingMessageCount(timeStamp, ip, port, message);
                break;
            case HEARTBEAT:
            case HEARTBEAT_OK:
            case LIST_RESOURCES:
            case LIST_RESOURCES_OK:
            case LIST_UNSTRUCTURED_CONNECTIONS:
            case LIST_UNSTRUCTURED_CONNECTIONS_OK:
            case LIST_SUPER_PEER_CONNECTIONS:
            case LIST_SUPER_PEER_CONNECTIONS_OK:
                addMaintenanceMessageCount(timeStamp, ip, port, message);
                break;
            case SER:
            case SER_OK:
                addSerMessage(timeStamp, ip, port, message);
                break;
            case SER_SUPER_PEER:
            case SER_SUPER_PEER_OK:
                addSerSuperPeerMessage(timeStamp, ip, port, message);
                break;
            default:
                logger.debug("Ignored message of type " + message.getType());
        }
    }

    /**
     * Add a new SER and SER_OK message to the history.
     *
     * @param timeStamp The timestamp at which the message was sent
     * @param ip        The ip of the node which sent the message
     * @param port      The port of the node which sent the message
     * @param message   The message that was used
     */
    private void addSerMessage(long timeStamp, String ip, int port, Message message) {
        int sequenceNumberIndex;
        if (message.getType() == MessageType.SER) {
            sequenceNumberIndex = MessageIndexes.SER_SEQUENCE_NUMBER;
        } else {
            sequenceNumberIndex = MessageIndexes.SER_OK_SEQUENCE_NUMBER;
        }
        long sequenceNumber = Long.parseLong(message.getData(sequenceNumberIndex));

        TraceableNode node = network.getNode(ip, port);
        Map<Long, SerMessage> serMessages = nodeSerMessages.computeIfAbsent(node, traceableNode -> new HashMap<>());
        SerMessage serMessage = serMessages.computeIfAbsent(sequenceNumber,
                aLong -> new SerMessage(message.getData(MessageIndexes.SER_QUERY), timeStamp));

        serMessage.increaseMessagesCount();
        if (message.getType() == MessageType.SER_OK) {
            if (serMessage.getHopCounts().size() == 0) {
                serMessage.setFirstHitTimeStamp(timeStamp);
            }
            serMessage.addHopCount(Integer.parseInt(message.getData(MessageIndexes.SER_OK_HOP_COUNT)));
        }
    }

    /**
     * Add a new SER_SUPER_PEER or SER_SUPER_PEER_OK message to the history.
     *
     * @param timeStamp The timestamp at which the message was sent
     * @param ip        The ip of the node which sent the message
     * @param port      The port of the node which sent the message
     * @param message   The message that was used
     */
    private void addSerSuperPeerMessage(long timeStamp, String ip, int port, Message message) {
        int sequenceNumberIndex;
        if (message.getType() == MessageType.SER) {
            sequenceNumberIndex = MessageIndexes.SER_SUPER_PEER_SEQUENCE_NUMBER;
        } else {
            sequenceNumberIndex = MessageIndexes.SER_SUPER_PEER_OK_SEQUENCE_NUMBER;
        }
        long sequenceNumber = Long.parseLong(message.getData(sequenceNumberIndex));

        TraceableNode node = network.getNode(ip, port);
        Map<Long, SerSuperPeerMessage> serSuperPeerMessages =
                nodeSerSuperPeerMessages.computeIfAbsent(node, traceableNode -> new HashMap<>());
        SerSuperPeerMessage serSuperPeerMessage =
                serSuperPeerMessages.computeIfAbsent(sequenceNumber, aLong -> new SerSuperPeerMessage(timeStamp));

        serSuperPeerMessage.increaseMessagesCount();
        if (message.getType() == MessageType.SER_SUPER_PEER_OK) {
            if (serSuperPeerMessage.getHopCounts().size() == 0) {
                serSuperPeerMessage.setFirstHitTimeStamp(timeStamp);
            }
            serSuperPeerMessage.addHopCount(
                    Integer.parseInt(message.getData(MessageIndexes.SER_SUPER_PEER_OK_HOP_COUNT)));
        }
    }

    /**
     * Add a new bootstrapping message to the history.
     *
     * @param timeStamp The timestamp at which the message was sent
     * @param ip        The ip of the node which sent the message
     * @param port      The port of the node which sent the message
     * @param message   The message that was used
     */
    private void addBootstrappingMessageCount(long timeStamp, String ip, int port, Message message) {
        bootstrappingMessageCount++;
    }

    /**
     * Add a new bootstrapping message to the history.
     *
     * @param timeStamp The timestamp at which the message was sent
     * @param ip        The ip of the node which sent the message
     * @param port      The port of the node which sent the message
     * @param message   The message that was used
     */
    private void addMaintenanceMessageCount(long timeStamp, String ip, int port, Message message) {
        maintenanceMessageCount++;
    }
}
