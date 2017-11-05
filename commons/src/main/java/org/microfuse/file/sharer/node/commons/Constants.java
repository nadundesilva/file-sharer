package org.microfuse.file.sharer.node.commons;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants used across the file sharer.
 */
public class Constants {
    public static final int BOOTSTRAP_SERVER_PORT = 5555;
    public static final int CONTINUOUS_TASK_INTERVAL = 3000;
    public static final int THREAD_DISABLE_TIMEOUT = 1000;
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static final int RMI_REGISTRY_PORT = 12452;
    public static final String RMI_REGISTRY_ENTRY_TRACER = "tracer";
    public static final String RMI_REGISTRY_ENTRY_NETWORK_HANDLER = "network_handler";

    private Constants() {       // Preventing from being initiated
    }
}
