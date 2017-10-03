package org.microfuse.file.sharer.node.core.communication.routing;

import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.core.Manager;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerListener;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.utils.MessageConstants;
import org.microfuse.file.sharer.node.core.utils.MessageIndexes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Router class.
 * <p>
 * This governs how the messages are routed through the P2P network.
 */
public class Router implements NetworkHandlerListener {
    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    private List<RouterListener> listenersList;
    private RoutingTable routingTable;

    private RoutingStrategy routingStrategy;
    private NetworkHandler networkHandler;

    public Router(NetworkHandler networkHandler, RoutingStrategy routingStrategy) {
        Configuration configuration = Manager.getConfiguration();

        try {
            routingTable = PeerType.getRoutingTableClass(configuration.getPeerType()).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Failed to instantiate routing table for " + configuration.getPeerType().getValue()
                    + ". Using the routing table for " + PeerType.ORDINARY_PEER.getValue() + " instead", e);
            configuration.setPeerType(PeerType.ORDINARY_PEER);
            routingTable = new OrdinaryPeerRoutingTable();
        }
        this.routingStrategy = routingStrategy;
        this.networkHandler = networkHandler;
    }

    @Override
    public void onMessageReceived(String fromAddress, int fromPort, String messageString) {
        Message message = Message.parse(messageString);
        route(routingTable.getUnstructuredNetworkRoutingTableNode(fromAddress, fromPort), message);
        runTasksOnMessageReceived(message);
    }

    @Override
    public void onMessageSendFailed(String toAddress, int toPort, String message) {
        // Marking the node as inactive
        Node receivingNode = routingTable.getUnstructuredNetworkRoutingTableNode(toAddress, toPort);
        if (receivingNode == null && Manager.isSuperPeer()) {
            receivingNode = ((SuperPeerRoutingTable) routingTable)
                    .getSuperPeerNetworkRoutingTableNode(toAddress, toPort);
        }
        if (receivingNode != null) {
            receivingNode.setAlive(false);
        }
    }

    /**
     * Route a message through the P2P network.
     *
     * @param fromNode The node from which the message was received by this node
     * @param message  The message to be sent
     */
    public void route(Node fromNode, Message message) {
        MessageType messageType = message.getType();

        if (messageType == MessageType.SER) {
            // Checking owned resources
            Set<OwnedResource> ownedResources = Manager.getResourceIndex()
                    .findResources(message.getData(MessageIndexes.SER_FILE_NAME));
            if (ownedResources.size() > 0) {
                Message serOkMessage = new Message();
                serOkMessage.setType(MessageType.SER_OK);

                List<String> serOkData = Arrays.asList(
                        Integer.toString(ownedResources.size()),
                        Manager.getConfiguration().getAddress(),
                        Integer.toString(Manager.getConfiguration().getPeerListeningPort())
                );
                ownedResources.forEach(resource -> serOkData.add(resource.getName()));

                serOkMessage.setData(serOkData);
                networkHandler.sendMessage(message.getData(MessageIndexes.SER_OK_SOURCE_IP),
                        Integer.parseInt(message.getData(MessageIndexes.SER_SOURCE_PORT)), serOkMessage);
            } else {
                // Updating the hop count
                Integer hopCount = Integer.parseInt(message.getData(MessageIndexes.SER_HOP_COUNT));
                hopCount++;
                message.setData(MessageIndexes.SER_HOP_COUNT, hopCount.toString());

                if (hopCount <= Manager.getConfiguration().getTimeToLive()) {
                    Set<Node> forwardingNodes = routingStrategy.getForwardingNodes(routingTable, fromNode, message);

                    forwardingNodes.stream().parallel()
                            .forEach(forwardingNode -> {
                                Message clonedMessage = message.clone();
                                logger.debug("Routing message to " + forwardingNode + ": " + clonedMessage.toString());
                                networkHandler.sendMessage(
                                        forwardingNode.getIp(), forwardingNode.getPort(), clonedMessage
                                );
                            });
                } else {
                    // Unable to find resource
                    Message serOkMessage = new Message();
                    serOkMessage.setType(MessageType.SER_OK);
                    serOkMessage.setData(Arrays.asList(MessageConstants.SER_OK_NOT_FOUND_FILE_COUNT,
                            MessageConstants.SER_OK_NOT_FOUND_IP, MessageConstants.SER_OK_NOT_FOUND_PORT));
                    networkHandler.sendMessage(message.getData(MessageIndexes.SER_OK_SOURCE_IP),
                            Integer.parseInt(message.getData(MessageIndexes.SER_SOURCE_PORT)), serOkMessage);
                }
            }
        }
    }

    /**
     * Run tasks to be run when a message intended for this node is received.
     *
     * @param message The message that was received
     */
    private void runTasksOnMessageReceived(Message message) {
        listenersList.stream().parallel()
                .forEach(routerListener -> routerListener.onMessageReceived(message));
    }

    /**
     * Get the routing strategy used by this router.
     *
     * @return The routing strategy used by the router
     */
    public RoutingStrategy getRoutingStrategy() {
        return routingStrategy;
    }

    /**
     * Set the routing strategy currently used by this router.
     *
     * @param routingStrategy The routing strategy used by this router
     */
    public void setRoutingStrategy(RoutingStrategy routingStrategy) {
        this.routingStrategy = routingStrategy;
        logger.debug("Changed routing strategy to " + routingStrategy.getName());
    }

    /**
     * Get the network handler used by this router.
     *
     * @return The network handler used by this router
     */
    public NetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    /**
     * Set a new Network handler.
     *
     * @param networkHandler The new network handler
     */
    public void setNetworkHandler(NetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
    }

    /**
     * Register a new listener.
     *
     * @param listener The new listener to be registered
     */
    public void registerListener(RouterListener listener) {
        if (listenersList.add(listener)) {
            logger.debug("Registered network handler listener " + listener.getClass());
        } else {
            logger.debug("Failed to register network handler listener " + listener.getClass());
        }
    }

    /**
     * Unregister an existing listener.
     *
     * @param listener The listener to be removed
     */
    public void unregisterListener(RouterListener listener) {
        if (listenersList.remove(listener)) {
            logger.debug("Unregistered network handler listener " + listener.getClass());
        } else {
            logger.debug("Failed to unregister network handler listener " + listener.getClass());
        }
    }

    /**
     * Unregister all existing listener.
     */
    public void clearListeners() {
        listenersList = new ArrayList<>();
        logger.debug("Cleared network handler listeners");
    }
}
