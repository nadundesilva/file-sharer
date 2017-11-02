package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Routing Strategy based on flooding the unstructured network.
 * <p>
 * Floods received messages to all the connected nodes.
 */
public class UnstructuredFloodingRoutingStrategy extends RoutingStrategy {
    public UnstructuredFloodingRoutingStrategy(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    @Override
    public String getName() {
        return RoutingStrategyType.UNSTRUCTURED_FLOODING.getValue();
    }

    @Override
    public Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message) {
        Set<Node> forwardingNodes = routingTable.getAllUnstructuredNetworkNodes();
        if (fromNode != null) {
            forwardingNodes.remove(fromNode);
        }

        forwardingNodes = forwardingNodes.stream().parallel()
                .filter(node -> node.isActive())
                .collect(Collectors.toSet());

        return new HashSet<>(forwardingNodes);
    }
}
