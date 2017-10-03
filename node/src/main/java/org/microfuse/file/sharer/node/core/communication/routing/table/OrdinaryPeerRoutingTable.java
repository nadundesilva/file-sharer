package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param assignedSuperPeer The super peer to be assigned to this node
     */
    public void setAssignedSuperPeer(Node assignedSuperPeer) {
        this.assignedSuperPeer = assignedSuperPeer;
    }
}
