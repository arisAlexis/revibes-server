package es.revib.server.rest.entities;

import es.revib.server.rest.util.AccessType;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Comment implements IEntity{

    public List<String> listUnserializableFields() {
        return Arrays.asList("owner","referenceId","likers");
    }

    @Override
    public IEntity strip(AccessType accessType) {
        return null;
    }

    private User owner;
    private String referenceId;
    private Long timestamp = new Date().getTime();
    private String body;

    public void setId(String id) {
        this.id = id;
    }

    private String id;
    private List<User> likers;

    public Comment() {

    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public User getOwner() {

        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public List<User> getLikers() {
        return likers;
    }

    public void setLikers(List<User> likers) {
        this.likers = likers;
    }
}
