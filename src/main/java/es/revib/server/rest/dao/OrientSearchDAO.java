package es.revib.server.rest.dao;

import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.Vertex;
import es.revib.server.rest.database.OrientDatabase;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jvnet.hk2.annotations.Service;

import javax.ws.rs.ServerErrorException;
import java.util.Iterator;

@Service
public class OrientSearchDAO implements ISearchDAO {

    private TransactionalGraph g= OrientDatabase.getInstance().getGraphTx();

    @Override
    public JSONObject getTrendingTags(int limit) {

        JSONObject container = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        // Output the results

        try {
            container.put("tags",jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new ServerErrorException(500);
        }
        return container;
    }

    @Override
    public JSONObject searchTags(String prefix, int limit) {

        JSONObject container = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        OrientGraph orientGraph=(OrientGraph)g;

        try {

        String sql="SELECT * FROM Tag WHERE tagName LUCENE \""+prefix + "*\" LIMIT "+limit;
        for (Vertex v : (Iterable<Vertex>) orientGraph.command(
                new OCommandSQL(sql)).execute()) {
            JSONObject jsonObject = new JSONObject();
            String tag =v.getProperty("tagName").toString();
            jsonObject.put("tagName", tag);
            jsonArray.put(jsonObject);
        }

        container.put("tags",jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return container;
    }

    @Override
    public JSONObject searchUsers(String prefix, int limit) {

        JSONObject container=new JSONObject();
        JSONArray jsonArray= new JSONArray();

        OrientGraph orientGraph=(OrientGraph)g;

        try {

            String sql="SELECT * FROM User WHERE [firstName,lastName,email,username] LUCENE \""+prefix + "*\" LIMIT "+limit;
            for (Vertex v : (Iterable<Vertex>) orientGraph.command(
                    new OCommandSQL(sql)).execute()) {
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("name",v.getProperty("firstName")+" " +v.getProperty("lastName"));
                jsonObject.put("username",v.getProperty("username").toString());
                jsonArray.put(jsonObject);

            }

            container.put("users",jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return container;

    }

    @Override
    public JSONObject relevantTags(String tagName, int limit) {
        JSONObject container=new JSONObject();
        JSONArray jsonArray= new JSONArray();
        try {
            OrientGraph orientGraph=(OrientGraph)g;

            String sql="select * from (select expand(bothV()) from (select expand(bothE('"+BaseGraphHelper.EDGE_CONNECTION+"')) " +
                    "from Tag where tagName='"+tagName+"' order by count desc)) where tagName <> '"+tagName+"' limit "+limit;

            for (Vertex v : (Iterable<Vertex>) orientGraph.command(
                    new OCommandSQL(sql)).execute()) {
                JSONObject jsonObject = new JSONObject();
                String tag =v.getProperty("tagName").toString();
                jsonObject.put("tagName", tag);
                jsonArray.put(jsonObject);
            }
            container.put("tags",jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return container;
    }
}
