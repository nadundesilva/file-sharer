package org.microfuse.file.sharer.node.commons.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base message class.
 * Contains all the information required to route a message through the network.
 */
public class Message implements Cloneable {
    private MessageType type;
    private List<String> data;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }

    @Override
    public Message clone() {
        Message clone;
        try {
            clone = (Message) super.clone();
        } catch (CloneNotSupportedException e) {
            clone = new Message();
        }
        clone.setType(type);
        clone.setData(new ArrayList<>(data));
        return clone;
    }

    public static Message parse(String message) {
        String[] messageSplit = message.split("\\s");

        Message parsedMessage = new Message();
        parsedMessage.setType(MessageType.valueOf(messageSplit[1]));
        parsedMessage.setData(Arrays.asList(Arrays.copyOfRange(messageSplit, 2, messageSplit.length)));
        return parsedMessage;
    }

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder(type.getValue()).append(" ");
        for (String dataItem : data) {
            message.append(dataItem);
        }
        return String.format("%04d", message.length() + 5) + " " + message.toString();
    }
}
