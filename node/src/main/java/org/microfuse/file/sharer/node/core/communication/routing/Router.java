package org.microfuse.file.sharer.node.core.communication.routing;

import com.google.common.collect.Lists;
import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.core.ServiceHolder;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerListener;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.utils.Constants;
import org.microfuse.file.sharer.node.core.utils.MessageConstants;
import org.microfuse.file.sharer.node.core.utils.MessageIndexes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
        try {
            routingTable = PeerType.getRoutingTableClass(ServiceHolder.getPeerType()).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Failed to instantiate routing table for " + ServiceHolder.getPeerType().getValue()
                    + ". Using the routing table for " + PeerType.ORDINARY_PEER.getValue() + " instead", e);
            ServiceHolder.demoteToOrdinaryPeer();
            routingTable = new OrdinaryPeerRoutingTable();
        }
        this.routingStrategy = routingStrategy;
        this.networkHandler = networkHandler;
        this.listenersList = new ArrayList<>();

        this.networkHandler.registerListener(this);
    }

    @Override
    public synchronized void onMessageReceived(String fromAddress, int fromPort, Message message) {
        MessageType messageType = message.getType();
        if (messageType != null && messageType == MessageType.SER) {
            route(routingTable.getUnstructuredNetworkRoutingTableNode(fromAddress, fromPort), message);
        } else {
            runTasksOnMessageReceived(message);
        }
    }

    @Override
    public synchronized void onMessageSendFailed(String toAddress, int toPort, Message message) {
        // Marking the node as inactive
        Node receivingNode = routingTable.getUnstructuredNetworkRoutingTableNode(toAddress, toPort);
        if (receivingNode == null && ServiceHolder.getPeerType() == PeerType.SUPER_PEER) {
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTable;
            receivingNode = superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode(toAddress, toPort);
            if (receivingNode == null) {
                receivingNode = superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode(toAddress, toPort);
            }
        }
        if (receivingNode != null) {
            receivingNode.setAlive(false);
        }
    }

    /**
     * Send a message directly to a node.
     *
     * @param toNode  The node to which the message needs to be sent
     * @param message The message to be sent
     */
    public synchronized void sendMessage(Node toNode, Message message) {
        networkHandler.sendMessage(toNode.getIp(), toNode.getPort(), message);
    }

    /**
     * Route a message through the P2P network.
     *
     * @param message  The message to be sent
     */
    public synchronized void route(Message message) {
        route(null, message);
    }

    /**
     * Route a message through the P2P network.
     *
     * @param fromNode The node from which the message was received by this node
     * @param message  The message to be sent
     */
    private synchronized void route(Node fromNode, Message message) {
        MessageType messageType = message.getType();

        if (messageType != null && messageType == MessageType.SER) {
            // Checking owned resources
            Set<OwnedResource> ownedResources = ServiceHolder.getResourceIndex()
                    .findResources(message.getData(MessageIndexes.SER_FILE_NAME));
            if (ownedResources.size() > 0) {
                Message serOkMessage = new Message();
                serOkMessage.setType(MessageType.SER_OK);

                List<String> serOkData = Lists.newArrayList(
                        Integer.toString(ownedResources.size()),
                        ServiceHolder.getConfiguration().getIp(),
                        Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()),
                        Integer.toString(Constants.INITIAL_HOP_COUNT)
                );
                ownedResources.forEach(resource -> serOkData.add(resource.getName()));

                serOkMessage.setData(serOkData);
                networkHandler.sendMessage(message.getData(MessageIndexes.SER_SOURCE_IP),
                        Integer.parseInt(message.getData(MessageIndexes.SER_SOURCE_PORT)), serOkMessage);
            } else {
                // Updating the hop count
                Integer hopCount = Integer.parseInt(message.getData(MessageIndexes.SER_HOP_COUNT));
                hopCount++;
                message.setData(MessageIndexes.SER_HOP_COUNT, hopCount.toString());

                if (hopCount <= ServiceHolder.getConfiguration().getTimeToLive()) {
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
                    serOkMessage.setData(Lists.newArrayList(MessageConstants.SER_OK_NOT_FOUND_FILE_COUNT,
                            MessageConstants.SER_OK_NOT_FOUND_IP, MessageConstants.SER_OK_NOT_FOUND_PORT));
                    networkHandler.sendMessage(message.getData(MessageIndexes.SER_SOURCE_IP),
                            Integer.parseInt(message.getData(MessageIndexes.SER_SOURCE_PORT)), serOkMessage);
                }
            }
        } else {
            logger.warn("Not routing message of type " + messageType + ". The Router will only route messages of type "
                    + MessageType.SER + ".");
        }
    }

    /**
     * Promote the current node to an ordinary peer.
     */
    public synchronized void promoteToSuperPeer() {
        if (routingTable instanceof OrdinaryPeerRoutingTable) {
            SuperPeerRoutingTable superPeerRoutingTable = new SuperPeerRoutingTable();
            superPeerRoutingTable.setBootstrapServer(routingTable.getBootstrapServer());
            superPeerRoutingTable.addAllUnstructuredNetworkRoutingTableEntry(
                    routingTable.getAllUnstructuredNetworkRoutingTableNodes());
            routingTable = superPeerRoutingTable;
        }
    }

    /**
     * Demote the current node to a super peer.
     */
    public synchronized void demoteToOrdinaryPeer() {
        if (routingTable instanceof SuperPeerRoutingTable) {
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = new OrdinaryPeerRoutingTable();
            ordinaryPeerRoutingTable.setBootstrapServer(routingTable.getBootstrapServer());
            ordinaryPeerRoutingTable.addAllUnstructuredNetworkRoutingTableEntry(
                    routingTable.getAllUnstructuredNetworkRoutingTableNodes());
            routingTable = ordinaryPeerRoutingTable;
        }
    }

    /**
     * Run tasks to be run when a message intended for this node is received.
     *
     * @param message The message that was received
     */
    public void runTasksOnMessageReceived(Message message) {
        listenersList.stream().parallel()
                .forEach(routerListener -> routerListener.onMessageReceived(message));
    }

    /**
     * Get the routing table used by this router.
     *
     * @return The routing table
     */
    public synchronized RoutingTable getRoutingTable() {
        return routingTable;
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
