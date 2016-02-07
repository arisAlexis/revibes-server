package es.revib.server.rest.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.revib.server.rest.util.AccessType;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.tika.language.LanguageIdentifier;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Activity implements IEntity{

    static class CATEGORIES {
        public static String KNOWLEDGE="KNOWLEDGE";
        public static String LABOUR="LABOUR";
        public static String OTHER="OTHER";
    }

    @Override
    public List<String> listUnserializableFields() {
        return Arrays.asList("events","owner");
    }

    @Override
    public Activity strip(AccessType accessType) {
        //todo
        return this;
    }

    private String language="en";

    public Activity() {
    }

    private String category;

    private String id;

    private Double lat;
    private Double lon;

    private User owner; //caching

    private Boolean isNew=true;

    @NotNull @NotEmpty private String type;
    @NotNull @NotEmpty @Size(min = 3, max = 50)private String title;

    private Long updateDate=new Date().getTime();

    @NotNull private String description;

    private String mainImage;
    private List<String> images=new ArrayList<>();

    private String status= Status.PENDING; //default
    private List<String> tags= new ArrayList<>();

    private Integer views=0;
    private List<Event> events=new ArrayList<>();

    public int hashCode() {

        HashCodeBuilder hc=new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
                // if deriving: appendSuper(super.hashCode()).
                append(title).append(tags).append(type);

        if (id!=null && !id.isEmpty()) {
            hc.append(id);
        }
        return hc.toHashCode();
    }

    public boolean equals(Object obj) {

        if (!(obj instanceof Activity))
            return false;
        if (obj == this)
            return true;

        Activity rhs = (Activity) obj;

        return new EqualsBuilder().
                // if deriving: appendSuper(super.equalsIgnoreCase(obj)).
                        append(title, rhs.title)
                .append(tags, rhs.tags).append(type,rhs.type).append(id,rhs.id).isEquals();
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(String mainImage) {
        this.mainImage = mainImage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(final Integer views) {
        this.views = views;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public Long getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Long updateDate) {
        this.updateDate = updateDate;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

}
