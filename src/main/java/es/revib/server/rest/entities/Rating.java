package es.revib.server.rest.entities;

import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

/**
 * This class gets stored in the rating edge
 */
public class Rating {

    @NotNull
    @Range(min = -1,max = 1)private Integer rating;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rating rating = (Rating) o;

        return !(id != null ? !id.equals(rating.id) : rating.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    private String id;

    private User ratingUser;
    private User receivingUser;
    private String review;
    private Event event;

    public Rating() {

    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {

        this.rating = rating;
    }

    public User getRatingUser() {
        return ratingUser;
    }

    public void setRatingUser(User ratingUser) {
        this.ratingUser = ratingUser;
    }

    public User getReceivingUser() {
        return receivingUser;
    }

    public void setReceivingUser(User receivingUser) {
        this.receivingUser = receivingUser;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
