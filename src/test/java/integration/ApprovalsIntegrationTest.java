package integration;

import es.revib.server.rest.entities.Activity;
import es.revib.server.rest.entities.Entities;
import es.revib.server.rest.entities.User;
import org.junit.*;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ApprovalsIntegrationTest extends IntegrationTest{

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

    @Test
    public void approveLinkTest() throws InterruptedException, UnsupportedEncodingException {

        String link="https://duckduckgo.com/arisalexis.jpg";

        //first for user

        Response response=client.target(hostname + "/rest/approval/link")
                .queryParam("referenceType", Entities.USER)
                .queryParam("referenceId", "arisalexis")
                .queryParam("url", link)
                .request().header("Cookie", getSessionId()).get();

        /* maybe throttle because its a little random which will hit the endpoint first and we hit the callback
        from the DummyApprovalService
        */
        Thread.sleep(1000);

        //now check our profile to see if the foto was approved and is included in our images
        response= client.target(hostname+"/rest/users/me").request().header("Cookie", getSessionId()).get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        User user = response.readEntity(User.class);
        assertEquals(true, user.getImages().contains(link));

        //now the same for an activity
        //we need to get the ID of one so we will hit the front endpoint
        response = client.target(hostname + "/rest/activities/byUser/me").request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        List<Activity> retActs=response.readEntity(new GenericType<List<Activity>>() {});
        String id=retActs.get(0).getId();

        response=client.target(hostname + "/rest/approval/link")
                .queryParam("referenceType", Entities.ACTIVITY)
                .queryParam("referenceId",id)
                .queryParam("url", link)
                .request().header("Cookie", getSessionId()).get();

        //now check our profile to see if the foto was approved and is included in our images
        response= client.target(hostname+"/rest/activities/"+ URLEncoder.encode(id,"UTF-8")).request().header("Cookie", getSessionId()).get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        Activity activity = response.readEntity(Activity.class);
        assertEquals(true, activity.getImages().contains(link));
    }
}
