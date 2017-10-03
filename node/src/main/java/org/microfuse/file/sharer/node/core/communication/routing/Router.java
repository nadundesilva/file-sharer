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
import org.microfuse.file.sharer.node.core.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        Configuration configuration = Manager.getConfigurationInstance();

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
    public void onMessageReceived(String fromAddress, String messageString) {
        Message message = Message.parse(messageString);
        route(routingTable.getUnstructuredNetworkRoutingTableNode(fromAddress), message);
        runTasksOnMessageReceived(message);
    }

    @Override
    public void onMessageSendFailed(String toAddress, String message) {
        Message msg = Message.parse(message);
        if (msg.getType() == MessageType.SER) {
            String ip = msg.getData(0);
            int port = Integer.parseInt(msg.getData(1));
            networkHandler.sendMessage(ip, port, msg);
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
            // Updating the hop count
            Integer hopCount = Integer.parseInt(message.getData(Constants.SER_MESSAGE_HOP_COUNT_INDEX));
            hopCount++;
            message.setData(Constants.SER_MESSAGE_HOP_COUNT_INDEX, hopCount.toString());

            if (hopCount <= Manager.getConfigurationInstance().getTimeToLive()) {
                List<Node> forwardingNodes = routingStrategy.getForwardingNodes(routingTable, fromNode, message);

                forwardingNodes.stream().parallel()
                        .forEach(forwardingNode -> {
                            Message clonedMessage = message.clone();

                            logger.debug("Routing message to " + forwardingNode + ": " + clonedMessage.toString());
                            networkHandler.sendMessage(forwardingNode.getIp(), forwardingNode.getPort(), clonedMessage);
                        });
            } else {
                // Unable to find resource
                Message serOkMessage = new Message();
                serOkMessage.setType(MessageType.SER_OK);
                serOkMessage.setData(Arrays.asList("0", Constants.SER_OK_NOT_FOUND_IP,
                        Constants.SER_OK_NOT_FOUND_PORT));
                networkHandler.sendMessage(message.getData(0), Integer.parseInt(message.getData(1)),
                        serOkMessage);
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
