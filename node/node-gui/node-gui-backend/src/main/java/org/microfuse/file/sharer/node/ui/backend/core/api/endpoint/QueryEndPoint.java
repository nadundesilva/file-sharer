package org.microfuse.file.sharer.node.ui.backend.core.api.endpoint;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.utils.QueryManager;
import org.microfuse.file.sharer.node.ui.backend.commons.APIConstants;
import org.microfuse.file.sharer.node.ui.backend.commons.Status;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
import org.microfuse.file.sharer.node.ui.backend.core.utils.ResponseUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Querying related end point.
 */
@Path("/query")
public class QueryEndPoint {
    @POST
    @Path("/{queryString}")
    public Response runQuery(@PathParam("queryString") String queryString) {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

        QueryManager queryManager = FileSharerHolder.getFileSharer().getServiceHolder().getQueryManager();
        queryManager.query(queryString);

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/")
    public Response getQueryResult() {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

        QueryManager queryManager = FileSharerHolder.getFileSharer().getServiceHolder().getQueryManager();
        Set<String> runningQueryStrings = queryManager.getRunningQueryStrings();
        response.put(APIConstants.DATA, runningQueryStrings);

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/{queryString}")
    public Response getQueryResult(@PathParam("queryString") String queryString) {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

        QueryManager queryManager = FileSharerHolder.getFileSharer().getServiceHolder().getQueryManager();
        List<AggregatedResource> aggregatedResourceList = queryManager.getQueryResults(queryString);
        response.put(APIConstants.DATA, aggregatedResourceList);

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
