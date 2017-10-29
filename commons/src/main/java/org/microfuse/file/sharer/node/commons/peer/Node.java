package org.microfuse.file.sharer.node.commons.peer;

import java.util.Objects;

/**
 * The node base interface.
 */
public class Node {
    private String ip;
    private int port;
    private NodeState state;

    public Node() {
        state = NodeState.ACTIVE;
    }

    public Node(String ip, int port) {
        this();
        this.ip = ip;
        this.port = port;
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

    public void setPort(String port) {
        setPort(Integer.parseInt(port));
    }

    public void setPort(int port) {
        this.port = port;
    }

    public NodeState getState() {
        return state;
    }

    public void setState(NodeState state) {
        this.state = state;
    }

    public boolean isActive() {
        return this.state != NodeState.INACTIVE;
    }

    @Override
    public String toString() {
        return ip + ":" + port;
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof Node) {
            Node nodeObject = (Node) object;
            return Objects.equals(nodeObject.getIp(), this.getIp())
                    && Objects.equals(nodeObject.getPort(), this.getPort());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (ip + ":" + port).hashCode();
    }
}
