package es.revib.server.rest.dao;

import es.revib.server.rest.entities.*;
import es.revib.server.rest.entities.request.EventRequest;
import es.revib.server.rest.util.AccessType;
import org.codehaus.jettison.json.JSONObject;
import org.jvnet.hk2.annotations.Contract;

import javax.ws.rs.ForbiddenException;
import java.util.List;

@Contract
public interface IActivityDAO {

    /**
     * we call this from the approval service to update the entry with the approved media or text
     * @param jsonObject
     */
    void approveCallback(JSONObject jsonObject);

    /**
     *
     * @param activityId
     * @param user if user is null then we are not logged in
     * @param accessType
     * @param increment if we want to increment the views counter
     * @return
     */
    Activity getActivity(String activityId,Object user,AccessType accessType,boolean increment,String status);
    List<Activity> getActivities(ActivityQueryBuilder queryBuilder);
    Activity updateActivity(Object user,Activity activity);
    void deleteActivity(Object user, String activityId);
    boolean isOwner(Object user, String activityId);

    Event createEvent(Object user,Event event) throws ForbiddenException;
    Event updateEvent(Object user,Event event);
    void cancelEvent(Object user,String eventId);

    List<Activity> getActivitiesByUser(Object user, AccessType accessType, int start, int size, String status);

    Event getEvent(String eventId,Object user,AccessType accessType,boolean increment);
    List<Event> getEventsByUser(Object user,AccessType accessType,int start,int size,String status);

    //REQUESTS
    EventRequest createNewEventRequest(Object user,EventRequest eventRequest,String activityId);
    EventRequest createExistingEventRequest(Object user, EventRequest eventRequest,String eventId);

    EventRequest getRequest(Object user, String requestId);

    void voteEventRequest(Object user, Vote vote,String requestId);
    EventRequest updateEventRequest(Object user, EventRequest eventRequest);
    void deleteEventRequest(Object user,String requestId);

    /**
     *
     * @param user can be either a String with username/email or and ID or a Vertex
     * @param activity
     * @return
     */
    Activity createActivity(Object user,Activity activity);

    Rating doRating(Object user, Rating rating,String eventId);
}
