package org.microfuse.file.sharer.node.commons;

/**
 * The node base interface.
 */
public class Node {
    private int nodeID;
    private String ip;
    private int port;

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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
