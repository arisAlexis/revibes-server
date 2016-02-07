package es.revib.server.rest.jersey;

import es.revib.server.rest.dao.IStreamDAO;
import es.revib.server.rest.entities.Comment;
import es.revib.server.rest.entities.Info;
import es.revib.server.rest.entities.User;
import org.codehaus.jettison.json.JSONException;
import org.joda.time.DateTime;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/streams")
public class StreamsEndpoint  {

    @Inject IStreamDAO streamDAO;

    @Context
    HttpServletRequest request;

    private String currentUserId=null;

    @PostConstruct
    void init() {

        if (request.isRequestedSessionIdValid()) {
            currentUserId=(String) request.getSession(false).getAttribute("userId");
        }
        else {
            currentUserId=(String)request.getAttribute("userId");
        }
    }


    @GET
    @Path("/byUser/{user}")
    @Restricted
    @Produces(APPLICATION_JSON)
    public Set<Info> getStreams(
            @QueryParam("timestamp") Long timestamp,
            @DefaultValue("50") @QueryParam("size") int size,
            @QueryParam("lat") Double lat,
            @QueryParam("lon") Double lon,
            @DefaultValue("5") @QueryParam("radius") Integer radius

    ) throws JSONException {

        if (timestamp==null) timestamp=new Date().getTime();

        return streamDAO.getByUser(currentUserId, timestamp, size,lat,lon,radius);
    }


    @GET
    @Path("{streamId}")
    @Restricted
    @Produces(APPLICATION_JSON)
    public Info getStream(@PathParam("streamId") String streamId) {
        return streamDAO.getStream(currentUserId, streamId);
    }

    @POST
    @Path("{streamId}/like")
    @Restricted
    public Response like(@PathParam("streamId") String streamId) {

        streamDAO.like(currentUserId,streamId);
        return Response.ok().build();
    }

    @POST
    @Path("{streamId}/comments/{commentId}/like")
    @Restricted
    public Response like(@PathParam("streamId") String streamId,@PathParam("commentId") String commentId) {

        streamDAO.like(currentUserId,commentId);
        return Response.ok().build();
    }

    @POST
    @Path("{streamId}/comments")
    @Restricted
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Comment comment(@PathParam("streamId") String streamId,Comment comment) {

        comment.setReferenceId(streamId);

        return streamDAO.newComment(currentUserId, comment);

    }

    @POST
    @Path("{streamId}/revibe")
    @Restricted
    public Response revibe(@PathParam("streamId") String streamId) {
        streamDAO.revibe(currentUserId, streamId);
        return Response.ok().build();
    }

}
