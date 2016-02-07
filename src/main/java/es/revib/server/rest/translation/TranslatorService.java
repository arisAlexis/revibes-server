package es.revib.server.rest.translation;

import es.revib.server.rest.entities.Action;
import es.revib.server.rest.entities.Info;
import es.revib.server.rest.entities.Entities;

import java.util.Locale;
import java.util.ResourceBundle;

public class TranslatorService {

    public static String getString(String s,Locale locale) {
        ResourceBundle messages=ResourceBundle.getBundle("MessagesBundle", locale);
        return messages.getString(s);
    }

    public static String buildSentence(Info infoMessage,Locale locale) {

        String s=null;
        switch (infoMessage.getAction()) {
            case Action.UPDATE_PROFILE: {
                ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle", locale);
                s = "_" + Entities.USER + " " + messages.getString("updated") + " " + messages.getString("his profile");
                break;
            }

        }

        return s;
    }

}
