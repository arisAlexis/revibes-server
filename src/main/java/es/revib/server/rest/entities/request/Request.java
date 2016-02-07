package es.revib.server.rest.entities.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.TransactionalGraph;
import es.revib.server.rest.entities.Status;
import es.revib.server.rest.entities.User;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Request {

    private String type; //Verb PARTICIPATE/MODIFY or Action ADD_EVENT

    private String requestMessage;

    private User requester;

    public String getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Request request = (Request) o;

        if (id != null ? !id.equals(request.id) : request.id != null) return false;
        if (status != null ? !status.equals(request.status) : request.status != null) return false;
        if (timestamp != null ? !timestamp.equals(request.timestamp) : request.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public Request() {
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String id;
    private Long timestamp=new Date().getTime();
    protected String status= Status.PENDING;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }
}
