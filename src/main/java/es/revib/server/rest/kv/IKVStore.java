package es.revib.server.rest.kv;

import org.codehaus.jettison.json.JSONObject;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface IKVStore {
    boolean put(String key,JSONObject value);
    boolean put(String key,String value);
    JSONObject get(String key);
    boolean delete(String key);
}
