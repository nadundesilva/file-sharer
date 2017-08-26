package org.microfuse.node.commons;

/**
 * The node base interface.
 */
public class Node {
    private int nodeID;
    private String address;

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof Node) {
            Node nodeObject = (Node) object;
            return nodeObject.getNodeID() == nodeID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return nodeID;
    }
}
