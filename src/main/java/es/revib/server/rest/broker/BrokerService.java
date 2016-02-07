package es.revib.server.rest.broker;

import es.revib.server.rest.email.IEmailService;
import es.revib.server.rest.entities.Info;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.kv.IKVStore;
import es.revib.server.rest.messaging.Message;
import es.revib.server.rest.translation.TranslatorService;
import es.revib.server.rest.util.CodingUtilities;
import es.revib.server.rest.util.Globals;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Locale;

/**
 * Class that contains all logic for notifying the user either through a direct broker if he is online or emailing/smsing him
 * in this class can be included logic for choosing the broker for each case (rabbit for web or push or cloud messaging
 * for android)
 *
 */
public class BrokerService {

    @Inject
    IKVStore kvStore;

    @Inject
    IEmailService emailService;

    @Inject
    IBroker broker;

    public void sendNotification(User user,Info notification) {

        if (user.isOnline()) {
                broker.sendNotification(user, notification);
            //think what to do about other devices here in the future
        }
        else {
            if (user.getPreferences().containsKey("email") && (Boolean)user.getPreferences().get("email")) {
                String htmlTemplate = new CodingUtilities().getResourceAsString("/i18n/templates/"+user.getPreferences().get("language")+"/new_notification.html");
                htmlTemplate=htmlTemplate.replace("INSERT USER HERE",user.getFirstName());
                Locale userLocale=new Locale(user.getPreferences().get("language").toString(),user.getPreferences().get("country").toString());
                emailService.sendMail(Globals.NO_REPLY_EMAIL, user.getEmail(), TranslatorService.getString("new_notification", userLocale), htmlTemplate);
            }
        }
    }

    public void sendMessage(User user,Message message) {

        if (user.isOnline()) {
                broker.sendMessage(user, message);
            }
        else {
            if (user.getPreferences().containsKey("email") && (Boolean)user.getPreferences().get("email")) {

                String htmlTemplate = new CodingUtilities().getResourceAsString("/i18n/templates/"+ user.getPreferences().get("language")+"/new_message_received.html");
                htmlTemplate=htmlTemplate.replace("INSERT USER HERE",user.getFirstName());
                htmlTemplate=htmlTemplate.replace("INSERT SENDER HERE",message.getSender());
                htmlTemplate=htmlTemplate.replace("INSERT MESSAGE HERE",message.getBody());
                Locale userLocale=new Locale(user.getPreferences().get("language").toString(),user.getPreferences().get("country").toString());
                emailService.sendMail(Globals.NO_REPLY_EMAIL,user.getEmail(), TranslatorService.getString("new_message_received", userLocale), htmlTemplate);

            }
        }
    }

    public void close() {

    }

    public void sendStream(User user,Info stream) {

        if (user.isOnline()) {
                broker.sendStream(user, stream);
            }

    }

}