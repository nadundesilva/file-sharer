package org.microfuse.node.core.communication.network;

import java.util.HashMap;
import java.util.Map;

/**
 * Network handler types.
 */
public enum NetworkHandlerType {
    SOCKET("Socket");

    /**
     * Contains the value to be displayed.
     */
    private String value;

    /**
     * The network handler class map.
     * Maps the network handler type to class.
     */
    private static Map<NetworkHandlerType, Class<? extends NetworkHandler>> networkHandlerClassMap;

    static {
        // Populating the network handler class map
        networkHandlerClassMap = new HashMap<>();
        networkHandlerClassMap.put(NetworkHandlerType.SOCKET, SocketNetworkHandler.class);
    }

    NetworkHandlerType(String value) {
        this.value = value;
    }

    /**
     * Get the value to be displayed.
     *
     * @return The value to be displayed
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the network handler class based on the network handler type.
     *
     * @param networkHandlerType The network handler type
     * @return The network handler class
     */
    public static Class<? extends NetworkHandler> getNetworkHandlerClass(NetworkHandlerType networkHandlerType) {
        return networkHandlerClassMap.get(networkHandlerType);
    }
}
