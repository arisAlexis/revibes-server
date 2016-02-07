package integration;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import config.TestServicesFactory;
import es.revib.server.rest.dao.BaseGraphHelper;
import es.revib.server.rest.dao.OrientGraphHelper;
import es.revib.server.rest.database.OrientDatabase;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.*;

import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SearchIntegrationTest extends IntegrationTest{

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
    public void connectedTagSearchTest() throws JSONException {

        //todo we could replace this orient specific code with multiple creations of activities but it's too much overhead

        //here we will also test the graphhelper connect tags function
        BaseGraphHelper graphHelper= TestServicesFactory.getGraphHelper();
        TransactionalGraph g=OrientDatabase.getInstance().getGraphTx();
        List<Vertex> tags=new ArrayList<>();
        Vertex v=g.addVertex("class:Tag");
        v.setProperty("tagName", "macaroni");
        g.commit();
        tags.add(v);
        v=g.addVertex("class:Tag");
        v.setProperty("tagName", "salsa");
        g.commit();
        tags.add(v);
        v=g.addVertex("class:Tag");
        v.setProperty("tagName", "cheese");
        g.commit();
        tags.add(v);

        g.commit();

        graphHelper.connectVertices(tags,"count");
        g.commit();

        //and now to create one more connection with only the first two remove the cheese
        Iterator<Vertex> vertexIterator=tags.listIterator();
        while (vertexIterator.hasNext()) {
            Vertex vertex=vertexIterator.next();
            if (vertex.getProperty("tagName").equals("cheese")) {
                vertexIterator.remove();
            }
        }

        graphHelper.connectVertices(tags,"count");
        g.commit();

        //now when we ask for relevant tags for pasta we should get first salsa and then cheese
        Response response=client.target(hostname+"/rest/search/tags/macaroni").request().get();
        assertEquals(true, response.hasEntity());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String res=response.readEntity(String.class);
        JSONObject jtags=new JSONObject(res);
        assertNotEquals(0,jtags.length());
        assertEquals(true, jtags.getJSONArray("tags").getJSONObject(0).getString("tagName").equals("salsa"));

    }

/*    @Ignore
    @Test
    public void trendingTagTest() throws JSONException {

        Response response=client.target(hostname+"/rest/search/tags/trending").request().get();
        assertEquals(true, response.hasEntity());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String res=response.readEntity(String.class);
        JSONObject tags=new JSONObject(res);
        assertNotEquals(0,tags.length());
        JSONArray jsonArray=tags.getJSONArray("tags");
        assertEquals("education", jsonArray.getJSONObject(0).getString("tagName"));
        assertEquals("programming",jsonArray.getJSONObject(1).getString("tagName"));
        assertEquals("random",jsonArray.getJSONObject(2).getString("tagName"));

    }*/

    @Test
    public void tagSearchTest() throws JSONException {

        //first test the tags with prefix
        Response response=client.target(hostname+"/rest/search/tags/").queryParam("prefix","wat").request().get();
        assertEquals(true, response.hasEntity());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String res=response.readEntity(String.class);
        JSONObject tags=new JSONObject(res);
        assertNotEquals(0,tags.length());
        assertEquals(true,tags.getJSONArray("tags").getJSONObject(0).getString("tagName").startsWith("wat"));

    }

    @Test
    public void userSearchTest() throws JSONException {

        Response response=client.target(hostname+"/rest/search/users/").queryParam("prefix","tes").request().get();
        assertEquals(true, response.hasEntity());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String res=response.readEntity(String.class);
        JSONObject users=new JSONObject(res);
        assertEquals(1,users.length());
        assertEquals(true, users.getJSONArray("users").getJSONObject(0).getString("name").startsWith("tes"));
    }

}
