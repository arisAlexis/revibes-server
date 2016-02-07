package es.revib.server.rest.dao;

import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import es.revib.server.rest.broker.BrokerService;
import es.revib.server.rest.broker.IBroker;
import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.entities.Comment;
import es.revib.server.rest.entities.Info;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.NotFoundException;
import java.util.*;

@Service
public class OrientStreamDAO  implements IStreamDAO {

    @Inject
    IORM orm;

    private TransactionalGraph g= OrientDatabase.getInstance().getGraphTx();
    private OrientGraphHelper orientGraphHelper =new OrientGraphHelper(g);

    @Override
    public void stream(@Valid Info stream, List vusers) {

        try {


            Vertex streamVertex = g.addVertex("class:Stream");
            stream.writeVertex(streamVertex, g);

            Iterator iterator = vusers.iterator();
            while (iterator.hasNext()) {
                Vertex vuser = (Vertex) iterator.next();
                Edge edge = g.addEdge(null, vuser, streamVertex, OrientGraphHelper.EDGE_STREAM);
                edge.setProperty("timestamp", stream.getTimestamp());

            }

            g.commit();
        }
            catch(Exception e) {
                g.rollback();
            }
    }

    @Override
    public Set<Info> getByUser(Object user, long timestamp, int size,Double lat, Double lon, Integer radius) {

        OrientGraph orientGraph=(OrientGraph)g;

        Comparator<Info> comparator = Comparator.comparing(o -> o.getTimestamp());
        Set<Info> streams = new TreeSet<>(comparator.reversed());

        Vertex vuser = orientGraphHelper.getVertexFromObject(user);
        String uId = vuser.getId().toString();

        //friends streams
        String sql = "select from (select expand(outE('" +
                OrientGraphHelper.EDGE_STREAM + "','" +
                OrientGraphHelper.EDGE_RETWEET + "','" +
                OrientGraphHelper.EDGE_LIKE + "')) " +
                "from (select expand(both('" + OrientGraphHelper.EDGE_FRIENDSHIP + "')) from " + uId + ")) where timestamp < " + timestamp +
                " limit " + size;

        orientGraphHelper.executeQuery(sql).forEach(e -> {
            Edge edge = (Edge) e;
            Vertex s = edge.getVertex(Direction.IN);
            //if (!s.getProperty("@class").equals("Stream")) return;

            Info i = orm.buildInfo(s);

            switch (edge.getLabel()) {
                case OrientGraphHelper.EDGE_LIKE:
                    i.setOrigin(Info.ORIGIN_LIKE);
                    break;
                case OrientGraphHelper.EDGE_COMMENT:
                    i.setOrigin(Info.ORIGIN_COMMENT);
                    break;
                case OrientGraphHelper.EDGE_RETWEET:
                    i.setOrigin(Info.ORIGIN_RETWEET);
                    break;
                default:
                    i.setOrigin(Info.ORIGIN_STREAM);
            }
            streams.add(i);
        });

        //todo comments require one more level of traversing

        //topics
        //todo get subscribed topic streams and add them to the vertexes

        //geo
        //todo according to the user's preferences lat/lon and/or current location get geolocation streams


        //todo combine those two

        sql="SELECT FROM Stream WHERE [lat,lon,$spatial] " +
                "NEAR ["+vuser.getProperty("lat")+","+vuser.getProperty("lon")+",{\"maxDistance\": "+radius+"}] AND timestamp < "+timestamp +
                " LIMIT "+size;
        //his residence
        for (Vertex v : (Iterable<Vertex>) orientGraph.command(
                new OCommandSQL(sql)).execute()) {
            Info stream=orm.buildInfo(v);
            stream.setOrigin(Info.ORIGIN_GEOLOCATION);
            streams.add(stream);
        }

        //current geolocation
        if (lat!=null && lon!=null) {
        sql="SELECT FROM Stream WHERE [lat,lon,$spatial] " +
                "NEAR [" + lat + "," + lon + ",{\"maxDistance\": " + radius + "}] AND timestamp < " + timestamp +
                " LIMIT " + size;

            for (Vertex v : (Iterable<Vertex>) orientGraph.command(
                    new OCommandSQL(sql)).execute()) {
                Info stream = orm.buildInfo(v);
                stream.setOrigin(Info.ORIGIN_GEOLOCATION);
                streams.add(stream);
            }
        }

        return streams;

    }

    /**
     * generic function to like some entity
     * @param user
     * @param id
     */
    @Override
    public void like(Object user, String id) {

        try {
            Vertex vuser = orientGraphHelper.getVertexFromObject(user);
            Vertex v = g.getVertex(id);
            if (v == null) throw new NotFoundException();

            Edge e = g.addEdge(null, vuser, v, OrientGraphHelper.EDGE_LIKE);
            e.setProperty("timestamp", new Date().getTime());

            g.commit();

        } catch (Exception e) {
            g.rollback();
            throw e;
        }
    }

    @Override
    public Info getStream(Object user, String id) {
        return orm.buildInfo(id);
    }

    @Override
    public Comment newComment(Object user, Comment comment) {

        try {

            Vertex vuser= orientGraphHelper.getVertexFromObject(user);
            Vertex vstream=g.getVertex(comment.getReferenceId());
            if (vstream==null) throw new NotFoundException();

            Vertex vcomment=g.addVertex("class:Comment");
            comment.writeVertex(vcomment,g);
            g.commit();
            comment.setId(vcomment.getId().toString());

            g.addEdge(null, vuser, vcomment, OrientGraphHelper.EDGE_COMMENT);
            g.addEdge(null,vcomment,vstream, OrientGraphHelper.EDGE_COMMENT);

            g.commit();

        }catch (Exception e) {
            g.rollback();
            throw e;
        }

        return comment;
    }

    @Override
    public void revibe(Object user, String streamId) {

        Vertex vstream=g.getVertex(streamId);
        Vertex vuser=orientGraphHelper.getVertexFromObject(user);
        try {
            Edge edge=g.addEdge(null, vuser, vstream, BaseGraphHelper.EDGE_RETWEET);
            edge.setProperty("timestamp",vstream.getProperty("timestamp"));
            g.commit();
        }
        catch (Exception e) {
            g.rollback();
        }
    }
}
