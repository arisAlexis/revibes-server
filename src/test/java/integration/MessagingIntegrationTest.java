package integration;

import config.TestServicesFactory;
import es.revib.server.rest.dao.IORM;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.messaging.Chat;
import es.revib.server.rest.messaging.Message;
import es.revib.server.rest.util.CodingUtilities;
import org.junit.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

@Ignore
public class MessagingIntegrationTest extends IntegrationTest {

    CodingUtilities codingUtilities=new CodingUtilities();
    User u1,u2;

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

        //DI this
        IORM orm= TestServicesFactory.getORM();
        u1=orm.buildUser(user1Email).stripMinimal();
        u2=orm.buildUser(user3Email).stripMinimal();

    }
    @After
    public void after() throws Exception {
        tearDown();
    }

    @Test
    public void sendMessage() {

        //first we need to create a new chat
        Chat chat=createAchat();

        Message pm=new Message();
        pm.setBody("test message");
        pm.setSender(u1.getUsername());

        Entity<Message> ent= Entity.entity(pm, MediaType.APPLICATION_JSON);
        Response response = client.target(hostname+"/rest/messaging/"+codingUtilities.toUtf(chat.getId())+"/messages").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }

    @Test
    public void getChats() {

        //first we need to create a new chat
        Chat chat=createAchat();

        Response response = client.target(hostname + "/rest/messaging/byUser/me")
                .queryParam("start","0")
                .request().header("Cookie", getSessionId())
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<Chat> retchats=response.readEntity(new GenericType<List<Chat>>() {
        });

        assertNotEquals(0, retchats.size());
        retchats.forEach(r -> assertEquals(r.getParticipants().contains(u1.getId()), true));
    }

    private Chat createAchat() {
        Chat chat=new Chat(Arrays.asList(u1.getId(),u2.getId()),"testuser1,testuser3");
        Response response = client.target(hostname + "/rest/messaging").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(Entity.entity(chat, MediaType.APPLICATION_JSON_TYPE));
        chat=response.readEntity(Chat.class);
        return chat;
    }

    @Test
    public void timestampUser() {
        Chat chat=createAchat();
        Response response = client.target(hostname + "/rest/messaging/"+codingUtilities.toUtf(chat.getId())+"/timestamp")
                .request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).get();

        response = client.target(hostname + "/rest/messaging/"+codingUtilities.toUtf(chat.getId()))
                .request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).get();
        chat=response.readEntity(Chat.class);
        assertTrue(chat.getUpdatedUsers().containsKey(u1.getId()));
    }

    @Test
    public void updateChat() {

        Chat chat=createAchat();
        chat.getParticipants().remove(u2.getId());
        Response response = client.target(hostname + "/rest/messaging/"+codingUtilities.toUtf(chat.getId()))
                .request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).put(Entity.entity(chat, MediaType.APPLICATION_JSON_TYPE));
        response = client.target(hostname + "/rest/messaging/"+codingUtilities.toUtf(chat.getId()))
                .request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).get();
        chat=response.readEntity(Chat.class);
        assertFalse(chat.getParticipants().contains(u2.getId()));
    }

    @Test
    public void getChatByReferenceId() {
        String refId="O1828";
        Chat chat=new Chat(Arrays.asList(u1.getId(),u2.getId()),"O1828","O1828:testuser1,testuser3");
        Response response = client.target(hostname + "/rest/messaging").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(Entity.entity(chat, MediaType.APPLICATION_JSON_TYPE));

        response = client.target(hostname + "/rest/messaging/byReferenceId/O1828")
                .request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).get();

        chat=response.readEntity(Chat.class);
        assertEquals("O1828", chat.getReferenceId());
    }

    @Test
    /**
     * get messages for an individual chat
     */
    public void getMessages() {

        Chat chat=createAchat();

        Message pm=new Message();
        pm.setBody("test message");
        pm.setSender(u1.getUsername());

        Entity<Message> ent= Entity.entity(pm, MediaType.APPLICATION_JSON);
        Response response = client.target(hostname+"/rest/messaging/"+codingUtilities.toUtf(chat.getId())+"/messages").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);
        pm=new Message();
        pm.setBody("test message2");
        pm.setSender(u1.getUsername());

        ent= Entity.entity(pm, MediaType.APPLICATION_JSON);
        response = client.target(hostname+"/rest/messaging/"+codingUtilities.toUtf(chat.getId())+"/messages").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);
        pm=new Message();
        pm.setBody("test message3");
        pm.setSender(u1.getUsername());

        ent= Entity.entity(pm, MediaType.APPLICATION_JSON);
        response = client.target(hostname+"/rest/messaging/"+codingUtilities.toUtf(chat.getId())+"/messages").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);
        pm=new Message();
        pm.setBody("test message4");
        pm.setSender(u1.getUsername());

        ent= Entity.entity(pm, MediaType.APPLICATION_JSON);
        response = client.target(hostname+"/rest/messaging/"+codingUtilities.toUtf(chat.getId())+"/messages").request(MediaType.APPLICATION_JSON).header("Cookie", getSessionId()).post(ent);

        response = client.target(hostname + "/rest/messaging/"+codingUtilities.toUtf(chat.getId())+"/messages")
                .queryParam("start","2")
                .request().header("Cookie", getSessionId())
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.hasEntity());

        List<Message> retMsgs = response.readEntity(new GenericType<List<Message>>() {
        });

        assertEquals(2, retMsgs.size());
        assertEquals("test message2",retMsgs.get(0).getBody());

    }

}
