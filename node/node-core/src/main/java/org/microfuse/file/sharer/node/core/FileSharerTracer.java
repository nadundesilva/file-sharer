package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeState;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.tracing.Network;
import org.microfuse.file.sharer.node.core.tracing.Traceable;
import org.microfuse.file.sharer.node.core.tracing.Tracer;
import org.microfuse.file.sharer.node.core.tracing.stats.History;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.microfuse.file.sharer.node.core.utils.TraceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tracing Manager.
 */
public class FileSharerTracer implements Tracer {
    private static final Logger logger = LoggerFactory.getLogger(FileSharerTracer.class);

    private ServiceHolder serviceHolder;
    private Network network;
    private History history;

    private Registry registry;

    private final Lock heartBeatLock;
    private Thread heartBeatThread;
    private boolean heartBeatingEnabled;

    public FileSharerTracer() {
        heartBeatLock = new ReentrantLock();
        heartBeatingEnabled = false;

        serviceHolder = new ServiceHolder();
        network = new Network();
        history = new History(network);

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
     * Enable heart beating to the nodes.
     */
    public void enableHeartBeating() {
        heartBeatLock.lock();
        try {
            if (!heartBeatingEnabled) {
                heartBeatingEnabled = true;
                heartBeatThread = new Thread(() -> {
                    while (heartBeatingEnabled) {
                        try {
                            Thread.sleep(serviceHolder.getConfiguration().getHeartbeatInterval());
                        } catch (InterruptedException ignored) {
                        }
                        network.getNodes().forEach(traceableNode -> {
                            boolean active = false;
                            try {
                                Traceable traceable = getTraceable(traceableNode.getIp(), traceableNode.getPort());
                                if (traceable != null) {
                                    active = traceable.heartbeat();
                                }
                            } catch (RemoteException e) {
                                logger.warn("Failed to connect to traceable node " + traceableNode.toString());
                            }
                            if (active) {
                                traceableNode.setState(NodeState.ACTIVE);
                            } else {
                                traceableNode.setState(NodeState.INACTIVE);
                            }
                        });
                    }
                    logger.info("Stopped Heart beating");
                });
                heartBeatThread.setPriority(Thread.NORM_PRIORITY);
                heartBeatThread.setDaemon(true);
                heartBeatThread.start();
                logger.info("Started Heart beating");
            }
        } finally {
            heartBeatLock.unlock();
        }
    }

    /**
     * Disable heart beating to the nodes.
     */
    public void disableHeartBeat() {
        heartBeatLock.lock();
        try {
            if (heartBeatingEnabled) {
                heartBeatingEnabled = false;
                if (heartBeatThread != null) {
                    heartBeatThread.interrupt();
                }
            }
        } finally {
            heartBeatLock.unlock();
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
     * Get the history of the network that is being traced.
     *
     * @return The history of the network being traced
     */
    public History getHistory() {
        return history;
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

        enableHeartBeating();
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

        disableHeartBeat();
    }

    /**
     * Get a traceable reference.
     *
     * @return The traceable RMI reference
     */
    private Traceable getTraceable(String ip, int port) {
        Traceable traceable = null;
        try {
            Registry tracerRegistry = LocateRegistry.getRegistry(ip, Constants.RMI_REGISTRY_PORT);
            traceable = (Traceable) tracerRegistry.lookup(TraceManager.getRMIRegistryEntry(
                    serviceHolder.getConfiguration().getRmiRegistryEntryPrefix(), ip, port
            ));
        } catch (NotBoundException | RemoteException e) {
            logger.warn("Failed to get hold of the tracer stub", e);
        }
        return traceable;
    }

    @Override
    public void register(long timeStamp, String ip, int port, RoutingTable currentRoutingTable) {
        network.getNode(ip, port).setState(NodeState.ACTIVE);

        currentRoutingTable.getAllUnstructuredNetworkNodes().forEach(node ->
                network.addUnstructuredNetworkConnection(ip, port, node.getIp(), node.getPort()));

        if (currentRoutingTable instanceof SuperPeerRoutingTable) {
            promoteToSuperPeer(timeStamp, ip, port);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) currentRoutingTable;

            superPeerRoutingTable.getAllAssignedOrdinaryNetworkNodes().forEach(node ->
                    network.addAssignedOrdinaryPeerConnection(ip, port, node.getIp(), node.getPort()));

            superPeerRoutingTable.getAllSuperPeerNetworkNodes().forEach(node ->
                    network.addSuperPeerNetworkConnection(ip, port, node.getIp(), node.getPort()));
        } else if (currentRoutingTable instanceof OrdinaryPeerRoutingTable) {
            demoteToOrdinaryPeer(timeStamp, ip, port);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) currentRoutingTable;

            Node node = ordinaryPeerRoutingTable.getAssignedSuperPeer();
            network.addAssignedOrdinaryPeerConnection(ip, port, node.getIp(), node.getPort());
        } else {
            logger.warn("Unknown routing table type");
        }
        logger.info("Registered new node " + ip + ":" + port);
    }

    @Override
    public void addUnstructuredNetworkConnection(long timeStamp, String ip1, int port1, String ip2, int port2) {
        network.addUnstructuredNetworkConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void addSuperPeerNetworkConnection(long timeStamp, String ip1, int port1, String ip2, int port2) {
        network.addSuperPeerNetworkConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void addAssignedOrdinaryPeerConnection(long timeStamp, String ip1, int port1, String ip2, int port2) {
        network.addAssignedOrdinaryPeerConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void removeUnstructuredNetworkConnection(long timeStamp, String ip1, int port1, String ip2, int port2) {
        network.removeUnstructuredNetworkConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void removeSuperPeerNetworkConnection(long timeStamp, String ip1, int port1, String ip2, int port2) {
        network.removeSuperPeerNetworkConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void removeAssignedOrdinaryPeerConnection(long timeStamp, String ip1, int port1, String ip2, int port2) {
        network.removeAssignedOrdinaryPeerConnection(ip1, port1, ip2, port2);
    }

    @Override
    public void promoteToSuperPeer(long timeStamp, String ip, int port) {
        network.getNode(ip, port).setPeerType(PeerType.SUPER_PEER);
        logger.info("Node " + ip + ":" + port + " promoted to a super peer");
    }

    @Override
    public void demoteToOrdinaryPeer(long timeStamp, String ip, int port) {
        network.getNode(ip, port).setPeerType(PeerType.ORDINARY_PEER);
        logger.info("Node " + ip + ":" + port + " demoted to a ordinary peer");
    }

    @Override
    public void notifyMessageSend(long timeStamp, String ip, int port, String receiverIP,
                                  int receiverPort, Message message) throws RemoteException {
        history.notifyMessageSend(timeStamp, ip, port, receiverIP, receiverPort, message);
    }

    @Override
    public void notifyMessageReceived(long timeStamp, String ip, int port, String senderIP,
                                      int senderPort, Message message) throws RemoteException {
        history.notifyMessageReceived(timeStamp, ip, port, senderIP, senderPort, message);

    }
}
