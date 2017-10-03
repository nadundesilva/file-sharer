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

/**
 * Manager singleton class.
 */
public class Manager {
    private static final Logger logger = LoggerFactory.getLogger(Manager.class);

    private static volatile Configuration configuration;
    private static volatile Router router;
    private static volatile ResourceIndex resourceIndex;

    static {
        // Creating instance of configuration
        {
            File configFile = new File("config.json");
            try {
                // Loading the configuration from config file
                String configString = String.join("", Files.readLines(configFile,
                        Constants.DEFAULT_CHARSET));
                configuration = new Gson().fromJson(configString, Configuration.class);
            } catch (IOException e) {
                logger.warn("Failed to load configuration from config file. Creating new configuration.", e);

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
        // Creating the instance of router
        {
            NetworkHandler networkHandler;
            try {
                networkHandler = NetworkHandlerType.getNetworkHandlerClass(configuration.getNetworkHandlerType())
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to instantiate " + configuration.getNetworkHandlerType().getValue()
                        + ". Using " + NetworkHandlerType.SOCKET.getValue() + " instead.", e);
                configuration.setNetworkHandlerType(NetworkHandlerType.SOCKET);
                networkHandler = new SocketNetworkHandler();
            }

            RoutingStrategy routingStrategy;
            try {
                routingStrategy = RoutingStrategyType.getRoutingStrategyClass(configuration.getRoutingStrategyType())
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to instantiate " + configuration.getRoutingStrategyType().getValue()
                        + ". Using " + RoutingStrategyType.UNSTRUCTURED_FLOODING.getValue() + " instead.", e);
                configuration.setRoutingStrategyType(RoutingStrategyType.UNSTRUCTURED_FLOODING);
                routingStrategy = new UnstructuredFloodingRoutingStrategy();
            }

            router = new Router(networkHandler, routingStrategy);
        }
        // Creating the instance of resource index
        {
            if (resourceIndex == null) {
                try {
                    resourceIndex = PeerType.getResourceIndexClass(configuration.getPeerType())
                            .newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    logger.error("Failed to instantiate resource index for " + configuration.getPeerType().getValue()
                            + ". Using resource index for " + PeerType.ORDINARY_PEER.getValue() + " instead.", e);
                    configuration.setPeerType(PeerType.ORDINARY_PEER);
                    resourceIndex = new ResourceIndex();
                }
            }
        }
    }

    /**
     * Promote the current node to an ordinary peer.
     */
    public static synchronized void promoteToSuperPeer() {
        // TODO : Implement promoting to super peer
    }

    /**
     * Demote the current node to a super peer.
     */
    public static synchronized void demoteToOrdinaryPeer() {
        // TODO : Implement demoting to ordinary peer
    }

    /**
     * Get a singleton instance of this nodes configuration.
     *
     * @return The configuration of this node
     */
    public static synchronized Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Get a singleton instance of the router used by this node.
     *
     * @return The router used by this node
     */
    public static synchronized Router getRouter() {
        return router;
    }

    /**
     * Returns the resource index instance used by this node.
     * This is not a singleton.
     *
     * @return The resource index instance
     */
    public static synchronized ResourceIndex getResourceIndex() {
        return resourceIndex;
    }

    /**
     * Return true if this is a super peer.
     *
     * @return true if this is a super peer
     */
    public static synchronized boolean isSuperPeer() {
        return resourceIndex instanceof SuperPeerResourceIndex;
    }
}
