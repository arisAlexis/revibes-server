package es.revib.server.rest.dao;

import es.revib.server.rest.entities.Status;
import es.revib.server.rest.util.AccessType;

public class ActivityQueryBuilder {

    private String tag;
    private String category;
    private Double lat;
    private Double lon;
    private Integer radius=5;
    private String keywords;
    private String status= Status.OPEN;
    private Integer start;
    private Integer size=20;

    public Integer getStart() {
        return start;
    }

    public ActivityQueryBuilder setStart(Integer start) {
        this.start = start;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public ActivityQueryBuilder setSize(Integer size) {
        this.size = size;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public ActivityQueryBuilder setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public ActivityQueryBuilder setCategory(String category) {
        this.category = category;
        return this;
    }

    public Double getLat() {
        return lat;
    }

    public ActivityQueryBuilder setLat(Double lat) {
        this.lat = lat;
        return this;
    }

    public Double getLon() {
        return lon;
    }

    public ActivityQueryBuilder setLon(Double lon) {
        this.lon = lon;
        return this;
    }

    public Integer getRadius() {
        return radius;
    }

    public ActivityQueryBuilder setRadius(Integer radius) {
        this.radius = radius;
        return this;
    }

    public String getKeywords() {
        return keywords;
    }

    public ActivityQueryBuilder setKeywords(String keywords) {
        this.keywords = keywords;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public ActivityQueryBuilder setStatus(String status) {
        this.status = status;
        return this;
    }

}
