package integration;

import es.revib.server.rest.entities.*;
import org.codehaus.jettison.json.JSONException;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class StreamsIntegrationTest extends IntegrationTest {

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

    /**
     * this also indirectly tests the revibe feature and the like and comments , geolocation and timestamp so it's a test All streams
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    @Test
    public void allStreamsTest() throws JSONException, UnsupportedEncodingException {

        //post a new activity and update our profile and then check them both
        Activity activity=new Activity();
        activity.setTitle("atitle");
        activity.setDescription("whatever");
        activity.setType(Verbs.OFFER);
        activity.setLat(40.842111);
        activity.setLon(1.349897);
        List<String> tags=new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");
        activity.setTags(tags);
        User u1=new User();
        u1.setId(userId);
        u1.setUsername("test user");
        activity.setOwner(u1);
        activity.setMainImage("plumber.jpg");

        Entity<Activity> ent= Entity.entity(activity, MediaType.APPLICATION_JSON);
        Response response =client.target(hostname + "/rest/activities").request(MediaType.APPLICATION_JSON)
                .header("Cookie", getSessionId())
                .post(ent);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true,response.hasEntity());
        Activity retAct=response.readEntity(Activity.class);

        response=client.target(hostname + "/rest/users/me").request()
                .header("Cookie", getSessionId())
                .get();
        User us=response.readEntity(User.class);
        us.setDescription("a different description");
        response = client.target(hostname + "/rest/users/" + URLEncoder.encode(us.getId(), "UTF-8")).request(MediaType.APPLICATION_JSON)
                .header("Cookie", getSessionId())
                .put(Entity.entity(us, MediaType.APPLICATION_JSON));

        response = client.target(hostname + "/rest/streams/byUser/me/").request()
                .header("Cookie", getSessionId(user3Email))
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        List<Info> streams=response.readEntity(new GenericType<List<Info>>() {});
        assertNotEquals(0,streams.size());
        boolean existsProfileUpdate=false;
        boolean existsNewActivity=false;
        Info stream=null;
        for (Info s : streams) {
            if (s.getAction().equalsIgnoreCase(Action.UPDATE_PROFILE) && s.getSourceDisplayNames().get(0).equalsIgnoreCase("tester testious")) {
                existsProfileUpdate=true;
            }
            if (s.getAction().equals(Action.OFFER_ACTIVITY) && s.getTargetIds().get(0).equals(retAct.getId())) {
                existsNewActivity=true;
                stream=s;
            }
        }
        assertEquals(true, existsProfileUpdate);
        assertEquals(true,existsNewActivity);

        //commenting on ari's stream
        Comment comment=new Comment();
        comment.setReferenceId(stream.getId());
        comment.setBody("mucho gusto mi primo");

        response=client.target(hostname + "/rest/streams/" + codingUtilities.toUtf(stream.getId()) + "/comments").request()
                .header("Cookie", getSessionId(user3Email))
                .post(Entity.entity(comment, MediaType.APPLICATION_JSON_TYPE));

        comment=response.readEntity(Comment.class);

        response=client.target(hostname + "/rest/streams/" + codingUtilities.toUtf(stream.getId()) + "/comments/" + codingUtilities.toUtf(comment.getId()) + "/like")
                .request().header("Cookie", getSessionId())
                .post(null);

        response = client.target(hostname + "/rest/streams/" + codingUtilities.toUtf(stream.getId()))
                .request().header("Cookie", getSessionId(user3Email))
                .get();

        stream=response.readEntity(Info.class);

        assertEquals("mucho gusto mi primo",stream.getComments().get(0).getBody());
        assertEquals("tester testious",stream.getComments().get(0).getLikers().get(0).getUsername());

        //test revibe feature where testuser2 is not friends with test and so she gets this stream from testuser3

        Info retweetedStream=streams.get(0);
        client.target(hostname + "/rest/streams/" + codingUtilities.toUtf(retweetedStream.getId()) + "/revibe")
                .request().header("Cookie", getSessionId(user3Email))
                .post(Entity.text(""));

        response = client.target(hostname + "/rest/streams/byUser/me/")
                .request().header("Cookie", getSessionId(user2Email))
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        streams=response.readEntity(new GenericType<List<Info>>() {});
        assertNotEquals(0, streams.size());

        Predicate<Info> exactMatch=(Info i)->i.getId().equals(retweetedStream.getId());
        Optional<Info> r=streams.stream().filter(exactMatch).findFirst();
        assertTrue(r.isPresent());
        assertEquals(r.get().getOrigin(), Info.ORIGIN_RETWEET);

        //geolocation.user2 is not friends with test she is receiving it only because its close to her

        response = client.target(hostname + "/rest/streams/byUser/me/")
                .queryParam("lat", 40.842111)
                .queryParam("lon", 1.349897)
                .request().header("Cookie", getSessionId(user2Email))
                .get();

        streams=response.readEntity(new GenericType<List<Info>>() {});
        assertTrue(streams.stream().anyMatch(st->st.ORIGIN_GEOLOCATION.equals(st.getOrigin())));

    }

}
