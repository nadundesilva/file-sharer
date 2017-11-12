package org.microfuse.file.sharer.node.ui.backend.core.api.endpoint;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.ui.backend.commons.APIConstants;
import org.microfuse.file.sharer.node.ui.backend.commons.Status;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerMode;
import org.microfuse.file.sharer.node.ui.backend.core.utils.ResponseUtils;

import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Configuration related endpoint.
 */
@Path("/config")
public class ConfigEndPoint {
    @GET
    public Response getConfig() {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            Configuration configuration = FileSharerHolder.getFileSharer().getServiceHolder().getConfiguration();
            response.put(APIConstants.DATA, configuration);
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveConfig(Configuration configuration) {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            FileSharerHolder.getFileSharer().getServiceHolder().updateConfiguration(configuration);
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/defaults")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveConfig() {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            FileSharerHolder.getFileSharer().getServiceHolder().getConfiguration().loadDefaults();
            FileSharerHolder.getFileSharer().getServiceHolder().saveConfiguration();

            Configuration configuration = FileSharerHolder.getFileSharer().getServiceHolder().getConfiguration();
            response.put(APIConstants.DATA, configuration);
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
