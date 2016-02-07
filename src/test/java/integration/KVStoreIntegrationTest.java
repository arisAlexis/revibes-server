package integration;

import db.OrientDBUtil;
import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.kv.IKVStore;
import es.revib.server.rest.kv.OrientKVStore;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import static org.junit.Assert.assertEquals;

public class KVStoreIntegrationTest {

    static Client client= ClientBuilder.newClient();
    static OrientDBUtil orientDBUtil = new OrientDBUtil();

    @BeforeClass
    public static void init() {

        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(OrientDatabase.getUser(), OrientDatabase.getPassword());
        client.register(feature);

    }

    @Before
    public void setUp() {
        //we do not have to import anything before the test
    }

    @Test
    /**
     * we do not implement update in our KVStores
     */
    public void CRDTest() throws JSONException {

        IKVStore kvStore= new OrientKVStore();
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("value","test");
        kvStore.put("name11", jsonObject);
        assertEquals("test", kvStore.get("name11").getString("value"));
        kvStore.delete("name11");
        assertEquals(null, kvStore.get("name11"));

    }

    @Ignore
    @Test
    /**
     * do we have a problem with multiple requests?
     */
    public void stressTest() {
        IKVStore kvStore= new OrientKVStore();
        for (int i=1;i<100;i++) {
            kvStore.put("lala","mysha");
            JSONObject value=kvStore.get("lala");
            kvStore.delete("lala");
        }
    }

    @After
    public void tearDown() {

        client.target(OrientDatabase.getHTTP_URL() + "/command/exchange/sql")
                .request().post(Entity.text("DELETE FROM index:exchangeKV"));
    }
}
