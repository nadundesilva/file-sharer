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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Routing Strategy based on random walking the super peer network.
 * <p>
 * Randomly selects a node from a node's neighbours to route the message to.
 */
public class SuperPeerRandomWalkRoutingStrategy implements RoutingStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerRandomWalkRoutingStrategy.class);

    @Override
    public String getName() {
        return RoutingStrategyType.SUPER_PEER_RANDOM_WALK.getValue();
    }

    @Override
    public Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        Set<Node> forwardingNodes = null;
        if (routingTable instanceof SuperPeerRoutingTable) {
            // Searching the aggregate index
            Set<AggregatedResource> resources = ((SuperPeerResourceIndex) Manager.getResourceIndex())
                    .findAggregatedResources(message.getData(MessageIndexes.SER_FILE_NAME));

            // Picking a node with a matching resource
            AggregatedResource randomResourceMatch = resources.stream()
                    .findAny()
                    .orElse(null);
            if (randomResourceMatch != null) {
                forwardingNodes = new HashSet<>(Collections.singletonList(
                        randomResourceMatch.getAllNodes().stream().findAny().orElse(null)
                ));
            }

            if (forwardingNodes == null) {
                // Random walking the super-peer network
                forwardingNodes = getRandomNode(
                        ((SuperPeerRoutingTable) routingTable).getAllSuperPeerNetworkRoutingTableNodes(),
                        fromNode
                );
            }
        } else if (routingTable instanceof OrdinaryPeerRoutingTable) {
            Node superPeer = ((OrdinaryPeerRoutingTable) routingTable).getAssignedSuperPeer();
            if (superPeer != null && superPeer.isAlive()) {
                // Passing over to the super peer network
                forwardingNodes = new HashSet<>(Collections.singletonList(superPeer));
            } else {
                // Random walking the unstructured network
                forwardingNodes = getRandomNode(
                        routingTable.getAllUnstructuredNetworkRoutingTableNodes(),
                        fromNode
                );
            }
        } else {
            logger.warn("The node is recognized as a super-peer, " +
                    "but does not contain a super-peer routing table");
        }
        return forwardingNodes;
    }

    /**
     * Get a random node form a set of nodes.
     *
     * @param nodes    The set of nodes to pick from
     * @param fromNode The node which sent the message to this node
     * @return The randomly picked node
     */
    private Set<Node> getRandomNode(Set<Node> nodes, Node fromNode) {
        List<Node> routingTableNodes = new ArrayList<>(nodes);
        if (fromNode != null) {
            routingTableNodes.remove(fromNode);
        }
        int forwardNodeIndex = ThreadLocalRandom.current().nextInt(0, nodes.size() - 1);
        return new HashSet<>(Collections.singletonList(routingTableNodes.get(forwardNodeIndex)));
    }
}