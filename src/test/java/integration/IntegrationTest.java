package integration;

import com.corundumstudio.socketio.Configuration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import config.App;
import es.revib.server.rest.broker.SocketIOServerImpl;
import es.revib.server.rest.util.Globals;
import db.OrientDBUtil;
import com.tinkerpop.blueprints.TransactionalGraph;
import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.util.CodingUtilities;
import es.revib.server.rest.util.HttpClientFactory;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public abstract class IntegrationTest {


    CodingUtilities codingUtilities=new CodingUtilities();

    static Client client=HttpClientFactory.getSslClient();

    static String user1Email="testuser1@test.com";
    static String user3Email="testuser3@test.com";
    static String user2Email="testuser2@test.com";

    static String defaultPassword="petten";
    static String hostname="https://localhost:8443/exchange";

    static String userId; //this is our testuser1 userid which changes everytime

    static OrientDBUtil orientDBUtil;

    public static void customInit() throws Exception {

        //GLOBALS
        Globals.S3_BUCKET="revib.es";
        Globals.S3_FOLDER="test";
        Globals.AWS_ACCESS_KEY="";
        Globals.AWS_SECRET_KEY="";
        Globals.SERVER_URL=hostname;
        Globals.AWS_SMTP_PASSWORD="";
        Globals.NO_REPLY_EMAIL="";
        Globals.TMP_DIR="/tmp";

        //ORIENTDB
        orientDBUtil=new OrientDBUtil();

        //TOMCAT
        String certPath=IntegrationTest.class.getClassLoader().getResource("tomcat_self.cert").getPath();
        String webAppPath=IntegrationTest.class.getClassLoader().getResource("webapp/").getPath();

        App.start(webAppPath,null ,certPath,"tomcat", "petten", 8082,8443);

        //SOCKET.IO
        Configuration configuration=new Configuration();
        configuration.setPort(8444);
        configuration.setKeyStorePassword("ioioio");

        InputStream stream = null;
        try {
            stream = new FileInputStream(certPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        SocketIOServerImpl.configuration=configuration;
        SocketIOServerImpl.getInstance();

    }

    public static void customShutdown() throws Exception {

        orientDBUtil.shutdown();

    }

    /**
     * Populate the database with some users before each test and delete them afterwards
     */
    public void setUp() throws Exception {

        orientDBUtil.populate();
        //prime it for tests
         userId=orientDBUtil.getDefaultUserId();

    }

    public void tearDown() throws Exception {

        orientDBUtil.empty();

    }

    public String getSessionId(String user) {

            Form f = new Form();
            f.param("user", user);
            f.param("password", defaultPassword);

            Response response = client.target(hostname + "/rest/auth/login").request()
                    .post(Entity.form(f));
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            String sessionId=response.getHeaderString("Set-Cookie");
            return sessionId;

    }

    public String getSessionId() {
        return getSessionId(user1Email);
    }

}
