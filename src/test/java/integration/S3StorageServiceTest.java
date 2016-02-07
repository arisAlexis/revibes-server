package integration;

import es.revib.server.rest.storage.IStorageService;
import es.revib.server.rest.storage.S3StorageService;
import es.revib.server.rest.util.Globals;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class S3StorageServiceTest  {

    Client client= ClientBuilder.newClient().register(JacksonFeature.class);

    @Test
    public void uploadDeleteFile() throws URISyntaxException {

        Globals.S3_BUCKET="revib.es";
        Globals.S3_FOLDER="test";
        Globals.AWS_ACCESS_KEY="";
        Globals.AWS_SECRET_KEY="";

        IStorageService storageService=new S3StorageService();
        URL url=S3StorageService.class.getClassLoader().getResource("img/error.png");
        File imgFile=new File(url.toURI());
        storageService.store("error.png", imgFile);

        Response response = client.target("http://revib.es.s3.amazonaws.com/test/error.png")
                .request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        storageService.delete("error.png");
        response = client.target("http://revib.es.s3.amazonaws.com/test/error.png")
                .request().get();
        assertNotEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }


}
