package es.revib.server.rest.entities.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.revib.server.rest.entities.*;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventRequest extends Request {

    //used in some cases for modify requests but can be null in other requests. only some endpoints ask for @Valid annotation
    @NotNull private Double lat;
    @NotNull private Double lon;
    @NotNull private String address;
    @NotNull private Long date;

    public List<String> listUnserializableFields() {
        return Arrays.asList("event","requester","activity");
    }

    private Event event;
    private Activity activity;

    private String democracyType=DICTATORSHIP; //default

    public static final String DICTATORSHIP="DICTATORSHIP";
    public static final String ABSOLUTE_MAJORITY="ABSOLUTE_MAJORITY";
    public static final String RELATIVE_MAJORITY="RELATIVE_MAJORITY";

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public EventRequest() {
    }

    private HashMap<String,Vote> votes;

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public void setStatus(String status) {
        if (status.equalsIgnoreCase(Status.VOTING)) {
            if (democracyType.equalsIgnoreCase(DICTATORSHIP)) {
                democracyType=ABSOLUTE_MAJORITY;
            }
        }
        this.status=status;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getDemocracyType() {
        return democracyType;
    }

    public void setDemocracyType(String democracyType) {
        this.democracyType = democracyType;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public HashMap<String, Vote> getVotes() {
        return votes;
    }

    public void setVotes(HashMap<String, Vote> votes) {
        this.votes = votes;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }
}
