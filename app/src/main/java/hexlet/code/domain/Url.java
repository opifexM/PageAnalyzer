package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.time.Instant;
import java.util.List;

@Entity
public class Url extends Model {
    @Id
    private long id;

    private String name;

    @WhenCreated
    private Instant createdAt;

    // один сайт к многим отчетам
    @OneToMany
    private List<UrlCheck> urlChecks;

    public Url(String name) {
        this.name = name;
    }

    /**
     * @return id for Url entity
     */
    public long getId() {
        return id;
    }

    /**
     * @return name (host:port) of website
     */
    public String getName() {
        return name;
    }

    /**
     * @return Time of last check
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * @return List of checks
     */
    public List<UrlCheck> getUrlChecks() {
        return urlChecks;
    }

    /**
     * @return Time of last check from List of checks
     */
    public Instant getLastCreatedAt() {
        if (urlChecks.isEmpty()) {
            return null;
        }
        if (urlChecks.size() == 1) {
            return urlChecks.get(0).getCreatedAt();
        }
        return urlChecks.get(urlChecks.size() - 1).getCreatedAt();
    }

    /**
     * @return HTML status code of last check
     */
    public int getLastStatusCode() {
        if (urlChecks.isEmpty()) {
            return 0;
        }
        if (urlChecks.size() == 1) {
            return urlChecks.get(0).getStatusCode();
        }
        return urlChecks.get(urlChecks.size() - 1).getStatusCode();
    }

}
