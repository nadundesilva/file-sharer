package org.microfuse.file.sharer.node.core;

import com.google.common.io.Files;
import com.google.gson.Gson;
import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.network.SocketNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.PeerType;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredFloodingRoutingStrategy;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.microfuse.file.sharer.node.core.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Manager singleton class.
 */
public class Manager {
    private static final Logger logger = LoggerFactory.getLogger(Manager.class);

    private static volatile PeerType peerType;
    private static volatile Configuration configuration;
    private static volatile Router router;
    private static volatile ResourceIndex resourceIndex;

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
        // TODO : Implement promoting to super peer
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
        // TODO : Implement demoting to ordinary peer
    }

    /**
     * Get a singleton instance of this nodes configuration.
     *
     * @return The configuration of this node
     */
    public static synchronized Configuration getConfiguration() {
        if (configuration == null) {
            File configFile = new File(Constants.CONFIG_FILE);
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
                            logger.warn("Failed to create configuration file: " + configFile.getAbsolutePath(), e1);
                        }
                    }
                } catch (IOException e1) {
                    logger.warn("Failed to create file " + configFile.getAbsolutePath(), e1);
                }
            }
        }
        return configuration;
    }

    /**
     * Get a singleton instance of the router used by this node.
     *
     * @return The router used by this node
     */
    public static synchronized Router getRouter() {
        if (router == null) {
            NetworkHandler networkHandler;
            try {
                networkHandler = NetworkHandlerType.getNetworkHandlerClass(getConfiguration().getNetworkHandlerType())
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to instantiate " + getConfiguration().getNetworkHandlerType().getValue()
                        + ". Using " + NetworkHandlerType.SOCKET.getValue() + " instead.", e);
                getConfiguration().setNetworkHandlerType(NetworkHandlerType.SOCKET);
                networkHandler = new SocketNetworkHandler();
            }

            RoutingStrategy routingStrategy;
            try {
                routingStrategy = RoutingStrategyType
                        .getRoutingStrategyClass(getConfiguration().getRoutingStrategyType())
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to instantiate " + getConfiguration().getRoutingStrategyType().getValue()
                        + ". Using " + RoutingStrategyType.UNSTRUCTURED_FLOODING.getValue() + " instead.", e);
                getConfiguration().setRoutingStrategyType(RoutingStrategyType.UNSTRUCTURED_FLOODING);
                routingStrategy = new UnstructuredFloodingRoutingStrategy();
            }

            router = new Router(networkHandler, routingStrategy);
        }
        return router;
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
                resourceIndex = PeerType.getResourceIndexClass(getPeerType())
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to instantiate resource index for " + getPeerType().getValue()
                        + ". Using resource index for " + PeerType.ORDINARY_PEER.getValue() + " instead.", e);
                resourceIndex = new ResourceIndex();
                demoteToOrdinaryPeer();
            }
        }
        return resourceIndex;
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
}
