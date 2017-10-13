package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param node The super peer to be assigned to this node
     */
    public void setAssignedSuperPeer(Node node) {
        this.assignedSuperPeer = node;
        if (node != null) {
            logger.debug("Changed assigned super peer to " + node.toString());
        } else {
            logger.debug("Removed assigned super peer");
        }
    }

    @Override
    public boolean removeFromAll(Node node) {
        boolean isSuccessful = super.removeFromAll(node);
        if (node.equals(getAssignedSuperPeer())) {
            setAssignedSuperPeer(null);
        }
        return removeUnstructuredNetworkRoutingTableEntry(node) || isSuccessful;
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
        if (requestedNode == null && Objects.equals(assignedSuperPeer.getIp(), ip) &&
                assignedSuperPeer.getPort() == port) {
            requestedNode = assignedSuperPeer;
        }
        return requestedNode;
    }

    @Override
    public void clear() {
        super.clear();
        assignedSuperPeer = null;
    }
}
