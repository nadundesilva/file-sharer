package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bootstrapping Manager.
 */
public class BootstrappingManager implements RouterListener {
    private static final Logger logger = LoggerFactory.getLogger(BootstrappingManager.class);

    private Router router;

    public BootstrappingManager(Router router) {
        this.router = router;
        this.router.registerListener(this);
    }

    @Override
    public void onMessageReceived(Node fromNode, Message message) {
        switch (message.getType()) {
            case REG_OK:
                handleRegOkMessage(fromNode, message);
                break;
            case UNREG_OK:
                handleUnregOkMessage(fromNode, message);
                break;
            case JOIN:
                handleJoinMessage(fromNode, message);
                break;
            case JOIN_OK:
                handleJoinOkMessage(fromNode, message);
                break;
            case LEAVE:
                handleLeaveMessage(fromNode, message);
                break;
            case LEAVE_OK:
                handleLeaveOkMessage(fromNode, message);
                break;
            default:
                logger.debug("Message " + message.toString() + " of unrecognized type ignored ");
        }
    }

    /**
     * Register the current node in the bootstrap server.
     */
    public void register() {

    }

    /**
     * Unregister the current node from the bootstrap server.
     */
    public void unregister() {

    }

    /**
     * Join the system.
     *
     * @param nodes The nodes to connect to
     */
    private void join(List<Node> nodes) {
        nodes.forEach(node -> {
            Message joinMessage = new Message();
            joinMessage.setData(MessageIndexes.JOIN_IP, ServiceHolder.getConfiguration().getIp());
            joinMessage.setData(MessageIndexes.JOIN_PORT,
                    Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
            router.sendMessage(node, joinMessage);
        });
    }

    /**
     * Leave the system.
     */
    public void leave() {

    }

    /**
     * Connect to a super peer.
     */
    public void connectToSuperPeer() {

    }

    /**
     * Search for a super peer in the system.
     */
    public void searchForSuperPeer() {

    }

    /**
     * Self assign current node as super peer.
     */
    public void selfAssignSuperPeer() {

    }

    /**
     * Handle REG_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleRegOkMessage(Node fromNode, Message message) {
        switch (message.getData(MessageIndexes.REG_OK_NODES_COUNT)) {
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR:
                logger.error("Unknown error in registering with bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort());
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_ALREADY_REGISTERED:
                logger.warn("Current node already registered to bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort());
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_ALREADY_OCCUPIED:
                logger.warn("Already registered to bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort());

                // Retrying
                ServiceHolder.getConfiguration()
                        .setPeerListeningPort(ServiceHolder.getConfiguration().getPeerListeningPort() + 1);
                logger.debug("Changing peer listening port to "
                        + ServiceHolder.getConfiguration().getPeerListeningPort() + " and retrying");
                register();
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_FULL:
                logger.warn("Bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort() + " full");
                break;
            default:
                int nodesCount = Integer.parseInt(message.getData(MessageIndexes.REG_OK_NODES_COUNT));
                if (nodesCount > 0) {
                    List<Node> nodesList = new ArrayList<>();
                    if (nodesCount <= 2) {
                        for (int i = 0; i < nodesCount * 2; i += 2) {
                            int messageIndex = MessageIndexes.REG_OK_IP_PORT_START + i;
                            Node node = new Node();
                            node.setIp(message.getData(messageIndex));
                            node.setPort(message.getData(messageIndex));
                            nodesList.add(node);
                        }
                    } else {
                        for (int i = 0; i < 2; i++) {
                            int messageIndex = MessageIndexes.REG_OK_IP_PORT_START
                                    + ThreadLocalRandom.current().nextInt(0, nodesCount);
                            Node node = new Node();
                            node.setIp(message.getData(messageIndex));
                            node.setPort(message.getData(messageIndex));
                            nodesList.add(node);
                        }
                    }

                    join(nodesList);
                }
        }
    }

    /**
     * Handle UNREG_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleUnregOkMessage(Node fromNode, Message message) {
        if (Objects.equals(message.getData(MessageIndexes.UNREG_OK_VALUE), MessageConstants.UNREG_OK_VALUE_SUCCESS)) {
            logger.info("Successfully unregistered from bootstrap server");
        } else if (Objects.equals(message.getData(MessageIndexes.UNREG_OK_VALUE),
                MessageConstants.UNREG_OK_VALUE_ERROR)) {
            logger.warn("Failed to create unstructured connection with " + fromNode.toString());
        } else {
            logger.debug("Unknown value " + message.getData(MessageIndexes.UNREG_OK_VALUE) + " in message \""
                    + message.toString() + "\"");
        }
    }

    /**
     * Handle JOIN type message.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinMessage(Node fromNode, Message message) {
        Node node = new Node();
        node.setIp(message.getData(MessageIndexes.JOIN_IP));
        node.setPort(message.getData(MessageIndexes.JOIN_PORT));
        node.setAlive(true);

        boolean isSuccessful = router.getRoutingTable().addUnstructuredNetworkRoutingTableEntry(node);

        Message replyMessage = new Message();
        message.setData(
                MessageIndexes.JOIN_OK_VALUE,
                (isSuccessful ? MessageConstants.JOIN_OK_VALUE_SUCCESS : MessageConstants.JOIN_OK_VALUE_ERROR)
        );
        router.sendMessage(node, replyMessage);
    }

    /**
     * Handle JOIN_OK type message.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinOkMessage(Node fromNode, Message message) {
        if (Objects.equals(message.getData(MessageIndexes.JOIN_OK_VALUE), MessageConstants.JOIN_OK_VALUE_SUCCESS)) {
            router.getRoutingTable().addUnstructuredNetworkRoutingTableEntry(fromNode);
            connectToSuperPeer();
        } else if (Objects.equals(message.getData(MessageIndexes.JOIN_OK_VALUE),
                MessageConstants.JOIN_OK_VALUE_ERROR)) {
            logger.warn("Failed to create unstructured connection with " + fromNode.toString());
        } else {
            logger.debug("Unknown value " + message.getData(MessageIndexes.JOIN_OK_VALUE) + " in message \""
                    + message.toString() + "\"");
        }
    }

    /**
     * Handle LEAVE type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleLeaveMessage(Node fromNode, Message message) {
        Node node = new Node();
        node.setIp(message.getData(MessageIndexes.LEAVE_IP));
        node.setPort(message.getData(MessageIndexes.LEAVE_PORT));
        node.setAlive(false);

        boolean isSuccessful = router.getRoutingTable().removeUnstructuredNetworkRoutingTableEntry(node);

        Message replyMessage = new Message();
        message.setData(
                MessageIndexes.LEAVE_OK_VALUE,
                (isSuccessful ? MessageConstants.LEAVE_OK_VALUE_SUCCESS : MessageConstants.LEAVE_OK_VALUE_ERROR)
        );

        router.sendMessage(node, replyMessage);
    }

    /**
     * Handle LEAVE_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleLeaveOkMessage(Node fromNode, Message message) {
        if (Objects.equals(message.getData(MessageIndexes.LEAVE_OK_VALUE), MessageConstants.LEAVE_OK_VALUE_SUCCESS)) {
            router.getRoutingTable().removeUnstructuredNetworkRoutingTableEntry(fromNode);
        } else if (Objects.equals(message.getData(MessageIndexes.LEAVE_OK_VALUE),
                MessageConstants.LEAVE_OK_VALUE_ERROR)) {
            logger.warn("Failed to disconnect unstructured connection with " + fromNode.toString());
        } else {
            logger.debug("Unknown value " + message.getData(MessageIndexes.LEAVE_OK_VALUE) + " in message \""
                    + message.toString() + "\"");
        }
    }
}
