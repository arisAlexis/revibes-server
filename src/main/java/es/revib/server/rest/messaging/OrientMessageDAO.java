package es.revib.server.rest.messaging;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.entities.request.Request;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jvnet.hk2.annotations.Service;

import javax.validation.Valid;
import javax.ws.rs.ForbiddenException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrientMessageDAO implements IMessageDAO {

    ODatabaseDocumentTx documentDatabase= OrientDatabase.getInstance().getDocumentDb();

    public OrientMessageDAO() {
    }

    @Override
    public Message postMessage(@Valid Chat chat,@Valid Message msg) {

        documentDatabase.activateOnCurrentThread();
        documentDatabase.begin();

        //in case the user forgot
        msg.setChatId(chat.getId());

        ODocument msgDoc=new ODocument("Message");
        msgDoc.field("sender",msg.getSender());
        msgDoc.field("body",msg.getBody());
        msgDoc.field("timestamp",msg.getTimestamp());
        msgDoc.field("chatId",msg.getChatId());
        msgDoc.save();
        documentDatabase.commit();

        msg.setId(msgDoc.getIdentity().toString());

        //update parent chat
        ODocument chatDoc=documentDatabase.load(new ORecordId(chat.getId()));
/*
        this is much slower

        List messages=chatDoc.field("messages");
        if (messages==null) {
            messages=new ArrayList<>();
        }
        messages.add(msgDoc);
        chatDoc.field("messages",messages);
        chatDoc.save();
*/
        documentDatabase.begin();
        chatDoc.field("timestamp", new Date().getTime());
        chatDoc.save();
        documentDatabase.commit();
        documentDatabase.command(new OCommandSQL("update " + chatDoc.getIdentity().toString() + " add messages = " + msgDoc.getIdentity().toString())).execute();

        return msg;

    }


    @Override
    public List<Chat> getChatsByUser(Object userId, int start, int size) {

        documentDatabase.activateOnCurrentThread();
        List<Chat> chats=new ArrayList<>();

        String sql="select * from Chat where participants IN ('"+userId.toString()+"') ORDER BY timestamp DESC SKIP "+start + " LIMIT "+size;
        List<ODocument> documents = documentDatabase.query(new OSQLSynchQuery<ODocument>(sql));

        for (ODocument document:documents) {
            chats.add(doc2Chat(document,0,1));
        }

        return chats;
    }

    @Override
    public Chat getChatByReferenceId(String referenceId) {

        documentDatabase.activateOnCurrentThread();
        String sql="select * from Chat where referenceId='"+referenceId+"'";

        List<ODocument> documents = documentDatabase.query(
                new OSQLSynchQuery<ODocument>(sql));
        if (documents.size()==1) return doc2Chat(documents.get(0), 0, 50);

        return null;
    }

    @Override
    public List<Message> getMessages(Object userId, String chatId, int start, int size) {

        documentDatabase.activateOnCurrentThread();
        List<ODocument> documents = documentDatabase.query(
                new OSQLSynchQuery<ODocument>("select * from Chat where @rid = "+chatId));

        if (documents.size()==1) {

            ODocument docChat=documents.get(0);
            List participants=((List<String>) docChat.field("participants"));
            if (!participants.contains(userId.toString())) {
                throw new ForbiddenException();
            }

            return getMessages(docChat, start, size);
        }

        return new ArrayList<>();
    }

    @Override
    public Chat getChat(String chatId) {

        documentDatabase.activateOnCurrentThread();
        List<ODocument> documents = documentDatabase.query(
                new OSQLSynchQuery<ODocument>("select * from Chat where @rid = "+chatId));
        if (documents.size()==1) {
            return doc2Chat(documents.get(0), 0, 50); //default values for fetching the latest 50 messages
        }
        return null;
    }

    /**
     * indicates when was the last time the user read this chat
     *
     * @param userId
     * @param chatId
     */
    @Override
    public void timestampUser(Object userId, String chatId) {

        //todo some kind of checks here preventing silly requests

        documentDatabase.activateOnCurrentThread();
        documentDatabase.begin();

        List<ODocument> documents = documentDatabase.query(
                new OSQLSynchQuery<ODocument>("select * from Chat where @rid = " + chatId));
        ODocument chatDoc=documents.get(0);
        HashMap<String,Long> updatedUsers=chatDoc.field("updatedUsers");
        if (updatedUsers==null) {
            updatedUsers=new HashMap<>();
        }

        updatedUsers.put(userId.toString(), new Date().getTime());
        chatDoc.field("updatedUsers", updatedUsers);

        chatDoc.save();
        documentDatabase.commit();
    }

    @Override
    public Chat updateChat(Object user, @Valid Chat chat) {

        documentDatabase.activateOnCurrentThread();
        List<ODocument> documents = documentDatabase.query(
                new OSQLSynchQuery<ODocument>("select * from Chat where @rid = "+chat.getId()));

        if (documents.size()==1) {
            documentDatabase.begin();
            ODocument docChat=documents.get(0);
            List participants=((List<String>) docChat.field("participants"));
            if (!participants.contains(user.toString())) {
                throw new ForbiddenException();
            }
            chat2Doc(chat, docChat);
            docChat.save();
            documentDatabase.commit();
        }

        return chat;

    }

    private Message doc2msg(ODocument document) {

        Message message = new Message();
        message.setBody(document.field("body"));
        message.setChatId(document.field("chatId"));
        message.setTimestamp(document.field("timestamp"));
        message.setSender(document.field("sender"));
        //this doesn't get populated
        message.setId(document.getIdentity().toString());
        return message;
    }

    private List<Message> getMessages(ODocument document,int start,int size) {

        documentDatabase.activateOnCurrentThread();

        List<Message> messages=new ArrayList<>();
        List<ODocument> docMessages=document.field("messages");

        /*
         according to OrientDB documentation this is a lazy linked list that will fetch only the records accessed, so its like performing an SQL limit call,
          but it is not 100% sure the behaviour is as expected.

         */

        if (docMessages!=null) {
            int reverseStart = docMessages.size() - 1 - (start);
            int reverseEnd = 1 + reverseStart - size;
            if (reverseEnd < 0) reverseEnd = 0; //prevent out of bounds

            for (int i = reverseStart; i >= reverseEnd; i--) {
                messages.add(doc2msg(docMessages.get(i)));
            }
        }

        /* Comparator<Message> comparator=Comparator.comparing(o -> o.getTimestamp());
        messages.sort(comparator.reversed());*/

        return messages;
    }

    /**
     * paginated function
     * @param start
     * @param size
     * @return
     */
    private Chat doc2Chat(ODocument document,int start,int size) {

        Chat chat=new Chat();

        chat.setParticipants((List<String>) document.field("participants"));
        chat.setUpdatedUsers((HashMap<String, Long>) document.field("updatedUsers"));
        chat.setTitle((String) document.field("title"));
        chat.setTimestamp((Long) document.field("timestamp"));
        chat.setReferenceId((String) document.field("referenceId"));
        chat.setMessages(getMessages(document, start, size));
        chat.setId(document.getIdentity().toString());

        return chat;

    }

    private void chat2Doc(Chat chat,ODocument document) {

        document.field("participants",chat.getParticipants());
        document.field("updatedUsers",chat.getUpdatedUsers());
        document.field("title",chat.getTitle());
        document.field("timestamp",chat.getTimestamp());
        document.field("referenceId",chat.getReferenceId());

    }

    @Override
    public Chat createChat(@Valid Chat chat) {

        documentDatabase.activateOnCurrentThread();
        documentDatabase.begin();
        if (chat.getTitle() == null) {
                chat.setTitle(StringUtils.join(chat.getParticipants(),','));
            }
            ODocument document = new ODocument("Chat");
        chat2Doc(chat, document);
        document.save();
        documentDatabase.commit();

        chat.setId(document.getIdentity().toString());

        return chat;
    }
}
