package es.revib.server.rest.messaging;

import com.google.gson.Gson;

import es.revib.server.rest.entities.User;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class Message {

    public Message() {
    }

    private String id;
    @NotNull @NotEmpty private String chatId;
    @NotNull @NotEmpty private String body;
    @NotNull @NotEmpty private String sender;
    private long timestamp=new Date().getTime();

    public Message(String chatId, String body, String sender) {
        this.chatId = chatId;
        this.body = body;
        this.sender = sender;
    }

    /**
     * returns a string that is compatible with our notifications system
     * @return
     */
    public JSONObject toJSON(boolean strip) {
        JSONObject jsonObject= null;
        try {
            jsonObject = new JSONObject(new Gson().toJson(this));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (strip) {
        }
        return jsonObject;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(final String chatId) {
        this.chatId = chatId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(final String sender) {
        this.sender = sender;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

}
