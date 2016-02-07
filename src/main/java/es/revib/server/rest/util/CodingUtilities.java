package es.revib.server.rest.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.tika.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Representation of date for past,now etc and adult means that date is >18years before
 */

public class CodingUtilities {

    public enum Temporal {
        PAST,NOW,FUTURE,NONE,ADULT
    }

    public  JSONArray JSONArrayConcat(JSONArray a,JSONArray b) {

        JSONArray mergedArray= new JSONArray();
        try {
        for (int i=0;i < a.length();i++) {

                mergedArray.put(a.get(i));

        }
        for (int i=0;i < b.length();i++) {
            mergedArray.put(a.get(i));
        }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mergedArray;
    }

    public   boolean checkDate(String s,Temporal t) {

        LocalDate lo;
        try {
            lo=LocalDate.parse(s);
        }
        catch (DateTimeParseException dpe) {
            return false;
        }

        switch (t) {
            case PAST: return lo.isBefore(LocalDate.now());
            case FUTURE: return lo.isAfter(LocalDate.now());
            default:return true;
        }

    }

    public boolean checkDate(long timestamp,Temporal t) {

        switch (t) {
            case PAST: return timestamp < new Date().getTime();
            case FUTURE: return timestamp > new Date().getTime();
            default:return true;
        }

    }

    public List<int[]> combination(Object[]  elements, int K) throws Exception {

        List<int[]> combos=new ArrayList<>();

        if (K<2) return combos;

        // get the length of the array
        // e.g. for {'A','B','C','D'} => N = 4
        int N = elements.length;

        if(K > N){
            throw new Exception("Invalid input, K > N");
        }

        // calculate the possible combinations
        // e.g. c(4,2)
        // c(N,K);

        // get the combination by index
        // e.g. 01 --> AB , 23 --> CD
        int combination[] = new int[K];

        // position of current index
        //  if (r = 1)              r*
        //  index ==>        0   |   1   |   2
        //  element ==>      A   |   B   |   C
        int r = 0;
        int index = 0;

        while(r >= 0){
            // possible indexes for 1st position "r=0" are "0,1,2" --> "A,B,C"
            // possible indexes for 2nd position "r=1" are "1,2,3" --> "B,C,D"

            // for r = 0 ==> index < (4+ (0 - 2)) = 2
            if(index <= (N + (r - K))){
                combination[r] = index;

                // if we are at the last position print and increase the index
                if(r == K-1){

                    //do something with the combination e.g. add to list or print
                    combos.add(combination.clone());
                    index++;
                }
                else{
                    // select index for next position
                    index = combination[r]+1;
                    r++;
                }
            }
            else{
                r--;
                if(r > 0)
                    index = combination[r]+1;
                else
                    index = combination[0]+1;
            }
        }

        return combos;
    }

    /**
     * supports only 1 level of nesting
     * @param o
     * @return
     */
    public Map<String,Object> jsonToMap(JSONObject o) {
        java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
      return new Gson().fromJson(o.toString(), mapType);

    }
    /**
     * generic method that scans if anything has been added to the original collection. removal is ok.
     * @param original
     * @param collection
     * @param <T>
     * @return
     */
    public <T> boolean newValuesAdded(Collection<T> original,Collection<T> collection) {

        Iterator iterator=collection.iterator();
        while (iterator.hasNext()) {
            if (!original.contains(iterator.next())) return true;
        }
        return false;
    }

    public String getResourceAsString(String path) {
        String s=null;
        URL url = getClass().getResource(path);
        InputStream in = null;
        try {
            in = url.openStream();
            s = IOUtils.toString(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
        }
        return s;
    }


    public void removeFromMap(Map map,List<String> keys) {
        for (String k:keys) {
            map.remove(k);
        }
    }

    /**
     * helper function
     * @return
     */
    public String toUtf(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


}

