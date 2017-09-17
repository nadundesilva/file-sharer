package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.RoutingTable;

import java.util.List;

/**
 * Routing Strategy based on flooding.
 * <p>
 * Floods received messages to all the connected nodes.
 */
public class FloodingRoutingStrategy implements RoutingStrategy {
    @Override
    public String getName() {
        return RoutingStrategyType.FLOODING.getValue();
    }

    @Override
    public List<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        List<Node> routingTableNodes = routingTable.getAllRoutingTableNodes();
        routingTableNodes.remove(fromNode);
        return routingTableNodes;
    }
}
