package es.revib.server.rest.dao;

import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import es.revib.server.rest.approval.IApprovalService;
import es.revib.server.rest.broker.BrokerService;
import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.entities.Status;
import es.revib.server.rest.entities.*;
import es.revib.server.rest.entities.request.EventRequest;
import es.revib.server.rest.util.AccessType;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.util.*;

@Service
public class OrientActivityDAO implements IActivityDAO{


    @Inject
    BrokerService brokerService;
    
    @Inject
    IStreamDAO streamDAO;

    @Inject
    IORM orm;

    private TransactionalGraph g= OrientDatabase.getInstance().getGraphTx();
    private OrientGraphHelper orientGraphHelper =new OrientGraphHelper(g);

    public OrientActivityDAO() {
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
            Activity activity = getActivity(jsonObject.getString("referenceId"),
                    jsonObject.getString("user"), AccessType.OWNER, false,Status.PENDING);
            String type = jsonObject.getString("type");
            switch (type) {
                case  IApprovalService.TYPE_FILE:
                case IApprovalService.TYPE_LINK: {
                    //if this is the first one, make it the mainImage and then the user will change it if he wants
                    String img=jsonObject.getString("value");
                    if (activity.getMainImage()==null) {
                        activity.setMainImage(img);
                    }
                    activity.getImages().add(img);
                    break;
                }
                case IApprovalService.TYPE_TEXT:
                    //activity.setDescription(jsonObject.getString("text"));
                    activity.setStatus(Status.OPEN);
                    break;
            }

            updateActivity(jsonObject.getString("user"), activity);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    /**
     * @param activityId
     * @param user       if user is null then we are not logged in
     * @param accessType
     * @param increment  if we want to increment the views counter
     * @return
     */
    @Override
    public Activity getActivity(String activityId, Object user, AccessType accessType, boolean increment,String status) {
        Activity activity=null;
        try {

            Vertex vuser=null;
            if (user != null) {
                vuser= orientGraphHelper.getVertexFromObject(user);
            }
            Vertex vactivity = g.getVertex(activityId);
            if (vactivity == null) throw new NotFoundException();

            activity=orm.buildActivity(vactivity);

            if (activity.getOwner().getId().equalsIgnoreCase(vuser.getId().toString())) {
                accessType = AccessType.OWNER;
            }

            if (increment) {
                //make edges
                if (vuser != null) {
                    Edge e = g.addEdge(null, vuser, vactivity, OrientGraphHelper.EDGE_VIEW);
                    e.setProperty("date", new Date().getTime());
                }

                Integer views = vactivity.getProperty("views");
                if (views == null) views = 0;
                views++;
                vactivity.setProperty("views", views);
                activity.setViews(views);
            }

            activity.setEvents(getEventsByActivity(vactivity,status));

        } catch (Exception e) {
            e.printStackTrace();
            g.rollback();
        } finally {
            g.commit();
        }
        return activity.strip(accessType);
    }

    public List<Activity> getActivities(ActivityQueryBuilder queryBuilder) {

        OrientGraph orientGraph=(OrientGraph)g;
        List<Activity> activities=new ArrayList<>();

        String sql="";
        if (queryBuilder.getTag()!=null && !queryBuilder.getTag().isEmpty()) {
            sql="SELECT FROM ( SELECT EXPAND(IN('"+ OrientGraphHelper.EDGE_TAG+"'))"
                    + " FROM Tag WHERE  tagName='"+queryBuilder.getTag() +"') WHERE ";
        }
        else {
            sql="SELECT FROM Activity WHERE ";
        }

        if (queryBuilder.getLat()!=null && queryBuilder.getLon()!=null) {
            sql+=" [lat,lon,$spatial] NEAR ["+queryBuilder.getLat()+","+queryBuilder.getLon()+",{\"maxDistance\": "+queryBuilder.getRadius()+"}] AND ";
        }

        if (queryBuilder.getCategory()!=null && !queryBuilder.getCategory().isEmpty()) {
            sql+=" categoryId='"+queryBuilder.getCategory()+"' AND ";
        }

        if (queryBuilder.getKeywords()!=null && !queryBuilder.getKeywords().isEmpty()) {
            sql+=" fullText LUCENE \""+queryBuilder.getKeywords()+"\" AND ";
        }

        if (queryBuilder.getStatus().equals(Status.ALL)) {
            sql+=" status <> null";
        }
        else {
            sql+=" status ='" + queryBuilder.getStatus() + "'";
        }

        sql+=" ORDER BY postDate DESC SKIP "+queryBuilder.getStart() + " LIMIT "+queryBuilder.getSize();

        for (Vertex v : (Iterable<Vertex>) orientGraph.command(
                new OCommandSQL(sql)).execute()) {
            Activity activity=orm.buildActivity(v);
            activity.setEvents(getEventsByActivity(v,queryBuilder.getStatus()));
            activities.add(activity);
        }
        return activities;

    }

    @Override
    public List<Activity> getActivitiesByUser(Object user, AccessType accessType, int start, int size,String status) {

        OrientGraph orientGraph=(OrientGraph)g;
        List<Activity> activities=new ArrayList<>();

        Vertex vuser= orientGraphHelper.getVertexFromObject(user);

        String sql="SELECT * FROM (SELECT EXPAND(OUT('"+ OrientGraphHelper.EDGE_OWNERSHIP+"'))"
                + " FROM "+vuser.getId()+")";
        if (!status.isEmpty() && !status.equalsIgnoreCase(Status.ALL)) {
            sql+=" WHERE status ='" + status + "'";
        }
        sql+=" ORDER BY postDate DESC SKIP "+start + " LIMIT "+size;
        for (Vertex v : (Iterable<Vertex>) orientGraph.command(
                new OCommandSQL(sql)).execute()) {
            Activity activity=orm.buildActivity(v);
            activity.setEvents(getEventsByActivity(v,status));
            activities.add(activity);
        }
        return activities;
    }


    private List<Event> getEventsByActivity(Vertex vactivity, String status) {

        List<Event> events=new ArrayList<>();

        Iterator<Vertex> vertexIterator=vactivity.getVertices(Direction.OUT, OrientGraphHelper.EDGE_EVENT).iterator();
        while (vertexIterator.hasNext()) {
            Vertex vevent=vertexIterator.next();
            if (status!=null && !status.equalsIgnoreCase(Status.ALL)) {
                if (!status.equalsIgnoreCase(vevent.getProperty("status"))) continue; //filter out
            }
            events.add(orm.buildEvent(vevent));
        }
        return events;
    }

    @Override
    public Event getEvent(String eventId,Object user,AccessType accessType,boolean increment) {
            try {

                Vertex vevent=g.getVertex(eventId);
                if (increment) {
                    g.addEdge(null, orientGraphHelper.getVertexFromObject(user),vevent, OrientGraphHelper.EDGE_VIEW);
                    vevent.setProperty("views", (int) vevent.getProperty("views") + 1);
                }
                return orm.buildEvent(vevent).strip(accessType);

            } catch (Exception e) {
                g.rollback();
                throw e;
            } finally {
                g.commit();
            }
    }

    @Override
    public List<Event> getEventsByUser(Object user, AccessType accessType, int start, int size,String status) {
        List<Event> events=new ArrayList<>();

        Vertex vuser= orientGraphHelper.getVertexFromObject(user);

        new GremlinPipeline<>().start(vuser)
                .out(OrientGraphHelper.EDGE_PARTICIPATION)
                .has("status",status)
                .range(start, start + size)
                .iterator().forEachRemaining(v -> {
            events.add(orm.buildEvent(v));
        });

        return events;
    }

    @Override
    public Activity updateActivity(Object user, Activity activity) {

        try {

        if (!isOwner(user,activity.getId())) {
            throw new ForbiddenException();
        }

        boolean wasNew = false;

        if (activity.getIsNew()) {
            activity.setIsNew(false);
            wasNew = true;
        }

            Vertex vactivity = g.getVertex(activity.getId());
            Vertex vuser = orientGraphHelper.getVertexFromObject(user);
            if (vactivity==null) throw new NotFoundException();

            //WRITE
            orientGraphHelper.setElementProperties(vactivity, activity.toMap(), null);

            //make the fullText field for indexing now
            String joinedString=String.join(" ", activity.getTags());
            vactivity.setProperty("fullText",activity.getTitle() + " " + joinedString + " " + activity.getDescription());

            //TAGS
            List<Vertex> dbTags=new ArrayList<>();

            for (String stag:activity.getTags()) {
                Iterator<Vertex> vertexIterator = g.query().has("tagName",stag).vertices().iterator();
                if (vertexIterator.hasNext()) {
                    dbTags.add(vertexIterator.next());
                }
                else {
                    Vertex newTag=g.addVertex("class:Tag");
                    newTag.setProperty("tagName",stag);
                    g.commit();
                    dbTags.add(newTag);
                }
            }

            orientGraphHelper.connectVertices(dbTags,"count");

            //this manual index is not sure it is working and has a issue in jira
           /* String joinedTags=String.join(" ",activity.getTags());
            String indexsql="INSERT INTO index:Activities_"+language+" (key,rid) VALUES(['"+activity.getTitle()+"','"+activity.getDescription()
                    +"','"+joinedTags+"'],"+vactivity.getId()+")";
            ((OrientTransactionalGraph)g).command(new OCommandSQL(indexsql)).execute();*/


            /*  STREAMS
                LOGIC: new activities are streamed, updated activities send only notifications to interested parties,
                new participations if allowed are streamed to  their friends
            */
            boolean sendStream=false;

            Info stream = new Info();
            List<Vertex> streamUserList=new ArrayList<>();

            if (wasNew) {
                sendStream=true;
                if (activity.getType().equalsIgnoreCase(Verbs.OFFER)) {
                    stream.setAction(Action.OFFER_ACTIVITY);
                } else if (activity.getType().equalsIgnoreCase(Verbs.REQUEST)) {
                    stream.setAction(Action.REQUEST_ACTIVITY);
                }

                stream.setSourceType(Entities.ACTIVITY);
                streamUserList.add(vuser);
            }

            //do this for previous streams in case we have mainImage change
            if (activity.getMainImage()!=null) {
                try {
                    Vertex vstream = (Vertex) g.query().has("@class", Entities.STREAM).has("targetId", activity.getId()).vertices().iterator().next();
                    vstream.setProperty("targetImage", activity.getMainImage());
                } catch (NoSuchElementException ne) {
                }
            }

            if (sendStream) {

                stream.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username").toString()));
                stream.setSourceType(Entities.USER);
                stream.setLat(activity.getLat());
                stream.setLon(activity.getLon());
                stream.setSourceIds(Arrays.asList(vuser.getId().toString()));
                stream.setSourceImages(Arrays.asList(vuser.getProperty("mainImage").toString()));
                stream.setTargetDisplayNames(Arrays.asList(activity.getTitle()));
                stream.setTargetType(Entities.ACTIVITY);
                stream.setTargetIds(Arrays.asList(vactivity.getId().toString()));
                if (vactivity.getProperty("mainImage") != null) {
                    stream.setTargetImages(Arrays.asList(vactivity.getProperty("mainImage").toString()));
                }
                stream.setPayload(vactivity.getProperty("title"));
                streamDAO.stream(stream, streamUserList);
            }

        } catch (Exception e) {
            g.rollback();
        } finally {
            g.commit();
        }

        return activity;
    }


    @Override
    public void deleteActivity(Object user, String activityId) {

        try {

            if (!isOwner(user,activityId)) throw new ForbiddenException();

            Vertex vactivity = g.getVertex(activityId);
            //todo notify all participants
            Iterator<Vertex> events = vactivity.getVertices(Direction.BOTH, OrientGraphHelper.EDGE_EVENT).iterator();
            while (events.hasNext()) {
                cancelEvent(events.next());
            }
            vactivity.setProperty("status",Status.CANCELLED);

        } catch (Exception e) {
            g.rollback();
            throw e;
        } finally {
            g.commit();
        }
    }

    @Override
    public boolean isOwner(Object user, String activityId) {

        Vertex vuser= orientGraphHelper.getVertexFromObject(user);
        List<Object> vertexIds= (List<Object>) vuser.query().labels(OrientGraphHelper.EDGE_OWNERSHIP).vertexIds();

        //we iterate here because each blueprints implementations give some other kind of object
        for (Object o:vertexIds) {
            if (o.toString().equalsIgnoreCase(activityId)) {
                return true;
            }
        }
            return false;

    }

    public EventRequest createNewEventRequest(Object user,EventRequest eventRequest,String activityId) {

        try {

            Vertex vactivity=g.getVertex(activityId);
            Vertex vowner=vactivity.getVertices(Direction.IN, OrientGraphHelper.EDGE_OWNERSHIP).iterator().next();
            Vertex requester = orientGraphHelper.getVertexFromObject(user);

            if (vowner.equals(requester)) throw new BadRequestException("just create an event without a request if you are the owner");

            Edge edge=g.addEdge(null, requester,vactivity , OrientGraphHelper.EDGE_EVENT_REQUEST);
            g.commit(); //to get the id
            eventRequest.setId(edge.getId().toString());
            orientGraphHelper.setElementProperties(edge,eventRequest,eventRequest.listUnserializableFields());
            Info note=new Info();

            note.setAction(eventRequest.getType());
            note.setSourceIds(Arrays.asList(requester.getId().toString()));
            note.setSourceDisplayNames(Arrays.asList(requester.getProperty("username").toString()));
            note.setSourceType(Entities.USER);
            note.setTargetType(Entities.ACTIVITY);
            note.setTargetIds(Arrays.asList(activityId));

            addNotification(vowner, note);
            brokerService.sendNotification(orm.buildUser(vowner),note);

            return eventRequest;

        } catch (Exception e) {
            g.rollback();
            throw e;
        } finally {
            g.commit();
        }
    }

    /**
     * make a new request for an event
     * @return
     */
    @Override
    public EventRequest createExistingEventRequest(Object user,EventRequest eventRequest,String eventId) {

        try {

            Vertex vuser= orientGraphHelper.getVertexFromObject(user);
            Vertex vevent=g.getVertex(eventId);
            Vertex vactivity=vevent.getVertices(Direction.IN, OrientGraphHelper.EDGE_EVENT).iterator().next();
            Vertex vowner=vactivity.getVertices(Direction.IN, OrientGraphHelper.EDGE_OWNERSHIP).iterator().next();

            Edge edge=g.addEdge(null, vuser,vevent , OrientGraphHelper.EDGE_EVENT_REQUEST);
            g.commit(); //to get the id
            eventRequest.setId(edge.getId().toString());
            orientGraphHelper.setElementProperties(edge,eventRequest,null);

            Info note=new Info();

            note.setAction(eventRequest.getType());
            note.setSourceIds(Arrays.asList(vuser.getId().toString()));
            note.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username").toString()));
            note.setSourceType(Entities.USER);
            note.setTargetType(Entities.EVENT);
            note.setTargetIds(Arrays.asList(vevent.getId().toString()));

            addNotification(vowner, note);
            brokerService.sendNotification(orm.buildUser(vowner), note);

            return eventRequest;

        }catch (Exception e) {
            g.rollback();
            throw e;
        } finally {
            g.commit();
        }
    }

    @Override
    public EventRequest getRequest(Object user,String requestId) {
        //todo make sure he is in the participants list

        Edge erequest=g.getEdge(requestId);
        return orm.buildEventRequest(erequest);
    }

    @Override
    public void voteEventRequest(Object user, Vote vote,String requestId) {
        try {

            Vertex vuser= orientGraphHelper.getVertexFromObject(user);
            //SECURITY
            Edge erequest=g.getEdge(requestId);
            if (!erequest.getProperty("status").toString().equalsIgnoreCase(Status.VOTING)) throw new BadRequestException("request is not pending");

            Vertex vevent=g.getVertex(erequest.getVertex(Direction.IN));
            List<Vertex> participants=new ArrayList<>();

            Iterator<Vertex> vertexIterator=vevent.getVertices(Direction.IN, OrientGraphHelper.EDGE_PARTICIPATION).iterator();
            while (vertexIterator.hasNext()) {
                Vertex v = vertexIterator.next();
                participants.add(v);
            }

            if (!participants.contains(vuser)) throw new ForbiddenException();

            HashMap<String,Map> votes=(erequest.getProperty("votes")==null)?new HashMap<>():erequest.getProperty("votes");

            Map actionMap=new HashMap<>();
            actionMap.put("action",vote.getAction());
            actionMap.put("message",vote.getMessage());
            votes.put(orm.buildUser(vuser).toMinimalJSONString(), actionMap);

            erequest.setProperty("votes", votes);

            //see if this vote determines the outcome
            //todo check for relative majority
            if (erequest.getProperty("democracyType").toString().equalsIgnoreCase(EventRequest.ABSOLUTE_MAJORITY)) {
                if (vote.getAction().equalsIgnoreCase(Verbs.REJECT)) {
                    erequest.setProperty("status",Status.REJECTED);
                    //inform all participants
                    Info note=new Info();
                    note.setAction(Action.REQUEST_REJECTED);
                    note.setTargetType(Entities.REQUEST);
                    note.setTargetIds(Arrays.asList(erequest.getId().toString()));
                    for (Vertex v:participants) {
                        addNotification(v,note);
                        brokerService.sendNotification(orm.buildUser(v),note);
                    }
                }
                else if (vote.getAction().equalsIgnoreCase(Verbs.ACCEPT)) {

                    if (votes.size()==participants.size()-1) { //the -1 is for the requester who obviously agrees
                        //if there was a reject vote with absolute majority then we wouldn't be here , thus no need to chekc
                        erequest.setProperty("status",Status.ACCEPTED);
                        //inform all participants
                        Info note=new Info();
                        note.setAction(Action.REQUEST_ACCEPTED);
                        note.setTargetType(Entities.REQUEST);
                        note.setTargetIds(Arrays.asList(erequest.getId().toString()));
                        for (Vertex v:participants) {
                            addNotification(v,note);
                            brokerService.sendNotification(orm.buildUser(v),note);
                        }

                        //make the action here
                        if (erequest.getProperty("type").toString().equalsIgnoreCase(Action.UPDATE_EVENT)) {
                            vevent.setProperty("date", erequest.getProperty("date"));
                            vevent.setProperty("address", erequest.getProperty("address"));
                            vevent.setProperty("lat", erequest.getProperty("lat"));
                            vevent.setProperty("lon", erequest.getProperty("lon"));
                        }
                        else if (erequest.getProperty("type").toString().equalsIgnoreCase(Action.PARTICIPATE_EVENT)) {
                            Vertex requester=erequest.getVertex(Direction.OUT);
                            Vertex vactivity=vevent.getVertices(Direction.IN, OrientGraphHelper.EDGE_EVENT).iterator().next();
                            g.addEdge(null,requester,vevent, OrientGraphHelper.EDGE_PARTICIPATION);

                            Info stream=new Info();
                            stream.setAction(Action.PARTICIPATE_EVENT);
                            stream.setSourceType(Entities.USER);
                            stream.setTargetType(Entities.EVENT);
                            stream.setSourceIds(Arrays.asList(requester.getId().toString()));
                            stream.setSourceDisplayNames(Arrays.asList(requester.getProperty("username").toString()));
                            stream.setTargetIds(Arrays.asList(vevent.getId().toString()));
                            stream.setTargetDisplayNames(Arrays.asList(vactivity.getProperty("title").toString()));
                            stream.setTargetImages(Arrays.asList(vactivity.getProperty("mainImage").toString()));
                           streamDAO.stream(stream, Arrays.asList(requester));

                        }

                    }
                }
            }

        }catch (Exception e) {
            g.rollback();
            throw e;
        } finally {
            g.commit();
        }

    }


    @Override
    /**
     * only owner can do that and can override all procedures
     */
    public EventRequest updateEventRequest(Object user, EventRequest eventRequest) {
        try {

            Vertex vuser = orientGraphHelper.getVertexFromObject(user);
            Edge erequest = g.getEdge(eventRequest.getId());
            Vertex requester = erequest.getVertex(Direction.OUT);

            if (!eventRequest.getType().equalsIgnoreCase(Action.ADD_EVENT)) {

                Vertex vevent = erequest.getVertex(Direction.IN);
                List<Vertex> participants = new ArrayList<>();
                Iterator<Vertex> vertexIterator = vevent.getVertices(Direction.IN, OrientGraphHelper.EDGE_PARTICIPATION).iterator();
                while (vertexIterator.hasNext()) {
                    Vertex v = vertexIterator.next();
                    participants.add(v);
                }

                Vertex vactivity = vevent.getVertices(Direction.IN, OrientGraphHelper.EDGE_EVENT).iterator().next();

                if (!vuser.equals(vactivity.getVertices(Direction.IN, OrientGraphHelper.EDGE_OWNERSHIP).iterator().next())) {
                    throw new ForbiddenException();
                }

                orientGraphHelper.setElementProperties(erequest,eventRequest,null);

                if (eventRequest.getStatus().equalsIgnoreCase(Status.ACCEPTED)) {

                    if (eventRequest.getType().equalsIgnoreCase(Action.PARTICIPATE_EVENT)) {

                        g.addEdge(null, requester, vevent, OrientGraphHelper.EDGE_PARTICIPATION);

                        //todo inform other participants about new member and stream to his friends
                        Info note = new Info();
                        note.setTargetType(Entities.EVENT);
                        note.setTargetIds(Arrays.asList(vevent.getId().toString()));
                        note.setTargetDisplayNames(Arrays.asList(vactivity.getProperty("title").toString()));
                        note.setAction(Action.PARTICIPATE_EVENT);
                        note.setSourceIds(Arrays.asList(requester.getId().toString()));
                        note.setSourceDisplayNames(Arrays.asList(requester.getProperty("username").toString()));
                        note.setSourceImages(Arrays.asList(requester.getProperty("mainImage").toString()));

                        for (Vertex p : participants) {
                            if (!p.equals(vuser)) {
                                addNotification(p, note);
                                brokerService.sendNotification(orm.buildUser(p), note);
                            }
                        }

                        Info stream = new Info();
                        stream.setAction(Action.PARTICIPATE_EVENT);
                        stream.setTargetIds(Arrays.asList(vevent.getId().toString()));
                        stream.setTargetDisplayNames(Arrays.asList(vactivity.getProperty("title").toString()));
                        stream.setTargetImages(Arrays.asList(vactivity.getProperty("mainImage").toString()));
                        stream.setSourceIds(Arrays.asList(requester.getId().toString()));
                        stream.setSourceDisplayNames(Arrays.asList(requester.getProperty("username").toString()));
                        stream.setSourceImages(Arrays.asList(requester.getProperty("mainImage").toString()));

                    } else if (eventRequest.getType().equalsIgnoreCase(Action.UPDATE_EVENT)) {
                        //todo inform all participants about new dates

                        Info note = new Info();
                        note.setTargetType(Entities.EVENT);
                        note.setTargetIds(Arrays.asList(vevent.getId().toString()));
                        note.setTargetDisplayNames(Arrays.asList(vactivity.getProperty("title").toString()));
                        note.setAction(Action.UPDATE_EVENT);

                        for (Vertex p : participants) {
                            if (!p.equals(vuser)) {
                                addNotification(p, note);
                                brokerService.sendNotification(orm.buildUser(p), note);
                            }
                        }

                    }
                } else if (eventRequest.getStatus().equalsIgnoreCase(Status.REJECTED)) {

                    Info note = new Info();
                    note.setAction(Action.REQUEST_REJECTED);
                    note.setTargetIds(Arrays.asList(eventRequest.getId()));

                    if (!eventRequest.getDemocracyType().equalsIgnoreCase(EventRequest.DICTATORSHIP)) {

                        //todo somehow indicate this was overriden!

                        for (Vertex p : participants) {
                            if (!p.equals(vuser)) {
                                addNotification(p, note);
                                brokerService.sendNotification(orm.buildUser(p), note);
                            }
                        }
                    } else {

                        //todo inform only the requester!
                        addNotification(requester, note);
                        brokerService.sendNotification(orm.buildUser(requester), note);

                    }
                } else if (eventRequest.getStatus().equalsIgnoreCase(Status.VOTING)) {

                    Info note = new Info();
                    note.setAction(Action.REQUEST_REFERRED);
                    note.setTargetIds(Arrays.asList(eventRequest.getId()));

                    for (Vertex p : participants) {
                        if (!p.equals(vuser)) {
                            addNotification(p, note);
                            brokerService.sendNotification(orm.buildUser(p), note);
                        }
                    }
                }
            }
            else { //ADD EVENT REQUEST

                Vertex vactivity = erequest.getVertex(Direction.IN);
                if (!vuser.equals(vactivity.getVertices(Direction.IN, OrientGraphHelper.EDGE_OWNERSHIP).iterator().next())) {
                    throw new ForbiddenException();
                }

                if (eventRequest.getStatus().equalsIgnoreCase(Status.ACCEPTED)) {

                    Vertex vevent = g.addVertex("class:Event");
                    g.commit();

                    vevent.setProperty("date", erequest.getProperty("date"));
                    vevent.setProperty("address", erequest.getProperty("address"));
                    vevent.setProperty("lat", erequest.getProperty("lat"));
                    vevent.setProperty("lon", erequest.getProperty("lon"));

                    //WRITE
                    g.addEdge(null, vactivity, vevent, OrientGraphHelper.EDGE_EVENT);
                    g.addEdge(null, requester, vevent, OrientGraphHelper.EDGE_PARTICIPATION);
                    g.addEdge(null, vuser, vevent, OrientGraphHelper.EDGE_PARTICIPATION);

                    //stream it to friends

                    Info stream = new Info();
                    stream.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username").toString()));
                    stream.setSourceIds(Arrays.asList(vuser.getId().toString()));
                    stream.setAction(Action.PARTICIPATE_EVENT);
                    stream.setSourceImages(Arrays.asList(vuser.getProperty("mainImage").toString()));
                    stream.setTargetDisplayNames(Arrays.asList(vactivity.getProperty("title").toString()));
                    stream.setTargetType(Entities.EVENT);
                    stream.setTargetIds(Arrays.asList(vevent.getId().toString()));
                    if (vactivity.getProperty("mainImage") != null) {
                        stream.setTargetImages(Arrays.asList(vactivity.getProperty("mainImage").toString()));
                    }
                    stream.setPayload(vactivity.getProperty("description").toString());
                    streamDAO.stream(stream, Arrays.asList(vuser));

                    //change some data
                    stream.setSourceDisplayNames(Arrays.asList(requester.getProperty("username").toString()));
                    stream.setSourceIds(Arrays.asList(requester.getId().toString()));
                    stream.setSourceImages(Arrays.asList(requester.getProperty("mainImage").toString()));
                    streamDAO.stream(stream, Arrays.asList(requester));

                    Info note = new Info();
                    note.setAction(Action.REQUEST_ACCEPTED);
                    note.setTargetIds(Arrays.asList(erequest.getId().toString()));
                    note.setReferenceIds(Arrays.asList(vevent.getId().toString()));
                    note.setReferenceDisplayNames(Arrays.asList(vactivity.getProperty("title").toString()));

                    addNotification(requester, note);
                    brokerService.sendNotification(orm.buildUser(requester), note);

                } else if (eventRequest.getStatus().equalsIgnoreCase(Status.REJECTED)) {

                    Info note = new Info();
                    note.setAction(Action.REQUEST_REJECTED);
                    note.setTargetIds(Arrays.asList(erequest.getId().toString()));
                    note.setReferenceIds(Arrays.asList(vactivity.getId().toString()));
                    note.setReferenceDisplayNames(Arrays.asList(vactivity.getProperty("title").toString()));

                    addNotification(requester, note);
                    brokerService.sendNotification(orm.buildUser(requester), note);

                }

            }
        } catch (Exception e) {
            g.rollback();
            throw e;
        } finally {
            g.commit();
        }

        return eventRequest;
    }

    @Override
    /**
     * this cancels the request if either it is pending or it was a participation request
     */
    public void deleteEventRequest(Object user,String requestId) {

        //only the owner of the request can do this
        try {

            Edge erequest=g.getEdge(requestId);
            if (erequest==null) throw new NotFoundException();

            Vertex vuser= orientGraphHelper.getVertexFromObject(user);

            if (!erequest.getVertex(Direction.OUT).equals(vuser))
                throw new ForbiddenException();

            if (erequest.getProperty("status").toString().equalsIgnoreCase(Status.ACCEPTED) &&
                    !erequest.getProperty("type").toString().equalsIgnoreCase(Action.PARTICIPATE_EVENT)) {
                throw new BadRequestException("request is already accepted and cannot be changed");
            }

            Vertex targetVertex=erequest.getVertex(Direction.IN);
            List<Vertex> participants=new ArrayList<>();
            if (targetVertex.getProperty("@class").toString().equalsIgnoreCase("Event")) {
                //inform other participants and the owner of the activity that user withdrew his participation
                Iterator<Vertex> vertexIterator=targetVertex.getVertices(Direction.IN, OrientGraphHelper.EDGE_PARTICIPATION).iterator();
                while (vertexIterator.hasNext()) {
                    Vertex v = vertexIterator.next();
                    participants.add(v);
                }
            }

            //here it's ok if it is accepted
            if (erequest.getProperty("type").toString().equalsIgnoreCase(Action.PARTICIPATE_EVENT)) {

                //remove this user from the event's participants
                erequest.setProperty("status",Status.CANCELLED);

                List<Edge> edgeList= orientGraphHelper.vertex2vertex(vuser,erequest.getVertex(Direction.IN), OrientGraphHelper.EDGE_PARTICIPATION);
                if (edgeList.size() >0) { //means he was a participant
                    g.removeEdge(edgeList.get(0));

                    Info note=new Info();
                    note.setAction(Action.CANCEL_PARTICIPATION);
                    note.setSourceType(Entities.USER);
                    note.setSourceIds(Arrays.asList(vuser.getId().toString()));
                    note.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username").toString()));
                    note.setSourceImages(Arrays.asList(vuser.getProperty("mainImage").toString()));
                    note.setTargetIds(Arrays.asList(targetVertex.getId().toString()));
                    note.setTargetType(Entities.EVENT);

                    for (Vertex p:participants) {
                        if (!p.equals(vuser)) {
                            addNotification(p,note);
                            brokerService.sendNotification(orm.buildUser(p),note);
                        }
                    }
                }

            }
            else if (erequest.getProperty("status").toString().equalsIgnoreCase(Status.PENDING)) {

                Info note = new Info();
                note.setAction(Action.REQUEST_CANCELLED);
                note.setSourceType(Entities.USER);
                note.setTargetType(Entities.REQUEST);
                note.setSourceIds(Arrays.asList(vuser.getId().toString()));
                note.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username")));
                note.setSourceImages(Arrays.asList(vuser.getProperty("mainImage")));
                Vertex vowner=g.getVertices("username",targetVertex.getProperty("owner").toString()).iterator().next();

                addNotification(vowner, note);
                brokerService.sendNotification(orm.buildUser(vowner), note);

            }
            else if (erequest.getProperty("status").toString().equalsIgnoreCase(Status.VOTING)) {

                //inform everyone that the request was cancelled
                Info note = new Info();
                note.setAction(Action.REQUEST_CANCELLED);
                note.setSourceType(Entities.USER);
                note.setTargetType(Entities.REQUEST);
                note.setSourceIds(Arrays.asList(vuser.getId().toString()));
                note.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username")));
                note.setSourceImages(Arrays.asList(vuser.getProperty("mainImage")));
                Vertex vowner=g.getVertices("username",targetVertex.getProperty("owner").toString()).iterator().next();

                addNotification(vowner, note);
                brokerService.sendNotification(orm.buildUser(vowner), note);

                for (Vertex p:participants) {
                    if (!p.equals(vuser)) {
                        addNotification(p,note);
                        brokerService.sendNotification(orm.buildUser(p),note);
                    }
                }
            }

            erequest.setProperty("status", Status.CANCELLED);

        }catch (Exception e) {
            g.rollback();
            throw e;
        } finally {
            g.commit();
        }

    }

    @Override
    public Event createEvent(Object user,Event event) {
        try {

            if (!isOwner(user, event.getActivityId())) throw new ForbiddenException();

            Vertex vevent=g.addVertex("class:Event");
            event.setId(vevent.getId().toString());
            orientGraphHelper.setElementProperties(vevent, event.toMap(), null);

        }catch (Exception e) {
            g.rollback();
        } finally {
            g.commit();
        }

        return event;
    }

    @Override
    public Event updateEvent(Object user, Event event) {

        try {

            Vertex vevent=g.getVertex(event.getId());
            //todo think about caching the title in the event?
            Vertex vactivity=g.getVertex(event.getActivityId());

            //WRITE
            orientGraphHelper.setElementProperties(vevent, event.toMap(), null);

        //PARTICIPANTS
        HashSet<User> newParticipants = new HashSet<>();
        newParticipants.addAll(event.getParticipants());

        Iterator<Edge> existingParticipants = vevent.getEdges(Direction.IN, OrientGraphHelper.EDGE_PARTICIPATION).iterator();
        while (existingParticipants.hasNext()) {
            Edge e = existingParticipants.next();
            Vertex v = e.getVertex(Direction.OUT);
            String userId = v.getId().toString();
            if (newParticipants.stream().noneMatch(u->u.getId().equals(userId))) {
                e.remove();

                Info notification = new Info();
                notification.setTargetIds(Arrays.asList(event.getId()));
                notification.setTargetDisplayNames(Arrays.asList(vactivity.getProperty("title")));
                notification.setAction(Action.DELETE_FROM_EVENT);
                brokerService.sendNotification(orm.buildUser(v), notification);

            } else {
                for (User u:newParticipants) {
                    if (u.getId().equals(userId)) newParticipants.remove(u);
                }
            }
        }

              /* NOTIFICATIONS
                we are notifying all interested parties that there was some kind of update
             */
            for (User participant : event.getParticipants()) {

                Info notification = new Info();
                notification.setReferenceIds(Arrays.asList(event.getId()));
                notification.setAction(Action.UPDATE_EVENT);
                brokerService.sendNotification(participant, notification);
            }

            Activity activity=new Activity();
            BeanUtils.populate(activity, orientGraphHelper.getElementProperties(vactivity,Arrays.asList("events")));
            event.setActivity(activity);


        }catch (Exception e) {
            g.rollback();
        } finally {
            g.commit();
        }
        return event;
    }

    /**
     * only for internal use after we have validated the ownership
     * @param vevent
     */
    private void cancelEvent(Vertex vevent) {
        try {

            vevent.setProperty("status",Status.CANCELLED);
            Iterator<Vertex> participants = vevent.getVertices(Direction.IN, OrientGraphHelper.EDGE_PARTICIPATION).iterator();
            while (participants.hasNext()) {
                Info notification = new Info();
                notification.setReferenceIds(Arrays.asList(vevent.getId().toString())); //this will redirect to the activity since it is deleted!
                notification.setAction(Action.CANCEL_EVENT);
                brokerService.sendNotification(orm.buildUser(participants.next()), notification);
            }

    }catch (Exception e) {
        g.rollback();
        throw e;
    } finally {
        g.commit();
    }
    }

    @Override
    public void cancelEvent(Object user, String eventId) {

            Vertex vevent=g.getVertex(eventId);
            if (vevent==null) throw new NotFoundException();
            if (!isOwner(user,vevent.getProperty("activityId"))) throw new ForbiddenException();

            cancelEvent(vevent);

    }

    /**
     * @param user can be either a String with username/email or and ID or a Vertex
     * @param activity
     * @return
     */
    @Override
    public Activity createActivity(Object user, Activity activity) {

        try {

            Vertex vactivity = g.addVertex("class:Activity");
            g.commit(); //otherwise we get a temp id in our vertex
            activity.setId(vactivity.getId().toString());

            User userPojo=orm.buildUser(user);
            activity.setOwner(userPojo.stripMinimal());

            Vertex vuser= orientGraphHelper.getVertexFromObject(user);
            g.addEdge(null, vuser, vactivity, OrientGraphHelper.EDGE_OWNERSHIP);

/*
            LanguageIdentifier identifier = new LanguageIdentifier(activity.getTitle() + " " + activity.getDescription());
            String language = identifier.getLanguage();
            activity.setLanguage(activity.detectLanguage());
*/

            //WRITE
            orientGraphHelper.setElementProperties(vactivity,activity.toMap(), null);

        } catch (Exception e) {
        } finally {
            g.commit();
        }
        return activity;
    }

    @Override
    public Rating doRating(Object user, Rating rating,String eventId) {


        try {

            Vertex vuser = orientGraphHelper.getVertexFromObject(user);
            Vertex receivingUser = g.getVertex(rating.getReceivingUser().getId());

            Event event;
            try {
                event = getEvent(eventId,vuser,AccessType.UNKNOWN,false);
            } catch (NotFoundException nf) {
                throw nf;
            }

            /*
               the logic is that a participant can rate an event that was offered or an owner can rate a participant for an event that was requested.
            */

            if (!event.getStatus().equalsIgnoreCase(Status.COMPLETED))
                throw new ForbiddenException();
            if (event.getActivity().getType().equalsIgnoreCase(Verbs.OFFER) && event.getParticipants().stream().noneMatch(u->u.getId().equals(vuser.getId().toString())))
                throw new ForbiddenException();
            if (event.getActivity().getType().equalsIgnoreCase(Verbs.REQUEST) && !event.getActivity().getOwner().getId().equalsIgnoreCase(vuser.getId().toString()))
                throw new ForbiddenException();

            Vertex vevent=g.getVertex(event.getId());

            //assure uniqueness
            orientGraphHelper.vertex2vertex(vuser,receivingUser, OrientGraphHelper.EDGE_RATING).forEach(e -> {
                if (e.getProperty("eventId").toString().equalsIgnoreCase(event.getId()))
                    throw new BadRequestException();
            });

            Edge erating = g.addEdge(null, vuser, vevent, OrientGraphHelper.EDGE_RATING);

            erating.setProperty("rating",rating.getRating());
            erating.setProperty("review",rating.getReview());
            erating.setProperty("receivingUser",receivingUser.getId().toString());

            Integer vibes=receivingUser.getProperty("vibes");
            receivingUser.setProperty("vibes",vibes+rating.getRating());

            Info stream = new Info();
            stream.setSourceDisplayNames(Arrays.asList(vuser.getProperty("username").toString()));
            stream.setTargetDisplayNames(Arrays.asList(receivingUser.getProperty("username").toString()));
            stream.setAction(Action.RATE);
            stream.setSourceType(Entities.USER);
            stream.setSourceImages(Arrays.asList(vuser.getProperty("mainImage").toString()));
            stream.setSourceIds(Arrays.asList(vuser.getId().toString()));
            stream.setTargetType(Entities.USER);
            stream.setTargetImages(Arrays.asList(receivingUser.getProperty("mainImage").toString()));
            stream.setTargetIds(Arrays.asList(receivingUser.getId().toString()));
            //todo think if we want to show the rating or not

            streamDAO.stream(stream, Arrays.asList(vuser, receivingUser));

            return rating;

        } catch (Exception e) {
            g.rollback();
            throw e;
        } finally {
            g.commit();
        }

    }
}