package es.revib.server.rest.kv;

import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.util.CodingUtilities;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.jvnet.hk2.annotations.Service;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Service
public class OrientKVStore implements IKVStore {


    CodingUtilities codingUtilities=new CodingUtilities();
    Client client;

    public Client buildClient() {
        Client client=ClientBuilder.newClient();
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(OrientDatabase.getUser(), OrientDatabase.getPassword());
        client.register(feature);
        return client;
    }

    public OrientKVStore() {

        client=buildClient();

    }

    @Override
    public boolean put(String key, JSONObject value) {

            Response response = client.target(OrientDatabase.getHTTP_URL() + "/index/exchange/exchangeKV/" + codingUtilities.toUtf(key))
                    .request()
                    .put(Entity.entity(value.toString(), MediaType.APPLICATION_JSON_TYPE));

            if (response.getStatus() == Response.Status.ACCEPTED.getStatusCode()) return true;
            else return false;
    }

    @Override
    public boolean put(String key,String value) {
        JSONObject jsonObject=new JSONObject();
        try {
            jsonObject.put("value",value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return put(key,jsonObject);
    }

    @Override
    public JSONObject get(String key) {

        Response response = client.target(OrientDatabase.getHTTP_URL() + "/index/exchange/exchangeKV/" + codingUtilities.toUtf(key))
                .request()
                .get();


        JSONArray jsonArray;
        JSONObject jsonObject;
        try {
            String str = response.readEntity(String.class);
            jsonArray = new JSONArray(str);
            jsonObject=jsonArray.getJSONObject(0);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return jsonObject;
    }

    @Override
    public boolean delete(String key) {

            Response response = client.target(OrientDatabase.getHTTP_URL() + "/index/exchange/exchangeKV/" + codingUtilities.toUtf(key))
                    .request()
                    .delete();
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return true;
            } else {
                return false;
            }

    }
}
