package integration;

import es.revib.server.rest.kv.IKVStore;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.kv.OrientKVStore;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AuthorizationIntegrationTest extends IntegrationTest {

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
    public void echoTest() throws JSONException {

        //now test for getting our profile (if we are logged in)
        Response response = client.target(hostname + "/rest/auth/echo").request().header("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());
        JSONObject juser=new JSONObject(response.readEntity(String.class));
        assertEquals("testuser1", juser.get("username"));
        assertEquals(false, juser.has("password"));

    }

    @Test
    /**
     * this test doesn't actually test the email service which has its own integration test
     * but only the auth functionality
     */
    public void emailVerifyTest() throws InterruptedException, UnsupportedEncodingException {
        //we will update our user with a new email
        Response response = client.target(hostname + "/rest/users/testuser1@test.com").request(MediaType.APPLICATION_JSON).header
                ("Cookie", getSessionId()).get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        User user=response.readEntity(User.class);

        user.setEmail("snow3@gmail.com"); //doesn't matter if it doesn't exist
        response = client.target(hostname + "/rest/users/" + URLEncoder.encode(user.getId(), "UTF-8"))
                .request(MediaType.APPLICATION_JSON).header
                        ("Cookie", getSessionId()).put(Entity.entity(user, MediaType.APPLICATION_JSON));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true,response.hasEntity());
        User updUser=response.readEntity(User.class);
        assertEquals(false,updUser.getEmailVerified());

        //first hit the auth endpoint with a random token
        response = client.target(hostname+"/rest/auth/emailVerify")
                .queryParam("user",codingUtilities.toUtf(user.getId()))
                .queryParam("token","adfjaksf")
                .request()
                .get();

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        //read the value from the kvstore and hit the auth endpoint

        String token= null;
        try {
            IKVStore kvStore= new OrientKVStore();
            token = kvStore.get("emailVerify"+user.getId()).getString("value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        response = client.target(hostname+"/rest/auth/emailVerify")
                .queryParam("user",codingUtilities.toUtf(user.getId()))
                .queryParam("token", token)
                .request()
                .get();

        response = client.target(hostname + "/rest/users/snow3@gmail.com").request(MediaType.APPLICATION_JSON).header
                ("Cookie", getSessionId("testuser2@test.com")).get();
        updUser=response.readEntity(User.class);
        assertEquals(true,updUser.getEmailVerified());

    }


    @Test
    public void loginTest() {

        Form f = new Form();
        f.param("user",user1Email);
        f.param("password", defaultPassword);

        Response response = client.target(hostname + "/rest/auth/login").request().post(Entity.form(f));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        //f.param just adds one more instead of replacing values
        f = new Form();
        f.param("user", user1Email);
        f.param("password", "wron");

        response = client.target(hostname + "/rest/auth/login").request().post(Entity.form(f));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

    }

    @Test
    public void logoutTest() {

        Response response = client.target(hostname + "/rest/auth/logout").request().header("Cookie", getSessionId()).get();
        //this is not going to work now because our sessionId got invalidaded
    }

    @Test
    public void resetPasswordTest() {

        String email="testuser2@test.com";
        Response response = client.target(hostname + "/rest/auth/resetPassword")
                .queryParam("email",email).request().get();

        //read the value from the kvstore and hit the auth endpoint
        IKVStore kvStore= new OrientKVStore();
        String token= null;
        try {
            token = kvStore.get("resetPassword"+email).getString("value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Form f = new Form();
        f.param("email", email);
        f.param("password", "yoohoo");
        f.param("token",token);
        response =client.target(hostname + "/rest/auth/resetPassword")
                .request()
                .post(Entity.form(f));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        f = new Form();
        f.param("user", "testuser2@test.com");
        f.param("password", "yoohoo");

        response = client.target(hostname + "/rest/auth/login").request().post(Entity.form(f));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }

}
