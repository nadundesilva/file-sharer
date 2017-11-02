package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.peer.Node;
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

/**
 * Routing Strategy based on random walking the super peer network.
 * <p>
 * Randomly selects a node from a node's neighbours to route the message to.
 */
public class SuperPeerRandomWalkRoutingStrategy extends RoutingStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerRandomWalkRoutingStrategy.class);

    public SuperPeerRandomWalkRoutingStrategy(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    @Override
    public String getName() {
        return RoutingStrategyType.SUPER_PEER_RANDOM_WALK.getValue();
    }

    @Override
    public Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        Set<Node> forwardingNodes = null;
        if (routingTable instanceof SuperPeerRoutingTable && resourceIndex instanceof SuperPeerResourceIndex) {
            // Searching the aggregate index
            Set<AggregatedResource> resources = ((SuperPeerResourceIndex) resourceIndex)
                    .findAggregatedResources(message.getData(MessageIndexes.SER_FILE_NAME));

            // Picking a node with a matching resource
            AggregatedResource randomResourceMatch = resources.stream()
                    .findAny()
                    .orElse(null);
            if (randomResourceMatch != null && resources.size() > 0) {
                forwardingNodes = new HashSet<>(randomResourceMatch.getAllNodes());
            }

            if (forwardingNodes == null || forwardingNodes.size() == 0) {
                // Random walking the super-peer network
                forwardingNodes = ((SuperPeerRoutingTable) routingTable).getAllSuperPeerNetworkNodes();
            }
        } else if (routingTable instanceof OrdinaryPeerRoutingTable) {
            forwardingNodes = getAssignedSuperPeer((OrdinaryPeerRoutingTable) routingTable);
        } else {
            logger.warn("Unknown routing table");
        }
        return getRandomNode(forwardingNodes, fromNode);
    }
}
