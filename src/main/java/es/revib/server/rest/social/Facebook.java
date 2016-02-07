package es.revib.server.rest.social;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import es.revib.server.rest.entities.User;

public class Facebook {

    FacebookClient facebookClient;

    public Facebook(String authToken) {
        this.facebookClient=new DefaultFacebookClient(authToken);
    }

    //ORM function
    public static User buildUser(JsonObject fbUser) {

        User user=new User();
        if (fbUser.has("email")) user.setEmail(fbUser.getString("email"));
        user.setFirstName(fbUser.getString("first_name"));
        user.setLastName(fbUser.getString("last_name"));
        user.setFacebookId(fbUser.getString("id"));
        if (fbUser.has("about")) user.setDescription(fbUser.getString("about"));
        if (fbUser.has("address")) user.setAddress(fbUser.getString("address"));

        user.setMainImage("http://http://graph.facebook.com/"+fbUser.getString("id")+"/picture");
        user.getPreferences().put("locale",fbUser.getString("locale"));
        user.setEmailVerified(true);

        return user;
    }

    public User getUserData() {
        JsonObject fbUser = facebookClient.fetchObject("me", JsonObject.class, Parameter.with("fields", "id, name,email,first_name,last_name,about,address,locale,birthday,devices,location"));

        return Facebook.buildUser(fbUser);
    }
    
}
