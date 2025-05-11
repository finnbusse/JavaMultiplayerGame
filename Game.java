import sas.*;                // Shapes & Sprites: View, Rectangle, Tools :contentReference[oaicite:1]{index=1}
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Game {
    // Server-Konfiguration
    private static final String SERVER_IP   = "0.0.0.0"; // bindet an alle Interfaces
    private static final int    SERVER_PORT = 12345;

    // Lokaler Spieler
    private final UUID   playerId = UUID.randomUUID();
    private final String username;

    // Netzwerk
    private Socket        socket;
    private PrintWriter   out;
    private BufferedReader in;

    private View view;

    // Model & Shape-Objekte
    private final ConcurrentMap<UUID, Player> players = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Rectangle> shapes = new ConcurrentHashMap<>();

    public Game(String username) {
        this.username = username;

        // 1) Fenster öffnen (einzige View-Instanz)
        view = new View(1024, 768, "tolles online game");

        // 2) Server verbinden & Registrierung
        connectToServer();

        // 3) Spawn-Position zufällig ermitteln und senden
        double startX = Tools.randomNumber(0, 1000);
        double startY = Tools.randomNumber(0, 750);
        sendPosition(startX, startY);

        // 4) Eigenes Model & Shape anlegen
        players.put(playerId, new Player(playerId, username, startX, startY));
        Rectangle ownFigure = new Rectangle(startX, startY, 50, 50);
        shapes.put(playerId, ownFigure);

        // 5) Thread: Server-Updates empfangen
        new Thread(this::receiveUpdates).start();

        // 6) Game-Loop: Input verarbeiten
        while (true) {
            handleInput();
            // kein explizites render nötig, Shapes zeichnen sich automatisch :contentReference[oaicite:2]{index=2}
            try { Thread.sleep(16); } catch (InterruptedException ignored) {}
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(getLocalIp(), SERVER_PORT);
            out    = new PrintWriter(socket.getOutputStream(), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if ("ENTER_REGISTER".equals(in.readLine())) {
                out.println("REGISTER:" + playerId + ":" + username);
            }
            System.out.println("Connected to server at " + getLocalIp() + ":" + SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLocalIp() throws SocketException {
        // erste non-loopback IPv4 zurückgeben
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || ni.isLoopback()) continue;
            for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                if (addr instanceof Inet4Address) return addr.getHostAddress();
            }
        }
        return "127.0.0.1";
    }

    private void sendPosition(double x, double y) {
        out.println("POSITION:" + playerId + ":" + x + "," + y);
    }

    private void receiveUpdates() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if ("STATE_UPDATE".equals(line)) {
                    while (!(line = in.readLine()).equals("END_UPDATE")) {
                        // foormat: uuid|username|x,y (-> als duble)
                        String[] parts = line.split("\\|", 3);
                        UUID   id = UUID.fromString(parts[0]);
                        String name = parts[1];
                        String[] xy = parts[2].split(",", 2);
                        double x = Double.parseDouble(xy[0]);
                        double y = Double.parseDouble(xy[1]);

                        // Model aktualisieren
                        Player p = players.get(id);
                        if (p == null) {
                            p = new Player(id, name, x, y);
                            players.put(id, p);
                            // neues Shape anlegen
                            Rectangle remoteFigure = new Rectangle(x, y, 50, 50);
                            shapes.put(id, remoteFigure);
                        } else {
                            p.setPosition(x, y);
                            shapes.get(id).moveTo(x, y);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleInput() {
        double dx = 0, dy = 0;
        if (view.keyUpPressed())    dy = -5;
        if (view.keyDownPressed())  dy = +5;
        if (view.keyLeftPressed())  dx = -5;
        if (view.keyRightPressed()) dx = +5;

        if (dx != 0 || dy != 0) {
            Player ownPlayer = players.get(playerId);
            ownPlayer.setPosition(ownPlayer.getX() + dx, ownPlayer.getY() + dy);
            shapes.get(playerId).move(dx, dy);
            sendPosition(ownPlayer.getX(), ownPlayer.getY());
        }
    }

    public static void main(String[] args) {
        new Game(args.length > 0 ? args[0] : "Player");
    }
}
