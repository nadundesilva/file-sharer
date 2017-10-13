package org.microfuse.file.sharer.node.core.utils;

import com.google.common.io.Files;
import com.google.gson.Gson;
import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.TCPSocketNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredFloodingRoutingStrategy;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * ServiceHolder singleton class.
 * <p>
 * This holds different types of services and implements methods for managing them.
 * This also holds the configuration and the peer type.
 */
public class ServiceHolder {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHolder.class);

    private static volatile PeerType peerType;
    private static volatile Configuration configuration;
    private static volatile Router router;
    private static volatile ResourceIndex resourceIndex;
    private static volatile OverlayNetworkManager overlayNetworkManager;
    private static volatile QueryManager queryManager;

    private ServiceHolder() {   // Preventing from being initiated
    }

    /**
     * Promote the current node to an ordinary peer.
     */
    public static synchronized void promoteToSuperPeer() {
        peerType = PeerType.SUPER_PEER;
        if (!(getResourceIndex() instanceof SuperPeerResourceIndex)) {
            ResourceIndex newResourceIndex = new SuperPeerResourceIndex();
            newResourceIndex.addAllResourceToIndex(getResourceIndex().getAllResourcesInIndex());
            resourceIndex = newResourceIndex;
        }
        getRouter().promoteToSuperPeer();
        logger.debug("Promoted to super peer");
    }

    /**
     * Demote the current node to a super peer.
     */
    public static synchronized void demoteToOrdinaryPeer() {
        peerType = PeerType.ORDINARY_PEER;
        if (getResourceIndex() instanceof SuperPeerResourceIndex) {
            ResourceIndex newResourceIndex = new ResourceIndex();
            newResourceIndex.addAllResourceToIndex(getResourceIndex().getAllResourcesInIndex());
            resourceIndex = newResourceIndex;
        }
        getRouter().demoteToOrdinaryPeer();
        logger.debug("Demoted to ordinary peer");
    }

    /**
     * Get a singleton instance of this nodes configuration.
     *
     * @return The configuration of this node
     */
    public static synchronized Configuration getConfiguration() {
        if (configuration == null) {
            File configFile = new File(NodeConstants.CONFIG_FILE);
            boolean configFileExists = false;
            try {
                // Loading the configuration from config file
                List<String> configFileLines = Files.readLines(configFile, Constants.DEFAULT_CHARSET);
                if (configFileLines.size() > 0) {
                    String configString = String.join("", configFileLines);
                    configuration = new Gson().fromJson(configString, Configuration.class);
                    configFileExists = true;
                }
            } catch (IOException e) {
                logger.warn("Failed to load configuration from config file. Creating new configuration.", e);
            }

            if (!configFileExists) {
                // Creating a new configuration file based on default values
                configuration = new Configuration();
                try {
                    if (!configFile.createNewFile()) {
                        try {
                            Files.write(new Gson().toJson(configuration).getBytes(
                                    Constants.DEFAULT_CHARSET), configFile);
                        } catch (IOException e1) {
                            logger.warn("Failed to write configuration to " + configFile.getAbsolutePath(), e1);
                        }
                    } else {
                        logger.warn("Failed to create file " + configFile.getAbsolutePath());
                    }
                } catch (IOException e1) {
                    logger.warn("Failed to create file " + configFile.getAbsolutePath(), e1);
                }
            }
        }
        return configuration;
    }

    /**
     * Returns the resource index instance used by this node.
     * This is not a singleton.
     *
     * @return The resource index instance
     */
    public static synchronized ResourceIndex getResourceIndex() {
        if (resourceIndex == null) {
            try {
                resourceIndex = ResourceIndex.getResourceIndexClass(getPeerType())
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to instantiate resource index for " + getPeerType().getValue()
                        + ". Using resource index for " + PeerType.ORDINARY_PEER.getValue() + " instead.", e);
                resourceIndex = new ResourceIndex();
            }
        }
        return resourceIndex;
    }

    /**
     * Get the bootstrapping manager singleton instance.
     *
     * @return The Bootstrapping Manager
     */
    public static synchronized OverlayNetworkManager getOverlayNetworkManager() {
        if (overlayNetworkManager == null) {
            overlayNetworkManager = new OverlayNetworkManager(getRouter());
        }
        return overlayNetworkManager;
    }

    /**
     * Get the query manager singleton instance.
     *
     * @return The Query Manager
     */
    public static synchronized QueryManager getQueryManager() {
        if (queryManager == null) {
            queryManager = new QueryManager(getRouter());
        }
        return queryManager;
    }

    /**
     * Return the peer type of this node.
     *
     * @return The peer type of the node
     */
    public static synchronized PeerType getPeerType() {
        if (peerType == null) {
            peerType = PeerType.ORDINARY_PEER;
        }
        return peerType;
    }

    /**
     * Change the network handler used by the system.
     *
     * @param networkHandlerType The network handler type to be used
     */
    public static synchronized void changeNetworkHandler(NetworkHandlerType networkHandlerType) {
        getConfiguration().setNetworkHandlerType(networkHandlerType);
        getRouter().changeNetworkHandler(instantiateNetworkHandler());
    }

    /**
     * Change the network handler used by the system.
     *
     * @param routingStrategyType The network handler type to be used
     */
    public static synchronized void changeRoutingStrategy(RoutingStrategyType routingStrategyType) {
        getConfiguration().setRoutingStrategyType(routingStrategyType);
        getRouter().changeRoutingStrategy(instantiateRoutingStrategy());
    }

    /**
     * Get a singleton instance of the router used by this node.
     *
     * @return The router used by this node
     */
    private static synchronized Router getRouter() {
        if (router == null) {
            router = new Router(instantiateNetworkHandler(), instantiateRoutingStrategy());
        }
        return router;
    }

    /**
     * Instantiate network handler based on configuration.
     *
     * @return The network handler
     */
    private static synchronized NetworkHandler instantiateNetworkHandler() {
        NetworkHandler networkHandler;
        try {
            networkHandler = NetworkHandler.getNetworkHandlerClass(getConfiguration().getNetworkHandlerType())
                    .newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Failed to instantiate " + getConfiguration().getNetworkHandlerType().getValue()
                    + ". Using " + NetworkHandlerType.TCP_SOCKET.getValue() + " instead.", e);
            getConfiguration().setNetworkHandlerType(NetworkHandlerType.TCP_SOCKET);
            networkHandler = new TCPSocketNetworkHandler();
        }
        return networkHandler;
    }

    /**
     * Instantiate routing strategy based on configuration.
     *
     * @return The routing strategy
     */
    private static synchronized RoutingStrategy instantiateRoutingStrategy() {
        RoutingStrategy routingStrategy;
        try {
            routingStrategy = RoutingStrategy.getRoutingStrategyClass(getConfiguration().getRoutingStrategyType())
                    .newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Failed to instantiate " + getConfiguration().getRoutingStrategyType().getValue()
                    + ". Using " + RoutingStrategyType.UNSTRUCTURED_FLOODING.getValue() + " instead.", e);
            getConfiguration().setRoutingStrategyType(RoutingStrategyType.UNSTRUCTURED_FLOODING);
            routingStrategy = new UnstructuredFloodingRoutingStrategy();
        }
        return routingStrategy;
    }
}