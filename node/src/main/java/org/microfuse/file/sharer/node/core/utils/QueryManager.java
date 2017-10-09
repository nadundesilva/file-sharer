package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query Manager.
 */
public class QueryManager implements RouterListener {
    private static final Logger logger = LoggerFactory.getLogger(QueryManager.class);

    private Router router;

    public QueryManager(Router router) {
        this.router = router;
        this.router.registerListener(this);
    }

    @Override
    public void onMessageReceived(Node fromNode, Message message) {
        if (message.getType() == MessageType.SER_OK) {
            handleSerOkMessages(fromNode, message);
        } else {
            logger.debug("Message " + message.toString() + " of unrecognized type ignored ");
        }
    }

    /**
     * Query the file sharer system for a file.
     *
     * @param fileName The name of the file to be queried for
     */
    public void query(String fileName) {
        Configuration configuration = ServiceHolder.getConfiguration();
        Message message = new Message();
        message.setType(MessageType.SER);
        message.setData(MessageIndexes.SER_SOURCE_IP, configuration.getIp());
        message.setData(MessageIndexes.SER_SOURCE_PORT, Integer.toString(configuration.getPeerListeningPort()));
        message.setData(MessageIndexes.SER_FILE_NAME, fileName);
        message.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(Constants.INITIAL_HOP_COUNT));
        router.route(message);
    }

    /**
     * Handle SER_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleSerOkMessages(Node fromNode, Message message) {

    }
}
