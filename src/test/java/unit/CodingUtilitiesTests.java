package unit;

import es.revib.server.rest.entities.Info;
import es.revib.server.rest.util.CodingUtilities;
import es.revib.server.rest.util.Globals;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CodingUtilitiesTests {

    @Test
    public void combinationsTest() throws Exception {
        //note that the values of the array are not important here
        List<int[]> combos=new CodingUtilities().combination(new Integer[3],2);
        assertArrayEquals(combos.get(0), new int[]{0, 1});
        assertArrayEquals(combos.get(1), new int[]{0, 2});
        assertArrayEquals(combos.get(2), new int[]{1, 2});
    }

    @Test
    public void jsonToMapTest() throws JSONException {
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("date",new Date().getTime());
        jsonObject.put("user","test");
        Map<String,Object> map=new CodingUtilities().jsonToMap(jsonObject);
        assertEquals("test",map.get("user"));
        if (!(map.get("date") instanceof Number)) fail();

        //now try with nesting
        jsonObject=new JSONObject("{name:'test',friend:{name:'testious',age:38}}");
        map=new CodingUtilities().jsonToMap(jsonObject);
        assertTrue(map.get("friend") instanceof Map);
    }

    @Test
    public void newValuesAddedTest() {
        List original= new ArrayList<>(Arrays.asList("a","b","c"));
        List collection=new ArrayList<>(Arrays.asList("a","c"));
        assertEquals(false,new CodingUtilities().newValuesAdded(original,collection));
        collection.add("d");
        assertEquals(true,new CodingUtilities().newValuesAdded(original,collection));
    }


}
