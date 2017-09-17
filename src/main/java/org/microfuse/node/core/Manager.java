package org.microfuse.node.core;

import com.google.common.io.Files;
import com.google.gson.Gson;
import org.microfuse.node.commons.Configuration;
import org.microfuse.node.core.communication.network.NetworkHandler;
import org.microfuse.node.core.communication.network.NetworkHandlerType;
import org.microfuse.node.core.communication.network.SocketNetworkHandler;
import org.microfuse.node.core.communication.routing.Router;
import org.microfuse.node.core.communication.routing.strategy.FloodingRoutingStrategy;
import org.microfuse.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.node.core.communication.ttl.FixedTimeToLiveStrategy;
import org.microfuse.node.core.communication.ttl.TimeToLiveStrategy;
import org.microfuse.node.core.communication.ttl.TimeToLiveStrategyType;
import org.microfuse.node.core.utils.Constants;
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
                String configString = String.join("", Files.readLines(configFile, Constants.DEFAULT_CHARSET));
                configurationInstance = new Gson().fromJson(configString, Configuration.class);
            } catch (IOException e) {
                // Creating a new configuration file based on default values
                configurationInstance = new Configuration();
                try {
                    if (!configFile.createNewFile()) {
                        try {
                            Files.write(
                                    new Gson().toJson(configurationInstance).getBytes(Constants.DEFAULT_CHARSET),
                                    configFile
                            );
                        } catch (IOException e1) {
                            logger.debug("Failed to create configuration file: " + configFile.getAbsolutePath(), e1);
                        }
                    }
                } catch (IOException e1) {
                    logger.debug("Failed to create file " + configFile.getAbsolutePath());
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
                logger.info("Failed to instantiate " + configuration.getNetworkHandlerType().getValue() + ". Using "
                        + NetworkHandlerType.SOCKET.getValue() + " instead");
                configuration.setNetworkHandlerType(NetworkHandlerType.SOCKET);
                networkHandler = new SocketNetworkHandler();
            }

            RoutingStrategy routingStrategy;
            try {
                routingStrategy = RoutingStrategyType.getRoutingStrategyClass(
                        configuration.getRoutingStrategyType()).newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.info("Failed to instantiate " + configuration.getRoutingStrategyType().getValue()
                        + ". Using " + RoutingStrategyType.FLOODING.getValue() + " instead");
                configuration.setRoutingStrategyType(RoutingStrategyType.FLOODING);
                routingStrategy = new FloodingRoutingStrategy();
            }

            TimeToLiveStrategy timeToLiveStrategy;
            try {
                timeToLiveStrategy = TimeToLiveStrategyType.getRoutingStrategyClass(
                        configuration.getTimeToLiveStrategyType()).newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.info("Failed to instantiate " + configuration.getTimeToLiveStrategyType().getValue()
                        + ". Using " + TimeToLiveStrategyType.FIXED.getValue() + " instead");
                configuration.setTimeToLiveStrategyType(TimeToLiveStrategyType.FIXED);
                timeToLiveStrategy = new FixedTimeToLiveStrategy();
            }

            routerInstance = new Router(networkHandler, routingStrategy, timeToLiveStrategy);
        }
        return routerInstance;
    }
}
