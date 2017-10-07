package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Routing strategy based on randomly walking the unstructured network.
 * <p>
 * Randomly selects a node from a node's neighbours to route the message to.
 */
public class UnstructuredRandomWalkRoutingStrategy implements RoutingStrategy {
    @Override
    public String getName() {
        return RoutingStrategyType.UNSTRUCTURED_RANDOM_WALK.getValue();
    }

    @Override
    public Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        List<Node> routingTableNodes = new ArrayList<>(routingTable.getAllUnstructuredNetworkRoutingTableNodes());
        if (fromNode != null) {
            routingTableNodes.remove(fromNode);
        }
        int forwardNodeIndex = ThreadLocalRandom.current().nextInt(0, routingTableNodes.size() - 1);
        return new HashSet<>(Collections.singletonList(routingTableNodes.get(forwardNodeIndex)));
    }
}
