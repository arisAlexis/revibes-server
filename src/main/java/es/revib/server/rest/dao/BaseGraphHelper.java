package es.revib.server.rest.dao;

import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONUtility;
import es.revib.server.rest.util.CodingUtilities;
import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.NotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/** Generic graph helper class working with Tinkerpop Blueprints. Note that the commit and rolldbacks are a responsibility of the caller class!
 *
 */
public abstract class BaseGraphHelper {

    public static final String EDGE_OWNERSHIP="Ownership";
    public static final  String EDGE_EVENT_REQUEST="EventRequest";
    public static final  String EDGE_EVENT="EventEdge";
    public static final  String EDGE_PARTICIPATION="Participation";
    public static final  String EDGE_RATING="Rating";
    public static final  String EDGE_VIEW="View";
    public static final  String EDGE_FRIENDSHIP="Friendship";
    public static final  String EDGE_FRIEND_REQUEST="FriendRequest";
    public static final  String EDGE_CONNECTION="Connection";
    public static final  String EDGE_STREAM="StreamEdge";
    public static final  String EDGE_TAG="TagEdge";
    public static final  String EDGE_RETWEET="RetweetEdge";
    public static final  String EDGE_LIKE="PlusOne";
    public static final  String EDGE_COMMENT="CommentEdge";

    CodingUtilities codingUtilities=new CodingUtilities();
    TransactionalGraph g;

    abstract boolean isId(String s);

    public BaseGraphHelper(TransactionalGraph g){
        this.g=g;
    }

    /**
     * helper function that detects the kind of user object that we have
     * @param user
     * @return
     */
    public Vertex getVertexFromObject(Object user) {

        Vertex vuser=null;

        if (user instanceof Vertex) vuser=(Vertex)user;
        else if (user instanceof String) {
            if (isId((String) user)) {
                vuser=g.getVertex(user);
            }
            else {
                vuser = findVertex("email", (String) user);
            }
        }

        return vuser;

    }

    /**
     * convenience method overload
     * @param element
     * @param o
     * @param exclusionList
     */
    public void setElementProperties(final Element element,final Object o,List<String> exclusionList) {

        Map m=null;
        try {
            m= BeanUtils.describe(o);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        setElementProperties(element,m,exclusionList);
    }

    /**
     * small helper function for not repeating code
     * @param column
     * @param value
     * @return
     */
    public Vertex findVertex(String column, String value) {
        Iterator<Vertex> vertexIterator=g.getVertices(column,value).iterator();
        if (!vertexIterator.hasNext()) throw new NotFoundException();
        return vertexIterator.next();
    }

    /**
     * this is intentionally abstract because we want maybe to strip some extra fields written by the specific database
     * @param element
     * @param exclusionList
     * @return
     */
    abstract Map<String, Object> getElementProperties(final Element element, List<String> exclusionList);

    /**
     * copy of ElementHelper's function from tinkerpop but not writing null values.
     * @param element
     * @param properties
     * @param exclusionList
     */
    public  void setElementProperties(Element element, final Map<String, Object> properties, List<String> exclusionList) {
        if (exclusionList==null) exclusionList=new ArrayList<>();

        for (Map.Entry<String, Object> property : properties.entrySet()) {
            if (property.getValue()!=null
                    && !property.getKey().equalsIgnoreCase("id")
                    && !property.getKey().equalsIgnoreCase("class")
                    && !exclusionList.contains(property.getKey())
                    ) {

                element.setProperty(property.getKey(), property.getValue());
            }
        }
    }
    /**
     * used to find edges between two vertices
     * the is a java representation of the following code in gremlin:
     * g.v(1).bothE.as('x').bothV.retain([g.v(3)]).back('x')
     *
     * another simpler way of doing it is:
     * existing = a.both('connected in article').filter{it == b}.count()
     *
     * @param v1
     * @param v2
     * @param edgeLabel if null the function returns all edges
     * @return
     */
    public List<Edge> vertex2vertex(Vertex v1,Vertex v2,String edgeLabel) {

        //todo SLOWEST CODE POSSIBLE IT ITERATES THROUGH ALL EDGES, FIND A GREMLIN EQUIVALENT

        List<Edge> edgeList = new ArrayList<>();
        if (edgeLabel==null || edgeLabel.isEmpty()) {
            edgeLabel= EDGE_CONNECTION;
        }

        Iterator<Edge> edgeIterator=v1.getEdges(Direction.BOTH,edgeLabel).iterator();
        while (edgeIterator.hasNext()) {
            Edge edge=edgeIterator.next();
            if ((edge.getVertex(Direction.IN).equals(v1) && edge.getVertex(Direction.OUT).equals(v2))
                    || (edge.getVertex(Direction.IN).equals(v2) && edge.getVertex(Direction.OUT).equals(v1))) {
                edgeList.add(edge);
            }
        }
        return edgeList;
    }

    /**
     * TThis function takes a list of vertices and interconnects them with the label edge, augmenting a counter column
     * on each edge
     * @param elements
     * @param counterColumnName
     */
    public void connectVertices(List<Vertex> elements,String counterColumnName) {

        List<int[]> combos = null;
        try {
            combos = codingUtilities.combination(new Integer[elements.size()], 2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int[] a : combos) {
            Vertex v1=elements.get(a[0]);
            Vertex v2= elements.get(a[1]);
            List<Edge> edges = vertex2vertex(v1,v2,EDGE_CONNECTION);
            //normally this should return exactly one edge but in the future we may need to do more generic stuff with edges
            Edge edge;
            if (edges.size()>0) {
                edge = edges.get(0);
            }
            else {
                edge=g.addEdge(null,v1,v2,EDGE_CONNECTION);
            }
            int connections = 0;
            if (edge.getProperty(counterColumnName) != null) {
                connections = edge.getProperty(counterColumnName);
            }
            connections++;
            edge.setProperty(counterColumnName, connections);
            g.commit();
        }
    }

    /**
     * Function to convert a vertex to JSON and which properties we want from the vertex
     * @param v
     * @param keys
     * @return
     */
    public JSONObject vertexToJSON(Vertex v,String[] keys) {

        JSONObject jo=null;
        HashSet<String> hashKeys=null;
        if (keys!=null)
            hashKeys=new HashSet<>(Arrays.asList(keys));

        try {
            jo= GraphSONUtility.jsonFromElement(v, hashKeys, GraphSONMode.COMPACT);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jo;
    }

}
