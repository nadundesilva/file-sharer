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
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * ServiceHolder singleton class.
 * <p>
 * This holds different types of services and implements methods for managing them.
 * This also holds the configuration and the peer type.
 */
public class ServiceHolder {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHolder.class);

    private volatile PeerType peerType;
    private volatile Configuration configuration;
    private volatile Router router;
    private volatile ResourceIndex resourceIndex;
    private volatile OverlayNetworkManager overlayNetworkManager;
    private volatile QueryManager queryManager;

    /**
     * Promote the current node to an ordinary peer.
     */
    public synchronized void promoteToSuperPeer() {
        peerType = PeerType.SUPER_PEER;
        if (!(getResourceIndex() instanceof SuperPeerResourceIndex)) {
            ResourceIndex newResourceIndex = new SuperPeerResourceIndex();
            newResourceIndex.addAllResourcesToIndex(getResourceIndex().getAllResourcesInIndex());
            resourceIndex = newResourceIndex;
        }
        getRouter().promoteToSuperPeer();
        logger.debug("Promoted to super peer");
    }

    /**
     * Demote the current node to a super peer.
     */
    public synchronized void demoteToOrdinaryPeer() {
        peerType = PeerType.ORDINARY_PEER;
        if (getResourceIndex() instanceof SuperPeerResourceIndex) {
            ResourceIndex newResourceIndex = new ResourceIndex();
            newResourceIndex.addAllResourcesToIndex(getResourceIndex().getAllResourcesInIndex());
            resourceIndex = newResourceIndex;
        }
        getRouter().demoteToOrdinaryPeer();
        logger.debug("Demoted to ordinary peer");
    }

    /**
     * Change the network handler used by the system.
     *
     * @param networkHandlerType The network handler type to be used
     */
    public synchronized void changeNetworkHandler(NetworkHandlerType networkHandlerType) {
        getConfiguration().setNetworkHandlerType(networkHandlerType);
        getRouter().changeNetworkHandler(instantiateNetworkHandler());
    }

    /**
     * Change the network handler used by the system.
     *
     * @param routingStrategyType The network handler type to be used
     */
    public synchronized void changeRoutingStrategy(RoutingStrategyType routingStrategyType) {
        getConfiguration().setRoutingStrategyType(routingStrategyType);
        getRouter().changeRoutingStrategy(instantiateRoutingStrategy());
    }

    /**
     * Clear all the stored services.
     */
    public synchronized void clear() {
        if (router != null) {
            router.shutdown();
        }
        if (overlayNetworkManager != null) {
            overlayNetworkManager.cancelSearchForSuperPeer();
        }
        saveConfiguration();

        peerType = null;
        configuration = null;
        router = null;
        resourceIndex = null;
        overlayNetworkManager = null;
        queryManager = null;
    }

    /**
     * Get a singleton instance of this nodes configuration.
     *
     * @return The configuration of this node
     */
    public synchronized Configuration getConfiguration() {
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
                saveConfiguration();
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
    public synchronized ResourceIndex getResourceIndex() {
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
    public synchronized OverlayNetworkManager getOverlayNetworkManager() {
        if (overlayNetworkManager == null) {
            overlayNetworkManager = new OverlayNetworkManager(getRouter(), this);
        }
        return overlayNetworkManager;
    }

    /**
     * Get the query manager singleton instance.
     *
     * @return The Query Manager
     */
    public synchronized QueryManager getQueryManager() {
        if (queryManager == null) {
            queryManager = new QueryManager(getRouter(), this);
        }
        return queryManager;
    }

    /**
     * Return the peer type of this node.
     *
     * @return The peer type of the node
     */
    public synchronized PeerType getPeerType() {
        if (peerType == null) {
            peerType = PeerType.ORDINARY_PEER;
        }
        return peerType;
    }

    /**
     * Get a singleton instance of the router used by this node.
     *
     * @return The router used by this node
     */
    public synchronized Router getRouter() {
        if (router == null) {
            router = new Router(instantiateNetworkHandler(), instantiateRoutingStrategy(), this);
        }
        return router;
    }

    /**
     * Instantiate network handler based on configuration.
     *
     * @return The network handler
     */
    private synchronized NetworkHandler instantiateNetworkHandler() {
        NetworkHandler networkHandler;
        try {
            networkHandler = NetworkHandler.getNetworkHandlerClass(getConfiguration().getNetworkHandlerType())
                    .getConstructor(ServiceHolder.class).newInstance(this);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                InvocationTargetException e) {
            logger.error("Failed to instantiate " + getConfiguration().getNetworkHandlerType().getValue()
                    + ". Using " + NetworkHandlerType.TCP_SOCKET.getValue() + " instead.", e);
            getConfiguration().setNetworkHandlerType(NetworkHandlerType.TCP_SOCKET);
            networkHandler = new TCPSocketNetworkHandler(this);
        }
        return networkHandler;
    }

    /**
     * Instantiate routing strategy based on configuration.
     *
     * @return The routing strategy
     */
    private synchronized RoutingStrategy instantiateRoutingStrategy() {
        RoutingStrategy routingStrategy;
        try {
            routingStrategy = RoutingStrategy.getRoutingStrategyClass(getConfiguration().getRoutingStrategyType())
                    .getConstructor(ServiceHolder.class).newInstance(this);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                InvocationTargetException e) {
            logger.error("Failed to instantiate " + getConfiguration().getRoutingStrategyType().getValue()
                    + ". Using " + RoutingStrategyType.UNSTRUCTURED_FLOODING.getValue() + " instead.", e);
            getConfiguration().setRoutingStrategyType(RoutingStrategyType.UNSTRUCTURED_FLOODING);
            routingStrategy = new UnstructuredFloodingRoutingStrategy(this);
        }
        return routingStrategy;
    }

    private synchronized void saveConfiguration() {
        File configFile = new File(NodeConstants.CONFIG_FILE);
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
