package org.microfuse.file.sharer.node.core.tracing.stats;

import java.util.ArrayList;
import java.util.List;

/**
 * SER_SUPER_PEER type message traced by the tracer.
 */
public class SerSuperPeerMessage {
    private long startTimeStamp;
    private long firstHitTimeStamp;
    private long messagesCount;
    private List<Integer> hopCounts;

    public SerSuperPeerMessage() {
        startTimeStamp = -1;
        firstHitTimeStamp = -1;
        messagesCount = 0;
        hopCounts = new ArrayList<>();
    }

    public void setStartTimeStamp(long startTimeStamp) {
        if (this.startTimeStamp == -1) {
            this.startTimeStamp = startTimeStamp;
        }
    }

    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    public long getFirstHitTimeStamp() {
        return firstHitTimeStamp;
    }

    public void setFirstHitTimeStamp(long firstHitTimeStamp) {
        if (this.firstHitTimeStamp == -1) {
            this.firstHitTimeStamp = firstHitTimeStamp;
        }
    }

    public long getMessagesCount() {
        return messagesCount;
    }

    public void increaseMessagesCount() {
        messagesCount++;
    }

    public List<Integer> getHopCounts() {
        return new ArrayList<>(hopCounts);
    }

    public void addHopCount(int hopCount) {
        this.hopCounts.add(hopCount);
    }
}
