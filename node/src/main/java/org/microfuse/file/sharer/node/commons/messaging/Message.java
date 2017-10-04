package org.microfuse.file.sharer.node.commons.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        // Skipping the first two words and fetching the message type
        Message parsedMessage = new Message();
        int i = 0;
        int currentDataStartIndex = 0;
        for (int j = 0; j < 2; j++) {
            while (messageString.charAt(i) != ' ') {
                i++;
            }
            i++;
            if (currentDataStartIndex == 0) {
                currentDataStartIndex = i;
            }
        }
        parsedMessage.setType(MessageType.parseMessageType(messageString.substring(currentDataStartIndex, i - 1)));
        currentDataStartIndex = i;

        // Fetching the data
        boolean isInsideQuote = false;
        List<String> data = new ArrayList<>();
        while (i < messageString.length()) {
            if (messageString.charAt(i) == '"') {
                if (isInsideQuote) {
                    data.add(messageString.substring(currentDataStartIndex, i));
                    if (i + 1 < messageString.length() && messageString.charAt(i + 1) == ' ') {
                        i++;
                    }
                }
                currentDataStartIndex = i + 1;
                isInsideQuote = !isInsideQuote;
            } else if (messageString.charAt(i) == ' ' && !isInsideQuote) {
                data.add(messageString.substring(currentDataStartIndex, i));
                currentDataStartIndex = i + 1;
            }
            i++;
        }

        // Adding the last data item
        if (currentDataStartIndex < i) {
            data.add(messageString.substring(currentDataStartIndex, i));
        }

        parsedMessage.setData(data);
        return parsedMessage;
    }

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder(type.getValue());
        for (String aData : data) {
            message.append(" ");
            if (aData.contains(" ")) {
                message.append("\"");
            }
            message.append(aData);
            if (aData.contains(" ")) {
                message.append("\"");
            }
        }
        return String.format("%04d", message.length() + 5) + " " + message.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof Message) {
            Message messageObject = (Message) object;
            return Objects.equals(messageObject.toString(), toString());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
