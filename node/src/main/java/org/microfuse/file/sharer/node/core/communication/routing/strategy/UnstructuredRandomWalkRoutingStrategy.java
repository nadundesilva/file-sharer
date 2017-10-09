package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;

import java.util.Set;

/**
 * Routing strategy based on randomly walking the unstructured network.
 * <p>
 * Randomly selects a node from a node's neighbours to route the message to.
 */
public class UnstructuredRandomWalkRoutingStrategy extends RoutingStrategy {
    @Override
    public String getName() {
        return RoutingStrategyType.UNSTRUCTURED_RANDOM_WALK.getValue();
    }

    @Override
    public Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        Set<Node> forwardingNodes = routingTable.getAllUnstructuredNetworkRoutingTableNodes();
        return getRandomNode(forwardingNodes, fromNode);
    }
}
