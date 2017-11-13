package org.microfuse.file.sharer.node.core.tracing.stats;

import org.microfuse.file.sharer.node.commons.communication.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps the history of the network.
 */
public class History {
    private static final Logger logger = LoggerFactory.getLogger(History.class);

    private long startUpTimeStamp;
    private Map<Long, SerMessage> serMessages;
    private Map<Long, SerSuperPeerMessage> serSuperPeerMessages;
    private long bootstrappingMessageCount;
    private long maintenanceMessageCount;

    public History() {
        startUpTimeStamp = System.currentTimeMillis();
        serMessages = new HashMap<>();
        serSuperPeerMessages = new HashMap<>();
        bootstrappingMessageCount = 0;
        maintenanceMessageCount = 0;
    }

    /**
     * Add a new message to the history.
     *
     * @param message The message that was used.
     */
    public void notifyMessageSend(Message message) {
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
                addBootstrappingMessageCount(message);
                break;
            case HEARTBEAT:
            case HEARTBEAT_OK:
            case LIST_RESOURCES:
            case LIST_RESOURCES_OK:
            case LIST_UNSTRUCTURED_CONNECTIONS:
            case LIST_UNSTRUCTURED_CONNECTIONS_OK:
            case LIST_SUPER_PEER_CONNECTIONS:
            case LIST_SUPER_PEER_CONNECTIONS_OK:
                addMaintenanceMessageCount(message);
                break;
            case SER:
            case SER_OK:
                addSerMessage(message);
                break;
            case SER_SUPER_PEER:
            case SER_SUPER_PEER_OK:
                addSerSuperPeerMessage(message);
                break;
            default:
                logger.debug("Ignored message of type " + message.getType());
        }
    }

    /**
     * Add a new SER and SER_OK message to the history.
     *
     * @param message The message that was used
     */
    private void addSerMessage(Message message) {
        int sequenceNumberIndex;
        if (message.getType() == MessageType.SER) {
            sequenceNumberIndex = MessageIndexes.SER_SEQUENCE_NUMBER;
        } else {
            sequenceNumberIndex = MessageIndexes.SER_OK_SEQUENCE_NUMBER;
        }
        long sequenceNumber = Long.parseLong(message.getData(sequenceNumberIndex));

        SerMessage serMessage = serMessages.computeIfAbsent(sequenceNumber,
                aLong -> new SerMessage(message.getData(MessageIndexes.SER_QUERY)));
        serMessage.increaseMessagesCount();
        if (message.getType() == MessageType.SER_OK) {
            serMessage.addHopCount(Integer.parseInt(message.getData(MessageIndexes.SER_OK_HOP_COUNT)));
        }
    }

    /**
     * Add a new SER_SUPER_PEER or SER_SUPER_PEER_OK message to the history.
     *
     * @param message The message that was used
     */
    private void addSerSuperPeerMessage(Message message) {
        int sequenceNumberIndex;
        if (message.getType() == MessageType.SER) {
            sequenceNumberIndex = MessageIndexes.SER_SUPER_PEER_SEQUENCE_NUMBER;
        } else {
            sequenceNumberIndex = MessageIndexes.SER_SUPER_PEER_OK_SEQUENCE_NUMBER;
        }
        long sequenceNumber = Long.parseLong(message.getData(sequenceNumberIndex));

        SerSuperPeerMessage serSuperPeerMessage = serSuperPeerMessages.computeIfAbsent(sequenceNumber,
                aLong -> new SerSuperPeerMessage());
        serSuperPeerMessage.increaseMessagesCount();
        if (message.getType() == MessageType.SER_SUPER_PEER_OK) {
            serSuperPeerMessage.addHopCount(
                    Integer.parseInt(message.getData(MessageIndexes.SER_SUPER_PEER_OK_HOP_COUNT)));
        }
    }

    /**
     * Add a new bootstrapping message to the history.
     *
     * @param message The message that was used
     */
    private void addBootstrappingMessageCount(Message message) {
        bootstrappingMessageCount++;
    }

    /**
     * Add a new bootstrapping message to the history.
     *
     * @param message The message that was used
     */
    private void addMaintenanceMessageCount(Message message) {
        maintenanceMessageCount++;
    }

    /**
     * Get the bootstrapping messages count.
     *
     * @return The bootstrapping messages count
     */
    public long getBootstrappingMessageCount() {
        return bootstrappingMessageCount;
    }

    /**
     * Get the overlay network maintenance messages count.
     *
     * @return The overlay network maintenance messages count
     */
    public long getMaintenanceMessageCount() {
        return maintenanceMessageCount;
    }

    /**
     * Get the system startup timestamp.
     *
     * @return The system start up time stamp
     */
    public long getStartUpTimeStamp() {
        return startUpTimeStamp;
    }
}
