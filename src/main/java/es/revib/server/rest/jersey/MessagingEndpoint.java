package es.revib.server.rest.jersey;

import es.revib.server.rest.broker.BrokerService;
import es.revib.server.rest.broker.IBroker;
import es.revib.server.rest.dao.IORM;
import es.revib.server.rest.dao.IUserDAO;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.messaging.*;
import es.revib.server.rest.util.AccessType;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Note that his endpoint is NOT REST compliant due to the complex messaging system structure
 */
@Path("messaging")
public class MessagingEndpoint {

    @Inject
    BrokerService brokerService;

    @Inject
    IMessageDAO messagingDAO;

    @Inject IUserDAO userDAO;

    @Inject
    IORM orm;

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

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{chatId}/messages")
    public Response sendMessage(@PathParam("chatId") String chatId,Message msg) {

        Chat chat=messagingDAO.getChat(chatId);

        //security
        User currentUser=userDAO.getUser(currentUserId, AccessType.UNKNOWN);
        msg.setSender(currentUser.getUsername());
        if (!chat.getParticipants().contains(currentUserId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        msg=messagingDAO.postMessage(chat,msg); //this now contains the parent id etc
        List<String> recipients=chat.getParticipants();
        for (String rId:recipients) {
            User ruser=orm.buildUser(rId);
            brokerService.sendMessage(ruser,msg);
            ruser.setMessages(ruser.getMessages()+1);
            userDAO.updateUser(ruser);
        }

        return Response.ok(msg).build();
    }

    @GET
    @Path("byUser/{user}")
    @Restricted
    @Produces(APPLICATION_JSON)
    public List<Chat> getChatsByUser(@PathParam("user") String user,
            @DefaultValue("0") @QueryParam("start") int start,
            @DefaultValue("10") @QueryParam("size") int size
    ) {
        return messagingDAO.getChatsByUser(currentUserId, start, size);
    }

    @GET
    @Path("/{chatId}/timestamp")
    @Restricted
    @Produces(APPLICATION_JSON)
    public Response timestamp(@PathParam("chatId") String chatId
    ) {
        messagingDAO.timestampUser(currentUserId,chatId);
        return Response.ok().build();
    }

    @PUT
    @Restricted
    @Path("{chatId}")
    public Chat updateChat(@PathParam("chatId") String chatId,@Valid Chat chat) {
        return messagingDAO.updateChat(currentUserId, chat);
    }

    @POST
    @Restricted
    public Response createChat(@Valid Chat chat) {
        return Response.ok(messagingDAO.createChat(chat)).build();
    }

    @GET
    @Path("/{chatId}")
    @Produces(APPLICATION_JSON)
    public Response getChat(@PathParam("chatId") String chatId) {
        Chat chat=messagingDAO.getChat(chatId);
        if (!chat.getParticipants().contains(currentUserId))
            return Response.status(Response.Status.FORBIDDEN).build();

        return Response.ok(chat).build();
    }

    @GET
    @Path("/{chatId}/messages")
    @Restricted
    @Produces(APPLICATION_JSON)
    public List<Message> getMessages(
            @DefaultValue("0") @QueryParam("start") int start,
            @DefaultValue("10") @QueryParam("size") int size,
            @PathParam("chatId") String chatId) {

        return messagingDAO.getMessages(currentUserId, chatId, start, size);
    }

    @GET
    @Path("/byReferenceId/{refId}")
    @Restricted
    @Produces(APPLICATION_JSON)
    public Response getChatbyReferenceId(@PathParam("refId") String refId) {

        Chat chat=messagingDAO.getChatByReferenceId(refId);

        if (!chat.getParticipants().contains(currentUserId))
            return Response.status(Response.Status.FORBIDDEN).build();

        return Response.ok(chat).build();
    }

}