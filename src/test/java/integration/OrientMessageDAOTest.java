package integration;

import es.revib.server.rest.entities.User;
import es.revib.server.rest.messaging.Chat;
import es.revib.server.rest.messaging.Message;
import es.revib.server.rest.messaging.OrientMessageDAO;
import org.junit.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class OrientMessageDAOTest extends IntegrationTest {

    static OrientMessageDAO orientMessageDAO;
    static User u1,u2;

    @BeforeClass
    public static void beforeClass() throws Exception {
        customInit();
        orientMessageDAO=new OrientMessageDAO();

        //we do not care about real ids
        u1=new User();
        u1.setFirstName("test");
        u1.setLastName("testlast");
        u1.setUsername("testuser1");
        u1.setId("1234");

        u2=new User();
        u2.setFirstName("test");
        u2.setLastName("u3");
        u2.setUsername("testuser3");
        u2.setId("1235");

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
    public void createChatTest() {
        Chat chat=new Chat(Arrays.asList(u1.getId(), u2.getId()),"testuser1,testuser3");
        Chat createdChat= orientMessageDAO.createChat(chat);
        String chatId=createdChat.getId();
        assertNotEquals(null, chatId);
        assertFalse(chatId.contains("-")); //this is orientdb's non-persistent id
        assertEquals(u1.getUsername()+","+u2.getUsername(), createdChat.getTitle());
    }

    @Test
    public void postMessageTest() {

        Chat chat=new Chat(Arrays.asList(u1.getId(),u2.getId()),"testuser1,testuser3");
        chat=orientMessageDAO.createChat(chat);
        Message createdMessage=orientMessageDAO.postMessage(chat, new Message(chat.getId(), "howdy", u1.getUsername()));
        assertNotEquals(null, createdMessage.getId());
        assertEquals("howdy", createdMessage.getBody());
    }


    @Test
    public void getChatTest() {

        Chat chat1=new Chat(Arrays.asList(u1.getId(),u2.getId()),"testuser1,testuser3");
        chat1=orientMessageDAO.createChat(chat1);
        Message msg1=orientMessageDAO.postMessage(chat1, new Message(chat1.getId(), "howdy", u1.getUsername()));
        //post a second message here to test the sort order
        Message msg2=orientMessageDAO.postMessage(chat1,new Message(chat1.getId(), "yo", u2.getUsername()));

        Chat chat=orientMessageDAO.getChat(chat1.getId());
        assertTrue(chat.getParticipants().contains(u2.getId()));
        assertEquals(2, chat.getMessages().size());
        Message m1=chat.getMessages().get(0);
        Message m2=chat.getMessages().get(1);
        assertEquals("yo", m1.getBody());
        assertEquals("howdy",m2.getBody());

    }

    @Test
    public void getChatsByUser() {

        Chat chat1=new Chat(Arrays.asList(u1.getId(),u2.getId()),"testuser1,testuser3");
        chat1=orientMessageDAO.createChat(chat1);
        Message msg1=orientMessageDAO.postMessage(chat1, new Message(chat1.getId(), "howdy", u1.getUsername()));
        Chat chat2= new Chat(Arrays.asList(u1.getId(),u2.getId()),"testuser1,testuser3");
        chat2=orientMessageDAO.createChat(chat2);
        Message msg2=orientMessageDAO.postMessage(chat2, new Message(chat2.getId(), "ela", u2.getUsername()));

        List<Chat> chats=orientMessageDAO.getChatsByUser(u1.getId(), 0, 10);
        assertEquals(2,chats.size());
        //is the order correct?
        assertEquals("ela",chats.get(0).getMessages().get(0).getBody());

    }

    @Test
    public void getChatByReferenceId() {
        Chat chat1= orientMessageDAO.createChat(new Chat(Arrays.asList(u1.getId(),u2.getId()),"A1143","a chat"));
        Message msg1=orientMessageDAO.postMessage(chat1, new Message(chat1.getId(), "howdy", u1.getUsername()));
        chat1=orientMessageDAO.getChatByReferenceId("A1143");
        assertEquals("a chat",chat1.getTitle());
    }

    @Test
    public void updateChatTest() {
        Chat chat=new Chat(Arrays.asList(u1.getId(),u2.getId()),"testuser1,testuser3");
        chat=orientMessageDAO.createChat(chat);
        chat.setTitle("custom title");
        chat=orientMessageDAO.updateChat(u1.getId(),chat);
        assertEquals("custom title",chat.getTitle());
    }

    @Test
    public void timestampUserTest() {

        Chat chat1=new Chat(Arrays.asList(u1.getId(),u2.getId()),"testuser1,testuser3");
        chat1=orientMessageDAO.createChat(chat1);
        Message msg1=orientMessageDAO.postMessage(chat1, new Message(chat1.getId(), "howdy", u1.getUsername()));

        orientMessageDAO.timestampUser(u1.getId(),chat1.getId());
        Chat updatedChat=orientMessageDAO.getChat(chat1.getId());
        HashMap<String,Long> updatedUsers=updatedChat.getUpdatedUsers();
        assertTrue(updatedUsers.containsKey(u1.getId()));
    }

    @Test
    public void getNextMessagesTest() {

        Chat chat1=new Chat(Arrays.asList(u1.getId(),u2.getId()),"testuser1,testuser3");
        chat1=orientMessageDAO.createChat(chat1);
        Message msg1=orientMessageDAO.postMessage(chat1, new Message(chat1.getId(), "howdy1", u1.getUsername()));
        Message msg2=orientMessageDAO.postMessage( chat1, new Message(chat1.getId(), "howdy2", u1.getUsername()));
        Message msg3=orientMessageDAO.postMessage(chat1, new Message(chat1.getId(), "howdy3", u1.getUsername()));
        Message msg4=orientMessageDAO.postMessage(chat1, new Message(chat1.getId(), "howdy4", u1.getUsername()));

        List<Message> messages=orientMessageDAO.getMessages(u1.getId(),chat1.getId(),0,2);
        assertEquals(2,messages.size());
        //are they ordered?
        assertEquals("howdy4",messages.get(0).getBody());
        assertEquals("howdy3",messages.get(1).getBody());

        //next messages
        messages=orientMessageDAO.getMessages(u1.getId(),chat1.getId(),2,4);
        assertEquals(2,messages.size());
        //are they ordered?
        assertEquals("howdy2",messages.get(0).getBody());
        assertEquals("howdy1",messages.get(1).getBody());

    }
}
