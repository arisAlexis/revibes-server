package es.revib.server.rest.dao;

import es.revib.server.rest.entities.*;
import es.revib.server.rest.entities.request.FriendRequest;
import es.revib.server.rest.entities.request.Request;
import es.revib.server.rest.util.AccessType;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jvnet.hk2.annotations.Contract;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@Contract
public interface IUserDAO {

    User dbAuthUser(String user, String plainText);

    /**
     * we call this from the approval service to update the entry with the approved media or text
     * @param jsonObject
     */
    void approveCallback(JSONObject jsonObject);

    User createUser(User user);
    User updateUser(User user);
    /**
     * this function just deactivates the user but doesn't delete him because of too many consequences in the graph
     * @param userId
     * @return
     */
    boolean deleteUser(String userId);

    FriendRequest addFriend(Object user, String friend,String message);

    void acceptFriend(Object user, String requestId,String message);
    void rejectFriend(Object user, String requestId,String message);

    List<Request> getRequests(Object user,int start,int size);

    List<User> getUsersByLocation(Double lat,Double lon,int radius,int start,int size);

    /**
     * uses a graph database
     * this function has a dual mode either deletes the friendship or deletes the friend request
     * @param user
     * @param friend
     */
    void deleteFriend(Object user,String friend);

    void deleteNotification(Object user,String id);
    List<User> getFriends(Object user);
    /**
     * Gets the data for the profile, if not owner then gets only publicly available data
     *
     * @param user can be either an a id or an email
     * @param accessType OWNER,UNKNOWN,VIEWER OR FRIEND
     * @return
     */
    User getUser(Object user, AccessType accessType);
    User getUserBy(String propertyName, String propertyValue, AccessType accessType);

    List<Rating> getRatings(Object user,int start,int size);


}
