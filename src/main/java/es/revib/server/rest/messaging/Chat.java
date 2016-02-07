package es.revib.server.rest.messaging;

import es.revib.server.rest.entities.User;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * class that represents a conversation between some people, the last message and last update
 */
public class Chat {

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    private String id;

    @NotNull
    private List<String> participants=new ArrayList<>();

    private HashMap<String,Long> updatedUsers;

    public Chat(List<String> participants, String referenceId, String title) {
        this.participants=participants;
        this.referenceId = referenceId;
        this.title = title;
    }

    public Chat(List<String> participants,String title) {
        this.participants=participants;
        this.title=title;
    }

    private Long timestamp=new Date().getTime();

    private String referenceId;

    private String title;

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(final List<Message> messages) {
        this.messages = messages;
    }

    private List<Message> messages;


    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    public Chat() {

    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public HashMap<String, Long> getUpdatedUsers() {
        return updatedUsers;
    }

    public void setUpdatedUsers(HashMap<String, Long> updatedUsers) {
        this.updatedUsers = updatedUsers;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }
}
