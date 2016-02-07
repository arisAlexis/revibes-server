package es.revib.server.rest.entities;

import javax.validation.constraints.NotNull;

public class Vote {

    @NotNull private String action;
    private String message;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
