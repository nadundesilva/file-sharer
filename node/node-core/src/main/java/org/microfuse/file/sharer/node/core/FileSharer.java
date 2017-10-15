package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node Manager class.
 */
public class FileSharer {
    private static final Logger logger = LoggerFactory.getLogger(FileSharer.class);

    private ServiceHolder serviceHolder;

    public FileSharer() {
        serviceHolder = new ServiceHolder();
    }

    /**
     * Start the current node.
     */
    public void start() {
        Thread thread = new Thread(() -> {
            logger.info("Starting Node");
            serviceHolder.getOverlayNetworkManager().register();
            serviceHolder.getOverlayNetworkManager().enableHeartBeat();
            serviceHolder.getOverlayNetworkManager().enableGossiping();
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * Shutdown the file sharer.
     */
    public void shutdown() {
        serviceHolder.getOverlayNetworkManager().disableGossiping();
        serviceHolder.getOverlayNetworkManager().disableHeartBeat();
        serviceHolder.getOverlayNetworkManager().unregister();
        serviceHolder.clear();
    }

    /**
     * Change the network handler used by the system.
     *
     * @param networkHandlerType The network handler type to be used
     */
    public synchronized void changeNetworkHandler(NetworkHandlerType networkHandlerType) {
        serviceHolder.changeNetworkHandler(networkHandlerType);
    }

    /**
     * Change the network handler used by the system.
     *
     * @param routingStrategyType The network handler type to be used
     */
    public synchronized void changeRoutingStrategy(RoutingStrategyType routingStrategyType) {
        serviceHolder.changeRoutingStrategy(routingStrategyType);
    }

    /**
     * Query the file sharer system for a file.
     *
     * @param fileName The name of the file to be queried for
     */
    public void query(String fileName) {
        serviceHolder.getQueryManager().query(fileName);
    }
}
