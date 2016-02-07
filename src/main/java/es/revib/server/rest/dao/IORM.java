package es.revib.server.rest.dao;

import es.revib.server.rest.entities.*;
import es.revib.server.rest.entities.request.EventRequest;
import es.revib.server.rest.entities.request.FriendRequest;
import org.jvnet.hk2.annotations.Contract;

/**
 * Generic interface for returning Entities
 *
 */
@Contract
public interface IORM {

    User buildUser(Object from);
    Activity buildActivity(Object from);
    Event buildEvent(Object from);
    Comment buildComment(Object from);
    Rating buildRating(Object from);
    Info buildInfo(Object from);
    EventRequest buildEventRequest(Object from);
    FriendRequest buildFriendRequest(Object from);

    void writeInfo(Info info,Object to);
}
