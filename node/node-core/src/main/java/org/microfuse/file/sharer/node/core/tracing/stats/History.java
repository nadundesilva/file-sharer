package org.microfuse.file.sharer.node.core.tracing.stats;

import org.microfuse.file.sharer.node.commons.communication.messaging.MessageIndexes;
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

    private transient Network network;

    private long startUpTimeStamp;
    private Map<TraceableNode, Map<Long, SerMessage>> nodeSerMessages;
    private Map<TraceableNode, Map<Long, SerSuperPeerMessage>> nodeSerSuperPeerMessages;
    private Map<TraceableNode, Long> bootstrappingMessageCounts;
    private Map<TraceableNode, Long> maintenanceMessageCounts;

    public History(Network network) {
        this.network = network;

        startUpTimeStamp = System.currentTimeMillis();
        nodeSerMessages = new HashMap<>();
        nodeSerSuperPeerMessages = new HashMap<>();
        bootstrappingMessageCounts = new HashMap<>();
        maintenanceMessageCounts = new HashMap<>();
    }

    /**
     * Add a new message sent to the history.
     *
     * @param timeStamp    The timestamp at which the message was sent
     * @param senderIP     The ip of the node which sent the message
     * @param senderPort   The port of the node which sent the message
     * @param receiverIP   The ip of the node which received the message
     * @param receiverPort The port of the node which received the message
     * @param message      The message that was used
     */
    public void notifyMessageSend(long timeStamp, String senderIP, int senderPort, String receiverIP, int receiverPort,
                                  Message message) {
        logger.info("Received notification of sending message " + message.toString());
        switch (message.getType()) {
            case REG:
            case JOIN:
            case JOIN_SUPER_PEER:
            case LEAVE:
            case UNREG:
                addBootstrappingMessageCount(timeStamp, senderIP, senderPort, receiverIP, receiverPort, message);
                break;
            case HEARTBEAT:
            case LIST_RESOURCES:
            case LIST_UNSTRUCTURED_CONNECTIONS:
            case LIST_SUPER_PEER_CONNECTIONS:
                addMaintenanceMessageCount(timeStamp, senderIP, senderPort, receiverIP, receiverPort, message);
                break;
            case SER:
                addSerMessageSend(timeStamp, senderIP, senderPort, receiverIP, receiverPort, message);
                break;
            case SER_SUPER_PEER:
                addSerSuperPeerOkMessageSend(timeStamp, senderIP, senderPort, receiverIP, receiverPort, message);
                break;
            default:
                logger.debug("Ignored sending of message of type " + message.getType());
        }
    }

    /**
     * Add a new message received to the history.
     *
     * @param timeStamp    The timestamp at which the message was sent
     * @param senderIP     The ip of the node which sent the message
     * @param senderPort   The port of the node which sent the message
     * @param receiverIP   The ip of the node which received the message
     * @param receiverPort The port of the node which received the message
     * @param message      The message that was used
     */
    public void notifyMessageReceived(long timeStamp, String senderIP, int senderPort, String receiverIP,
                                      int receiverPort, Message message) {
        logger.info("Received notification of receiving message " + message.toString());
        switch (message.getType()) {
            case REG_OK:
            case JOIN_OK:
            case JOIN_SUPER_PEER_OK:
            case LEAVE_OK:
            case UNREG_OK:
                addBootstrappingMessageCount(timeStamp, senderIP, senderPort, receiverIP, receiverPort, message);
                break;
            case HEARTBEAT_OK:
            case LIST_RESOURCES_OK:
            case LIST_UNSTRUCTURED_CONNECTIONS_OK:
            case LIST_SUPER_PEER_CONNECTIONS_OK:
                addMaintenanceMessageCount(timeStamp, senderIP, senderPort, receiverIP, receiverPort, message);
                break;
            case SER_OK:
                addSerOkMessageOkReceived(timeStamp, senderIP, senderPort, receiverIP, receiverPort, message);
                break;
            case SER_SUPER_PEER_OK:
                addSerSuperPeerOkMessageReceived(timeStamp, senderIP, senderPort, receiverIP, receiverPort, message);
                break;
            default:
                logger.debug("Ignored receiving of message of type " + message.getType());
        }
    }

    /**
     * Add a new SER message send to the history.
     *
     * @param timeStamp    The timestamp at which the message was sent
     * @param senderIP     The ip of the node which sent the message
     * @param senderPort   The port of the node which sent the message
     * @param receiverIP   The ip of the node which received the message
     * @param receiverPort The port of the node which received the message
     * @param message      The message that was used
     */
    private void addSerMessageSend(long timeStamp, String senderIP, int senderPort, String receiverIP, int receiverPort,
                                   Message message) {
        long sequenceNumber = Long.parseLong(message.getData(MessageIndexes.SER_SEQUENCE_NUMBER));
        String query = message.getData(MessageIndexes.SER_QUERY);
        String sourceIP = message.getData(MessageIndexes.SER_SOURCE_IP);
        int sourcePort = Integer.parseInt(message.getData(MessageIndexes.SER_SOURCE_PORT));
        TraceableNode node = network.getNode(sourceIP, sourcePort);

        Map<Long, SerMessage> serMessages = nodeSerMessages.computeIfAbsent(node, traceableNode -> new HashMap<>());
        SerMessage serMessage = serMessages.computeIfAbsent(sequenceNumber, aLong -> new SerMessage(query));

        serMessage.increaseMessagesCount();
        serMessage.setStartTimeStamp(timeStamp);
    }

    /**
     * Add a new SER_OK message receive to the history.
     *
     * @param timeStamp    The timestamp at which the message was sent
     * @param senderIP     The ip of the node which sent the message
     * @param senderPort   The port of the node which sent the message
     * @param receiverIP   The ip of the node which received the message
     * @param receiverPort The port of the node which received the message
     * @param message      The message that was used
     */
    private void addSerOkMessageOkReceived(long timeStamp, String senderIP, int senderPort, String receiverIP,
                                           int receiverPort, Message message) {
        long sequenceNumber = Long.parseLong(message.getData(MessageIndexes.SER_OK_SEQUENCE_NUMBER));
        String query = message.getData(MessageIndexes.SER_OK_QUERY_STRING);
        TraceableNode node = network.getNode(receiverIP, receiverPort);

        Map<Long, SerMessage> serMessages = nodeSerMessages.computeIfAbsent(node, traceableNode -> new HashMap<>());
        SerMessage serMessage = serMessages.computeIfAbsent(sequenceNumber, aLong -> new SerMessage(query));

        serMessage.increaseMessagesCount();
        serMessage.setFirstHitTimeStamp(timeStamp);
        serMessage.addHopCount(Integer.parseInt(message.getData(MessageIndexes.SER_OK_HOP_COUNT)));
    }

    /**
     * Add a new SER_SUPER_PEER message send to the history.
     *
     * @param timeStamp    The timestamp at which the message was sent
     * @param senderIP     The ip of the node which sent the message
     * @param senderPort   The port of the node which sent the message
     * @param receiverIP   The ip of the node which received the message
     * @param receiverPort The port of the node which received the message
     * @param message      The message that was used
     */
    private void addSerSuperPeerOkMessageSend(long timeStamp, String senderIP, int senderPort, String receiverIP,
                                              int receiverPort, Message message) {
        long sequenceNumber = Long.parseLong(message.getData(MessageIndexes.SER_SUPER_PEER_SEQUENCE_NUMBER));
        String sourceIP = message.getData(MessageIndexes.SER_SUPER_PEER_SOURCE_IP);
        int sourcePort = Integer.parseInt(message.getData(MessageIndexes.SER_SUPER_PEER_SOURCE_PORT));
        TraceableNode node = network.getNode(sourceIP, sourcePort);

        Map<Long, SerSuperPeerMessage> serSuperPeerMessages =
                nodeSerSuperPeerMessages.computeIfAbsent(node, traceableNode -> new HashMap<>());
        SerSuperPeerMessage serSuperPeerMessage =
                serSuperPeerMessages.computeIfAbsent(sequenceNumber, aLong -> new SerSuperPeerMessage());

        serSuperPeerMessage.increaseMessagesCount();
        serSuperPeerMessage.setStartTimeStamp(timeStamp);
    }

    /**
     * Add a new SER_SUPER_PEER_OK message receive to the history.
     *
     * @param timeStamp    The timestamp at which the message was sent
     * @param senderIP     The ip of the node which sent the message
     * @param senderPort   The port of the node which sent the message
     * @param receiverIP   The ip of the node which received the message
     * @param receiverPort The port of the node which received the message
     * @param message      The message that was used
     */
    private void addSerSuperPeerOkMessageReceived(long timeStamp, String senderIP, int senderPort, String receiverIP,
                                                  int receiverPort, Message message) {
        long sequenceNumber = Long.parseLong(message.getData(MessageIndexes.SER_SUPER_PEER_OK_SEQUENCE_NUMBER));
        TraceableNode node = network.getNode(receiverIP, receiverPort);

        Map<Long, SerSuperPeerMessage> serSuperPeerMessages =
                nodeSerSuperPeerMessages.computeIfAbsent(node, traceableNode -> new HashMap<>());
        SerSuperPeerMessage serSuperPeerMessage = serSuperPeerMessages.computeIfAbsent(sequenceNumber,
                aLong -> new SerSuperPeerMessage());

        serSuperPeerMessage.increaseMessagesCount();
        serSuperPeerMessage.setFirstHitTimeStamp(timeStamp);
        serSuperPeerMessage.addHopCount(Integer.parseInt(message.getData(MessageIndexes.SER_SUPER_PEER_OK_HOP_COUNT)));
    }

    /**
     * Add a new bootstrapping message to the history.
     *
     * @param timeStamp    The timestamp at which the message was sent
     * @param senderIP     The ip of the node which sent the message
     * @param senderPort   The port of the node which sent the message
     * @param receiverIP   The ip of the node which received the message
     * @param receiverPort The port of the node which received the message
     * @param message      The message that was used
     */
    private void addBootstrappingMessageCount(long timeStamp, String senderIP, int senderPort, String receiverIP,
                                              int receiverPort, Message message) {
        TraceableNode node = network.getNode(senderIP, senderPort);
        bootstrappingMessageCounts.compute(node, (traceableNode, aLong) -> (aLong == null ? 0 : aLong) + 1);
    }

    /**
     * Add a new bootstrapping message to the history.
     *
     * @param timeStamp    The timestamp at which the message was sent
     * @param senderIP     The ip of the node which sent the message
     * @param senderPort   The port of the node which sent the message
     * @param receiverIP   The ip of the node which received the message
     * @param receiverPort The port of the node which received the message
     * @param message      The message that was used
     */
    private void addMaintenanceMessageCount(long timeStamp, String senderIP, int senderPort, String receiverIP,
                                            int receiverPort, Message message) {
        TraceableNode node = network.getNode(senderIP, senderPort);
        maintenanceMessageCounts.compute(node, (traceableNode, aLong) -> (aLong == null ? 0 : aLong) + 1);
    }
}
