package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;

/**
 * A Java RMI based network handler.
 * <p>
 * Uses Java Remte Method Invocation to communicate with other nodes.
 */
public class RMINetworkHandler extends NetworkHandler { // TODO : Implement RMI based network handler
    public RMINetworkHandler(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    @Override
    public String getName() {
        return NetworkHandlerType.RMI.getValue();
    }

    @Override
    public void sendMessage(String ip, int port, Message message) {

    }

    @Override
    public void shutdown() {

    }
}
