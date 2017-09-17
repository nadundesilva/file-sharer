package org.microfuse.node.core.communication.ttl;

import org.microfuse.node.commons.messaging.Message;
import org.microfuse.node.commons.messaging.MessageType;
import org.microfuse.node.core.Manager;

/**
 * Time to live strategy based on a fixed time.
 *
 * A fixed time is set as the time to live
 */
public class FixedTimeToLiveStrategy implements TimeToLiveStrategy {
    @Override
    public void updateInitialTimeToLive(Message message) {
        message.setType(MessageType.REQUEST);
        message.setTimeToLive(Manager.getConfigurationInstance().getInitialTimeToLive());
    }

    @Override
    public void updateRetryingTimeToLive(Message message) {
        message.setType(MessageType.REQUEST);
        message.setTimeToLive(0);
    }
}
