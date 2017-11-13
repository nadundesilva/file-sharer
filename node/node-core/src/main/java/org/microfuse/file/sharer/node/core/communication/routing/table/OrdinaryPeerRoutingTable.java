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
    private static final long serialVersionUID = 105L;
    private static final transient Logger logger = LoggerFactory.getLogger(OrdinaryPeerRoutingTable.class);

    private Node assignedSuperPeer;

    public OrdinaryPeerRoutingTable(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    public OrdinaryPeerRoutingTable(ServiceHolder serviceHolder, SuperPeerRoutingTable superPeerRoutingTable) {
        this(serviceHolder);

        // Copying all the unstructured nodes
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
        if (!(Objects.equals(ip, serviceHolder.getConfiguration().getIp())
                && port == serviceHolder.getConfiguration().getPeerListeningPort())) {
            Node node = get(ip, port);
            if (node == null) {
                node = new Node(ip, port);
            }
            setAssignedSuperPeer(node);
        } else {
            logger.info("Dropped request to add self");
        }
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

        // Removing the assigned super peer if it is inactive
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
            logger.info("Adding assigned super peer to " + node.toString());
        } else {
            logger.info("Removing assigned super peer");
        }

        // Notifying the tracer
        Tracer tracer = serviceHolder.getTracer();
        if (tracer != null) {
            if (this.assignedSuperPeer != null) {
                try {
                    tracer.removeAssignedOrdinaryPeerConnection(
                            serviceHolder.getConfiguration().getIp(),
                            serviceHolder.getConfiguration().getPeerListeningPort(),
                            this.assignedSuperPeer.getIp(), this.assignedSuperPeer.getPort()
                    );
                } catch (RemoteException e) {
                    logger.warn("Failed to add assigned super peer to the tracer", e);
                }
            }
            if (node != null) {
                try {
                    tracer.addAssignedOrdinaryPeerConnection(
                            serviceHolder.getConfiguration().getIp(),
                            serviceHolder.getConfiguration().getPeerListeningPort(),
                            node.getIp(), node.getPort()
                    );
                } catch (RemoteException e) {
                    logger.warn("Failed to remove assigned super peer from the tracer", e);
                }
            }
        }

        this.assignedSuperPeer = node;
    }
}
