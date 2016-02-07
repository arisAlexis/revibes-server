package es.revib.server.rest.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.revib.server.rest.util.AccessType;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Info implements IEntity {

    public final static String ORIGIN_RETWEET="Retweet";
    public final static String ORIGIN_LIKE="Like";
    public final static String ORIGIN_COMMENT="Comment";
    public final static String ORIGIN_STREAM="Stream";
    public final static String ORIGIN_GEOLOCATION="Geolocation";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Info info = (Info) o;

        if (!timestamp.equals(info.timestamp)) return false;
        return id.equals(info.id);

    }

    @Override
    public int hashCode() {
        int result = timestamp.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    private Long timestamp=new Date().getTime();
    private String origin; //stream type (retweet,like for example)
    @NotEmpty private List<String> sourceIds;
    private List<String> sourceDisplayNames;
    @NotEmpty private List<String> targetIds;
    private List<String> targetDisplayNames;
    private List<String> referenceDisplayNames;
    private List<String> sourceImages;
    private List<String> targetImages;
    private List<String> referenceImages;
    private String referenceType;
    @NotEmpty private String action;
    @NotEmpty private String sourceType;
    @NotEmpty private String targetType;
    private String payload;
    private String id=UUID.randomUUID().toString(); //if we actually create a Vertex then it's not needed. if it gets cached as JSON then we do.
    private Double lon;
    private Double lat;

    private List<Comment> comments;
    private List<User> likers;

    @Override
    public List<String> listUnserializableFields() {
        return Arrays.asList("origin","comments");
    }

    @Override
    public IEntity strip(AccessType accessType) {
        return null;
    }

    public List<String> getSourceImages() {
        return sourceImages;
    }

    public void setSourceImages(List<String> sourceImages) {
        this.sourceImages = sourceImages;
    }

    public List<String> getTargetImages() {
        return targetImages;
    }

    public void setTargetImages(List<String> targetImages) {
        this.targetImages = targetImages;
    }

    public List<String> getSourceIds() {
        return sourceIds;
    }

    public void setSourceIds(List<String> sourceIds) {
        this.sourceIds = sourceIds;
    }

    public List<String> getSourceDisplayNames() {
        return sourceDisplayNames;
    }

    public void setSourceDisplayNames(List<String> sourceDisplayNames) {
        this.sourceDisplayNames = sourceDisplayNames;
    }

    public List<String> getTargetIds() {
        return targetIds;
    }

    public void setTargetIds(List<String> targetIds) {
        this.targetIds = targetIds;
    }

    public List<String> getTargetDisplayNames() {
        return targetDisplayNames;
    }

    public void setTargetDisplayNames(List<String> targetDisplayNames) {
        this.targetDisplayNames = targetDisplayNames;
    }

    public List<String> getReferenceDisplayNames() {
        return referenceDisplayNames;
    }

    public void setReferenceDisplayNames(List<String> referenceDisplayNames) {
        this.referenceDisplayNames = referenceDisplayNames;
    }

    public List<String> getReferenceIds() {
        return referenceIds;
    }

    public void setReferenceIds(List<String> referenceIds) {
        this.referenceIds = referenceIds;
    }

    private List<String> referenceIds;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Info() {
    }

    public void setId(String id) {
        this.id=id;
    }

    public String getId() {
        return this.id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public List<String> getReferenceImages() {
        return referenceImages;
    }

    public void setReferenceImages(List<String> referenceImages) {
        this.referenceImages = referenceImages;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<User> getLikers() {
        return likers;
    }

    public void setLikers(List<User> likers) {
        this.likers = likers;
    }
}
