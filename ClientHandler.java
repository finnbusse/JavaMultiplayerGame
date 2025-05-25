import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameState state;
    private UUID playerId;
    private String username;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket, GameState state) {
        this.socket = socket;
        this.state = state;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Registrierung auf dem Server
            out.println("ENTER_REGISTER");
            String reg = in.readLine();
            if (reg != null && reg.startsWith("REGISTER:")) {
                String[] parts = reg.split(":", 3);
                playerId = UUID.fromString(parts[1]);
                username = parts[2];
            } else {
                playerId = UUID.randomUUID();
                username = reg != null ? reg : "unknown";
            }

            // An random Punkt im View spawnen
            double spawnX = new Random().nextDouble() * 1000;
            double spawnY = new Random().nextDouble() * 750;

            // Spieler dem GameState hinzufügen
            state.addPlayer(playerId, username, spawnX, spawnY);
            broadcastState(true);

            // Positions-Updates empfangen und verarbeiten
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("{")) {
                    try {
                        JsonObject jsonMessage = gson.fromJson(line, JsonObject.class);
                        if (jsonMessage.has("type") && "position".equals(jsonMessage.get("type").getAsString())) {
                            UUID id = UUID.fromString(jsonMessage.get("id").getAsString());
                            double x = jsonMessage.get("x").getAsDouble();
                            double y = jsonMessage.get("y").getAsDouble();
                            state.updatePosition(id, x, y);
                            broadcastState(false);
                        }
                    } catch (Exception e) {
                        System.out.println("Fehler beim Parsen der JSON-Nachricht: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Connection to player " + username + " lost");
        } finally {
            try {
                // Spieler aus dem Spiel entfernen, wenn die Verbindung getrennt wurde
                if (playerId != null) {
                    state.removePlayer(playerId);
                }
                // Handler aus der Liste entfernen
                synchronized (Server.handlers) {
                    Server.handlers.remove(this);
                }
                // Zustand an alle anderen Clients senden
                broadcastState(false);
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void broadcastState(boolean includeBlocks) {
        List<Player> all = state.getPlayers();
        List<Block> blocks = state.getBlocks();

        synchronized (Server.handlers) {
            for (ClientHandler h : Server.handlers) {
                try {
                    JsonObject gameState = new JsonObject();
                    
                    // Spielerdaten hinzufügen
                    JsonArray playersArray = new JsonArray();
                    for (Player p : all) {
                        JsonObject playerObj = new JsonObject();
                        playerObj.addProperty("id", p.getId().toString());
                        playerObj.addProperty("username", p.getUsername());
                        playerObj.addProperty("x", p.getX());
                        playerObj.addProperty("y", p.getY());
                        playersArray.add(playerObj);
                    }
                    gameState.add("players", playersArray);
                    
                    // Blöcke hinzufügen, falls benötigt
                    if (includeBlocks) {
                        JsonArray blocksArray = new JsonArray();
                        for (Block b : blocks) {
                            JsonObject blockObj = new JsonObject();
                            blockObj.addProperty("x", b.getX());
                            blockObj.addProperty("y", b.getY());
                            blockObj.addProperty("width", b.getWidth());
                            blockObj.addProperty("height", b.getHeight());
                            
                            JsonObject colorObj = new JsonObject();
                            colorObj.addProperty("r", b.getColor().getRed());
                            colorObj.addProperty("g", b.getColor().getGreen());
                            colorObj.addProperty("b", b.getColor().getBlue());
                            blockObj.add("color", colorObj);
                            
                            blocksArray.add(blockObj);
                        }
                        gameState.add("blocks", blocksArray);
                    }
                    
                    h.out.println(gson.toJson(gameState));
                } catch (Exception e) {
                    // Ignoriere Fehler beim Senden an einzelne Clients
                }
            }
        }
    }
}