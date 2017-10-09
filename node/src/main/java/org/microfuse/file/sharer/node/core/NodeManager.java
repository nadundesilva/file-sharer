package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node Manager class.
 */
public class NodeManager {
    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);

    /**
     * Start the current node.
     */
    public static void start() {
        Thread thread = new Thread(() -> {
            logger.info("Starting Node");

            // TODO : Implement node startup
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * Query the file sharer system for a file.
     *
     * @param fileName The name of the file to be queried for
     */
    public static void query(String fileName) {
        ServiceHolder.getQueryManager().query(fileName);
    }

    private NodeManager() {     // Preventing from being initiated
    }
}
