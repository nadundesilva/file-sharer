package org.microfuse.file.sharer.node.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System Manager class.
 */
public class NodeManager {
    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);

    public static void start() {
        Thread thread = new Thread(() -> {
            logger.info("Starting Node");

            // TODO : implement node startup
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private NodeManager() {     // Preventing from being initiated
    }
}
