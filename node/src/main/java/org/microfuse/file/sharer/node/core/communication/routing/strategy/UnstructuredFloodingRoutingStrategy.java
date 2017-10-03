package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;

import java.util.Set;

/**
 * Routing Strategy based on flooding the unstructured network.
 * <p>
 * Floods received messages to all the connected nodes.
 */
public class UnstructuredFloodingRoutingStrategy implements RoutingStrategy {
    @Override
    public String getName() {
        return RoutingStrategyType.UNSTRUCTURED_FLOODING.getValue();
    }

    @Override
    public Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        Set<Node> routingTableNodes = routingTable.getAllUnstructuredNetworkRoutingTableNodes();
        routingTableNodes.remove(fromNode);
        return routingTableNodes;
    }
}
