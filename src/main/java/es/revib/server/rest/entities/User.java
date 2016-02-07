package es.revib.server.rest.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.revib.server.rest.util.AccessType;
import es.revib.server.rest.util.Globals;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class User implements IEntity{

    @Override
    public List<String> listUnserializableFields() {
        return Arrays.asList("ratings");
    }

    private String id;

    private String flags="";

    //mandatory fields
    @NotNull @NotEmpty @Email private String email;
    private String username;
    @NotNull
    @Pattern(regexp = "[A-Za-z]*")
    @Size(min = 2, max = 25) private String firstName;
    @NotNull
    @Pattern(regexp = "[A-Za-z]*")
    @Size(min = 2, max = 25) private String lastName;
    private Integer registrationDate;

    private String facebookId;

    private List<Rating> ratings=new ArrayList<>();

    private Map<String,Object> preferences;

    private Integer messages=0;
    private Boolean online=false;

    @Size(max=100) private String address;

    private List<Map> notifications;
    @NotNull private Map password;

    private Boolean emailVerified=false;

    @Size(max=100) private String phone;
    private Integer birthdate; //epoch days
    @Size(max=2500) private String description;
    private List<String> tags;
    private String mainImage="http://"+ Globals.S3_BUCKET+".s3.amazonaws.com/"+Globals.S3_FOLDER+"/user.png"; //default image icon
    private List<String> images;
    private Integer vibes=0;

    private Double lat;
    private Double lon;

    private Long lastLogin;

    /**
     * this function looks at the user preferences and removes sensitive data according to the viewers level. it is not fully implemented.
     */
    public User strip(AccessType accessType) {

        //todo access control

        if (accessType!=AccessType.OWNER) {
            if (preferences.containsKey("email_hidden") && (Boolean) preferences.get("email_hidden")) {
                email = null;
            }
            if (preferences.containsKey("address_hidden") && (Boolean) preferences.get("address_hidden")) {
                email = null;
            }
            if (preferences.containsKey("phone_hidden") && (Boolean) preferences.get("phone_hidden")) {
                email = null;
            }
            password=null;
            notifications = null;
            preferences=null;
        }

        return this;
    }

    /**
     * produces a minimal user that is embedded in other objects such as events, ratings etc
     */
    public User stripMinimal() {
        strip(AccessType.VIEWER);
        images=null;
        lat=null;
        lon=null;
        emailVerified=null;
        facebookId=null;
        messages=null;
        registrationDate=null;
        return this;
    }

    /**
     * hack method to return a JSON string representation of this object
     * @return
     */
    public String toMinimalJSONString() {

        JSONObject jsonObject=new JSONObject();
        try {
            jsonObject.append("id",id);
            jsonObject.append("username",username);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();

    }
    /**
     * this function sets the username according to whichever strategy we have at the moment such as first Name + first letter from last Name etc
     */
    public String buildUsername() {
        username=firstName+" "+lastName;
        return username;
    }

    public int hashCode() {

        HashCodeBuilder hc=new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
                // if deriving: appendSuper(super.hashCode()).
                append(firstName).append(lastName);
        if (id !=null && !id.isEmpty()) {
            hc.append(id);
        }
        return hc.toHashCode();
    }


    public User() {
       initPreferences();
    }

    public void initPreferences() {
        this.preferences=new HashMap<>();
        //provide some initial values here
        this.getPreferences().put("language","en");
        this.getPreferences().put("country","US");
        this.getPreferences().put("email",true);
        this.getPreferences().put("locale","en_US");
    }

    public boolean equals(Object obj) {

        if (!(obj instanceof User))
            return false;
        if (obj == this)
            return true;

        User rhs = (User) obj;

        return new EqualsBuilder().append(id, rhs.id).isEquals();
    }



    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName.toLowerCase();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName.toLowerCase();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }


    public String getPhone() {
        return phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public Integer getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(final Integer birthdate) {
        this.birthdate = birthdate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(final String mainImage) {
        this.mainImage = mainImage;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(final List<String> images) {
        this.images = images;
    }

    public Integer getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(final Integer registrationDate) {
        this.registrationDate = registrationDate;
    }

    public List<Map> getNotifications() {
        return notifications;
    }

    public void setNotifications(final List<Map> notifications) {
        this.notifications = notifications;
    }

    public Integer getMessages() {
        return messages;
    }

    public void setMessages(Integer messages) {
        this.messages = messages;
    }

    public Boolean isOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
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

    public Integer getVibes() {
        return vibes;
    }

    public void setVibes(Integer vibes) {
        this.vibes = vibes;
    }

    public List<Rating> getRatings() {
        return ratings;
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }

    public Map getPreferences() {
        return preferences;
    }

    public void setPreferences(Map preferences) {
        this.preferences = preferences;
    }

    public Map getPassword() {
        return password;
    }

    public void setPassword(Map password) {
        this.password = password;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public Long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }
}
