package es.revib.server.rest.entities.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.revib.server.rest.entities.Action;
import es.revib.server.rest.entities.User;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendRequest extends Request {

    public FriendRequest() {
        this.setType(Action.SEND_FRIEND_REQUEST);
    }

    private User receiver;

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }
}
