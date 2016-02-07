package es.revib.server.rest.dao;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.entities.*;
import es.revib.server.rest.entities.request.EventRequest;
import es.revib.server.rest.entities.request.FriendRequest;
import org.apache.commons.beanutils.BeanUtils;
import org.jvnet.hk2.annotations.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;

@Service
public class OrientORM implements IORM {

    TransactionalGraph g;
    OrientGraphHelper orientGraphHelper;

    public OrientORM() {
        g= OrientDatabase.getInstance().getGraphTx();
        orientGraphHelper =new OrientGraphHelper(g);
    }

    @Override
    public User buildUser(Object from) {

        User user=new User();
        Vertex v= orientGraphHelper.getVertexFromObject(from);
        try {
            BeanUtils.populate(user, new OrientGraphHelper(g).getElementProperties(v, null));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        if (user.getPreferences()==null) user.initPreferences();

        //get all his ratings
        v.getEdges(Direction.IN, OrientGraphHelper.EDGE_RATING).forEach(e -> user.getRatings().add(buildRating(e)));

        user.setPassword(null);//this is ALWAYS

        return user;
    }

    @Override
    public Activity buildActivity(Object from) {
        Activity activity=new Activity();
        Vertex v= orientGraphHelper.getVertexFromObject(from);

        try {
            BeanUtils.populate(activity, new OrientGraphHelper(g).getElementProperties(v, null));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        //also find the owner
        Vertex vuser=v.getVertices(Direction.IN, OrientGraphHelper.EDGE_OWNERSHIP).iterator().next();
        activity.setOwner(buildUser(vuser).stripMinimal());
        return activity;
    }

    @Override
    public Event buildEvent(Object from) {

        Event event=new Event();
        Vertex v= orientGraphHelper.getVertexFromObject(from);
        try {
            BeanUtils.populate(event, new OrientGraphHelper(g).getElementProperties(v, null));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        event.setId(v.getId().toString());

        v.getVertices(Direction.IN, OrientGraphHelper.EDGE_PARTICIPATION)
                .iterator().forEachRemaining(vpart -> event.getParticipants().add(buildUser(vpart).stripMinimal()));

        event.setActivityId(v.getVertices(Direction.IN, OrientGraphHelper.EDGE_EVENT).iterator().next().getId().toString());

        //this could be lazy
        event.setActivity(buildActivity(v.getVertices(Direction.IN, OrientGraphHelper.EDGE_EVENT).iterator().next()));

        return event;
    }

    @Override
    public Comment buildComment(Object from) {

        Comment comment=new Comment();
        Vertex v= orientGraphHelper.getVertexFromObject(from);
        comment.setOwner(buildUser((v.getVertices(Direction.IN, OrientGraphHelper.EDGE_COMMENT).iterator().next())).stripMinimal());
        comment.setReferenceId(v.getVertices(Direction.OUT, OrientGraphHelper.EDGE_COMMENT).iterator().next().getId().toString());
        comment.setTimestamp(v.getProperty("timestamp"));
        comment.setBody(v.getProperty("body"));
        comment.setId(v.getId().toString());

        //todo get likes list
        ArrayList<User> likers=new ArrayList<>();
        v.getVertices(Direction.IN, OrientGraphHelper.EDGE_LIKE).forEach(vuser-> likers.add(buildUser(vuser).stripMinimal()));
        comment.setLikers(likers);
        return comment;
    }

    @Override
    public Rating buildRating(Object from) {
        Edge e=(Edge) from;
        Rating rating=new Rating();

        rating.setId(e.getId().toString());
        rating.setRatingUser(buildUser(e.getVertex(Direction.OUT)).stripMinimal());
        rating.setReceivingUser(buildUser(g.getVertex(e.getProperty("receivingUser").toString())).stripMinimal());
        rating.setRating(e.getProperty("rating"));
        rating.setEvent(buildEvent(e.getVertex(Direction.IN)));
        rating.setReview(e.getProperty("review"));

        return rating;
    }

    @Override
    public Info buildInfo(Object from) {
        Info info=new Info();
        Vertex v=null;
        if (from instanceof Vertex)
        {
            v=(Vertex) from;
        }
        else if (from instanceof String) {
            if (orientGraphHelper.isId((String)from)) {
                v=g.getVertex((String)from);
            }
        }
        else throw new IllegalArgumentException();

        try {
            Map map = new OrientGraphHelper(g).getElementProperties(v, null);
            BeanUtils.populate(info, map);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        //add comments and likes
        ArrayList<User> likers=new ArrayList<>();
        v.getVertices(Direction.IN, OrientGraphHelper.EDGE_LIKE).forEach(vuser-> likers.add(buildUser(vuser).stripMinimal()));
        info.setLikers(likers);
        ArrayList<Comment> comments=new ArrayList<>();
        v.getVertices(Direction.IN, OrientGraphHelper.EDGE_COMMENT).forEach(vc-> comments.add(buildComment(vc)));
        info.setComments(comments);
        return info;

    }

    @Override
    public EventRequest buildEventRequest(Object from) {

        Edge e=(Edge) from;
        EventRequest eventRequest=new EventRequest();
        eventRequest.setId(e.getId().toString());

        Map m= orientGraphHelper.getElementProperties(e,null);
        try {
            BeanUtils.populate(eventRequest,m);
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }

        eventRequest.setRequester(buildUser(e.getVertex(Direction.OUT)).stripMinimal());
        if (eventRequest.getType().equals(Action.ADD_EVENT)) {
            eventRequest.setActivity(buildActivity(e.getVertex(Direction.IN)));
        }
        else {
            eventRequest.setEvent(buildEvent(e.getVertex(Direction.IN)));
        }
        return eventRequest;
    }

    @Override
    public FriendRequest buildFriendRequest(Object from) {

        Edge e=(Edge) from;
        FriendRequest friendRequest=new FriendRequest();
        friendRequest.setId(e.getId().toString());
        friendRequest.setRequestMessage(e.getProperty("requestMessage"));
        friendRequest.setStatus(e.getProperty("status"));
        friendRequest.setRequester(buildUser(e.getVertex(Direction.OUT)).stripMinimal());
        friendRequest.setReceiver(buildUser(e.getVertex(Direction.IN)).stripMinimal());
        friendRequest.setType(Action.SEND_FRIEND_REQUEST);

        return friendRequest;
    }

    @Override
    public void writeInfo(Info info,Object o) {

        Vertex v=(Vertex) o;

            v.setProperty("timestamp", info.getTimestamp());
            if (info.getSourceIds()!= null) v.setProperty("sourceIds", info.getSourceIds());
            if (info.getSourceDisplayNames() != null) v.setProperty("sourceDisplayNames", info.getSourceDisplayNames());
            if (info.getTargetIds() != null) v.setProperty("targetIds", info.getTargetIds());
            if (info.getTargetDisplayNames()!= null) v.setProperty("targetDisplayNames",info.getTargetDisplayNames());
            if (info.getReferenceDisplayNames()!= null) v.setProperty("referenceDisplayNames", info.getReferenceDisplayNames());
            if (info.getReferenceIds() != null) v.setProperty("referenceIds", info.getReferenceIds());
            if (info.getSourceImages() != null) v.setProperty("sourceImages", info.getSourceImages());
            if (info.getTargetImages()!= null) v.setProperty("targetImages", info.getTargetImages());
            if (info.getReferenceImages() != null) v.setProperty("referenceImages", info.getReferenceImages());
            if (info.getTargetType() != null) v.setProperty("targetType", info.getTargetType());
            if (info.getReferenceType() != null) v.setProperty("referenceType", info.getReferenceType());
            v.setProperty("action",info.getAction()); //cannot be null
            if (info.getPayload()!= null) v.setProperty("payload", info.getPayload());
            if (info.getLat() != null) v.setProperty("lat", info.getLat());
            if (info.getLon() != null) v.setProperty("lon", info.getLon());

    }
}
