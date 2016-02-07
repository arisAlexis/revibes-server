package es.revib.server.rest.broker;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.DisconnectListener;
import es.revib.server.rest.util.Globals;
import es.revib.server.rest.util.HttpClientFactory;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

public class SocketIOServerImpl {


    private static SocketIOServer server;
    private static SocketIOServerImpl ourInstance=null;
    public static Configuration configuration;

    public SocketIOServerImpl() {

        configuration.setAuthorizationListener(new AuthorizationListener() {
            @Override
            public boolean isAuthorized(HandshakeData data) {
                /*
                we will use an httpclient here and hit the endpoint because otherwise we get into a mess with
                 DI injections and complicated services
                 */

                Client client = HttpClientFactory.getSslClient();
                Form f = new Form();
                f.param("user", data.getSingleUrlParam("userId"));
                f.param("password", data.getSingleUrlParam("password"));
                f.param("facebookToken",data.getSingleUrlParam("facebookToken"));

                Response response = client.target(Globals.SERVER_URL + "/rest/auth/login").request()
                        .post(Entity.form(f));

                if (response.getStatus() != 200) return false;

                String json = response.readEntity(String.class);

                JSONObject jsonObject=new JSONObject();
                try {
                    jsonObject = new JSONObject(json);
                if (jsonObject.get("id").equals(data.getSingleUrlParam("userId"))){
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
                return false;
            }
        });
       server = new SocketIOServer(configuration);

        server.addConnectListener(client -> {
            String userId=client.getHandshakeData().getSingleUrlParam("userId");
            client.joinRoom(userId);

        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {

                String userId = socketIOClient.getHandshakeData().getSingleUrlParam("userId");
                socketIOClient.leaveRoom(userId);
                //hit the logout endpoint to say we disconnected (at least once)
                Client httpClient = HttpClientFactory.getSslClient();

            }
        });

        System.out.println("starting socket io server...");
        server.start();

    }

    public SocketIOServer getServer() {
        return server;
    }

    public static SocketIOServerImpl getInstance() {
        if (ourInstance==null) {
            ourInstance=new SocketIOServerImpl();
        }
        return ourInstance;
    }

}
