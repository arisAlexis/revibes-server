package integration;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class StorageIntegrationTest extends IntegrationTest {


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
    public void uploadDeleteFile() throws IOException, JSONException, URISyntaxException {

        MultiPart multiPart = new MultiPart();
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file",
                new File(StorageIntegrationTest.class.getClassLoader().getResource("img/error.png").toURI()), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        multiPart.bodyPart(fileDataBodyPart);
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

        client.register(MultiPartFeature.class);

        Response response = client.target(hostname+"/rest/storage/upload")
                .request(MediaType.MULTIPART_FORM_DATA_TYPE)
                .header("Cookie", getSessionId())
                .post(Entity.entity(multiPart, multiPart.getMediaType()));

        assertEquals(200,response.getStatus());
        assertEquals(true,response.hasEntity());

        //get the url and test if the file exists
        String responseString=response.readEntity(String.class);
        JSONObject jsonObject=new JSONObject(responseString);
        String url=jsonObject.getString("url");
        response=client.target(url).request().get();
        assertEquals(200,response.getStatus());

        //now delete it
        String tmp[]=url.split("\\/");
        String filename=tmp[tmp.length-1];
        response=client.target(hostname+"/rest/storage/delete/")
                .queryParam("filename",filename)
                .request()
                .header("Cookie", getSessionId())
                .get();
        assertEquals(200,response.getStatus());
        response=client.target(url).request().get();
        assertNotEquals(200, response.getStatus());
    }
}
