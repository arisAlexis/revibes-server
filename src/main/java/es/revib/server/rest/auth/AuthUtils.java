package es.revib.server.rest.auth;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import es.revib.server.rest.database.OrientDatabase;
import org.codehaus.jettison.json.JSONException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.DatatypeConverter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthUtils {


    //Add salt
    private static  String getSalt() throws NoSuchAlgorithmException
    {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt.toString();
    }

    /**
     * returns a JSON object with {password:xxx,salt:yyy} for storing in the db
     * @param plainText
     * @return
     */
    public static Map constructPassword(String plainText) {
        String hashedPassword=null;
        String salt= null;
        try {
            salt = getSalt();
            hashedPassword=get_SHA_256_SecurePassword(plainText,salt);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Map password=new HashMap<>();
        password.put("hash",hashedPassword);
        password.put("salt",salt);

        return password;
    }
    /**
     * Decode the basic auth and convert it to array login/password
     *
     * @param auth The string encoded authentication
     * @return The login (case 0), the password (case 1)
     */
    public  static String[] decode(String auth) {
        //Replacing "Basic THE_BASE_64" to "THE_BASE_64" directly
        auth = auth.replaceFirst("[B|b]asic ", "");

        //Decode the Base64 into byte[]
        byte[] decodedBytes = DatatypeConverter.parseBase64Binary(auth);

        //If the decode fails in any case
        if (decodedBytes == null || decodedBytes.length == 0) {
            return null;
        }

        //Now we can convert the byte[] into a splitted array :
        //  - the first one is login,
        //  - the second one password
        return new String(decodedBytes).split(":", 2);
    }

    public static String get_SHA_256_SecurePassword(String passwordToHash, String salt) throws NoSuchAlgorithmException {
        String generatedPassword = null;

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] bytes = md.digest(passwordToHash.getBytes());
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++)
            {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();

        return generatedPassword;
    }

}
