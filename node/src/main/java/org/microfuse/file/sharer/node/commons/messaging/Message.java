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

    public Message() {
        type = MessageType.ERROR;
        data = new ArrayList<>();
    }

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

    public String getData(int index) {
        return data.get(index);
    }

    public void setData(int index, String dataItem) {
        data.remove(index);
        data.add(index, dataItem);
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

    public static Message parse(String messageString) {
        String[] messageSplit = messageString.split("\\s");

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
