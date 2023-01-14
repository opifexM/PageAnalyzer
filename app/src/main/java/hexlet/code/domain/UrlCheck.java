package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.time.Instant;

@Entity
public class UrlCheck extends Model {
    @Id
    private long id;

    private int statusCode;

    private String title;
    private String h1;

    //TODO
//    @Lob
    private String description;

    @ManyToOne
    private Url url;

    @WhenCreated
    private Instant createdAt;

    public UrlCheck(int statusCode, String title, String h1, String description, Url url) {
        this.statusCode = statusCode;
        this.title = title;
        this.h1 = h1;
        this.description = description;
        this.url = url;
    }

    /**
     * @return id for UrlCheck entity
     */
    public long getId() {
        return id;
    }

    /**
     * @return HTTP last status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return Website title
     * */
    public String getTitle() {
        return title;
    }

    /**
     * @return Website H1 text
     */
    public String getH1() {
        return h1;
    }

    /**
     * @return Website description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Website url address
     */
    public Url getUrl() {
        return url;
    }

    /**
     * @return Time of last check
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
