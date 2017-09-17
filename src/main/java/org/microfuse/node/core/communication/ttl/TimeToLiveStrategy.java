package org.microfuse.node.core.communication.ttl;

import org.microfuse.node.commons.messaging.Message;

/**
 * The strategy in determining the time to live.
 */
public interface TimeToLiveStrategy {
    /**
     * Update the initial time to live of a message
     * This might be different from which is used when retrying
     *
     * @param message The message of which the time to list should be updated
     */
    void updateInitialTimeToLive(Message message);

    /**
     * Update the time to live of a message
     *
     * @param message The message of which the time to list should be updated
     */
    void updateRetryingTimeToLive(Message message);
}
