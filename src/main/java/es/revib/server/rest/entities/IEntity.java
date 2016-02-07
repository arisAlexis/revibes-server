package es.revib.server.rest.entities;

import com.google.gson.Gson;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import es.revib.server.rest.dao.OrientGraphHelper;
import es.revib.server.rest.util.AccessType;
import es.revib.server.rest.util.CodingUtilities;
import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface IEntity {

    default List<String> listUnserializableFields() {
      return new ArrayList<>();
    }

    default Map toMap() {

            CodingUtilities codingUtilities=new CodingUtilities();
            Map map= null;
            try {
                map = codingUtilities.jsonToMap(new JSONObject(new Gson().toJson(this).toString()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            codingUtilities.removeFromMap(map, listUnserializableFields());
            return map;

    }

    default void writeVertex(Vertex vertex,TransactionalGraph g) {
            new OrientGraphHelper(g).setElementProperties(vertex, toMap(),listUnserializableFields());
    }

    IEntity strip(AccessType accessType);

}
