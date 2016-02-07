package integration;

import es.revib.server.rest.entities.*;
import es.revib.server.rest.entities.request.EventRequest;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import static org.junit.Assert.*;

public class ActivitiesIntegrationTest extends IntegrationTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        customInit();
    }
    @AfterClass
    public static void afterClass() throws Exception {
        customShutdown();
    }

    @Before
    public void before() throws Exception {
        setUp();
    }
    @After
    public void after() throws Exception {
        tearDown();
    }

    @Ignore
    @Test
    public void postCommentTest() throws InterruptedException, UnsupportedEncodingException {

   /*     Response response = client.target(hostname + "/rest/activities/byUser/testuser1").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});

        Comment comment=new Comment();
        comment.setSender("testuser1");
        comment.setTimestamp(348323483L);
        comment.setBody("blabla");
        Entity<Comment> ent= Entity.entity(comment,MediaType.APPLICATION_JSON);
        response = client.target(hostname+"/rest/activities/"+URLEncoder.encode(retActs.get(0).getId(),"UTF-8")
                +"/comments")
                .request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        //we call with login session here because otherwise we have a VIEWER access type and the comments get filtered!
        response = client.target(hostname + "/rest/activities/"+URLEncoder.encode(retActs.get(0).getId(), "UTF-8")).request()
                .header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        Activity retAct=response.readEntity(Activity.class);

        assertEquals(true,retAct.getComments().contains(comment));*/

    }

    @Test
    public void updateActivityTest() throws JSONException, InterruptedException, UnsupportedEncodingException {

        Activity activity=new Activity();
        activity.setTitle("a title");
        activity.setDescription("whatever");
        activity.setType(Verbs.OFFER);
        List<String> tags=new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");
        activity.setTags(tags);
        activity.setMainImage("plumber.jpg");
        Entity<Activity> ent= Entity.entity(activity,
                MediaType.APPLICATION_JSON);
        Response response = client.target(hostname+"/rest/activities").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true,response.hasEntity());
        Activity retAct=response.readEntity(Activity.class);
        //not a very good way but we include the id in the .equals method
        activity.setId(retAct.getId());
        assertEquals(activity,retAct);

        //should be validated now
        response = client.target(hostname + "/rest/activities/" + URLEncoder.encode(activity.getId(), "UTF-8"))
                .request(MediaType.APPLICATION_JSON).header
                        ("Cookie", getSessionId()).get();
        retAct=response.readEntity(Activity.class);
        retAct.setDescription("lala");

        response = client.target(hostname+"/rest/activities/"+ URLEncoder.encode(activity.getId(), "UTF-8")).request(MediaType.APPLICATION_JSON).header
                ("Cookie", getSessionId()).put(Entity.entity(retAct,MediaType.APPLICATION_JSON));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        Activity updAct=response.readEntity(Activity.class);
        assertEquals(retAct,updAct);

    }
    @Test
    public void createActivityTest() throws JSONException {

        Activity activity=new Activity();
        activity.setTitle("a title");
        activity.setDescription("whatever");
        activity.setType(Verbs.OFFER);
        List<String> tags=new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");
        activity.setTags(tags);
        User u1=new User();
        u1.setId(userId);
        u1.setUsername("testuser1");
        u1.stripMinimal();
        activity.setOwner(u1);
        activity.setMainImage("plumber.jpg");

        Entity<Activity> ent= Entity.entity(activity,MediaType.APPLICATION_JSON);
        Response response = client.target(hostname+"/rest/activities").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true,response.hasEntity());
        Activity retAct=response.readEntity(Activity.class);

        activity.setId(retAct.getId());
        assertEquals(activity,retAct);

    }

    @Test public void getActivitiesByCategoryAndLocation() {

        //first create an activity
        Activity activity=new Activity();
        activity.setTitle("pipe");
        activity.setDescription("couch surfing");
        activity.setType(Verbs.OFFER);
        activity.setCategory("AB23");
        List<String> tags=new ArrayList<>();
        tags.add("couch");
        tags.add("surfing");
        activity.setTags(tags);
        activity.setMainImage("plumber.jpg");

        Entity<Activity> ent= Entity.entity(activity,MediaType.APPLICATION_JSON);
        Response response = client.target(hostname+"/rest/activities").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        Activity retAct=response.readEntity(Activity.class);
        //not a very good way but we need to get the id and compare the rest of the values
        activity.setId(retAct.getId());

        //try to get it first only by category
        response = client.target(hostname + "/rest/activities/")
                .queryParam("categoryId=AB23")
                .request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});
        assertNotEquals(0, retActs.size());

    }

    @Test public void getActivitiesByTextAndLocation() throws InterruptedException {

        //test that we have none with these coords
        Response response = client.target(hostname + "/rest/activities/")
                .queryParam("lat", 38.842111)
                .queryParam("lon",5.349897)
                .queryParam("radius",5)
                .request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});
        assertEquals(0, retActs.size());

        //first create an activity
        Activity activity=new Activity();
        activity.setTitle("pipe");
        activity.setLat(38.842111);
        activity.setLon(5.349897);
        activity.setDescription("couch surfing");
        activity.setType(Verbs.OFFER);
        List<String> tags=new ArrayList<>();
        tags.add("couch");
        tags.add("surfing");
        activity.setTags(tags);
        activity.setMainImage("plumber.jpg");

        Entity<Activity> ent= Entity.entity(activity,MediaType.APPLICATION_JSON);
        response = client.target(hostname+"/rest/activities").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        Activity retAct=response.readEntity(Activity.class);
        //not a very good way but we need to get the id and compare the rest of the values
        activity.setId(retAct.getId());

        response = client.target(hostname + "/rest/activities/")
                .queryParam("keywords","couch surfing")
                .queryParam("lat",38.842111)
                .queryParam("lon", 5.349897)
                .queryParam("radius",5)
                .request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        retActs=response.readEntity(new GenericType<List<Activity>>() {});
        assertNotEquals(0, retActs.size());

    }

    @Test
    /**
     * here we will test elastic search natural language queries with the english language
     */
    public void getActivitiesByText() throws InterruptedException {

        //first create an activity
        Activity activity=new Activity();
        activity.setTitle("my awesome couch");
        activity.setDescription("couch surfing");
        activity.setType(Verbs.OFFER);
        List<String> tags=new ArrayList<>();
        tags.add("couch");
        tags.add("surfing");
        activity.setTags(tags);
        activity.setMainImage("plumber.jpg");

        Entity<Activity> ent= Entity.entity(activity, MediaType.APPLICATION_JSON);
        Response response = client.target(hostname+"/rest/activities").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        Activity retAct=response.readEntity(Activity.class);
        //not a very good way but we need to get the id and compare the rest of the values
        activity.setId(retAct.getId());

        response = client.target(hostname + "/rest/activities/")
                .queryParam("keywords", "couch surfing")
                .request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});
        assertNotEquals(0, retActs.size());

    }

    @Test
    public void eventRequestsFlow() throws JSONException {

        /*
        todo make it more thorough because we only test for acceptance and not rejection and most importantly
        OR split it into smaller tests
         */

        //propose a new event for an activity, reject it, propose a new event again, accept it, make sure both of them are participants and the event exists with status Open

        //find an open activity from testuser1
        List<Activity> activities=getActivities(Status.OPEN);
        Activity activity=null;
        for (Activity a:activities) {
            if (a.getStatus().equalsIgnoreCase(Status.OPEN)) {
                activity=a;
                break;
            }
        }

        EventRequest eventRequest=new EventRequest();
        eventRequest.setDate(1450094383L);
        eventRequest.setAddress("an address");
        eventRequest.setLon(2.343);
        eventRequest.setLat(34.3432);
        eventRequest.setRequestMessage("yo mate do you wanna do this?");
        eventRequest.setType(Action.ADD_EVENT);

        Response response=client.target(hostname + "/rest/activities/" + codingUtilities.toUtf(activity.getId()) + "/requests")
                .request(MediaType.APPLICATION_JSON_TYPE).header("Cookie", getSessionId(user3Email))
                .post(Entity.entity(eventRequest, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(200, response.getStatus());
        eventRequest=response.readEntity(EventRequest.class);

        //REPLY
        eventRequest.setStatus(Status.ACCEPTED);

        response=client.target(hostname + "/rest/activities/requests/" + codingUtilities.toUtf(eventRequest.getId()))
                .request().header("Cookie", getSessionId())
                .put(Entity.entity(eventRequest, MediaType.APPLICATION_JSON_TYPE));

        //test if event is created
        response = client.target(hostname + "/rest/activities/" + codingUtilities.toUtf(activity.getId()))
                .request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true,response.hasEntity());
        Activity retAct=response.readEntity(Activity.class);
        boolean contains=false;
        String eventId=null;
        for (Event e:retAct.getEvents()) {
            if (e.getAddress().equalsIgnoreCase("an address")) {
                eventId=e.getId(); //keep it for later use
                assertEquals(e.getActivityId(),activity.getId()); //are we talking about the original activity here?
                assertEquals(Status.OPEN,e.getStatus());
                contains=true;
                break;
            }
        }

        assertEquals(true, contains);

        //let's add another user so we have two users to test the voting system
        EventRequest participationRequest=new EventRequest();
        participationRequest.setType(Action.PARTICIPATE_EVENT);
        participationRequest.setRequestMessage("Looks cool, I want to come with you");

        response=client.target(hostname+"/rest/activities/" + codingUtilities.toUtf(activity.getId()) + "/events/"+
                codingUtilities.toUtf(eventId) +"/requests/")
        .request(MediaType.APPLICATION_JSON_TYPE).header("Cookie", getSessionId("testuser2@test.com"))
                .post(Entity.entity(participationRequest, MediaType.APPLICATION_JSON_TYPE));
        participationRequest=response.readEntity(EventRequest.class);

        //reject her

        participationRequest.setStatus(Status.REJECTED);
        response=client.target(hostname + "/rest/activities/requests/" + codingUtilities.toUtf(participationRequest.getId()))
                .request().header("Cookie", getSessionId())
                .put(Entity.entity(participationRequest, MediaType.APPLICATION_JSON_TYPE));

        //check that she is not a participant
        response=client.target(hostname+"/rest/activities/"+codingUtilities.toUtf(activity.getId()) + "/events/"+codingUtilities.toUtf(eventId))
                        .request().header("Cookie", getSessionId()).get();
        Event retEvent=response.readEntity(Event.class);
        assertEquals(false,retEvent.getParticipants().contains("claire"));

        //AGAIN
        //let's add another user so we have two users to test the voting system

        participationRequest=new EventRequest();
        participationRequest.setType(Action.PARTICIPATE_EVENT);
        participationRequest.setRequestMessage("hey,pleeeeaseee?");

        response=client.target(hostname + "/rest/activities/" + codingUtilities.toUtf(activity.getId()) + "/events/" +
                codingUtilities.toUtf(eventId) + "/requests/")
                .request().header("Cookie", getSessionId("testuser2@test.com"))
                .post(Entity.entity(participationRequest, MediaType.APPLICATION_JSON_TYPE));

        participationRequest=response.readEntity(EventRequest.class);

        //accept her

        participationRequest.setStatus(Status.ACCEPTED);
        response=client.target(hostname + "/rest/activities/requests/" + codingUtilities.toUtf(participationRequest.getId()))
                .request().header("Cookie", getSessionId())
                .put(Entity.entity(participationRequest, MediaType.APPLICATION_JSON_TYPE));


        //check that she is a participant
        response=client.target(hostname+"/rest/activities/"+codingUtilities.toUtf(activity.getId()) + "/events/"+codingUtilities.toUtf(eventId))
                .request().header("Cookie", getSessionId()).get();
        retEvent=response.readEntity(Event.class);
        assertEquals(true,retEvent.getParticipants().stream().anyMatch(u->u.getUsername().equalsIgnoreCase("testuser2")));

        //test that testuser3 has a notification that testuser2 was added as a participant
        response = client.target(hostname + "/rest/auth/echo/").request()
                .header("Cookie", getSessionId("testuser3@test.com")).get();
        assertEquals(true, response.hasEntity());
        String res=response.readEntity(String.class);
        JSONObject userData=new JSONObject(res);
        JSONArray notifications=userData.getJSONArray("notifications");
        boolean found=false;
        for (int i=0;i<notifications.length();i++){
            JSONObject jsonObject=notifications.getJSONObject(i);
            if (jsonObject.get("action").equals(Action.PARTICIPATE_EVENT) && jsonObject.getJSONArray("sourceDisplayNames").getString(0).equals("testuser2")){
                found = true;
            }
        }
        assertEquals(true, found);

        //now send a modification request for the same event
        eventRequest=new EventRequest();
        eventRequest.setType(Action.UPDATE_EVENT);
        eventRequest.setDate(145009444443L);
        eventRequest.setAddress("address 25");
        eventRequest.setLon(2.343);
        eventRequest.setLat(34.3432);
        eventRequest.setRequestMessage("can we please change the address and time?");

        response=client.target(hostname + "/rest/activities/" + codingUtilities.toUtf(activity.getId()) + "/events/"+
                codingUtilities.toUtf(eventId) +"/requests/")
                .request().header("Cookie", getSessionId("testuser3@test.com"))
                .post(Entity.entity(eventRequest, MediaType.APPLICATION_JSON_TYPE));

        eventRequest=response.readEntity(EventRequest.class);

        //start the voting system test

        eventRequest.setStatus(Status.VOTING);
        eventRequest.setDemocracyType(EventRequest.ABSOLUTE_MAJORITY);

        response=client.target(hostname + "/rest/activities/requests/" + codingUtilities.toUtf(eventRequest.getId()))
                .request().header("Cookie", getSessionId())
                .put(Entity.entity(eventRequest, MediaType.APPLICATION_JSON_TYPE));


        //now test that user2 sees the request and also she has a notification for it and she can vote
        response=client.target(hostname+"/rest/users/me/requests").request().header("Cookie", getSessionId(user3Email)).get();
        JSONArray requests=new JSONArray(response.readEntity(String.class));
        contains=false;
        for (int i=0;i<requests.length();i++) {
            JSONObject request=requests.getJSONObject(i);
            if (request.getString("id").equalsIgnoreCase(eventRequest.getId())) {
                contains=true;
                assertEquals(request.getString("status"),Status.VOTING);
                break;
            }
        }
        assertEquals(true,contains);

        response = client.target(hostname + "/rest/auth/echo/").request()
                .header("Cookie", getSessionId("testuser3@test.com")).get();
        assertEquals(true, response.hasEntity());
        res=response.readEntity(String.class);
        userData=new JSONObject(res);
        notifications=userData.getJSONArray("notifications");
        found=false;
        for (int i=0;i<notifications.length();i++){
            JSONObject jsonObject=notifications.getJSONObject(i);
            if (jsonObject.get("action").equals(Action.REQUEST_REFERRED) && jsonObject.getJSONArray("targetIds").getString(0).equals(eventRequest.getId())){
                found = true;
            }
        }
        assertEquals(true, found);

        //vote as user2
        Vote vote=new Vote();
        vote.setAction(Verbs.ACCEPT);
        vote.setMessage("count me in!");
        response=client.target(hostname + "/rest/activities/requests/" + codingUtilities.toUtf(eventRequest.getId()) + "/votes")
                .request().header("Cookie", getSessionId(user2Email))
                .post(Entity.entity(vote,MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200,response.getStatus());


        vote=new Vote();
        vote.setAction(Verbs.ACCEPT);
        vote.setMessage("ok guys if you want it so bad I will change it");
        response=client.target(hostname + "/rest/activities/requests/" + codingUtilities.toUtf(eventRequest.getId()) + "/votes")
                .request().header("Cookie", getSessionId())
                .post(Entity.entity(vote,MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200,response.getStatus());

        //test if event is changed
        response = client.target(hostname + "/rest/activities/" + codingUtilities.toUtf(activity.getId()) + "/events/" + codingUtilities.toUtf(eventId))
                .request().header("Cookie", getSessionId()).get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true,response.hasEntity());
        Event event=response.readEntity(Event.class);
        assertEquals("address 25",event.getAddress());

        //try to delete the request (should result in badrequest because it is already accepted)
        response=client.target(hostname + "/rest/activities"
                + "/requests/" + codingUtilities.toUtf(eventRequest.getId()))
                .request().header("Cookie", getSessionId(user3Email))
                .delete();

        assertEquals(400,response.getStatus());

        //todo get a list of the requests and see if the request is accepted
        response=client.target(hostname + "/rest/users/me/requests").request().header("Cookie", getSessionId()).get();
        requests=new JSONArray(response.readEntity(String.class));
        contains=false;
        for (int i=0;i<requests.length();i++) {
            JSONObject request=requests.getJSONObject(i);
            if (request.getString("id").equalsIgnoreCase(eventRequest.getId())) {
                contains=true;
                assertEquals(request.getString("status"),Status.ACCEPTED);
                break;
            }
        }
        assertEquals(true,contains);

        //send an un-participate request as user2 and then check the event for participants
        response=client.target(hostname + "/rest/activities/requests/" + codingUtilities.toUtf(participationRequest.getId()))
                .request().header("Cookie", getSessionId(user2Email))
                .delete();

        assertEquals(200,response.getStatus());

        //check that she is NOT a participant
        response=client.target(hostname + "/rest/activities/" + codingUtilities.toUtf(activity.getId()) + "/events/" + codingUtilities.toUtf(eventId))
                .request().header("Cookie", getSessionId(user1Email))
                .get();

        retEvent=response.readEntity(Event.class);
        assertEquals(false,retEvent.getParticipants().contains("testuser2"));

    }

    private List<Activity> getActivities(String status) {

        Response response = client.target(hostname + "/rest/activities/byUser/me")
                .queryParam("status",status)
                .request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});
        return retActs;
    }

    @Test
    public void getActivitiesTest() throws JSONException, UnsupportedEncodingException {

        List<Activity> retActs=getActivities(Status.OPEN);
        assertEquals(2, retActs.size()); //hardcoded value here

        //find an activity that has events
        Activity testActivity=null;
        for (Activity activity:retActs) {
            if (activity.getTitle().equalsIgnoreCase("Plumbing work")) {
                testActivity=activity;
                break;
            }
        }
        Response response = client.target(hostname + "/rest/activities/" + URLEncoder.encode(testActivity.getId(), "UTF-8"))
                .request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true,response.hasEntity());
        Activity retAct=response.readEntity(Activity.class);
        assertEquals(testActivity.getTitle(),retAct.getTitle());
        assertEquals(2,retAct.getEvents().size());

    }

    @Test
    public void getActivitiesByTag() {

        Response response = client.target(hostname + "/rest/activities/")
                .queryParam("tag", "water").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});
        assertEquals(1, retActs.size());//hardcoded value here, there are actually two but one is closed status

    }

    @Test
    public void getActivitiesByLocation() throws JSONException, InterruptedException {

        Response response = client.target(hostname + "/rest/activities/")
                .queryParam("lat", 44.842111)
                .queryParam("lon", 2.349897)
                .queryParam("radius",5)
                .request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});
        assertNotEquals(2, retActs.size());

    }

    @Test
    public void getEventsByUser() {

        Response response=client.target(hostname + "/rest/activities/byUser/me/events")
                .request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        List<Event> retEvents=response.readEntity(new GenericType<List<Event>>() {
        });
        assertEquals(2, retEvents.size());
    }


    @Test
    public void deleteActivityTest() throws UnsupportedEncodingException {

        Response response = client.target(hostname + "/rest/activities/byUser/me")
                .request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});
        String actId=retActs.get(0).getId();
        response = client.target(hostname + "/rest/activities/" + URLEncoder.encode(actId, "UTF-8"))
                .request().header("Cookie", getSessionId()).delete();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        response = client.target(hostname + "/rest/activities/" + URLEncoder.encode(actId, "UTF-8")).request().header("Cookie", getSessionId()).get();
        assertEquals(200,response.getStatus());


    }

}
