import sas.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Game {
    // basic server configuration für client
    private static final String SERVER_IP   = "95.119.141.178"; // lokale ip
    private static final int    SERVER_PORT = 12345;

    // config für eigenen player
    private final UUID   playerId = UUID.randomUUID();
    private final String username;

    // socket und so netzwerk
    private Socket        socket;
    private PrintWriter   out;
    private BufferedReader in;

    private View view;

    // referenzieren der model und shape objektee
    private final ConcurrentMap<UUID, Player> players = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Rectangle> shapes = new ConcurrentHashMap<>();

    public Game(String username) {
        this.username = username;

        view = new View(1024, 768, "tolles online game");

        connectToServer();

        double startX = Tools.randomNumber(0, 1000);
        double startY = Tools.randomNumber(0, 750);
        sendPosition(startX, startY);

        players.put(playerId, new Player(playerId, username, startX, startY));
        Rectangle ownFigure = new Rectangle(startX, startY, 50, 50);
        shapes.put(playerId, ownFigure);

        // thread abkoppeln um parallel die aktuellen player positionen zu empfaangenn
        new Thread(this::receiveUpdates).start();



        while (true) {
            startGame();
            try { Thread.sleep(16); } catch (InterruptedException ignored) {}
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out    = new PrintWriter(socket.getOutputStream(), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if ("ENTER_REGISTER".equals(in.readLine())) {
                out.println("REGISTER:" + playerId + ":" + username);
            }
            System.out.println("Connected to server at " + SERVER_IP + ":" + SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendPosition(double x, double y) {
        out.println("POSITION:" + playerId + ":" + x + "," + y);
    }

    private void receiveUpdates() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if ("STATE_UPDATE".equals(line)) {
                    // Erstelle eine Liste der aktuellen Spieler-IDs vom Server
                    Set<UUID> receivedPlayerIds = new HashSet<>();
                    
                    while (!(line = in.readLine()).equals("END_UPDATE")) {
                        // formaat: uuid|username|x,y (-> als double)
                        String[] parts = line.split("\\|", 3);
                        UUID id = UUID.fromString(parts[0]);
                        receivedPlayerIds.add(id);

                        // ursprüngliches geruckle bei online-spiel entfernen: synchronisation des eigenen spielers hier nicht notwendig
                        if (!playerId.equals(id)) {
                            String name = parts[1];
                            String[] xy = parts[2].split(",", 2);
                            double x = Double.parseDouble(xy[0]);
                            double y = Double.parseDouble(xy[1]);

                            // spieler modell aktualisieren und in ConcurrentMap rein packen
                            Player p = players.get(id);
                            if (p == null) {
                                p = new Player(id, name, x, y);
                                players.put(id, p);
                                // neues rectangle je spieler -> die anderen spieler werden lokal bei jedem gerendert und NICHT(!!) über player klasse
                                Rectangle remoteFigure = new Rectangle(x, y, 50, 50);
                                shapes.put(id, remoteFigure);
                            } else {
                                p.setPosition(x, y);
                                shapes.get(id).moveTo(x, y);
                            }
                        }
                    }
                    
                    // Entferne Spieler, die nicht mehr in der Aktualisierung enthalten sind
                    Set<UUID> toRemove = new HashSet<>();
                    for (UUID id : players.keySet()) {
                        if (!receivedPlayerIds.contains(id) && !id.equals(playerId)) {
                            toRemove.add(id);
                        }
                    }
                    
                    for (UUID id : toRemove) {
                        players.remove(id);
                        Rectangle shape = shapes.remove(id);
                        if (shape != null) {
                            view.remove(shape);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Verbindung zum Server verloren");
        }
    }


    // hier spiel intigrieren!

    private void startGame() {
        double dx = 0, dy = 0;
        if (view.keyUpPressed()) dy = -5;
        if (view.keyDownPressed()) dy = +5;
        if (view.keyLeftPressed()) dx = -5;
        if (view.keyRightPressed()) dx = +5;
        if (view.keyPressed(' ')) dx = +10;

        if (dx != 0 || dy != 0) {
            Player ownPlayer = players.get(playerId);
            ownPlayer.setPosition(ownPlayer.getX() + dx, ownPlayer.getY() + dy);
            shapes.get(playerId).move(dx, dy);
            sendPosition(ownPlayer.getX(), ownPlayer.getY());
        }
    }





    // nur für intellij benötigt, um game klasse als programm auszuführen

    public static void main(String[] args) {
        new Game(args.length > 0 ? args[0] : "Player");
    }
}