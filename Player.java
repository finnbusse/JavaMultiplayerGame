import java.util.UUID;

/**
 * Repräsentiert einen Spieler – reine Datenklasse ohne Grafik.
 */
public class Player {
    private final UUID   id;
    private final String username;
    private double       x, y;

    public Player(UUID id, String username, double x, double y) {
        this.id       = id;
        this.username = username;
        this.x        = x;
        this.y        = y;
    }

    public UUID   getId()       { return id; }
    public String getUsername(){ return username; }
    public double getX()        { return x; }
    public double getY()        { return y; }

    /** Setzt die exakte Position (für Server-Updates). */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
