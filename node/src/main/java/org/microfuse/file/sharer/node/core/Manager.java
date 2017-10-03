package org.microfuse.file.sharer.node.core;

import com.google.common.io.Files;
import com.google.gson.Gson;
import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.network.SocketNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.FloodingRoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;
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

    private static volatile Configuration configurationInstance;
    private static volatile Router routerInstance;

    /**
     * Get a singleton instance of this nodes configuration.
     *
     * @return The configuration of this node
     */
    public static synchronized Configuration getConfigurationInstance() {
        if (configurationInstance == null) {
            File configFile = new File("config.json");
            try {
                // Loading the configuration from config file
                String configString = String.join("", Files.readLines(configFile,
                        Constants.DEFAULT_CHARSET));
                configurationInstance = new Gson().fromJson(configString, Configuration.class);
            } catch (IOException e) {
                logger.warn("Failed to load configuration from config file. Creating new configuration.", e);

                // Creating a new configuration file based on default values
                configurationInstance = new Configuration();
                try {
                    if (!configFile.createNewFile()) {
                        try {
                            Files.write(new Gson().toJson(configurationInstance).getBytes(
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
        return configurationInstance;
    }

    /**
     * Get a singleton instance of the router used by this node.
     *
     * @return The router used by this node
     */
    public static synchronized Router getRouterInstance() {
        if (routerInstance == null) {
            Configuration configuration = getConfigurationInstance();

            NetworkHandler networkHandler;
            try {
                networkHandler = NetworkHandlerType.getNetworkHandlerClass(
                        configuration.getNetworkHandlerType()).newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to instantiate " + configuration.getNetworkHandlerType().getValue()
                        + ". Using " + NetworkHandlerType.SOCKET.getValue() + " instead", e);
                configuration.setNetworkHandlerType(NetworkHandlerType.SOCKET);
                networkHandler = new SocketNetworkHandler();
            }

            RoutingStrategy routingStrategy;
            try {
                routingStrategy = RoutingStrategyType.getRoutingStrategyClass(
                        configuration.getRoutingStrategyType()).newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to instantiate " + configuration.getRoutingStrategyType().getValue()
                        + ". Using " + RoutingStrategyType.FLOODING.getValue() + " instead", e);
                configuration.setRoutingStrategyType(RoutingStrategyType.FLOODING);
                routingStrategy = new FloodingRoutingStrategy();
            }

            routerInstance = new Router(networkHandler, routingStrategy);
        }
        return routerInstance;
    }
}
