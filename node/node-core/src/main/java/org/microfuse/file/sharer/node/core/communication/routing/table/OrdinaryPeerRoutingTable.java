package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.tracing.Tracer;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Objects;
import java.util.Set;

/**
 * The routing table containing the node information for ordinary peers.
 * <p>
 * Contains the connections and the previous node in paths the message travels.
 */
public class OrdinaryPeerRoutingTable extends RoutingTable {
    private static final Logger logger = LoggerFactory.getLogger(OrdinaryPeerRoutingTable.class);

    private Node assignedSuperPeer;

    public OrdinaryPeerRoutingTable(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    public OrdinaryPeerRoutingTable(ServiceHolder serviceHolder, SuperPeerRoutingTable superPeerRoutingTable) {
        this(serviceHolder);
        superPeerRoutingTable.getAllUnstructuredNetworkNodes()
                .forEach(this::addUnstructuredNetworkRoutingTableEntry);
    }

    /**
     * Get the super peer assigned to this node.
     *
     * @return The super peer assigned to this node
     */
    public Node getAssignedSuperPeer() {
        return assignedSuperPeer;
    }

    /**
     * Set the assigned super peer for this node.
     *
     * @param ip   The ip of the super peer to be assigned to this node
     * @param port The port of the super peer to be assigned to this node
     */
    public void setAssignedSuperPeer(String ip, int port) {
        Node node = get(ip, port);
        if (node == null) {
            node = new Node(ip, port);
        }
        setAssignedSuperPeer(node);
    }

    @Override
    public boolean removeFromAll(String ip, int port) {
        boolean isSuccessful = super.removeFromAll(ip, port);
        if (new Node(ip, port).equals(getAssignedSuperPeer())) {
            setAssignedSuperPeer(null);
        }
        return removeUnstructuredNetworkRoutingTableEntry(ip, port) || isSuccessful;
    }

    @Override
    public Set<Node> getAll() {
        Set<Node> nodes = super.getAll();
        if (assignedSuperPeer != null) {
            nodes.add(assignedSuperPeer);
        }
        return nodes;
    }

    @Override
    public Node get(String ip, int port) {
        Node requestedNode = super.get(ip, port);
        if (requestedNode == null && assignedSuperPeer != null && Objects.equals(assignedSuperPeer.getIp(), ip) &&
                assignedSuperPeer.getPort() == port) {
            requestedNode = assignedSuperPeer;
        }
        return requestedNode;
    }

    @Override
    public void clear() {
        super.clear();
        setAssignedSuperPeer(null);
    }

    @Override
    public void collectGarbage() {
        super.collectGarbage();
        if (assignedSuperPeer != null && !assignedSuperPeer.isActive()) {
            removeFromAll(assignedSuperPeer.getIp(), assignedSuperPeer.getPort());
        }
    }

    /**
     * Set the assigned super peer for this node.
     *
     * @param node The super peer to be assigned to this node
     */
    private void setAssignedSuperPeer(Node node) {
        if (node != null) {
            logger.debug("Adding assigned super peer to " + node.toString());

            // Notifying the tracer
            Tracer tracer = serviceHolder.getTraceManager().getTracerReference();
            if (tracer != null) {
                try {
                    tracer.addAssignedOrdinaryPeerConnection(
                            serviceHolder.getConfiguration().getIp(),
                            serviceHolder.getConfiguration().getPeerListeningPort(),
                            node.getIp(), node.getPort()
                    );
                } catch (RemoteException e) {
                    logger.warn("Failed to add assigned ordinary peer connection to the tracer", e);
                }
            }
        } else {
            logger.debug("Removing assigned super peer");

            // Notifying the tracer
            Tracer tracer = serviceHolder.getTraceManager().getTracerReference();
            if (tracer != null) {
                try {
                    tracer.removeAssignedOrdinaryPeerConnection(
                            serviceHolder.getConfiguration().getIp(),
                            serviceHolder.getConfiguration().getPeerListeningPort(),
                            assignedSuperPeer.getIp(), assignedSuperPeer.getPort()
                    );
                } catch (RemoteException e) {
                    logger.warn("Failed to remove assigned ordinary peer connection from the tracer", e);
                }
            }
        }
        this.assignedSuperPeer = node;
    }
}
