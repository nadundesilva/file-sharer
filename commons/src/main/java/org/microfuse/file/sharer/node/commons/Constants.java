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

    public static final String TRACER_REGISTRY_ENTRY = "tracer";

    private Constants() {       // Preventing from being initiated
    }
}
