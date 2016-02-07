package db;

import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;
import es.revib.server.rest.dao.OrientGraphHelper;
import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.entities.Activity;
import es.revib.server.rest.entities.Entities;
import es.revib.server.rest.entities.Event;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.util.CodingUtilities;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Central script that populates and empties all the databases for testing
 */
public class OrientDBUtil {

    public static OServer oServer=null;

    private static void createServer() {
        try {
            if (oServer==null) {
                oServer = OServerMain.create();
                oServer.startup(OrientDBUtil.class.getClassLoader().getResourceAsStream("orientdb-server-config.xml"));
                oServer.activate();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUpDatabase() {

        boolean inmemory=true;

        if (inmemory) {
            createServer();
            OrientDatabase.setUser("testuser");
            OrientDatabase.setPassword("test123");
            OrientDatabase.setUrl("memory:");
        }
        else {
            OrientDatabase.setUser("root");
            OrientDatabase.setPassword("root123");
            OrientDatabase.setUrl("remote:localhost");
        }

        OrientDatabase.setHTTP_URL("http://localhost:2480");
        g=OrientDatabase.getInstance().getGraphTx();
        documentDatabase=OrientDatabase.getInstance().getDocumentDb();
        orientGraphHelper=new OrientGraphHelper(g);
        buildStructure();

    }

    public String test_data_dir = "";
    TransactionalGraph g ;
    ODatabaseDocumentTx documentDatabase;

    OrientGraphHelper orientGraphHelper;
    CodingUtilities codingUtilities=new CodingUtilities();

    /**
     * this method is used to build originally the structure of the database especially if its an in-memory one for tests
     */
    public void buildStructure() {

        //we suppose here that the test_data_dir and the databases are configured
        try {
            documentDatabase.command(new OCommandScript(FileUtils.readFileToString(new File(test_data_dir + "/orient_messaging_structure.script")))).execute();
            OrientDatabase.getInstance().getGraphNoTx().command(new OCommandScript(FileUtils.readFileToString(new File(test_data_dir + "/orient_exchange_structure.script")))).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void populate() {
        //we suppose here that the test_data_dir and the databases are configured
        try {
            //documentDatabase.command(new OCommandScript(FileUtils.readFileToString(new File(test_data_dir + "/orient_messaging_structure.script")))).execute();
            OrientDatabase.getInstance().getGraphNoTx().command(new OCommandScript(FileUtils.readFileToString(new File(test_data_dir + "/orient_exchange_insert_data.script")))).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public OrientDBUtil() {
        String test_data_dir=OrientDBUtil.class.getClassLoader().getResource("test_data").getPath();
        this.test_data_dir=System.getProperty( "os.name" ).contains( "indow" ) ? test_data_dir.substring(1):test_data_dir;
        setUpDatabase();
    }

    public String getDefaultUserId() {
        Vertex v = g.getVertices("username", "testuser1").iterator().next();
        return v.getId().toString();
    }

    public OrientDBUtil(String test_data_dir) {
        this.test_data_dir=test_data_dir;
        setUpDatabase();
    }

    //quick custom stuff
    public static void main(String[] args) throws Exception {

        String test_data_dir="";
        String url="";
        String user="";
        String pass="";
        String httpUrl="";

        for (String s:args) {
            if (s.startsWith("--test_data_dir=")) {
                test_data_dir=s.split("=")[1];
                break;
            }
            if (s.startsWith("--url=")) {
                url=s.split("=")[1];
                break;
            }
            if (s.startsWith("--user")) {
                user=s.split("=")[1];
                break;
            }
            if (s.startsWith("--pass")) {
                pass=s.split("=")[1];
                break;
            }
            if (s.startsWith("--http_url=")) {
                httpUrl=s.split("=")[1];
                break;
            }
        }

        if (url.isEmpty() || user.isEmpty() || pass.isEmpty() || test_data_dir.isEmpty() || httpUrl.isEmpty()) {
            System.out.println("usage: \n" +
                    "--url example memory: or remote:localhost or plocal:../dir \n"+
                    "--user \n" +
                    "--pass \n" +
                    "--test_data_dir \n" +
                    "--http_url\n");
        }

        OrientDatabase.setUser(user);
        OrientDatabase.setPassword(pass);
        OrientDatabase.setUrl(url);
        OrientDatabase.setHTTP_URL(httpUrl);

        OrientDBUtil orientDBUtil=new OrientDBUtil();
        orientDBUtil.populate();
        orientDBUtil.shutdown();

    }

    public void shutdown() {

        //documentDatabase.close();
        g.shutdown();

/*
  //we leave this open because the next test will run and the database will not be fast enough to close/open (it's not a synced operation).
  if (oServer!=null) {
            oServer.shutdown();
        }*/
    }

    public void empty() {

        //the exchange db is being deleted because we populate it every time

        documentDatabase.activateOnCurrentThread();
        //we need to specifically add all classes here
        documentDatabase.begin();
        documentDatabase.command(new OCommandSQL("delete from Chat")).execute();
        documentDatabase.command(new OCommandSQL("delete from Message")).execute();
        documentDatabase.commit();

    }
}
