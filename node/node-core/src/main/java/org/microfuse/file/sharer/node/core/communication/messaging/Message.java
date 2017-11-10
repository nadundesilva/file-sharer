package org.microfuse.file.sharer.node.core.communication.messaging;

import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Message class.
 */
public class Message implements Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(Message.class);

    private MessageType type;
    private List<String> data;

    private static final Character MESSAGE_DATA_SEPARATOR = ' ';

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
        if (index < data.size()) {
            data.remove(index);
        }
        data.add(index, dataItem);
    }

    public void removeData(int index) {
        data.remove(index);
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

    /**
     * Parse a Message object from a string.
     *
     * @param messageString The string which contains the message
     * @return The relevant message objects representing the message string
     */
    public static Message parse(String messageString) {
        Message parsedMessage = null;
        try {
            // Skipping the first two words and fetching the message type
            parsedMessage = new Message();
            int i = 0;
            int currentDataStartIndex = 0;
            for (int j = 0; j < 2; j++) {
                while (messageString.charAt(i) != MESSAGE_DATA_SEPARATOR) {
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
                        if (i + 1 < messageString.length() && messageString.charAt(i + 1) == MESSAGE_DATA_SEPARATOR) {
                            i++;
                        }
                    }
                    currentDataStartIndex = i + 1;
                    isInsideQuote = !isInsideQuote;
                } else if (messageString.charAt(i) == MESSAGE_DATA_SEPARATOR && !isInsideQuote) {
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
        } catch (StringIndexOutOfBoundsException e) {
            logger.warn("Failed to parse invalid message " + messageString, e);
        }
        return parsedMessage;
    }

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder(type.getValue());
        for (String aData : data) {
            message.append(MESSAGE_DATA_SEPARATOR);
            if (aData.contains(MESSAGE_DATA_SEPARATOR.toString())) {
                message.append("\"");
            }
            message.append(aData);
            if (aData.contains(MESSAGE_DATA_SEPARATOR.toString())) {
                message.append("\"");
            }
        }
        return String.format("%04d", message.length() + 5) + MESSAGE_DATA_SEPARATOR + message.toString();
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
