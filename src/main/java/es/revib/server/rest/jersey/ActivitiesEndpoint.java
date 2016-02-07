package es.revib.server.rest.jersey;

import es.revib.server.rest.approval.IApprovalService;
import es.revib.server.rest.dao.ActivityQueryBuilder;
import es.revib.server.rest.dao.IActivityDAO;
import es.revib.server.rest.dao.IUserDAO;
import es.revib.server.rest.entities.Status;
import es.revib.server.rest.entities.*;
import es.revib.server.rest.entities.request.EventRequest;
import es.revib.server.rest.kv.IKVStore;
import es.revib.server.rest.util.AccessType;
import es.revib.server.rest.util.CodingUtilities;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("activities")
public class ActivitiesEndpoint {

    @Inject IApprovalService approvalService;

    @Inject IKVStore kvStore;

    @Inject IActivityDAO activityDAO;

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
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    public Response getActivity(
            @PathParam("id") String id,
            @DefaultValue("false") @QueryParam("view") boolean view,
            @DefaultValue("All") @QueryParam("status") String status
    ) throws JSONException {

        try {
            if (currentUserId!=null) {
                Activity activity=activityDAO.getActivity(id, currentUserId, AccessType.UNKNOWN, view, status);
                return Response.ok(activity).build();
            } else {
                return Response.ok(activityDAO.getActivity(id, null, AccessType.UNKNOWN, view, status)).build();
            }
        }
        catch (NotFoundException nf) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("{id}/events/{eventId}")
    @Restricted
    @Produces(APPLICATION_JSON)
    public Response getEvent(
            @PathParam("id") String id,
            @PathParam("eventId") String eventId,
            @DefaultValue("false") @QueryParam("view") boolean view
    ) throws JSONException {

        try {
            if (currentUserId!=null) {
                return Response.ok(activityDAO.getEvent(eventId, currentUserId, AccessType.UNKNOWN, view)).build();
            } else {
                return Response.ok(activityDAO.getEvent(eventId, null, AccessType.UNKNOWN, view)).build();
            }
        }
        catch (NotFoundException nf) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }


    @GET
    @Path("/byUser/{user}")
    @Restricted
    @Produces(APPLICATION_JSON)
    public List<Activity> getActivitiesByUser(
            @DefaultValue("0") @QueryParam("start") int start,
            @DefaultValue("10") @QueryParam("size") int size,
            @DefaultValue("Open") @QueryParam("status") String status,
            @PathParam("user") String user) throws JSONException {

        List<Activity> activities;
        if (user.equals("me") || user.equalsIgnoreCase(currentUserId)) {
            activities=activityDAO.getActivitiesByUser(currentUserId, AccessType.OWNER, start, size,status);
        }
        else {
            activities=activityDAO.getActivitiesByUser(user, AccessType.VIEWER, start, size,Status.OPEN);
        }
        return activities;
    }

    @GET
    @Path("/byUser/{user}/events")
    @Restricted
    @Produces(APPLICATION_JSON)
    public List<Event> getEventsByUser(
            @DefaultValue("0") @QueryParam("start") int start,
            @DefaultValue("10") @QueryParam("size") int size,
            @PathParam("user") String user) throws JSONException {
        if (user.equals("me")) {
            return activityDAO.getEventsByUser(currentUserId, AccessType.OWNER, start, size,Status.OPEN);
        }
        else {
            return activityDAO.getEventsByUser(user, AccessType.VIEWER, start, size,Status.OPEN);
        }
    }

    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public List<Activity> getActivities(
            @QueryParam("keywords") String keywords,
            @QueryParam("tag") String tag,
            @QueryParam("categoryId") String categoryId,
            @QueryParam("status") String status,
            @QueryParam("lat") Double lat,
            @QueryParam("lon") Double lon,
            @QueryParam("radius") Integer radius,
            @DefaultValue("0") @QueryParam("start") int start,
            @DefaultValue("10") @QueryParam("size") int size
    )
            throws JSONException {

        ActivityQueryBuilder queryBuilder=new ActivityQueryBuilder();
        queryBuilder.setKeywords(keywords);
        queryBuilder.setTag(tag);
        queryBuilder.setCategory(categoryId);
        queryBuilder.setStart(start);
        queryBuilder.setSize(size);
        queryBuilder.setLat(lat);
        queryBuilder.setLon(lon);
        queryBuilder.setRadius(radius);

        return activityDAO.getActivities(queryBuilder);
    }

    @POST
    @Restricted
    @Path("{id}/events")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    /**
     * only the owner can do that, others have to post a request first
     */
    public Event createEvent(@PathParam("id") String id,@Valid Event event) {

        event.setActivityId(id);
        return activityDAO.createEvent(currentUserId, event);

    }

    @GET
    @Restricted
    @Produces(APPLICATION_JSON)
    @Path("/requests/{requestId}")
    public EventRequest getRequest(@PathParam("requestId") String requestId) {

        return activityDAO.getRequest(currentUserId, requestId);
    }

    @POST
    @Restricted
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("{activityId}/requests")
    public EventRequest createNewEventRequest(@PathParam("activityId") String activityId,EventRequest eventRequest) {

        eventRequest.setRequester(userDAO.getUser(currentUserId,AccessType.OWNER));
        //SECURITY give default value
        eventRequest.setDemocracyType(EventRequest.DICTATORSHIP);
        return activityDAO.createNewEventRequest(currentUserId, eventRequest,activityId);

    }

    @POST
     @Restricted
     @Consumes(APPLICATION_JSON)
     @Produces(APPLICATION_JSON)
     @Path("{activityId}/events/{eventId}/requests")
     public EventRequest createExistingEventRequest(@PathParam("eventId") String eventId,EventRequest eventRequest) {

        return activityDAO.createExistingEventRequest(currentUserId, eventRequest, eventId);
    }

    @PUT
    @Restricted
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/requests/{requestId}")
    public EventRequest updateEventRequest(@PathParam("requestId") String requestId, EventRequest eventRequest)
    {
        return activityDAO.updateEventRequest(currentUserId, eventRequest);
    }

    @POST
    @Restricted
    @Consumes(APPLICATION_JSON)
    @Path("/requests/{requestId}/votes")
    public Response voteEventRequest(@PathParam("requestId") String requestId,@Valid Vote vote) {
        activityDAO.voteEventRequest(currentUserId,vote,requestId);
        return Response.ok().build();
    }

    @DELETE
    @Restricted
    @Produces(APPLICATION_JSON)
    /** same thing as the cancelEventRequest above just different route
     *
     */
    @Path("/requests/{requestId}")
    public Response deleteEventRequest(@PathParam("requestId") String requestId) {
        try {
            activityDAO.deleteEventRequest(currentUserId, requestId);
        }
        catch (NotFoundException nf) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        catch (BadRequestException be) {
            return Response.status(400).build();
        }
        return Response.ok("{}").build(); //this is done for backbone because jquery expects some json confirmation
        // along with the 200OK code
    }


    @PUT
    @Restricted
    @Path("{id}/events/{eventId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Event updateEvent(@PathParam("id") String id,@PathParam("eventId") String eventId,@Valid Event event) {

        return activityDAO.updateEvent(currentUserId,event);

    }

    @DELETE
    @Path("{id}/events/{eventId}")
    @Restricted
    public Response deleteEvent(
            @PathParam("id") String id,
            @PathParam("eventId") String eventId
    ) throws Exception {
        try {
            activityDAO.cancelEvent(currentUserId, eventId);
        }
        catch (NotFoundException nf) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok("{}").build(); //this is done for backbone because jquery expects some json confirmation
        // along with the 200OK code
    }

    /*    @POST
        @Restricted
        @Path("{id}/events/{eventId}/comments")
        @Consumes(APPLICATION_JSON)
        @Produces(APPLICATION_JSON)
        public Response postComment(
                @PathParam("id") String id,
                @PathParam("eventId") String eventId,
                Comment comment
        ) {
            if (!comment.getSender().equalsIgnoreCase(currentUsername)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            activityDAO.postComment(eventId,vuser,comment);
            return Response.ok().build();
        }*/

        @POST
        @Restricted
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Activity createActivity(@Valid Activity activity) {

        //this is not the correct pathway to upload images, they need to get approved first
        activity.setMainImage(null);
        activity.setImages(null);

        Activity createdActivity= activityDAO.createActivity(currentUserId,activity);

        //sumbit for approval
        JSONObject jsonObject=new JSONObject();

        String tags="";
        for (String tag:createdActivity.getTags()) tags+=" "+tag;

            JSONObject value=null;

            try {
                value=new JSONObject();
                value.put("title",activity.getTitle());
                value.put("description",activity.getDescription());
                value.put("tags",tags);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String uid= UUID.randomUUID().toString();
        try {
            jsonObject.put("uid",uid);
            jsonObject.put("user",currentUserId);
            jsonObject.put("referenceType", Entities.ACTIVITY);
            jsonObject.put("referenceId",createdActivity.getId());
            jsonObject.put("type", IApprovalService.TYPE_TEXT);
            jsonObject.put("value",value.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        kvStore.put(uid,jsonObject);
        //WARNING this is important if we are using a dummy service to be after we store the object in the kvstore!
        approvalService.sendForApproval(uid,value.toString());

        return createdActivity;
    }

    @PUT @Path("{id}")
    @Restricted
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Activity updateActivity(@PathParam("id") String id,@Valid Activity activity) {

        CodingUtilities codingUtilities=new CodingUtilities();

        //determine if the activity was changed
        Activity originActivity=activityDAO.getActivity(activity.getId(),currentUserId,AccessType.OWNER,false,activity.getStatus());

        //SECURITY CHECKS
        if (originActivity.getStatus().equalsIgnoreCase(Status.PENDING)) {
            activity.setStatus(Status.PENDING);
        }
        if (!activity.getImages().contains(activity.getMainImage())) {
            activity.setMainImage(originActivity.getMainImage());
        }

        //if a user wants to remove an image it's not a problem but he cannot add one here
        if (codingUtilities.newValuesAdded(originActivity.getImages(),activity.getImages())) {
            activity.setImages(originActivity.getImages());
        }

        //we want to compare them

        if (!originActivity.getDescription().equalsIgnoreCase(activity.getDescription())
                || codingUtilities.newValuesAdded(originActivity.getTags(),activity.getTags())
                || !originActivity.getTitle().equalsIgnoreCase(activity.getTitle())) {
            activity.setStatus(Status.PENDING);

            //sumbit for approval
            JSONObject jsonObject=new JSONObject();

            String tags="";
            for (String tag:activity.getTags()) tags+=" "+tag;

            JSONObject value=null;

            try {
            value=new JSONObject();
            value.put("title",activity.getTitle());
            value.put("description",activity.getDescription());
                value.put("tags",tags);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String uid=UUID.randomUUID().toString();
            try {
                jsonObject.put("uid",uid);
                jsonObject.put("user",currentUserId);
                jsonObject.put("referenceType", Entities.ACTIVITY);
                jsonObject.put("referenceId", activity.getId());
                jsonObject.put("type",IApprovalService.TYPE_TEXT);
                jsonObject.put("value",value.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            activity=activityDAO.updateActivity(currentUserId,activity);


            kvStore.put(uid,jsonObject);
            //WARNING this is important if we are using a dummy service to be after we store the object in the kvstore!
            approvalService.sendForApproval(uid,value.toString());

        }
        else {
            activity=activityDAO.updateActivity(currentUserId,activity);
        }

        return activity;
        }

    @DELETE @Path("{id}")
    @Restricted
    public Response deleteActivity(
            @PathParam("id") String id
    ) throws Exception {
            activityDAO.deleteActivity(currentUserId,id);
            return Response.ok("{}").build(); //this is done for backbone because jquery expects some json confirmation
            // along with the 200OK code
    }

    @POST
    @Path("{id}/events/{eventId}/rating")
    @Restricted
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Rating doRating(@PathParam("id") String id,@PathParam("eventId") String eventId,@Valid Rating rating) {
        return activityDAO.doRating(currentUserId,rating,eventId);
    }
}
