package org.microfuse.file.sharer.node.ui.backend.core.api.endpoint;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.ui.backend.commons.APIConstants;
import org.microfuse.file.sharer.node.ui.backend.commons.Status;
import org.microfuse.file.sharer.node.ui.backend.core.api.request.SaveResourcesRequest;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerMode;
import org.microfuse.file.sharer.node.ui.backend.core.utils.ResponseUtils;

import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resources related endpoint.
 */
@Path("/resources")
public class ResourcesEndPoint {
    @GET
    public Response getResourcesList() {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            ResourceIndex resourceIndex = FileSharerHolder.getFileSharer().getServiceHolder().getResourceIndex();
            Set<OwnedResource> ownedResources = resourceIndex.getAllOwnedResources();
            response.put(APIConstants.DATA, ownedResources);
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveResourcesList(SaveResourcesRequest request) {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            ResourceIndex resourceIndex = FileSharerHolder.getFileSharer().getServiceHolder().getResourceIndex();
            resourceIndex.clearOwnedResources();
            request.getResourceNames().forEach(resourceName ->
                    resourceIndex.addOwnedResource(resourceName, null));
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
