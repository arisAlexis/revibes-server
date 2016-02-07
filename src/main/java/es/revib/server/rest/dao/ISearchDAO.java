package es.revib.server.rest.dao;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jvnet.hk2.annotations.Contract;

import javax.xml.ws.Response;

@Contract
public interface ISearchDAO {

    JSONObject getTrendingTags(int limit);
    JSONObject searchTags(String prefix,int limit);
    JSONObject searchUsers(String prefix,int limit);
    JSONObject relevantTags(String tag,int limit);

}
