package org.microfuse.file.sharer.node.server.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Used for generating custom responses.
 * This is used when creating a separate object for the response is not required.
 */
public class ResponseUtils {
    /**
     * Creates a response map which can be used for returning custom responses.
     *
     * @return The response map which can be used for encoding into JSON
     */
    public static Map<String, Object> generateCustomResponse(Status status) {
        Map<String, Object> response = new HashMap<>();
        response.put(APIConstants.STATUS, status);
        return response;
    }

    private ResponseUtils() {   // Preventing from being initiated
    }
}
