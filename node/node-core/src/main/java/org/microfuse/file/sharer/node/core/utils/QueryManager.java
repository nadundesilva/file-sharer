package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Query Manager.
 */
public class QueryManager implements RouterListener {
    private static final Logger logger = LoggerFactory.getLogger(QueryManager.class);

    private ServiceHolder serviceHolder;
    private Router router;

    private Map<String, List<AggregatedResource>> queryResults;
    private final ReadWriteLock queryResultsLock;

    public QueryManager(Router router, ServiceHolder serviceHolder) {
        this.serviceHolder = serviceHolder;
        this.router = router;
        this.router.registerListener(this);

        queryResultsLock = new ReentrantReadWriteLock();
        queryResults = new HashMap<>();
    }

    @Override
    public void onMessageReceived(Node fromNode, Message message) {
        if (message.getType() == MessageType.SER_OK) {
            handleSerOkMessages(fromNode, message);
        } else {
            logger.debug("Message " + message.toString() + " of unrecognized type ignored ");
        }
    }

    @Override
    public void onMessageSendFailed(Node toNode, Message message) {

    }

    /**
     * Query the file sharer system for a file.
     *
     * @param queryString The name of the file to be queried for
     */
    public void query(String queryString) {
        Configuration configuration = serviceHolder.getConfiguration();
        Message message = new Message();
        message.setType(MessageType.SER);
        message.setData(MessageIndexes.SER_SOURCE_IP, configuration.getIp());
        message.setData(MessageIndexes.SER_SOURCE_PORT, Integer.toString(configuration.getPeerListeningPort()));
        message.setData(MessageIndexes.SER_FILE_NAME, queryString);
        message.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(NodeConstants.INITIAL_HOP_COUNT));
        router.route(message);
    }

    /**
     * Get the results for a query.
     *
     * @param queryString The query string which was used to search
     * @return The results for the query
     */
    public List<AggregatedResource> getQueryResults(String queryString) {
        queryResultsLock.readLock().lock();
        try {
            List<AggregatedResource> aggregatedResources = queryResults.get(queryString);
            if (aggregatedResources == null) {
                aggregatedResources = new ArrayList<>();
            }
            return aggregatedResources;
        } finally {
            queryResultsLock.readLock().unlock();
        }
    }

    /**
     * Clear the query results that had been stored.
     */
    public void clearQueryResults() {
        queryResultsLock.writeLock().lock();
        try {
            queryResults.clear();
        } finally {
            queryResultsLock.writeLock().unlock();
        }
    }

    /**
     * Handle SER_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleSerOkMessages(Node fromNode, Message message) {
        queryResultsLock.writeLock().lock();
        try {
            List<AggregatedResource> results = queryResults.computeIfAbsent(
                    message.getData(MessageIndexes.SER_OK_QUERY_STRING),
                    k -> new ArrayList<>()
            );

            for (int i = 0; i < Integer.parseInt(message.getData(MessageIndexes.SER_OK_FILE_COUNT)); i++) {
                String fileName = message.getData(MessageIndexes.SER_OK_FILE_NAME_START + i);

                AggregatedResource aggregatedResource = null;
                for (AggregatedResource result : results) {
                    if (Objects.equals(result.getName(), fileName)) {
                        aggregatedResource = result;
                    }
                }
                if (aggregatedResource == null) {
                    aggregatedResource = new AggregatedResource(fileName);
                    results.add(i, aggregatedResource);
                    i--;
                }

                aggregatedResource.addNode(new Node(
                        message.getData(MessageIndexes.SER_OK_IP),
                        Integer.parseInt(message.getData(MessageIndexes.SER_OK_PORT))
                ));
            }
        } finally {
            queryResultsLock.writeLock().unlock();
        }
    }
}
