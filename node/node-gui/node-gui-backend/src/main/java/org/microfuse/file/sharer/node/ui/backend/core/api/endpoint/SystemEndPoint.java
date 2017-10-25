package org.microfuse.file.sharer.node.ui.backend.core.api.endpoint;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.ui.backend.commons.Status;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
import org.microfuse.file.sharer.node.ui.backend.core.utils.ResponseUtils;

import java.util.Map;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Configuration related endpoint.
 */
@Path("/system")
public class SystemEndPoint {
    @POST
    @Path("/start")
    public Response start() {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

        FileSharerHolder.getFileSharer().start();

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/restart")
    public Response restart() {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

        FileSharerHolder.getFileSharer().restart();

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/shutdown")
    public Response shutdown() {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

        FileSharerHolder.getFileSharer().shutdown();

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
