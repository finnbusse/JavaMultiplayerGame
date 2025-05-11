import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repräsentiert einen Spieler mit ID, Namen und beliebig erweiterbaren Zusatzdaten.
 */
public class Player {
    private final UUID id;
    private final String username;
    private String currentStatus = "";
    private final Map<String, Object> extra = new ConcurrentHashMap<>();

    public Player(UUID id, String username) {
        this.id = id;
        this.username = username;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String status) {
        this.currentStatus = status;
    }

    /** Beliebige Zusatzdaten abspeichern */
    public void putExtra(String key, Object value) {
        extra.put(key, value);
    }

    /** Zusatzdaten auslesen */
    public Object getExtra(String key) {
        return extra.get(key);
    }
}
