package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.commons.tracing.TracingMode;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.tracing.Network;
import org.microfuse.file.sharer.node.core.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Tracing Manager.
 */
public class TraceManager implements Tracer {
    private static final Logger logger = LoggerFactory.getLogger(TraceManager.class);

    private ServiceHolder serviceHolder;
    private TracingMode mode;
    private Network network;

    public TraceManager(ServiceHolder serviceHolder) {
        this.serviceHolder = serviceHolder;
        mode = TracingMode.OFF;
        network = new Network();
    }

    /**
     * Change the tracing mode of this node.
     *
     * @param mode The new tracing mode to be used.
     */
    public void changeMode(TracingMode mode) {
        if (this.mode != mode) {
            logger.debug("Changed the tracing mode to " + mode.getValue());
            this.mode = mode;

            // Starting/stopping Java RMI serving Tracer
            if (mode == TracingMode.TRACER) {
                startServingTracer();
            } else {
                stopServingTracer();
            }

            // Register or unregister in the tracer
            Tracer tracer = getTracerReference();
            if (tracer != null) {
                tracer.register(
                        serviceHolder.getConfiguration().getIp(),
                        serviceHolder.getConfiguration().getPeerListeningPort(),
                        serviceHolder.getRouter().getRoutingTable()
                );
            }
        }
    }

    /**
     * Get the current tracing mode.
     *
     * @return The current tracing mode
     */
    public TracingMode getMode() {
        return mode;
    }

    /**
     * Get the network that is being traced.
     *
     * @return The network being traced
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Get a tracer reference.
     * Returns null if the tracing mode of this node is not traceable.
     *
     * @return The tracer RMI reference
     */
    public Tracer getTracerReference() {
        Tracer tracer = null;
        if (mode == TracingMode.TRACEABLE) {
            try {
                Registry registry = LocateRegistry.getRegistry(
                        serviceHolder.getConfiguration().getTracerServeIP(),
                        serviceHolder.getConfiguration().getTracerServePort()
                );
                tracer = (Tracer) registry.lookup(Constants.TRACER_REGISTRY_ENTRY);
            } catch (Exception e) {
                logger.warn("Failed to get hold of the tracer stub", e);
            }
        } else {
            logger.debug("Ignored request to get tracer reference since this is not a traceable node");
        }
        return tracer;
    }

    /**
     * Start serving tracer in the RMI registry.
     */
    private void startServingTracer() {
        logger.debug("Starting tracer listening");
        if (mode == TracingMode.TRACER) {
            try {
                Remote stub = UnicastRemoteObject.exportObject(
                        this, serviceHolder.getConfiguration().getTracerServePort());
                Registry registry = LocateRegistry.getRegistry();
                registry.bind(Constants.TRACER_REGISTRY_ENTRY, stub);
                logger.debug("Bind RMI registry item " + Constants.TRACER_REGISTRY_ENTRY + " with object from class "
                        + stub.getClass());
            } catch (Exception e) {
                logger.warn("Failed to serve the RMI remote", e);
            }
        } else {
            logger.debug("Ignored request to serve tracer since this is not a tracer");
        }
    }

    /**
     * Stop serving tracer in the RMI registry.
     */
    private void stopServingTracer() {
        logger.debug("Stopping tracer listening");
        if (mode == TracingMode.TRACER) {
            try {
                Registry registry = LocateRegistry.getRegistry();
                registry.unbind(Constants.TRACER_REGISTRY_ENTRY);
                logger.debug("Unbind RMI registry item " + Constants.TRACER_REGISTRY_ENTRY);
            } catch (Exception e) {
                logger.warn("Failed to stop serving tracer");
            }
        } else {
            logger.debug("Ignored request to stop serve tracer since this is not a tracer");
        }
    }

    @Override
    public void register(String ip, int port, RoutingTable currentRoutingTable) {
        currentRoutingTable.getAllUnstructuredNetworkNodes().forEach(node ->
                network.addUnstructuredNetworkConnection(ip, port, node.getIp(), node.getPort()));

        if (currentRoutingTable instanceof SuperPeerRoutingTable) {
            network.getNode(ip, port).setPeerType(PeerType.SUPER_PEER);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) currentRoutingTable;

            superPeerRoutingTable.getAllAssignedOrdinaryNetworkNodes().forEach(node ->
                    network.addAssignedOrdinaryPeerConnection(ip, port, node.getIp(), node.getPort()));

            superPeerRoutingTable.getAllSuperPeerNetworkNodes().forEach(node ->
                    network.addSuperPeerNetworkConnection(ip, port, node.getIp(), node.getPort()));
        } else {
            network.getNode(ip, port).setPeerType(PeerType.ORDINARY_PEER);
        }
        logger.debug("Registered new node " + ip + ":" + "port");
    }

    @Override
    public void addUnstructuredNetworkConnection(String ip1, int port1, String ip2, int port2) {
        network.addUnstructuredNetworkConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void addSuperPeerNetworkConnection(String ip1, int port1, String ip2, int port2) {
        network.addSuperPeerNetworkConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void addAssignedOrdinaryPeerConnection(String ip1, int port1, String ip2, int port2) {
        network.addAssignedOrdinaryPeerConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void removeUnstructuredNetworkConnection(String ip1, int port1, String ip2, int port2) {
        network.removeUnstructuredNetworkConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void removeSuperPeerNetworkConnection(String ip1, int port1, String ip2, int port2) {
        network.removeSuperPeerNetworkConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void removeAssignedOrdinaryPeerConnection(String ip1, int port1, String ip2, int port2) {
        network.removeAssignedOrdinaryPeerConnection(ip1, port1, ip2, port2);
    }
}
