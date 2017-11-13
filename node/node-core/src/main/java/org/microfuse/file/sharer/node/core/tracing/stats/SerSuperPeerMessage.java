package org.microfuse.file.sharer.node.core.tracing.stats;

import java.util.ArrayList;
import java.util.List;

/**
 * SER_SUPER_PEER type message traced by the tracer.
 */
public class SerSuperPeerMessage {
    private long messagesCount;
    private List<Integer> hopCounts;

    public SerSuperPeerMessage() {
        messagesCount = 0;
        hopCounts = new ArrayList<>();
    }

    public void increaseMessagesCount() {
        messagesCount++;
    }

    public long getMessagesCount() {
        return messagesCount;
    }

    public List<Integer> getHopCounts() {
        return new ArrayList<>(hopCounts);
    }

    public void addHopCount(int hopCount) {
        this.hopCounts.add(hopCount);
    }
}
