package es.revib.server.rest.dao;

import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import es.revib.server.rest.approval.IApprovalService;
import es.revib.server.rest.broker.BrokerService;
import es.revib.server.rest.broker.IBroker;
import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.entities.Status;
import es.revib.server.rest.kv.IKVStore;
import es.revib.server.rest.email.IEmailService;
import es.revib.server.rest.entities.*;
import es.revib.server.rest.entities.request.FriendRequest;
import es.revib.server.rest.entities.request.Request;
import es.revib.server.rest.util.AccessType;
import es.revib.server.rest.auth.AuthUtils;
import es.revib.server.rest.util.CodingUtilities;
import es.revib.server.rest.util.Globals;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OrientUserDAO  implements IUserDAO{


    @Inject
    BrokerService brokerService;

    @Inject
    IStreamDAO streamDAO;

    @Inject IKVStore kvStore;

    @Inject IEmailService emailService;

    @Inject
    IORM orm;

    private TransactionalGraph g=OrientDatabase.getInstance().getGraphTx();
    private OrientGraphHelper orientGraphHelper =new OrientGraphHelper(g);

    public OrientUserDAO() {

    }

    /**
     * Takes a plain text password and checks it with the hash stored in the database
     *
     * in the database we keep the hashed password with the salt as a JSON object string
     * @param user
     * @param plainText
     * @return null if login is not correct, and the username of the user in case we had the email for login
     */
    @Override
    public User dbAuthUser(String user, String plainText) throws ForbiddenException,NotFoundException,BadRequestException{

        Vertex vuser=null;
        //check if its an email
        Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
        Matcher m = p.matcher(user);
        boolean matchFound = m.matches();
        if (matchFound) {
            //it's email and not username
            Iterator<Vertex> iter=g.getVertices("email",user).iterator();
            if (!iter.hasNext()) {
                throw new NotFoundException();
            }
            vuser=iter.next();
        }
        else if(orientGraphHelper.isId(user)) {
            vuser=g.getVertex(user);
            if (vuser==null) {
                throw new NotFoundException();
            }
        }
        else throw new BadRequestException();

        Map password= vuser.getProperty("password");

        String newHash= null;
        try {
            newHash = AuthUtils.get_SHA_256_SecurePassword(plainText, password.get("salt").toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (newHash.equalsIgnoreCase(password.get("hash").toString())) {
            return orm.buildUser(vuser);
        }
        else {
            throw new ForbiddenException();
        }

    }

    void addNotification(Vertex v,Info note) {
        List<Map> notifications;
        if (v.getProperty("notifications") == null) {
            notifications = new ArrayList<>();
        }
        else {
            notifications= v.getProperty("notifications");
        }
        notifications.add(note.toMap());
        v.setProperty("notifications",notifications);
        g.commit();
    }

    /**
     * we call this from the approval service to update the entry with the approved media or text
     *
     * @param jsonObject
     */
    @Override
    public void approveCallback(JSONObject jsonObject) {
        //determine if it's a media file or profile text
        try {
            User user=getUser(jsonObject.getString("user"),AccessType.OWNER);
            String type=jsonObject.getString("type");
            String img=jsonObject.getString("value");
            switch (type) {
                case IApprovalService.TYPE_FILE:
                case IApprovalService.TYPE_LINK: {
                    user.getImages().add(img);
                    if (user.getMainImage()==null) {
                        user.setMainImage(img);
                    }
                    break;
                }
                case IApprovalService.TYPE_TEXT : user.setDescription(jsonObject.getString("value")); break;
            }
            updateUser(user);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public User createUser(User user) {

        //note that we may not have a password here because of a social login
        if (user.getPassword()!=null) {
            user.setPassword(AuthUtils.constructPassword(user.getPassword().get("text").toString()));
        }

        try {

            Vertex vuser = g.addVertex("class:User");

            //if user hasn't selected
            if (user.getUsername()==null) user.buildUsername();

            orientGraphHelper.setElementProperties(vuser, user.toMap(), null);
            g.commit(); //otherwise the id is temporary
            user.setId(vuser.getId().toString());

        } catch (Exception e) {
            g.rollback();
            throw e;
        }

        //add his email verification link

        String token= UUID.randomUUID().toString();
        kvStore.put("emailVerify" + user.getUsername(),token );

        String htmlTemplate = new CodingUtilities().getResourceAsString("/i18n/templates/"+user.getPreferences().get("language")+"/email_verification.html");

        String baseUrl= Globals.SERVER_URL+"/rest/auth/emailVerify";
        //email him using the email service

        htmlTemplate=htmlTemplate.replace("INSERT USER HERE",user.getFirstName() + " " + user.getLastName());
        htmlTemplate=htmlTemplate.replace("INSERT LINK HERE",
                baseUrl+"?user=" + user.getUsername() + "&token=" +token);

        //todo uncomment when we are production ready
        //emailService.sendMail(Globals.NO_REPLY_EMAIL, user.getEmail(), "email verification from revibes", htmlTemplate);

        return user;
    }

    @Override
    public User updateUser(User user) {

        boolean notifiable=false; //only some property changes trigger streams to friends

        try {

            Vertex vuser = g.getVertex(user.getId());

            //security but to be seen
            user.buildUsername();

            if (user.getPassword() != null && user.getPassword().get("text") != null) {
                Map password = AuthUtils.constructPassword(user.getPassword().get("text").toString());
                user.setPassword(password);
            }

            if ((user.getDescription()!=null && !user.getDescription().equals(vuser.getProperty("description"))) || (user.getMainImage()!=null && !user.getMainImage().equals(vuser.getProperty("mainImage")))){
                notifiable=true;
            }

            if (!vuser.getProperty("email").toString().equalsIgnoreCase(user.getEmail())) {

                vuser.setProperty("email",user.getEmail());

                user.setEmailVerified(false);
                //add his email verification link

                String token=UUID.randomUUID().toString();
                kvStore.put("emailVerify" + user.getId(),token );

                String htmlTemplate = new CodingUtilities().getResourceAsString("/i18n/templates/"+user.getPreferences().get("language")+"/email_verification.html");

                String baseUrl=Globals.SERVER_URL+"/rest/auth/emailVerify";
                //email him using the email service

                htmlTemplate=htmlTemplate.replace("INSERT USER HERE",user.getFirstName() + " " + user.getLastName());
                htmlTemplate=htmlTemplate.replace("INSERT LINK HERE",
                        baseUrl+"?user=" + new CodingUtilities().toUtf(user.getId()) + "&token=" +token);

                //emailService.sendMail(Globals.NO_REPLY_EMAIL, user.getEmail(), "email verification from revibes", htmlTemplate);

            }

            //here the update happens
            orientGraphHelper.setElementProperties(vuser,user.toMap(),null);

            if (notifiable) {
                //notify his friends about his profile update

                Info stream = new Info();
                stream.setSourceType(Entities.USER);
                stream.setAction(Action.UPDATE_PROFILE);
                stream.setSourceIds(Arrays.asList(user.getId()));
                stream.setSourceDisplayNames(Arrays.asList(user.getUsername()));
                stream.setSourceImages(Arrays.asList(vuser.getProperty("mainImage").toString()));

                streamDAO.stream(stream, Arrays.asList(vuser));
            }

            g.commit();

        } catch (Exception e) {
            g.rollback();
        }

        return user;
    }

    /**
     * this function just deactivates the user but doesn't delete him because of too many consequences in the graph
     *
     * @param userId
     * @return
     */
    @Override
    public boolean deleteUser(String userId) {
        return false;
    }

    private boolean isFriend(Vertex vuser,String friend) {
        return isFriend(vuser, orientGraphHelper.getVertexFromObject(friend));
    }

    private boolean isFriend(Vertex vuser, Vertex friend) {

            Iterator<Edge> edgeIterator = vuser.query().labels(OrientGraphHelper.EDGE_FRIENDSHIP).direction(Direction.BOTH).edges().iterator();

            while (edgeIterator.hasNext()) {
                Edge e = edgeIterator.next();
                if (e.getVertex(Direction.OUT).equals(vuser)) {
                    if (e.getVertex(Direction.IN).equals(friend)) {
                        return true;
                    }
                } else {
                    if (e.getVertex(Direction.OUT).equals(friend)) {
                        return true;
                    }
                }
            }

            return false;

    }

    @Override
    public void acceptFriend(Object user,String requestId,String message) {
        try {

            Edge edge=g.getEdge(requestId);

            if (edge==null) {
                throw new NotFoundException();
            }

            Vertex vuser = orientGraphHelper.getVertexFromObject(user);

            Vertex fuser=edge.getVertex(Direction.OUT);

            //do nothing
            if (isFriend(vuser, fuser)) {
                throw new BadRequestException("users are already friends");
            }

            HashMap<String,Map> responses=new HashMap();
            HashMap<String,String> actions=new HashMap<>();
            actions.put("action",Verbs.ACCEPT);
            actions.put("message",message);

            responses.put(vuser.getProperty("username").toString(),actions);
            edge.setProperty("responses",responses);

                if (edge.getProperty("status").toString().equalsIgnoreCase(Status.PENDING)) { //found it!
                    edge.setProperty("status",Status.ACCEPTED);

                    g.addEdge(null,vuser,fuser, OrientGraphHelper.EDGE_FRIENDSHIP);

                    //inform him

                    Info note = new Info();
                    note.setAction(Action.ACCEPT_FRIENDSHIP);
                    note.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username").toString()));
                    note.setSourceIds(Arrays.asList(vuser.getId().toString()));
                    note.setTargetIds(Arrays.asList(fuser.getId().toString()));
                    note.setTargetDisplayNames(Arrays.asList(vuser.getProperty("username").toString()));

                    addNotification(fuser,note);
                    brokerService.sendNotification(orm.buildUser(fuser), note);

                    //STREAM TO BOTH
                    //stream to both groups of their friends
                    //order of this stream is random

                    Info stream=new Info();
                    stream.setAction(Action.BECOME_FRIENDS);
                    stream.setSourceImages(Arrays.asList(fuser.getProperty("mainImage").toString()));
                    stream.setSourceIds(Arrays.asList(fuser.getId().toString()));
                    stream.setTargetImages(Arrays.asList(vuser.getProperty("mainImage").toString()));
                    stream.setTargetIds(Arrays.asList(fuser.getId().toString()));
                    stream.setSourceType(Entities.USER);
                    stream.setTargetType(Entities.USER);
                    stream.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username").toString()));
                    stream.setTargetDisplayNames(Arrays.asList(fuser.getProperty("username").toString()));

                    streamDAO.stream(stream, Arrays.asList(vuser, fuser));
                }

            g.commit();

        } catch (Exception e) {
            e.printStackTrace();
            g.rollback();
            throw e;
        }
    }

    /**
     * send a friend request
     * @param user
     * @param friend
     */
    @Override
    public FriendRequest addFriend(Object user, String friend, String message) {

        try {

            Vertex vuser = orientGraphHelper.getVertexFromObject(user);

            //do nothing
            if (isFriend(vuser, friend)) {
                return null;
            }

            //also we may already have a friendRequest from him so don't bust the user's balls if he ignored it.
            Vertex fuser = orientGraphHelper.getVertexFromObject(friend);
            Iterator<Edge> edgesIterator= (Iterator<Edge>) ((OrientVertex)vuser).getEdges(Direction.BOTH, OrientGraphHelper.EDGE_FRIEND_REQUEST); //we could use the vertex2vertex function in graphhelper
            if (edgesIterator.hasNext()) {
                Edge edge=edgesIterator.next();
                if (edge.getProperty("status").toString().equalsIgnoreCase(Status.REJECTED)) {
                    return null; //think about throwing an error here maybe?
                }
            }

            Edge edge=g.addEdge(null, vuser, fuser, OrientGraphHelper.EDGE_FRIEND_REQUEST);
            g.commit();//to get the id
            edge.setProperty("timestamp", new Date().getTime());
            edge.setProperty("status",Status.PENDING);
            edge.setProperty("requestMessage",message);

            g.commit();//to get the id

            //inform him

            Info note = new Info();
            note.setAction(Action.SEND_FRIEND_REQUEST);
            note.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username").toString()));
            note.setSourceIds(Arrays.asList(vuser.getId().toString()));
            note.setTargetIds(Arrays.asList(fuser.getId().toString()));
            note.setTargetDisplayNames(Arrays.asList(fuser.getProperty("username").toString()));

            addNotification(fuser, note);
            brokerService.sendNotification(orm.buildUser(fuser), note);

            return orm.buildFriendRequest(edge);

        } catch (Exception e) {
            e.printStackTrace();
            g.rollback();
            throw e;
        }
    }

    @Override
    public void rejectFriend(Object user, String requestId,String message) {

        try {
            Edge edge=g.getEdge(requestId);

            if (edge==null) {
                throw new NotFoundException();
            }

            Vertex vuser = orientGraphHelper.getVertexFromObject(user);

            Vertex fuser=edge.getVertex(Direction.OUT);

            //do nothing
            if (isFriend(vuser, fuser)) {
                throw new BadRequestException("users are already friends");
            }

            HashMap<String,Map> responses=new HashMap();
            HashMap<String,String> actions=new HashMap<>();
            actions.put("action", Verbs.REJECT);
            actions.put("message", message);

            responses.put(vuser.getProperty("username").toString(), actions);
            edge.setProperty("responses", responses);

                if (edge.getProperty("status").toString().equalsIgnoreCase(Status.PENDING)) { //found it!
                    edge.setProperty("status",Status.REJECTED);

                    //inform him

                    Info note = new Info();
                    note.setAction(Action.REJECT_FRIENDSHIP);
                    note.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username")));
                    note.setSourceIds(Arrays.asList(vuser.getId().toString()));
                    note.setTargetIds(Arrays.asList(fuser.getId().toString()));
                    note.setTargetDisplayNames(Arrays.asList(vuser.getProperty("username")));

                    addNotification(fuser,note);
                    brokerService.sendNotification(orm.buildUser(fuser), note);
                }

            g.commit();
        } catch (Exception e) {
            e.printStackTrace();
            g.rollback();
            throw e;
        }
    }

    @Override
    public List<Request> getRequests(Object user,int start, int size) {

        Set<Request> requests=new HashSet<>(); //we may get duplicates here

        Vertex vuser= orientGraphHelper.getVertexFromObject(user);

        //we will add sequentially first the we sent or received directly
        vuser.getEdges(Direction.BOTH, OrientGraphHelper.EDGE_FRIEND_REQUEST, OrientGraphHelper.EDGE_EVENT_REQUEST).forEach(edge -> {
            if (edge.getLabel().equalsIgnoreCase(OrientGraphHelper.EDGE_FRIEND_REQUEST)) {
                requests.add(orm.buildFriendRequest(edge));
            } else if (edge.getLabel().equalsIgnoreCase(OrientGraphHelper.EDGE_EVENT_REQUEST)) {
                requests.add(orm.buildEventRequest(edge));
            }
        });

        //requests that were sent to activities we own
        vuser.getVertices(Direction.OUT, OrientGraphHelper.EDGE_OWNERSHIP).forEach(v -> {
            v.getEdges(Direction.IN, OrientGraphHelper.EDGE_EVENT_REQUEST).forEach(eventR -> {
                requests.add(orm.buildEventRequest(eventR));
            });

            v.getVertices(Direction.OUT, OrientGraphHelper.EDGE_EVENT).forEach(eventV -> {
                eventV.getEdges(Direction.IN, OrientGraphHelper.EDGE_EVENT_REQUEST).forEach(eventR -> {
                    requests.add(orm.buildEventRequest(eventR));
                });
            });
        });

        //requests that are up for voting in events that we participate but we do not own
        vuser.getVertices(Direction.OUT, OrientGraphHelper.EDGE_PARTICIPATION).forEach(v-> {
            Vertex vactivity=v.getVertices(Direction.IN, OrientGraphHelper.EDGE_EVENT).iterator().next();
            if (!vuser.equals(vactivity.getVertices(Direction.IN, OrientGraphHelper.EDGE_OWNERSHIP).iterator().next())) {
                v.getEdges(Direction.IN, OrientGraphHelper.EDGE_EVENT_REQUEST).forEach(eventR -> {
                    if (eventR.getProperty("status").toString().equalsIgnoreCase(Status.VOTING)) {
                        requests.add(orm.buildEventRequest(eventR));
                    }
                });
            }
        });

        List<Request> requestList=new ArrayList<>();
        requestList.addAll(requests);
        Comparator<Request> comparator=Comparator.comparing(o -> o.getTimestamp());
        requestList.sort(comparator.reversed());

        return requestList;
    }

    @Override
    public List<User> getUsersByLocation(Double lat, Double lon, int radius,int start,int size) {

        OrientGraph orientGraph=(OrientGraph)g;
        List<User> users=new ArrayList<>();
        String sql="SELECT FROM User WHERE [lat,lon,$spatial] " +
                "NEAR ["+lat+","+lon+",{\"maxDistance\": "+radius+"}] " +
                " SKIP "+start + " LIMIT "+size;

        for (Vertex v : (Iterable<Vertex>) orientGraph.command(
                new OCommandSQL(sql)).execute()) {
            users.add(getUser(v, AccessType.VIEWER));
        }

        return users;
    }

    /**
     * uses a graph database
     * this function has a dual mode either deletes the friendship or deletes the friend request
     *
     * @param user
     * @param friend
     */
    @Override
    public void deleteFriend(Object user, String friend) {
        Vertex vuser = orientGraphHelper.getVertexFromObject(user);
        try {

            //do nothing
            if (!isFriend(vuser, friend)) {
                return;
            } else {
                Iterator<Edge> edgeIterator = vuser.query().labels(OrientGraphHelper.EDGE_FRIENDSHIP).direction(Direction.BOTH).edges().iterator();

                while (edgeIterator.hasNext()) {
                    Edge e = edgeIterator.next();
                    boolean remove=false;
                    if (e.getVertex(Direction.OUT).equals(vuser)) {
                        if (orientGraphHelper.isId(friend)) {
                            if (e.getVertex(Direction.IN).getId().toString().equals(friend)) {
                                remove=true;
                            }
                        }
                        else if (e.getVertex(Direction.IN).getProperty("email").toString().equalsIgnoreCase(friend)) {
                            remove=true;
                        }
                    } else {
                        if (orientGraphHelper.isId(friend)) {
                            if (e.getVertex(Direction.OUT).getId().toString().equals(friend)) {
                                remove=true;
                            }
                        }
                        else if (e.getVertex(Direction.OUT).getProperty("email").toString().equalsIgnoreCase(friend)) {
                            remove=true;
                        }
                    }

                    if (remove) e.remove();
                }
            }
        } catch (Exception e) {
            g.rollback();
            throw e;
        } finally {
            g.commit();
        }
    }


    @Override
    public void deleteNotification(Object user, String id) {


        try {

            Vertex vuser = orientGraphHelper.getVertexFromObject(user);
            List<Map> notifications;
            if (vuser.getProperty("notifications") == null) {
                throw new BadRequestException();
            }

            notifications= vuser.getProperty("notifications");

            Iterator it = notifications.iterator();
            while (it.hasNext()) {
                Map n=(Map) it.next();
                if (n.get("id").toString().equalsIgnoreCase(id)) {
                    notifications.remove(n);
                    break;
                }
            }

            vuser.setProperty("notifications", notifications);

        } catch (Exception e) {
            g.rollback();
            throw e;
        } finally {
            g.commit();
        }

    }

    @Override
    public List<User> getFriends(Object user) {
        Vertex vuser = orientGraphHelper.getVertexFromObject(user);

            List<User> friends = new ArrayList<>();

            Iterator<Vertex> vertexIterator= vuser.getVertices(Direction.BOTH, OrientGraphHelper.EDGE_FRIENDSHIP).iterator();
            while (vertexIterator.hasNext()) {
                Vertex v=vertexIterator.next();
                User friend = orm.buildUser(v);
                friends.add(friend);
            }
            return friends;
    }

    /**
     * Gets the data for the profile, if not owner then gets only publicly available data
     *
     * @param user can be either a vertex,id or email
     * @param accessType OWNER,UNKNOWN,VIEWER OR FRIEND
     * @return
     */
    @Override
    public User getUser(Object user, AccessType accessType) {

        Vertex vuser=null;
        if (user instanceof Vertex) {
            vuser=(Vertex) user;
        }
        else if (user instanceof String){
            Iterator<Vertex> vertexIterator;
            if (orientGraphHelper.isId((String) user)) {
                vuser = g.getVertex((String) user);
                if (vuser == null) {
                    throw new NotFoundException();
                }
            } else if (((String) user).contains("@")) {
                vertexIterator = g.getVertices("email", (String) user).iterator();
                if (!vertexIterator.hasNext()) {
                    throw new NotFoundException();
                }
                vuser = vertexIterator.next();
            }
            else {
                throw new BadRequestException();
            }
        }
        else {
            throw new IllegalArgumentException();
        }

        User retUser=orm.buildUser(vuser);
        retUser.strip(accessType);

        return retUser;
    }

    @Override
    public User getUserBy(String propertyName, String propertyValue, AccessType accessType) {
        Vertex vuser=null;
        Iterator<Vertex> vertexIterator=g.getVertices(propertyName,propertyValue).iterator();
        if (!vertexIterator.hasNext()) {
            throw new NotFoundException();
        }
        vuser = vertexIterator.next();
        User retUser=orm.buildUser(vuser);
        retUser.strip(accessType);

        return retUser;
    }


    @Override
    public List<Rating> getRatings(Object user,int start,int size) {

        HashSet<Rating> ratings=new HashSet<>();

        Vertex vuser= orientGraphHelper.getVertexFromObject(user);

        new GremlinPipeline<>().start(vuser)
                .out(OrientGraphHelper.EDGE_PARTICIPATION)
                .has("status", Status.COMPLETED)
                .inE(OrientGraphHelper.EDGE_RATING)
                .has("receivingUser",vuser.getId().toString())
                .range(start, start + size)
                .iterator().forEachRemaining(eRating -> {
            ratings.add(orm.buildRating(eRating));
        });

        List<Rating> ratingList=new ArrayList<>();
        ratingList.addAll(ratings);

        return ratingList;
    }

/*
    @Override
    public void setPreferences(Object user,Map preferences) {

        try {

            Vertex vuser=orientGraphHelper.getVertexFromObject(user);
            vuser.setProperty("preferences",preferences);

        } catch (Exception e) {
            e.printStackTrace();
            g.rollback();
            throw e;
        } finally {
            g.commit();
        }
    }
*/

}
