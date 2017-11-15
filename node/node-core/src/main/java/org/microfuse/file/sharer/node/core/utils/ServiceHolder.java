package org.microfuse.file.sharer.node.core.utils;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.commons.tracing.TraceableState;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.TCPSocketNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredFloodingRoutingStrategy;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.microfuse.file.sharer.node.core.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ServiceHolder singleton class.
 * <p>
 * This holds different types of services and implements methods for managing them.
 * This also holds the configuration and the peer type.
 */
public class ServiceHolder {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHolder.class);

    private PeerType peerType;
    private TraceableState traceableState;
    private Configuration configuration;
    private Router router;
    private ResourceIndex resourceIndex;
    private OverlayNetworkManager overlayNetworkManager;
    private QueryManager queryManager;
    private TraceManager traceManager;

    private Lock peerTypeLock;
    private Lock traceableStateLock;
    private Lock configurationLock;
    private Lock routerLock;
    private Lock resourceIndexLock;
    private Lock overlayNetworkManagerLock;
    private Lock queryManagerLock;
    private Lock traceManagerLock;

    private Thread automatedGarbageCollectionThread;
    private boolean automatedGarbageCollectionEnabled;

    private Lock automatedGarbageCollectionLock;

    public ServiceHolder() {
        peerTypeLock = new ReentrantLock();
        traceableStateLock = new ReentrantLock();
        configurationLock = new ReentrantLock();
        routerLock = new ReentrantLock();
        resourceIndexLock = new ReentrantLock();
        overlayNetworkManagerLock = new ReentrantLock();
        queryManagerLock = new ReentrantLock();
        traceManagerLock = new ReentrantLock();

        automatedGarbageCollectionEnabled = false;
        automatedGarbageCollectionLock = new ReentrantLock();
    }

    /**
     * Enable automated garbage collection.
     */
    public void enableAutomatedGarbageCollection() {
        automatedGarbageCollectionLock.lock();
        try {
            if (!automatedGarbageCollectionEnabled) {
                automatedGarbageCollectionEnabled = true;
                automatedGarbageCollectionThread = new Thread(() -> {
                    while (automatedGarbageCollectionEnabled) {
                        try {
                            Thread.sleep(getConfiguration().getAutomatedGarbageCollectionInterval());
                        } catch (InterruptedException ignored) {
                        }
                        collectGarbage();
                    }
                    logger.info("Stopped gossiping");
                });
                automatedGarbageCollectionThread.setPriority(Thread.MIN_PRIORITY);
                automatedGarbageCollectionThread.setDaemon(true);
                automatedGarbageCollectionThread.start();
                logger.info("Started automated garbage collection");
            }
        } finally {
            automatedGarbageCollectionLock.unlock();
        }
    }

    /**
     * Disable automated garbage collection.
     */
    public void disableAutomatedGarbageCollection() {
        automatedGarbageCollectionLock.lock();
        try {
            if (automatedGarbageCollectionEnabled) {
                automatedGarbageCollectionEnabled = false;
                if (automatedGarbageCollectionThread != null) {
                    automatedGarbageCollectionThread.interrupt();
                }
            }
        } finally {
            automatedGarbageCollectionLock.unlock();
        }
    }

    /**
     * Collect garbage objects.
     */
    public void collectGarbage() {
        logger.info("Collecting routing table garbage");
        routerLock.lock();
        try {
            if (router != null) {
                router.getRoutingTable().collectGarbage();
            }
        } finally {
            routerLock.unlock();
        }

        logger.info("Collecting resource index garbage");
        resourceIndexLock.lock();
        try {
            if (resourceIndex != null) {
                resourceIndex.collectGarbage();
            }
        } finally {
            resourceIndexLock.unlock();
        }

        logger.info("Collecting routing strategy garbage");
        routerLock.lock();
        try {
            if (router != null) {
                router.getRoutingStrategy().collectGarbage();
            }
        } finally {
            routerLock.unlock();
        }
    }

    /**
     * Promote the current node to an ordinary peer.
     */
    public void promoteToSuperPeer() {
        peerTypeLock.lock();
        try {
            peerType = PeerType.SUPER_PEER;
        } finally {
            peerTypeLock.unlock();
        }

        resourceIndexLock.lock();
        try {
            ResourceIndex resourceIndex = getResourceIndex();
            if (!(resourceIndex instanceof SuperPeerResourceIndex)) {
                this.resourceIndex = new SuperPeerResourceIndex(this, resourceIndex);
            }
        } finally {
            resourceIndexLock.unlock();
        }

        routerLock.lock();
        try {
            getRouterInstance().promoteToSuperPeer();
        } finally {
            routerLock.unlock();
        }
        logger.info("Promoted to super peer");

        // Notifying the tracer
        Tracer tracer = getTracer();
        if (tracer != null) {
            try {
                tracer.promoteToSuperPeer(
                        System.currentTimeMillis(),
                        getConfiguration().getIp(), getConfiguration().getPeerListeningPort()
                );
            } catch (RemoteException e) {
                logger.warn("Failed to notify tracer of the promotion", e);
            }
        }
    }

    /**
     * Demote the current node to a super peer.
     */
    public void demoteToOrdinaryPeer() {
        peerTypeLock.lock();
        try {
            peerType = PeerType.ORDINARY_PEER;
        } finally {
            peerTypeLock.unlock();
        }

        resourceIndexLock.lock();
        try {
            ResourceIndex resourceIndex = getResourceIndex();
            if (resourceIndex instanceof SuperPeerResourceIndex) {
                this.resourceIndex = new ResourceIndex(this, (SuperPeerResourceIndex) resourceIndex);
            }
        } finally {
            resourceIndexLock.unlock();
        }

        routerLock.lock();
        try {
            getRouterInstance().demoteToOrdinaryPeer();
        } finally {
            routerLock.unlock();
        }
        logger.info("Demoted to ordinary peer");

        // Notifying the tracer
        Tracer tracer = getTracer();
        if (tracer != null) {
            try {
                tracer.demoteToOrdinaryPeer(
                        System.currentTimeMillis(),
                        getConfiguration().getIp(), getConfiguration().getPeerListeningPort()
                );
            } catch (RemoteException e) {
                logger.warn("Failed to notify tracer of the demotion", e);
            }
        }
    }

    /**
     * Clear all the stored services.
     */
    public void clear() {
        routerLock.lock();
        try {
            if (router != null) {
                router.shutdown();
            }
            router = null;
            logger.info("Cleared router");
        } finally {
            routerLock.unlock();
        }

        overlayNetworkManagerLock.lock();
        try {
            if (overlayNetworkManager != null) {
                overlayNetworkManager.cancelSearchForSuperPeer();
            }
            overlayNetworkManager = null;
            logger.info("Cleared overlay network manager");
        } finally {
            overlayNetworkManagerLock.unlock();
        }

        peerTypeLock.lock();
        try {
            peerType = null;
            logger.info("Cleared peer type");
        } finally {
            peerTypeLock.unlock();
        }

        traceableStateLock.lock();
        try {
            traceableState = null;
            logger.info("Cleared traceable state");
        } finally {
            traceableStateLock.unlock();
        }

        configurationLock.lock();
        try {
            saveConfigurationToFile(configuration);
            configuration = null;
            logger.info("Cleared configuration");
        } finally {
            configurationLock.unlock();
        }

        resourceIndexLock.lock();
        try {
            resourceIndex = null;
            logger.info("Cleared resource index");
        } finally {
            resourceIndexLock.unlock();
        }

        queryManagerLock.lock();
        try {
            queryManager = null;
            logger.info("Cleared query manager");
        } finally {
            queryManagerLock.unlock();
        }

        traceManagerLock.lock();
        try {
            if (traceManager != null) {
                traceManager.shutdown();
            }
            traceManager = null;
            logger.info("Cleared trace manager");
        } finally {
            traceManagerLock.unlock();
        }
    }

    /**
     * Get a singleton instance of this nodes configuration.
     * This is not a singleton.
     * However this is the instance used by all classes in the file sharer.
     *
     * @return The configuration of this node
     */
    public Configuration getConfiguration() {
        configurationLock.lock();
        try {
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
                    saveConfigurationToFile(configuration);
                }
            }
            return configuration;
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Returns the resource index instance used by this node.
     * This is not a singleton.
     * However this is the instance used by all classes in the file sharer.
     *
     * @return The resource index instance
     */
    public ResourceIndex getResourceIndex() {
        resourceIndexLock.lock();
        try {
            if (resourceIndex == null) {
                try {
                    resourceIndex = ResourceIndex.getResourceIndexClass(getPeerType())
                            .getConstructor(ServiceHolder.class).newInstance(this);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                        InvocationTargetException e) {
                    logger.error("Failed to instantiate resource index for " + getPeerType().getValue()
                            + ". Using resource index for " + PeerType.ORDINARY_PEER.getValue() + " instead.", e);
                    resourceIndex = new ResourceIndex(this);
                }
            }
            return resourceIndex;
        } finally {
            resourceIndexLock.unlock();
        }
    }

    /**
     * Get the bootstrapping manager singleton instance.
     * This is not a singleton.
     * However this is the instance used by all classes in the file sharer.
     *
     * @return The Bootstrapping Manager
     */
    public OverlayNetworkManager getOverlayNetworkManager() {
        overlayNetworkManagerLock.lock();
        try {
            if (overlayNetworkManager == null) {
                overlayNetworkManager = new OverlayNetworkManager(this);
            }
            return overlayNetworkManager;
        } finally {
            overlayNetworkManagerLock.unlock();
        }
    }

    /**
     * Get the query manager singleton instance.
     * This is not a singleton.
     * However this is the instance used by all classes in the file sharer.
     *
     * @return The Query Manager
     */
    public QueryManager getQueryManager() {
        queryManagerLock.lock();
        try {
            if (queryManager == null) {
                queryManager = new QueryManager(this);
            }
            return queryManager;
        } finally {
            queryManagerLock.unlock();
        }
    }

    /**
     * Return the peer type of this node.
     *
     * @return The peer type of the node
     */
    public PeerType getPeerType() {
        peerTypeLock.lock();
        try {
            if (peerType == null) {
                peerType = PeerType.ORDINARY_PEER;
            }
            return peerType;
        } finally {
            peerTypeLock.unlock();
        }
    }

    /**
     * Get a singleton instance of the router used by this node.
     * This is not a singleton.
     * However this is the instance used by all classes in the file sharer.
     *
     * @return The router used by this node
     */
    public Router getRouter() {
        routerLock.lock();
        try {
            return getRouterInstance();
        } finally {
            routerLock.unlock();
        }
    }

    /**
     * Update the configuration used by the file sharer.
     *
     * @param configuration The configuration to be used
     */
    public void updateConfiguration(Configuration configuration) {
        configurationLock.lock();
        try {
            if (configuration != null) {
                // Checking if the network handler needs to be replaced
                boolean networkHandlerReplaceRequired = false;
                if (configuration.getNetworkHandlerType() != this.configuration.getNetworkHandlerType() ||
                        configuration.getPeerListeningPort() != this.configuration.getPeerListeningPort() ||
                        configuration.getNetworkHandlerThreadCount()
                                != this.configuration.getNetworkHandlerThreadCount()) {
                    networkHandlerReplaceRequired = true;
                }

                // Checking if the routing strategy needs to be replaced
                boolean routingStrategyReplaceRequired = false;
                if (configuration.getRoutingStrategyType() != this.configuration.getRoutingStrategyType()) {
                    routingStrategyReplaceRequired = true;
                }

                this.configuration = configuration;

                // Changing the relevant component based on the new configuration
                if (networkHandlerReplaceRequired) {
                    getRouter().changeNetworkHandler(instantiateNetworkHandler());
                }
                if (routingStrategyReplaceRequired) {
                    getRouter().changeRoutingStrategy(instantiateRoutingStrategy());
                }

                saveConfigurationToFile(configuration);
            }
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Save the configuration used by this file sharer.
     */
    public void saveConfiguration() {
        configurationLock.lock();
        try {
            saveConfigurationToFile(configuration);
        } finally {
            configurationLock.unlock();
        }
    }

    /**
     * Get a tracer reference.
     * Returns null if the tracing mode of this node is not traceable.
     *
     * @return The tracer RMI reference
     */
    public Tracer getTracer() {
        Tracer tracer = null;
        if (getTraceableState() == TraceableState.TRACEABLE) {
            try {
                Registry tracerRegistry = LocateRegistry.getRegistry(
                        getConfiguration().getTracerIP(),
                        Constants.RMI_REGISTRY_PORT
                );
                tracer = (Tracer) tracerRegistry.lookup(Constants.RMI_REGISTRY_ENTRY_TRACER);
            } catch (NotBoundException | RemoteException e) {
                logger.warn("Failed to get hold of the tracer stub", e);
            }
        } else {
            logger.info("Ignored request to get tracer reference since this is not a traceable node");
        }
        return tracer;
    }

    /**
     * Change the tracing mode of this node.
     *
     * @param traceableState The new tracing mode to be used.
     */
    public void changeTraceableState(TraceableState traceableState) {
        if (getTraceableState() != traceableState) {
            this.traceableState = traceableState;

            // Register in the tracer
            Tracer tracer = getTracer();
            if (tracer != null) {
                traceManagerLock.lock();
                try {
                    getTraceManager().start();
                } finally {
                    traceManagerLock.unlock();
                }
                try {
                    tracer.register(
                            System.currentTimeMillis(),
                            getConfiguration().getIp(), getConfiguration().getPeerListeningPort(),
                            getRouter().getRoutingTable()
                    );
                } catch (RemoteException e) {
                    logger.warn("Failed to register node in tracer", e);
                }
            } else {
                traceManagerLock.lock();
                try {
                    getTraceManager().shutdown();
                } finally {
                    traceManagerLock.unlock();
                }
            }
            logger.info("Changed the traceable state to " + traceableState.getValue());
        }
    }

    /**
     * Get the current tracing mode.
     *
     * @return The current tracing mode
     */
    public TraceableState getTraceableState() {
        traceableStateLock.lock();
        try {
            if (traceableState == null) {
                traceableState = TraceableState.OFF;
            }
            return traceableState;
        } finally {
            traceableStateLock.unlock();
        }
    }

    /**
     * Instantiate network handler based on configuration.
     *
     * @return The network handler
     */
    private NetworkHandler instantiateNetworkHandler() {
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
    private RoutingStrategy instantiateRoutingStrategy() {
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

    /**
     * Save the current configuration to file.
     */
    private void saveConfigurationToFile(Configuration configuration) {
        File configFile = new File(NodeConstants.CONFIG_FILE);
        try {
            if (!configFile.createNewFile()) {
                try {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    Files.write(gson.toJson(configuration).getBytes(
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

    /**
     * Get the router instance.
     *
     * @return The router instance
     */
    private Router getRouterInstance() {
        if (router == null) {
            router = new Router(instantiateNetworkHandler(), instantiateRoutingStrategy(), this);
        }
        return router;
    }

    /**
     * Get the trace manager instance.
     *
     * @return The trace manager instance
     */
    private TraceManager getTraceManager() {
        traceManagerLock.lock();
        try {
            if (traceManager == null) {
                traceManager = new TraceManager(this);
            }
            return traceManager;
        } finally {
            traceManagerLock.unlock();
        }
    }
}
