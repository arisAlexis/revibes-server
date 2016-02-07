package es.revib.server.rest.jersey;

import es.revib.server.rest.approval.IApprovalService;
import es.revib.server.rest.broker.BrokerService;
import es.revib.server.rest.broker.IBroker;
import es.revib.server.rest.dao.IActivityDAO;
import es.revib.server.rest.dao.IUserDAO;
import es.revib.server.rest.entities.Action;
import es.revib.server.rest.entities.Info;
import es.revib.server.rest.entities.Entities;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.kv.IKVStore;
import es.revib.server.rest.storage.IStorageService;
import es.revib.server.rest.util.AccessType;
import es.revib.server.rest.util.CodingUtilities;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.UUID;

@Path("approval")
public class ApprovalEndpoint {


    @Inject
    BrokerService brokerService;

    @Inject IApprovalService approvalService;

    @Inject
    IStorageService storageService;

    @Inject
    IKVStore kvStore;

    @Inject
    IActivityDAO activityDAO;

    @Inject IUserDAO userDAO;
    @Context
    HttpServletRequest request;

    private String currentUserId=null;

    @PostConstruct
    void init() {

        if (request.isRequestedSessionIdValid()) {
            currentUserId=(String) request.getSession(false).getAttribute("userId");
        }
        else {
            currentUserId=(String)request.getAttribute("userId");
        }
    }

    @GET
    @Path("/file")
    public Response approveFile(@QueryParam("url") String url,
                                @QueryParam("referenceType") String type,
                                @QueryParam("referenceId") String referenceId) {

        JSONObject jsonObject=new JSONObject();
        String key= UUID.randomUUID().toString();

        try {
            jsonObject.put("value",url);
            jsonObject.put("user",currentUserId);
            jsonObject.put("referenceType",type);
            jsonObject.put("referenceId",referenceId);
            jsonObject.put("type", IApprovalService.TYPE_FILE);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        kvStore.put(key,jsonObject);

        //WARNING this is important if we are using a dummy service to be after we store the object in the kvstore!
        approvalService.sendForApproval(key,url);

        return Response.ok().build();

    }

    @GET
    @Path("/link")
    public Response approveLink(@QueryParam("url") String url,
                                @QueryParam("referenceType") String type,
                                @QueryParam("referenceId") String referenceId) {


        JSONObject jsonObject=new JSONObject();
        String key= UUID.randomUUID().toString();
        try {
            jsonObject.put("value",url);
            jsonObject.put("user",currentUserId);
            jsonObject.put("referenceType",type);
            jsonObject.put("referenceId",referenceId);
            jsonObject.put("type",IApprovalService.TYPE_LINK);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        kvStore.put(key,jsonObject);
        approvalService.sendForApproval(key,url);

        return Response.ok().build();

    }


    @GET
    @Path("/text")
    public Response approveText(@QueryParam("text") String text,
                                @QueryParam("referenceType") String type,
                                @QueryParam("referenceId") String referenceId) {

        JSONObject jsonObject=new JSONObject();
        String key= UUID.randomUUID().toString();
        try {
            jsonObject.put("user",currentUserId);
            jsonObject.put("referenceType",type);
            jsonObject.put("referenceId",referenceId);
            jsonObject.put("type",IApprovalService.TYPE_TEXT);
            jsonObject.put("value",text);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        kvStore.put(key,jsonObject);
        //WARNING this is important if we are using a dummy service to be after we store the object in the kvstore!
        approvalService.sendForApproval(key,text);

        return Response.ok().build();

    }

    @Path("/callback")
    @GET
    public void callback(
            @QueryParam("key") String key,
            @QueryParam("status") String status) {

        Info notification=new Info();

        try {

            JSONObject jsonObject= kvStore.get(key);
            User currentUser=userDAO.getUser(jsonObject.getString("user"), AccessType.OWNER);
            String referenceType=jsonObject.getString("referenceType");
            String type=jsonObject.getString("type");
            String value= (String) jsonObject.get("value");

            if (status.equalsIgnoreCase(IApprovalService.APPROVED)) {

                if (referenceType.equalsIgnoreCase(Entities.USER)) {
                    //give it to the user dao
                    userDAO.approveCallback(jsonObject);
                }
                else if (referenceType.equalsIgnoreCase(Entities.ACTIVITY)) {
                    activityDAO.approveCallback(jsonObject);
                }
                if (type.equalsIgnoreCase(IApprovalService.TYPE_FILE) || type.equals(IApprovalService.TYPE_LINK)) {
                    notification.setAction(Action.MEDIA_ACCEPTED);
                    notification.setReferenceIds(Arrays.asList(jsonObject.getString("referenceId")));
                }
                else if (type.equalsIgnoreCase(IApprovalService.TYPE_TEXT)) {
                    notification.setAction(Action.TEXT_ACCEPTED);
                    notification.setReferenceIds(Arrays.asList(jsonObject.getString("referenceId")));
                    //todo get the activity title for nicer presentation
                }
            }
            else {

                //todo not very good code quality here, rewrite if possible

                String rejectionObject = jsonObject.getString("value"); //this represents either a url/link or the text

                if (type.equalsIgnoreCase(IApprovalService.TYPE_FILE)) {

                    storageService.delete(storageService.deUrlize(value));

                    //here we will delete the file so we will just tell him the reference
/*
                    rejectionObject=" media file for ";
                    if (referenceType.equalsIgnoreCase(Nouns.USER)) rejectionObject+="your profile";
                    else if (referenceType.equalsIgnoreCase(Nouns.ACTIVITY)) {
                        rejectionObject+="http://www.revib.es/app.html#viewActivity/"+jsonObject.getString("referenceId");
                    }

                    */
                }

                String emailTemplate = new CodingUtilities().getResourceAsString("/i18n/templates/"+ currentUser.getPreferences().get("language")+"/rejected.html");

/*
            IEmailService emailService= EmailServiceFactory.getEmailService();
            emailService.sendMail(Globals.NO_REPLY_EMAIL,userDAO.getEmail(jsonObject.getString("user")),
                    Translator.getString("rejection_notice",currentLocale),emailTemplate);
*/

            }

            brokerService.sendNotification(currentUser,notification);
            kvStore.delete(key);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
