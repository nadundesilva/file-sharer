package org.microfuse.node.core.communication.routing.strategy;

import org.microfuse.node.commons.Node;
import org.microfuse.node.commons.messaging.Message;
import org.microfuse.node.core.communication.routing.RoutingTable;

import java.util.List;

/**
 * Routing Strategy based on flooding.
 *
 * Floods received messages to all the connected nodes.
 */
public class FloodingRoutingStrategy implements RoutingStrategy {
    @Override
    public String getName() {
        return RoutingStrategyType.FLOODING.getValue();
    }

    @Override
    public List<Node> getForwardingNode(RoutingTable routingTable, Node fromNode, Message message) {
        List<Node> routingTableNodes = routingTable.getAllRoutingTableNodes();
        routingTableNodes.remove(fromNode);
        return routingTableNodes;
    }
}
