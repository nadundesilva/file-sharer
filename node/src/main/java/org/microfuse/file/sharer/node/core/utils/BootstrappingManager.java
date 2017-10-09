package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;

/**
 * Bootstrapping Manager.
 */
public class BootstrappingManager implements RouterListener {
    private Router router;

    public BootstrappingManager(Router router) {
        this.router = router;
        this.router.registerListener(this);
    }

    @Override
    public void onMessageReceived(Message message) {

    }
}
