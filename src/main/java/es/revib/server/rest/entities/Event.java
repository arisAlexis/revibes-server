package es.revib.server.rest.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.revib.server.rest.util.AccessType;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.validation.constraints.NotNull;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Event implements  Comparable<Event>,IEntity {


    @Override
    public List<String> listUnserializableFields() {
        return Arrays.asList("activity","participants");
    }

    @Override
    public Event strip(AccessType accessType) {

        return this;
    }

    private String id;
    private Activity activity; //parent

    private List<String> images;

    @NotNull String activityId;

    @NotNull private String address;

    private Double lat;
    private Double lon;

    private String status=Status.OPEN;

    private Long updateDate =new Date().getTime();
    //@CheckTemporal(Util.Temporal.FUTURE)

    private Integer views=0;
    private Long date;
    private List<User> participants = new ArrayList<>();

    public void setId(String id) {
        this.id=id;
    }

    public String getId() {
        return this.id;
    }

    public boolean equals(Object obj) {

        if (!(obj instanceof Event))
            return false;
        if (obj == this)
            return true;

        Event rhs = (Event) obj;

        return new EqualsBuilder()
                // if deriving: appendSuper(super.equalsIgnoreCase(obj)).
                .append(address,rhs.address).append(participants,rhs.participants).append(date,rhs.date)
                .append(id, rhs.id).isEquals();

    }

public Event() {}


    public int compareTo(final Event o) {
        if (updateDate > o.getUpdateDate()) return -1;
        if (updateDate < o.getUpdateDate()) return 1;
        return 0;
    }

    public int hashCode() {

        HashCodeBuilder hc=new HashCodeBuilder(17, 31) // two randomly chosen prime numbers
                // if deriving: appendSuper(super.hashCode()).
                .append(address).append(date).append(activityId);

        if (id!=null && !id.isEmpty()) {
            hc.append(id);
        }
        return hc.toHashCode();
    }

    public void setParticipants(final List<User> participants) {
        this.participants = participants;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public Long getDate() {
        return date;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public Long getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(final Long updateDate) {
        this.updateDate = updateDate;
    }


    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Activity getActivity() {
        return activity;
    }
    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setActivityId(String id) {
        this.activityId=id;
    }
    public String getActivityId() {return activityId;}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
