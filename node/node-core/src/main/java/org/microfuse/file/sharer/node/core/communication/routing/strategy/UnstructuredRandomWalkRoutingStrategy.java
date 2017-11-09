package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;

import java.util.Set;

/**
 * Routing strategy based on randomly walking the unstructured network.
 * <p>
 * Randomly selects a node from a node's neighbours to route the message to.
 */
public class UnstructuredRandomWalkRoutingStrategy extends RoutingStrategy {
    public UnstructuredRandomWalkRoutingStrategy(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    @Override
    public String getName() {
        return RoutingStrategyType.UNSTRUCTURED_RANDOM_WALK.getValue();
    }

    @Override
    public Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        // Getting all the unstructured network nodes
        Set<Node> forwardingNodes = routingTable.getAllUnstructuredNetworkNodes();

        // Selecting a random node
        return getRandomNode(message, forwardingNodes, fromNode);
    }
}
