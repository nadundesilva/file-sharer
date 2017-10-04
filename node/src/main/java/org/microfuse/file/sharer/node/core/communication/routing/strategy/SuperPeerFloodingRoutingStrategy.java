package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.Manager;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.microfuse.file.sharer.node.core.utils.MessageIndexes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Routing Strategy based on flooding the super peer network.
 * <p>
 * Floods received messages to all the connected nodes.
 */
public class SuperPeerFloodingRoutingStrategy implements RoutingStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerFloodingRoutingStrategy.class);

    @Override
    public String getName() {
        return RoutingStrategyType.SUPER_PEER_FLOODING.getValue();
    }

    @Override
    public Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        Set<Node> forwardingNodes = null;
        if (routingTable instanceof SuperPeerRoutingTable) {
            // Searching the aggregate index
            Set<AggregatedResource> resources = ((SuperPeerResourceIndex) Manager.getResourceIndex())
                    .findAggregatedResources(message.getData(MessageIndexes.SER_FILE_NAME));

            // Picking a node with a matching resource
            if (resources != null && resources.size() > 0) {
                forwardingNodes = new HashSet<>();
                for (AggregatedResource resource : resources) {
                    forwardingNodes.addAll(resource.getAllNodes());
                }
            }

            if (forwardingNodes == null) {
                // Random walking the super-peer network
                forwardingNodes = ((SuperPeerRoutingTable) routingTable).getAllSuperPeerNetworkRoutingTableNodes();
            }
        } else if (routingTable instanceof OrdinaryPeerRoutingTable) {
            Node superPeer = ((OrdinaryPeerRoutingTable) routingTable).getAssignedSuperPeer();
            if (superPeer != null && superPeer.isAlive()) {
                // Passing over to the super peer network
                forwardingNodes = new HashSet<>(Collections.singletonList(superPeer));
            } else {
                // Random walking the unstructured network
                forwardingNodes = routingTable.getAllUnstructuredNetworkRoutingTableNodes();
            }
        } else {
            logger.warn("The node is recognized as a super-peer, " +
                    "but does not contain a super-peer routing table");
        }
        if (forwardingNodes != null) {
            forwardingNodes.remove(fromNode);
        }
        return forwardingNodes;
    }
}
