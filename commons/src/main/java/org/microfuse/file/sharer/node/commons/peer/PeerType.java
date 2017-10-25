package org.microfuse.file.sharer.node.commons.peer;

/**
 * Peer types.
 */
public enum PeerType {
    SUPER_PEER("Super Peer"),
    ORDINARY_PEER("Ordinary Peer");

    /**
     * Contains the value to be displayed.
     */
    private String value;

    PeerType(String value) {
        this.value = value;
    }

    /**
     * Get the value to be displayed.
     *
     * @return The value to be displayed
     */
    public String getValue() {
        return value;
    }
}
