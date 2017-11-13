package org.microfuse.file.sharer.node.core.tracing.stats;

import java.util.ArrayList;
import java.util.List;

/**
 * SER type message traced by the tracer.
 */
public class SerMessage {
    private String query;
    private long messagesCount;
    private List<Integer> hopCounts;

    public SerMessage(String query) {
        this.query = query;
        messagesCount = 0;
        hopCounts = new ArrayList<>();
    }

    public String getQuery() {
        return query;
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
