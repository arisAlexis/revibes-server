package es.revib.server.rest.auth;

import com.restfb.exception.FacebookOAuthException;
import es.revib.server.rest.dao.IUserDAO;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.social.Facebook;
import es.revib.server.rest.util.AccessType;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.util.Date;

public class LogonService {

    @Inject
    IUserDAO userDAO;

    public User basicAuth(String basicUser, String basicPassword) throws ForbiddenException, NotFoundException, BadRequestException {
        User user = userDAO.dbAuthUser(basicUser, basicPassword);
        return user;
    }

    public User fbAuth(String token) throws ForbiddenException {

        Facebook facebook = new Facebook(token);
        User fbUser;
        try {
            fbUser = facebook.getUserData();
        } catch (FacebookOAuthException foe) {
            throw new ForbiddenException();
        }
        //find the user
        User user;
        try {
            user = userDAO.getUserBy("facebookId", fbUser.getFacebookId(), AccessType.OWNER);
        } catch (NotFoundException nfe) {
            //try again with the email?
            try {
                user = userDAO.getUser(fbUser.getEmail(), AccessType.OWNER);
                user.setFacebookId(fbUser.getFacebookId());
                user = userDAO.updateUser(user);
            } catch (NotFoundException secondnfe) {
                user = userDAO.createUser(facebook.getUserData());
            }
        }

        return user;
    }

    public void disconnect(User user, String device) {

    }

    public void connect(User user) {

        user.setLastLogin(new Date().getTime());
        userDAO.updateUser(user);

    }


}
