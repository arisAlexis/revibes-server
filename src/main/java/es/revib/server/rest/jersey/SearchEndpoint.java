package es.revib.server.rest.jersey;

import es.revib.server.rest.dao.ISearchDAO;
import org.codehaus.jettison.json.JSONObject;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * This endpoint is used for searching and maybe in the future for recommendations
 */
@Path("search")
public class SearchEndpoint {

    @Inject ISearchDAO searchDAO;

    @GET
    @Path("/tags")
    @Produces(APPLICATION_JSON)
    public Response searchTags(@QueryParam("prefix") String prefix,
                               @DefaultValue("10") @QueryParam("limit") int limit ) {
        JSONObject jsonObject=searchDAO.searchTags(prefix,limit);
        return Response.ok(jsonObject.toString()).build();
    }

    /**
     * returns a list of the most frequently connected tags
     *
     * @param tag
     * @param limit
     * @return
     */
    @GET
    @Path("/tags/{tag}")
    @Produces(APPLICATION_JSON)
    public Response relevantTags(@PathParam("tag") String tag,
                               @DefaultValue("10") @QueryParam("limit") int limit ) {
        JSONObject jsonObject=searchDAO.relevantTags(tag,limit);
        return Response.ok(jsonObject.toString()).build();
    }

 /*   @GET
    @Path("/tags/trending")
    @Produces(APPLICATION_JSON)
    public Response searchTags(@DefaultValue("10") @QueryParam("limit") int limit ) {
        JSONObject jsonObject=searchDAO.getTrendingTags(limit);
        return Response.ok(jsonObject.toString()).build();
    }*/

    @GET
    @Path("/users")
    @Produces(APPLICATION_JSON)
    public Response searchUsers(@QueryParam("prefix") String prefix,
                               @DefaultValue("10") @QueryParam("limit") int limit ) {
        JSONObject jsonObject=searchDAO.searchUsers(prefix,limit);
        return Response.ok(jsonObject.toString()).build();
    }
}
