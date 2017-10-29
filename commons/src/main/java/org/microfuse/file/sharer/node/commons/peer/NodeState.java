package org.microfuse.file.sharer.node.commons.peer;

/**
 * Node states.
 */
public enum NodeState {
    ACTIVE("Active"),
    PENDING_INACTIVATION("Pending Inactivation"),
    INACTIVE("Inactive");

    /**
     * Contains the value to be displayed.
     */
    private String value;

    NodeState(String value) {
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
