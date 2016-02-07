package integration;

import es.revib.server.rest.entities.*;
import es.revib.server.rest.entities.request.EventRequest;
import es.revib.server.rest.entities.request.FriendRequest;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.*;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class UsersIntegrationTest extends IntegrationTest {

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
    //we are not using usernames anymore
    public void checkUser() throws JSONException {
        Form f=new Form();
        f.param("email","testuser1@test.com");

        Response response = client.target(hostname + "/rest/users/check")
                .request().header("Cookie", getSessionId()).post(Entity.form(f));
        assertEquals(true, response.hasEntity());
        String responseString=response.readEntity(String.class);
        JSONObject jsonObject=new JSONObject(responseString);
        assertEquals("false",jsonObject.getString("valid"));

        f=new Form();
        f.param("email","snowboard567@gmail.com");
        response = client.target(hostname + "/rest/users/check")
                .request().header("Cookie", getSessionId()).post(Entity.form(f));
        assertEquals(true, response.hasEntity());
        responseString=response.readEntity(String.class);
        jsonObject=new JSONObject(responseString);
        assertEquals("true",jsonObject.getString("valid"));

    }

    @Test
    public void getUserTest() throws JSONException {

        Response response = client.target(hostname+"/rest/users/testuser3@test.com").request().header("Cookie", getSessionId()).get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        User user = response.readEntity(User.class);
        assertEquals("testuser3", user.getUsername().toString());

        //test for private data
        assertEquals(null, user.getAddress());
        assertEquals(null, user.getPhone());
        assertEquals(null, user.getPassword()); //most important :)

        response = client.target(hostname + "/rest/users/fgjrlkfjw42422").request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

    }

    @Test
    public void createUserTest() throws JSONException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        User user = new User();
        user.setFirstName("firstname");
        user.setLastName("lastname");

        Entity<User> ent = Entity.entity(user, MediaType.APPLICATION_JSON);
        //first test without all arguments to make sure it fails
        Response response = client.target(hostname + "/rest/users").request(MediaType.APPLICATION_JSON).post(ent);
        assertEquals(400, response.getStatus());

        user.setEmail("mp@l.com");

        Map password=new HashMap<>();
        password.put("text", "opensesame");
        user.setPassword(password);

        List<Map> notifications=new ArrayList<>();
        Info notification=new Info();
        notification.setTargetType(Entities.ACTIVITY);
        notification.setAction(Action.UPDATE_EVENT);
        notifications.add(notification.toMap());
        user.setNotifications(notifications);

        user.setAddress("address 8");
        user.setRegistrationDate(145500);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set violations=validator.validate(user);

        ent = Entity.entity(user, MediaType.APPLICATION_JSON);

        response = client.target(hostname + "/rest/users").request(MediaType.APPLICATION_JSON).post(ent);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        User retUser = response.readEntity(User.class);
        user.setId(retUser.getId());
        assertEquals(user, retUser);

    }

    /**
     * we will make a full integration test with sending a friend request, then reject it then send it again then accept it and also delete a friend
     */
    @Test
    public void friendRequestTest() throws JSONException {

        //delete him
        Response response = client.target(hostname + "/rest/users/me/friends/testuser3@test.com")
                .request().header("Cookie", getSessionId()).delete();
        assertEquals(Response.Status.OK.getStatusCode(),response.getStatus());
        //get my friends list
        response = client.target(hostname + "/rest/users/me/friends").request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        List<User> retFriends=response.readEntity(new GenericType<List<User>>() {});
        boolean contained=false;
        for (User u:retFriends) {
            if (u.getFirstName().equals("test3")) {
                contained=true;
                break;
            }
        }
        assertEquals(false, contained);

        //send a new request
        FriendRequest friendRequest=new FriendRequest();
        friendRequest.setRequestMessage("pleaseee?");

        response = client.target(hostname + "/rest/users/me/friends/testuser3@test.com").request()
                .header("Cookie", getSessionId()).post(Entity.entity(friendRequest,MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK.getStatusCode(),response.getStatus());

        friendRequest=response.readEntity(FriendRequest.class);

        //see if we have a notification as testuser3 now and also test a bit the notifications
        response = client.target(hostname + "/rest/auth/echo/").request()
                .header("Cookie", getSessionId(user3Email)).get();
        assertEquals(true, response.hasEntity());
        String res=response.readEntity(String.class);
        JSONObject userData=new JSONObject(res);
        JSONArray notifications=userData.getJSONArray("notifications");
        boolean found=false;
        for (int i=0;i<notifications.length();i++){
            JSONObject jsonObject=notifications.getJSONObject(i);
            if (jsonObject.get("action").equals(Action.SEND_FRIEND_REQUEST) && jsonObject.getJSONArray("targetDisplayNames").get(0).equals("testuser3")){
                    found = true;
            }
        }
        assertEquals(true, found);

        response = client.target(hostname + "/rest/users/me/friends/requests/"+codingUtilities.toUtf(friendRequest.getId()))
                .queryParam("action",Verbs.ACCEPT)
                .request()
                .header("Cookie", getSessionId(user3Email)).get();
        assertEquals(Response.Status.OK.getStatusCode(),response.getStatus());

        //get my friends list
        response = client.target(hostname + "/rest/users/me/friends/").request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        retFriends=response.readEntity(new GenericType<List<User>>() {});
        assertNotEquals(0,retFriends.size());
        contained=false;
        for (User u:retFriends) {
            if (u.getEmail().equals(user3Email)) {
                contained=true;
                break;
            }
        }
        assertEquals(true,contained);
    }


    @Test
    public void getFriendsTest() {

        //todo make it more thorough

        Response response = client.target(hostname + "/rest/users/me/friends/").request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<User> retFriends=response.readEntity(new GenericType<List<User>>() {});
        assertNotEquals(0, retFriends.size());

    }

    @Test
    public void getUsersByLocation() {

        User user = new User();
        user.setFirstName("firstname");
        user.setLastName("lastname");
        user.setEmail("m@l.com");
        Map password=new HashMap<>();
        password.put("text","opensesame");
        user.setPassword(password);
        user.setAddress("address 8");
        user.setBirthdate(15455);
        user.setLat(48.842111);
        user.setLon(2.349897);

        Entity<User> ent = Entity.entity(user, MediaType.APPLICATION_JSON);

        Response response = client.target(hostname + "/rest/users").request(MediaType.APPLICATION_JSON).post(ent);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        User retUser = response.readEntity(User.class);
        user.setId(retUser.getId());
        assertEquals(user, retUser);

        response = client.target(hostname + "/rest/users/")
                .queryParam("lat",48.842111)
                .queryParam("lon",2.349897)
                .queryParam("radius",15)
                .request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<User> retUsers=response.readEntity(new GenericType<List<User>>() {});
        assertNotEquals(0, retUsers.size());
        //todo add test to see if they are really inside the radius
    }

    @Test
    public void deleteNotificationTest() throws JSONException {

        Response response = client.target(hostname + "/rest/users/"+codingUtilities.toUtf(userId)+"/notifications").request().header("Cookie", getSessionId()).get();
        assertEquals(true, response.hasEntity());
        String res=response.readEntity(String.class);
        JSONArray notifications=new JSONArray(res);
        assertEquals(2,notifications.length());
        String id=notifications.getJSONObject(0).getString("id");
        response = client.target(hostname + "/rest/users/me/notifications/"+codingUtilities.toUtf(id)).request().header("Cookie", getSessionId()).delete();
        response = client.target(hostname + "/rest/users/"+codingUtilities.toUtf(userId)+"/notifications").request().header("Cookie", getSessionId()).get();
        res=response.readEntity(String.class);
        notifications=new JSONArray(res);
        assertEquals(1,notifications.length());

    }

    @Test
    public void getRequestsTest() throws UnsupportedEncodingException, JSONException {

        //here we send a friend request and a participation request to an event owned by testuser1 and then test the output
        //send a new request
        FriendRequest friendRequest=new FriendRequest();
        friendRequest.setRequestMessage("pleaseee?");
        Response response = client.target(hostname + "/rest/users/me/friends/"+user2Email).request().header("Cookie", getSessionId()).post(Entity.entity(friendRequest,MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK.getStatusCode(),response.getStatus());

        //get all our activities
        response = client.target(hostname + "/rest/activities/byUser/me").request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});
        EventRequest eventRequest=new EventRequest();
        eventRequest.setType(Action.ADD_EVENT);
        eventRequest.setRequestMessage("hey u wanna do this together?");
        eventRequest.setAddress("address 1");
        eventRequest.setLat(34.09);
        eventRequest.setLon(2.3);
        eventRequest.setDate(1408725300056L);

        response = client.target(hostname + "/rest/activities/"+ URLEncoder.encode(retActs.get(0).getId(), "UTF-8")+"/requests")
                .request().header("Cookie", getSessionId(user3Email)).post(Entity.entity(eventRequest, MediaType.APPLICATION_JSON));

        response = client.target(hostname + "/rest/users/me/requests").request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        JSONArray requests=new JSONArray(response.readEntity(String.class));
        assertEquals(2,requests.length());

    }

    @Test
    public void getRatingsTest() throws UnsupportedEncodingException {

        //first we need to create a rating
        Response response=client.target(hostname + "/rest/activities/byUser/me")
                .queryParam("status", Status.ALL)
                        .request(MediaType.APPLICATION_JSON_TYPE).header("Cookie", getSessionId()).get();
        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});
        //find an activity with a completed event
        String activityId="";
        String eventId="";

        for (Activity activity:retActs) {
            for (Event event:activity.getEvents()) {
                if (event.getStatus().equalsIgnoreCase(Status.COMPLETED)) {
                    activityId=activity.getId();
                    eventId=event.getId();
                    break;
                }
            }
        }

        assertFalse(eventId.isEmpty());

        Rating rating=new Rating();
        rating.setReview("good man");
        rating.setRating(1);
        User u1=new User();
        u1.setId(userId);
        u1.stripMinimal();
        rating.setReceivingUser(u1);

        response=client.target(hostname + "/rest/activities/" +
                URLEncoder.encode(activityId, "UTF-8") + "/events/" +
                URLEncoder.encode(eventId, "UTF-8") + "/rating")
                .request(MediaType.APPLICATION_JSON_TYPE).header("Cookie", getSessionId(user3Email))
                .post(Entity.entity(rating, MediaType.APPLICATION_JSON_TYPE));

        response=client.target(hostname+"/rest/users/me/ratings").request().header("Cookie", getSessionId()).get();
        List<Rating> ratings=response.readEntity(new GenericType<List<Rating>>() {});
        assertEquals(1,ratings.size());
        assertEquals(eventId,ratings.get(0).getEvent().getId());

    }

    @Test
    public void updateUserTest() throws JSONException, UnsupportedEncodingException {

        Response response = client.target(hostname + "/rest/users/me").request(MediaType.APPLICATION_JSON).header
                ("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        User user=response.readEntity(User.class);

        user.setDescription("Paris legend");
        response = client.target(hostname + "/rest/users/" + URLEncoder.encode(user.getId(), "UTF-8"))
                .request(MediaType.APPLICATION_JSON).header
                ("Cookie", getSessionId()).put(Entity.entity(user, MediaType.APPLICATION_JSON));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true,response.hasEntity());
        User updUser=response.readEntity(User.class);
        assertEquals(user,updUser);
    }

    @Ignore
    @Test
    public void deleteUserTest() {

            //try to delete someone else
            Response response = client.target(hostname + "/rest/users/" + user3Email).request().header("Cookie", getSessionId()).delete();
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

            //delete ourself
            response = client.target(hostname + "/rest/users/" + userId).request().header("Cookie", getSessionId()).get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            response = client.target(hostname + "/rest/users/" + userId).request().header("Cookie", getSessionId()).delete();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            //try to login to see if we got  invalidated
            Form f = new Form();
            f.param("user", "arisalexis");
            f.param("password", "petten");

            response = client.target(hostname + "/rest/auth/login").request().post(Entity.form(f));
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

    }

}
