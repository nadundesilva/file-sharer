package org.microfuse.file.sharer.node.commons.tracing;

import java.util.Objects;

/**
 * A connection in the network.
 */
public class NetworkConnection {
    private TraceableNode node1;
    private TraceableNode node2;

    public NetworkConnection(TraceableNode node1, TraceableNode node2) {
        // To make the connections consistent an order is forced
        if (node1.getPort() < node2.getPort()) {
            this.node1 = node1;
            this.node2 = node2;
        } else if (node1.getPort() > node2.getPort()) {
            this.node1 = node2;
            this.node2 = node1;
        } else {
            String[] node1IPSplit = node1.getIp().split("\\.");
            String[] node2IPSplit = node2.getIp().split("\\.");

            for (int i = 0; i < node1IPSplit.length; i++) {
                int node1IPSplitItem = Integer.parseInt(node1IPSplit[i]);
                int node2IPSplitItem = Integer.parseInt(node2IPSplit[i]);
                if (node1IPSplitItem < node2IPSplitItem) {
                    this.node1 = node1;
                    this.node2 = node2;
                } else if (node1IPSplitItem > node2IPSplitItem) {
                    this.node1 = node2;
                    this.node2 = node1;
                }
            }

            if (this.node1 == null && this.node2 == null) {
                this.node1 = node1;
                this.node2 = node2;
            }
        }
    }

    public TraceableNode getNode1() {
        return node1;
    }

    public TraceableNode getNode2() {
        return node2;
    }

    @Override
    public String toString() {
        return node1.toString() + "<=>" + node2.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof NetworkConnection) {
            NetworkConnection networkConnectionObject = (NetworkConnection) object;
            if (Objects.equals(networkConnectionObject.getNode1(), this.getNode1())) {
                return Objects.equals(networkConnectionObject.getNode2(), this.getNode2());
            } else {
                return Objects.equals(networkConnectionObject.getNode1(), this.getNode2())
                        && Objects.equals(networkConnectionObject.getNode2(), this.getNode1());
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
