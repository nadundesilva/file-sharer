package org.microfuse.file.sharer.node.ui.backend.core.api.endpoint;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.ui.backend.commons.APIConstants;
import org.microfuse.file.sharer.node.ui.backend.commons.Status;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerMode;
import org.microfuse.file.sharer.node.ui.backend.core.utils.ResponseUtils;

import java.util.Map;
import javax.ws.rs.GET;
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
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            FileSharerHolder.getFileSharer().start();
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/restart")
    public Response restart() {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            FileSharerHolder.getFileSharer().restart();
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/shutdown")
    public Response shutdown() {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            FileSharerHolder.getFileSharer().shutdown();
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(Constants.TASK_INTERVAL);
                } catch (InterruptedException ignored) {
                }
                System.exit(0);
            });
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/mode")
    public Response getMode() {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

        response.put(APIConstants.DATA, FileSharerHolder.getMode());

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
