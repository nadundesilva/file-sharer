package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Routing strategy based on randomly walking the p2p network.
 * <p>
 * Randomly selects a node from a node's neighbours to route the message to.
 */
public class RandomWalkRoutingStrategy implements RoutingStrategy {
    @Override
    public String getName() {
        return RoutingStrategyType.RANDOM_WALK.getValue();
    }

    @Override
    public Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        List<Node> routingTableNodes = new ArrayList<>(routingTable.getAllUnstructuredNetworkRoutingTableNodes());
        routingTableNodes.remove(fromNode);
        int forwardNodeIndex = ThreadLocalRandom.current().nextInt(0, routingTableNodes.size() - 1);
        return new HashSet<>(Collections.singletonList(routingTableNodes.get(forwardNodeIndex)));
    }
}
