package es.revib.server.rest.jersey;

import es.revib.server.rest.auth.AuthUtils;
import es.revib.server.rest.auth.LogonService;
import es.revib.server.rest.dao.IActivityDAO;
import es.revib.server.rest.dao.IUserDAO;
import es.revib.server.rest.kv.IKVStore;
import es.revib.server.rest.email.IEmailService;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.translation.TranslatorService;
import es.revib.server.rest.util.*;
import org.codehaus.jettison.json.JSONException;
import org.hibernate.validator.constraints.Email;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/auth")
public class AuthorizationEndpoint {

    @Inject
    IUserDAO userDAO;

    @Inject
    IActivityDAO activityDAO;

    @Inject IKVStore kvStore;

    @Inject IEmailService emailService;

    @Context
    HttpServletRequest request;

    @Inject LogonService logonService;

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

    /**
     * This function is used by the client that has a valid session but doesn't know its username after browser closure
     *
     * @return
     */
    @GET
    @Path("/echo")
    @Restricted
    @Produces(APPLICATION_JSON)
    public Response echo() throws JSONException {
        return Response.ok(userDAO.getUser(currentUserId,AccessType.OWNER)).build();
    }

    /**
     * @param paramUser can be either id or username
     * @param password
     * @param keepAlive
     * @return
     * @throws JSONException
     * @throws NoSuchAlgorithmException
     */
    @POST
    @Path("/login")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    public Response login(
            //@Pattern(regexp = "[A-Za-z0-9]*")
            @NotNull
            //@Size(min = 3, max = 25)
            @FormParam("user") String paramUser,
            @NotNull
            //@Size(min = 6,max=25) //this is also checked on the client, we don't care about attacks here because they will only create a weak password
            @FormParam("password") String password,
            @FormParam("facebookToken") String facebookToken,
            @DefaultValue("false") @FormParam("skipSession") boolean skipSession,
            @DefaultValue("false") @FormParam("keepAlive") boolean keepAlive
    ) throws JSONException, NoSuchAlgorithmException {

        User user = null;
        try {
            if (paramUser != null && password != null) {
                user = logonService.basicAuth(paramUser.toLowerCase(), password);
            } else if (facebookToken != null) {
                user = logonService.fbAuth(facebookToken);

            }
        } catch (ForbiddenException fe) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (NotFoundException ne) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!skipSession) {
            HttpSession session = request.getSession(true);
            if (keepAlive) {
                session.setMaxInactiveInterval(604800); //keep him logged in for a week
            } else {
                session.setMaxInactiveInterval(1800);
            }

            session.setAttribute("userId", user.getId());
        }
        return Response.ok(user).build();

    }

/*    *//**
     * returns an object with all the collections prefetched so we don't hit the server for each collection many times when we log in
     *
     * @param userId
     * @return
     *//*

    public JSONObject getBootstrappedUser(String userId) {

        JSONObject juser = null;

        try {
            Gson gson = new Gson();
            juser = new JSONObject(gson.toJson(userDAO.getUser(userId, AccessType.OWNER)));
            //bootstrap some collections and add them here!
            List<User> friends = userDAO.getFriends(userId);
            //we do not need all their data
            friends=friends.stream().map(User::stripMinimal).collect(Collectors.toList());
            juser.put("friends", new JSONArray(gson.toJson(friends)));
            List<Activity> activities = activityDAO.getActivitiesByUser(userId, AccessType.OWNER, 0, 10, Status.ALL);
            juser.put("activities", new JSONArray(gson.toJson(activities)));
            List<Event> events=activityDAO.getEventsByUser(userId,AccessType.OWNER,0,10,Status.OPEN);
            juser.put("events", new JSONArray(gson.toJson(events)));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return juser;
    }*/

    /**
     * this always returns OK
     *
     * @return
     */
    @GET
    @Path("/logout")
    @Restricted
    public Response logout() {

        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }

        return Response.ok().build();
    }

    @GET
    @Path("/emailVerify")
    public Response emailVerify(@QueryParam("user") String user, @QueryParam("token") String token) {

        String dbToken = null;
        try {
            dbToken = kvStore.get("emailVerify" + user).getString("value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (dbToken == null || !dbToken.equals(token)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        kvStore.delete("emailVerify" + user);
        User thisUser = userDAO.getUser(user, AccessType.OWNER);
        thisUser.setEmailVerified(true);
        userDAO.updateUser(thisUser);

        return Response.ok().build();
    }

    @GET
    @Path("/resetPassword")
    public Response resetPasswordAsk(@QueryParam("email") @Email String email) {

        String token = UUID.randomUUID().toString();
        kvStore.put("resetPassword" + email, token);
        User user = userDAO.getUser(email, AccessType.UNKNOWN);
        String htmlTemplate = new CodingUtilities().getResourceAsString("/i18n/templates/" + user.getPreferences().get("language")+"/email_verification.html");
                //get our base url
                String baseUrl = request.getRequestURL().toString();
        //email him using the email service
        htmlTemplate = htmlTemplate.replace("INSERT USER HERE", user.getFirstName() + " " + user.getLastName());
        htmlTemplate = htmlTemplate.replace("INSERT LINK HERE", baseUrl);
        htmlTemplate = htmlTemplate.replace("INSERT EMAIL HERE", user.getEmail());
        htmlTemplate = htmlTemplate.replace("INSERT TOKEN HERE", token);

        Locale userLocale=new Locale(user.getPreferences().get("language").toString(),user.getPreferences().get("country").toString());
        emailService.sendMail(Globals.NO_REPLY_EMAIL, user.getEmail(), TranslatorService.getString("email_verification", userLocale), htmlTemplate);

        return Response.ok().build();
    }

    @POST
    @Path("/resetPassword")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response resetPasswordDo(@FormParam("email") @Email String email,
                                    @FormParam("token") String token,
                                    @FormParam("password") String password) {

        if (email==null || token==null || password==null) return Response.status(Response.Status.BAD_REQUEST).build();

        String dbToken = null;
        try {
            dbToken = kvStore.get("resetPassword" + email).getString("value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (dbToken == null || !dbToken.equals(token)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        kvStore.delete("resetPassword" + email);
        User thisUser = userDAO.getUser(email, AccessType.OWNER);
        Map newPass= AuthUtils.constructPassword(password);
        thisUser.setPassword(newPass);
        userDAO.updateUser(thisUser);

        return Response.ok().entity("Password was reset OK").type(MediaType.TEXT_HTML).build();
    }
}
