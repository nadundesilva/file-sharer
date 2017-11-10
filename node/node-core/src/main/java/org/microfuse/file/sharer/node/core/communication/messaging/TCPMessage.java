package org.microfuse.file.sharer.node.core.communication.messaging;

import java.util.Objects;

/**
 * TCP message class.
 * Used by the TCP Network Handler
 */
public class TCPMessage implements Cloneable {
    private String sourceIP;
    private int sourcePort;
    private Message message;

    private static final Character MESSAGE_DATA_SEPARATOR = ' ';

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
    
    @Override
    public TCPMessage clone() {
        TCPMessage clone;
        try {
            clone = (TCPMessage) super.clone();
        } catch (CloneNotSupportedException e) {
            clone = new TCPMessage();
        }
        clone.setSourceIP(sourceIP);
        clone.setSourcePort(sourcePort);
        clone.setMessage((message != null ? message.clone() : null));
        return clone;
    }

    /**
     * Parse a TCP Message object from a string.
     *
     * @param messageString The string which contains the TCP message
     * @return The relevant TCP message objects representing the message string
     */
    public static TCPMessage parse(String messageString) {
        TCPMessage tcpMessage = new TCPMessage();

        // Getting the UDP message type
        int firstSeparatorIndex = messageString.indexOf(MESSAGE_DATA_SEPARATOR);
        tcpMessage.setSourceIP(messageString.substring(0, firstSeparatorIndex));

        // Getting the message IP
        int secondSeparatorIndex = messageString.indexOf(MESSAGE_DATA_SEPARATOR, firstSeparatorIndex + 1);
        tcpMessage.setSourcePort(Integer.parseInt(
                messageString.substring(firstSeparatorIndex + 1, secondSeparatorIndex)));

        // Getting the message delivered by the UDP layer
        if (secondSeparatorIndex < messageString.length()) {
            tcpMessage.setMessage(Message.parse(messageString.substring(secondSeparatorIndex + 1)));
        }
        
        return tcpMessage;
    }

    @Override
    public String toString() {
        return sourceIP + MESSAGE_DATA_SEPARATOR + sourcePort
                + (message != null ? MESSAGE_DATA_SEPARATOR + message.toString() : "");
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof TCPMessage) {
            TCPMessage messageObject = (TCPMessage) object;
            return Objects.equals(messageObject.toString(), toString());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
