package es.revib.server.rest.dao;

import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.util.AccessType;
import es.revib.server.rest.util.CodingUtilities;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONUtility;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.NotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * this class contain utilities that are common in DAO classes
 */
public class OrientGraphHelper extends BaseGraphHelper{

    public OrientGraphHelper(TransactionalGraph g) {
        super(g);
    }

    public List<Element> executeQuery(String sql) {

        List<Element> elements=new ArrayList<>();
        OrientGraph orientGraph=(OrientGraph)g;
        ((Iterable<Element>) orientGraph.command(new OCommandSQL(sql)).execute()).forEach(elements::add);

        return elements;
    }

    @Override
    public boolean isId(String s) {
        return (s.contains("#"))?true:false; //todo make it more thorough
    }

    public  Map<String, Object> getElementProperties(final Element element, List<String> exclusionList) {

        if (exclusionList==null) exclusionList=new ArrayList<>();
        final Map<String, Object> properties = new HashMap<String, Object>();
        final List<String> finalExclusionList;
        if (exclusionList!=null) finalExclusionList= exclusionList; //mumbo jumbo of lambda expressions wanting a final variable
        else finalExclusionList=new ArrayList<>();
        element.getPropertyKeys().stream().filter(key -> !finalExclusionList.contains(key)).forEach(key -> properties.put(key, element.getProperty(key)));
        properties.put("id", element.getId().toString());
        properties.remove("class");
        return properties;

    }

}
