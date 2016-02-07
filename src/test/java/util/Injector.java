package util;

import es.revib.server.rest.entities.Activity;
import org.codehaus.jettison.json.JSONException;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * used to inject real time in the database using the application running now, much like an integration test but on the current endpoint
 */
public class Injector {

    String hostname="http://localhost:8080";
    Client client = ClientBuilder.newClient().register(JacksonFeature.class);
    String sessionId=null;
    String foreignSessionId=null;
    String userId; //this is our testuser1 userid which changes everytime
    String username="testuser1";

    public void createActivity() {

 /*       Activity activity=new Activity();
        activity.setTitle("a title");
        activity.setDescription("whatever");
        activity.setAddress("an address");
        activity.setDate(1418725300000L);
        activity.setLat(48.842111);
        activity.setLon(1.001);
        activity.setType("Offer");
        List<String> tags=new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");
        activity.setTags(tags);

        Entity<Activity> ent= Entity.entity(activity, MediaType.APPLICATION_JSON);
        Response response = client.target(hostname+"/rest/activities").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);
        System.out.println(response.getStatus());*/

    }

    public String getSessionId(String user) {

        String defaultPassword="opensesame";

        if (sessionId==null) {
            Form f = new Form();
            f.param("user", user);
            f.param("password", defaultPassword);

            Response response = client.target(hostname + "/rest/auth/login").request()
                    .post(Entity.form(f));
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            this.sessionId=response.getHeaderString("Set-Cookie");
            return sessionId;
        }
        else {
            return sessionId;
        }
    }

    public String getSessionId() {
        return getSessionId("testuser1");
    }

    public static void main(String args[]) throws JSONException, IOException {

        Injector injector=new Injector();
        injector.createActivity();

    }

}
