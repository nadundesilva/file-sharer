package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.communication.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Routing Strategy based on flooding the super peer network.
 * <p>
 * Floods received messages to all the connected nodes.
 */
public class SuperPeerFloodingRoutingStrategy extends RoutingStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerFloodingRoutingStrategy.class);

    public SuperPeerFloodingRoutingStrategy(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    @Override
    public String getName() {
        return RoutingStrategyType.SUPER_PEER_FLOODING.getValue();
    }

    @Override
    public Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        // TODO : Keep track of messages already routed through this node ?
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        Set<Node> forwardingNodes = null;
        if (routingTable instanceof SuperPeerRoutingTable && resourceIndex instanceof SuperPeerResourceIndex) {
            // Searching the aggregate index
            Set<AggregatedResource> resources = ((SuperPeerResourceIndex) resourceIndex)
                    .findAggregatedResources(message.getData(MessageIndexes.SER_QUERY));

            // Picking a node with a matching resource
            if (resources != null && resources.size() > 0) {
                forwardingNodes = new HashSet<>();
                for (AggregatedResource resource : resources) {
                    forwardingNodes.addAll(resource.getAllNodes());
                }
            }

            if (forwardingNodes == null || forwardingNodes.size() == 0) {
                // Random walking the super-peer network
                forwardingNodes = ((SuperPeerRoutingTable) routingTable).getAllSuperPeerNetworkNodes();
            }
        } else if (routingTable instanceof OrdinaryPeerRoutingTable) {
            forwardingNodes = getAssignedSuperPeer((OrdinaryPeerRoutingTable) routingTable);
        } else {
            logger.warn("Unknown routing table and resource index");
        }

        if (forwardingNodes != null) {
            // Removing the node which sent the message to this node
            if (fromNode != null) {
                forwardingNodes.remove(fromNode);
            }
        } else {
            // Adding an empty set since no nodes were selected
            forwardingNodes = new HashSet<>();
        }

        // Removing inactive nodes
        forwardingNodes = forwardingNodes.stream().parallel()
                .filter(Node::isActive)
                .collect(Collectors.toSet());

        // Removing nodes to which this message had been already sent
        forwardingNodes = filterUnCachedNodes(message, fromNode, new HashSet<>(forwardingNodes));

        // Sending through the unstructured network if no nodes are found
        if (forwardingNodes.size() == 0) {
            forwardingNodes = filterUnCachedNodes(message, fromNode, routingTable.getAllUnstructuredNetworkNodes());
        }

        return forwardingNodes;
    }
}
