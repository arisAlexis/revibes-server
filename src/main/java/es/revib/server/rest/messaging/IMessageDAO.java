package es.revib.server.rest.messaging;

import org.jvnet.hk2.annotations.Contract;

import javax.validation.Valid;
import java.util.List;

@Contract
public interface IMessageDAO {

    Message postMessage(@Valid Chat chat,@Valid Message msg);

    List<Chat> getChatsByUser(Object user,int start,int size);

    Chat getChatByReferenceId(String referenceId);

    List<Message> getMessages(Object user,String chatId,int start,int size);

    Chat getChat(String chatId);

    /**
     * indicates when was the last time the user read this chat
     * @param userId
     * @param chatId
     */
    void timestampUser(Object userId,String chatId);

    Chat updateChat(Object user,@Valid Chat chat);

    Chat createChat(@Valid Chat chat);

}
