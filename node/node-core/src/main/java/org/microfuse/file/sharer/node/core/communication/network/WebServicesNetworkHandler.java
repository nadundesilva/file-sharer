package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;

/**
 * A web services based network handler.
 * <p>
 * Uses web services to communicate with other nodes.
 */
public class WebServicesNetworkHandler extends NetworkHandler { // TODO : Implement web services based network handler
    public WebServicesNetworkHandler(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    @Override
    public String getName() {
        return NetworkHandlerType.WEB_SERVICES.getValue();
    }

    @Override
    public void sendMessage(String ip, int port, Message message) {

    }

    @Override
    public void shutdown() {

    }
}
