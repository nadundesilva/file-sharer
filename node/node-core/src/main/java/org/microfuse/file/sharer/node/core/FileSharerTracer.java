package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeState;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.tracing.Network;
import org.microfuse.file.sharer.node.core.tracing.Tracer;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Tracing Manager.
 */
public class FileSharerTracer implements Tracer {
    private static final Logger logger = LoggerFactory.getLogger(FileSharerTracer.class);

    private ServiceHolder serviceHolder;
    private Network network;

    private Registry registry;

    public FileSharerTracer() {
        serviceHolder = new ServiceHolder();
        network = new Network();

        // Starting the RMI registry. Fails if it is already running.
        try {
            LocateRegistry.createRegistry(Constants.RMI_REGISTRY_PORT);
        } catch (RemoteException e) {
            logger.warn("RMI registry already exists at port "
                    + Constants.RMI_REGISTRY_PORT, e);
        }

        // Retrieving reference to RMI registry
        try {
            registry = LocateRegistry.getRegistry(
                    serviceHolder.getConfiguration().getTracerIP(),
                    Constants.RMI_REGISTRY_PORT
            );
        } catch (RemoteException e) {
            logger.warn("Failed to fetch RMI registry", e);
        }
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
     * Start serving tracer in the RMI registry.
     */
    public void start() {
        System.setProperty(Constants.RMI_HOME_SYSTEM_PROPERTY, serviceHolder.getConfiguration().getTracerIP());

        try {
            Remote stub = UnicastRemoteObject.exportObject(
                    this, serviceHolder.getConfiguration().getTracerPort());
            registry.rebind(Constants.RMI_REGISTRY_ENTRY_TRACER, stub);
            logger.info("Bind RMI registry item " + Constants.RMI_REGISTRY_ENTRY_TRACER
                    + " with object from class " + this.getClass());
        } catch (RemoteException e) {
            logger.warn("Failed to serve the RMI remote", e);
        }
        logger.info("Started tracer listening");
    }

    /**
     * Stop serving tracer in the RMI registry.
     */
    public void shutdown() {
        logger.info("Stopping tracer listening");

        // Removing this object from the RMI registry
        try {
            registry.unbind(Constants.RMI_REGISTRY_ENTRY_TRACER);
            logger.info("Unbind RMI registry item " + Constants.RMI_REGISTRY_ENTRY_TRACER);
        } catch (NotBoundException | RemoteException e) {
            logger.warn("Failed to stop serving tracer");
        }

        // Un-exporting this object
        try {
            while (UnicastRemoteObject.unexportObject(this, false)) { }
            logger.info("Un-exported object");
        } catch (NoSuchObjectException e) {
            logger.warn("Failed to un-export object", e);
        }
    }

    @Override
    public void register(String ip, int port, RoutingTable currentRoutingTable) {
        network.getNode(ip, port).setState(NodeState.ACTIVE);

        currentRoutingTable.getAllUnstructuredNetworkNodes().forEach(node ->
                network.addUnstructuredNetworkConnection(ip, port, node.getIp(), node.getPort()));

        if (currentRoutingTable instanceof SuperPeerRoutingTable) {
            network.getNode(ip, port).setPeerType(PeerType.SUPER_PEER);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) currentRoutingTable;

            superPeerRoutingTable.getAllAssignedOrdinaryNetworkNodes().forEach(node ->
                    network.addAssignedOrdinaryPeerConnection(ip, port, node.getIp(), node.getPort()));

            superPeerRoutingTable.getAllSuperPeerNetworkNodes().forEach(node ->
                    network.addSuperPeerNetworkConnection(ip, port, node.getIp(), node.getPort()));
        } else if (currentRoutingTable instanceof OrdinaryPeerRoutingTable) {
            network.getNode(ip, port).setPeerType(PeerType.ORDINARY_PEER);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) currentRoutingTable;

            Node node = ordinaryPeerRoutingTable.getAssignedSuperPeer();
            network.addAssignedOrdinaryPeerConnection(ip, port, node.getIp(), node.getPort());
        } else {
            logger.warn("Unknown routing table type");
        }
        logger.info("Registered new node " + ip + ":" + "port");
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

    @Override
    public void promoteToSuperPeer(String ip, int port) {
        network.getNode(ip, port).setPeerType(PeerType.SUPER_PEER);
    }

    @Override
    public void demoteToOrdinaryPeer(String ip, int port) {
        network.getNode(ip, port).setPeerType(PeerType.ORDINARY_PEER);
    }
}
