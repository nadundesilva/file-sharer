package org.microfuse.node.commons.messaging;

import org.microfuse.node.core.Manager;

import java.util.Stack;

/**
 * Base message class.
 * Contains all the information required to route a message through the network.
 */
public class Message implements Cloneable {
    private int sourceNodeID;
    private int destinationNodeID;
    private MessageType type;
    private Stack<Integer> path;    // Path keys used in each node to store the previous node in the path
    private String content;
    private int timeToLive;

    public Message() {
        timeToLive = Manager.getConfigurationInstance().getStartingTimeToLive();
    }

    public int getSourceNodeID() {
        return sourceNodeID;
    }

    public void setSourceNodeID(int sourceNodeID) {
        this.sourceNodeID = sourceNodeID;
    }

    public int getDestinationNodeID() {
        return destinationNodeID;
    }

    public void setDestinationNodeID(int destinationNodeID) {
        this.destinationNodeID = destinationNodeID;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setPath(Stack<Integer> path) {
        this.path = path;
    }

    public void pushPathNode(int encryptedNodeID) {
        path.push(encryptedNodeID);
    }

    public int popPathNode() {
        return path.pop();
    }

    public int peekPathNode() {
        return path.peek();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    @Override
    public Message clone() {
        Message clone;
        try {
            clone = (Message) super.clone();
        } catch (CloneNotSupportedException e) {
            clone = new Message();
        }

        clone.setSourceNodeID(sourceNodeID);
        clone.setDestinationNodeID(destinationNodeID);
        clone.setType(type);
        clone.setContent(content);
        clone.setTimeToLive(timeToLive);

        Stack<Integer> clonedPath = new Stack<>();
        for (int pathKey : path) {
            clonedPath.push(pathKey);
        }
        clone.setPath(clonedPath);

        return clone;
    }
}
