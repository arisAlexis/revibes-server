package es.revib.server.rest.jersey;

import com.google.gson.Gson;
import es.revib.server.rest.dao.IUserDAO;
import es.revib.server.rest.entities.*;
import es.revib.server.rest.entities.request.FriendRequest;
import es.revib.server.rest.entities.request.Request;
import es.revib.server.rest.util.AccessType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.ws.rs.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.FileReader;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("users")
public class UsersEndpoint {

    @Inject
    IUserDAO userDAO;

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
    @Path("{userId}/notifications")
    @Produces(APPLICATION_JSON)
    public Response getNotifications(@PathParam("userId") String userId) {
        if (userId.equalsIgnoreCase(currentUserId) || userId.equalsIgnoreCase("me")) {
            return Response.ok(userDAO.getUser(currentUserId, AccessType.OWNER).getNotifications(), MediaType.APPLICATION_JSON).build();
        }
        else {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("{user}")
    @Restricted
    @Produces(APPLICATION_JSON)
    public Response getUser(
            @PathParam("user") String user) {

        if (user.equalsIgnoreCase(currentUserId) || user.equalsIgnoreCase("me")) {
            return Response.ok(userDAO.getUser(currentUserId, AccessType.OWNER), MediaType.APPLICATION_JSON).build();
        }

        User dbUser;
        try {
            dbUser = userDAO.getUser(user, AccessType.UNKNOWN);
        } catch (NotFoundException nf) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        catch (BadRequestException bd) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok(dbUser).build();
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public User createUser(@Valid User user) throws JSONException, NoSuchAlgorithmException {

        return userDAO.createUser(user);
    }

    @PUT
    @Path("{userId}") //pathparam is ignored, only useful for REST
    @Restricted
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public User updateUser(User user) throws NoSuchAlgorithmException, JSONException {

        if (!user.getId().equalsIgnoreCase(currentUserId))
            throw new ForbiddenException();

        //images are not allowed here only through the approval service
        User originUser = userDAO.getUser(user.getId(), AccessType.OWNER);
        user.setMainImage(originUser.getMainImage());
        user.setImages(originUser.getImages());

        return userDAO.updateUser(user);
    }

    @DELETE
    @Path("{userId}")
    @Restricted
    public Response deleteUser(
            @Pattern(regexp = "[0-9]*")
            @PathParam("id") String id) {

        if (!id.equalsIgnoreCase(currentUserId) && !id.equalsIgnoreCase("me"))
            return Response.status(Response.Status.FORBIDDEN).build();

        boolean ok = userDAO.deleteUser(id);
        if (!ok)
            return Response.status(Response.Status.BAD_REQUEST).build();

        return Response.ok().build();
    }

    @GET
    @Path("/{user}/ratings")
    @Produces(APPLICATION_JSON)
    public List<Rating> getRatings(
            @PathParam("user") String user,
        @DefaultValue("0") @QueryParam("start") int start,
        @DefaultValue("10") @QueryParam("size") int size )
    {
        if (user.equals("me") || user.equals(currentUserId)) {
            return userDAO.getRatings(currentUserId, start, size);
        }
        else {
            return userDAO.getRatings(user, start, size);
        }
    }

    @GET
    @Path("{userId}/requests")
    @Restricted
    @Produces(APPLICATION_JSON)
    public Response getRequests(
            @PathParam("userId") String userId,
            @DefaultValue("0") @QueryParam("start") int start,
            @DefaultValue("10") @QueryParam("size") int size
    ) {

        if (!userId.equalsIgnoreCase("me") && !userId.equalsIgnoreCase(currentUserId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        List<Request> requests=userDAO.getRequests(currentUserId,start,size);
        JSONArray jsonArray=null;
        try {
            jsonArray=new JSONArray(new Gson().toJson(requests).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Response.ok(jsonArray.toString()).build();
    }

    @GET
    @Path("{user}/friends")
    @Produces(APPLICATION_JSON)
    @Restricted
    public Response getMyFriends(
            @PathParam("user") String user
    ) {
        if (user.equalsIgnoreCase("me") || user.equalsIgnoreCase(currentUserId)) {
            return Response.ok(userDAO.getFriends(currentUserId)).build();
        }
        else {
            return Response.ok(userDAO.getFriends(user)).build();
        }

    }

    @POST
    @Path("{userId}/friends/{friend}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Restricted
    public Response addFriend(
            @PathParam("userId") String userId,
            @PathParam("friend") String friend,
            FriendRequest friendRequest
    ){
        if (!userId.equalsIgnoreCase("me") && !userId.equalsIgnoreCase(currentUserId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
            return Response.ok(userDAO.addFriend(currentUserId, friend,friendRequest.getRequestMessage())).build();
    }

    @GET
    @Path("{userId}/friends/requests/{requestId}")
    @Restricted
    public Response replyFriendRequest(
            @PathParam("userId") String userId,
            @PathParam("requestId")String requestId,
                                       @QueryParam("action") String action,
                                       @QueryParam("message") String message) {

        if (!userId.equalsIgnoreCase("me") && !userId.equalsIgnoreCase(currentUserId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (action.equalsIgnoreCase(Verbs.ACCEPT)) {
                userDAO.acceptFriend(currentUserId, requestId,message);
        }
        else if (action.equalsIgnoreCase(Verbs.REJECT)) {
             userDAO.rejectFriend(currentUserId, requestId,message);
        }

        return Response.ok().build();

    }

    @DELETE
    @Path("{userId}/friends/{friend}")
    @Restricted
    public Response deleteFriend(
            @PathParam("userId") String userId,
            @PathParam("friend") String friend
    ) {
        if (!userId.equalsIgnoreCase("me") && !userId.equalsIgnoreCase(currentUserId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        userDAO.deleteFriend(currentUserId, friend);
        //we always return OK here
        return Response.ok().build();
    }

    @DELETE
    @Path("{userId}/notifications/{id}")
    @Restricted
    public Response deleteNotifications(
            @PathParam("userId") String userId,
            @PathParam("id") String id
    ) {
        if (!userId.equalsIgnoreCase("me") && !userId.equalsIgnoreCase(currentUserId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        userDAO.deleteNotification(currentUserId, id);
        //we always return OK here
        return Response.ok().build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Response getUsersByLocation(
            @QueryParam("lat") Double lat,
            @QueryParam("lon") Double lon,
            @DefaultValue("10") @QueryParam("radius") int radius,
            @DefaultValue("0") @QueryParam("start") int start,
            @DefaultValue("10") @QueryParam("size") int size
    ) {
        if (lat==null || lon==null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok(userDAO.getUsersByLocation(lat,lon,radius,start,size)).build();
    }


}
