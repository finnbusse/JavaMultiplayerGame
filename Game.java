import sas.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.awt.Color;

public class Game {
    // Basic server configuration für client
    private static final String SERVER_IP = "loc01.finnbusse.de"; // lokale IP
    private static final int SERVER_PORT = 12345;

    // Config für eigenen player
    private final UUID playerId = UUID.randomUUID();
    private final String username;

    // Socket und Netzwerk
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    private final View view;

    // Referenzieren der Model und Shape Objekte
    private final ConcurrentMap<UUID, Player> players = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Rectangle> shapes = new ConcurrentHashMap<>();

    public Game(String username) {
        this.username = username;

        view = new View(1024, 768, "tolles online game");

        view.setBackgroundColor(Color.GREEN);

        connectToServer();

        double startX = Tools.randomNumber(0, 1000);
        double startY = Tools.randomNumber(0, 750);
        sendPosition(startX, startY);

        players.put(playerId, new Player(playerId, username, startX, startY));
        Rectangle ownFigure = new Rectangle(startX, startY, 50, 50);
        shapes.put(playerId, ownFigure);
        
        // Erzeuge lokale Landschaft aus den Blöcken in GameState
        GameState gameState = new GameState();
        for (Block block : gameState.getBlocks()) {
            Rectangle blockShape = new Rectangle(
                block.getX(), 
                block.getY(), 
                block.getWidth(), 
                block.getHeight(), 
                block.getColor()
            );
        }

        // Thread abkoppeln um parallel die aktuellen Player Positionen zu empfangen
        new Thread(this::receiveUpdates).start();

        // Game-Loop
        while (true) {
            startGame();
            try { Thread.sleep(16); } catch (InterruptedException ignored) {}
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if ("ENTER_REGISTER".equals(in.readLine())) {
                out.println("REGISTER:" + playerId + ":" + username);
            }
            System.out.println("Connected to server at " + SERVER_IP + ":" + SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPosition(double x, double y) {
        JsonObject positionUpdate = new JsonObject();
        positionUpdate.addProperty("type", "position");
        positionUpdate.addProperty("id", playerId.toString());
        positionUpdate.addProperty("x", x);
        positionUpdate.addProperty("y", y);
        out.println(gson.toJson(positionUpdate));
    }

    private void receiveUpdates() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("{")) {
                    try {
                        JsonObject gameState = gson.fromJson(line, JsonObject.class);
                        
                        // Erstelle eine Liste der aktuellen Spieler-IDs vom Server
                        Set<UUID> receivedPlayerIds = new HashSet<>();
                        
                        // Verarbeite die Spieler
                        if (gameState.has("players")) {
                            JsonArray playersArray = gameState.getAsJsonArray("players");
                            
                            for (int i = 0; i < playersArray.size(); i++) {
                                JsonObject playerObj = playersArray.get(i).getAsJsonObject();
                                UUID id = UUID.fromString(playerObj.get("id").getAsString());
                                receivedPlayerIds.add(id);
                                
                                // Synchronisation des eigenen Spielers hier nicht notwendig
                                if (!playerId.equals(id)) {
                                    String name = playerObj.get("username").getAsString();
                                    double x = playerObj.get("x").getAsDouble();
                                    double y = playerObj.get("y").getAsDouble();
                                    
                                    // Spieler Modell aktualisieren und in ConcurrentMap packen
                                    Player p = players.get(id);
                                    if (p == null) {
                                        p = new Player(id, name, x, y);
                                        players.put(id, p);
                                        // Neues Rechteck je Spieler -> die anderen Spieler werden lokal bei jedem gerendert und NICHT(!!) über die Player Klasse
                                        Rectangle remoteFigure = new Rectangle(x, y, 50, 50);
                                        shapes.put(id, remoteFigure);
                                        // view.add(remoteFigure); // Füge das Rechteck zum View hinzu
                                    } else {
                                        p.setPosition(x, y);
                                        shapes.get(id).moveTo(x, y);
                                    }
                                }
                            }
                        }
                        
                        // Entferne Spieler, die nicht mehr in der Aktualisierung enthalten sind (offline/ disconnect)
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
                        
                    } catch (Exception e) {
                        System.err.println("Error processing JSON: " + line);
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Connection to server lost");
            e.printStackTrace();
        }
    }

    // Hier Spiel integrieren
    private void startGame() {
        double dx = 0, dy = 0;
        if (view.keyUpPressed()) dy = -5;
        if (view.keyDownPressed()) dy = +5;
        if (view.keyLeftPressed()) dx = -5;
        if (view.keyRightPressed()) dx = +5;
        if (view.keyPressed(' ')) summonAttack(players.get(playerId).getX() + dx, players.get(playerId).getY() + dy);

        if (dx != 0 || dy != 0) {
            Player ownPlayer = players.get(playerId);
            double newX = ownPlayer.getX() + dx;
            double newY = ownPlayer.getY() + dy;

            ownPlayer.setPosition(newX, newY);
            shapes.get(playerId).moveTo(newX, newY);
            sendPosition(newX, newY);
        }
    }

    private void summonAttack(double pX, double pY) {
        Circle attack = new Circle(pX, pY, 10, Color.BLACK);
    }

    // Nur für IntelliJ benötigt, um die Game-Klasse als Programm auszuführen
    public static void main(String[] args) {
        new Game(args.length > 0 ? args[0] : "Player");
    }
}