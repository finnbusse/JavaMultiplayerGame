import java.io.*;
import java.net.*;
import java.util.*;


// client handler verarbeitet die beim server ankommenden positionen, speichert sie und gibt sie weiter
// jeweils ein clienthandler objekt wird für jeden spieler erzeugt



public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameState state;
    private UUID playerId;
    private String username;
    private PrintWriter out; // gegenstück zum Bufferedreader; schreibt/ sendet daten an bufferedreader objekte
    private BufferedReader in; // liest eingehende werte aus daten stream

    public ClientHandler(Socket socket, GameState state) {
        this.socket = socket;
        this.state  = state;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);


            // registrierung auf dem server

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
            // an random punkt im view spawnen


            double spawnX = new Random().nextDouble()*1000;
            double spawnY = new Random().nextDouble()*750;

            // spieler dem gamestate hinzufügen
            state.addPlayer(playerId, username, spawnX, spawnY);
            broadcastState(true);

            // !!wichtigstes : hier die positions updates empfangen und folglich verarbeiten
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("POSITION:")) {
                    // Format "POSITION:<uuid>:<x>,<y>"
                    String[] m = line.split(":", 3);
                    UUID   id = UUID.fromString(m[1]);
                    String[] xy = m[2].split(",",2);
                    double x = Double.parseDouble(xy[0]);
                    double y = Double.parseDouble(xy[1]);
                    state.updatePosition(id, x, y);
                    broadcastState(false);
                }
            }

        } catch (IOException e) {
            System.out.println("Connection to player " + username + " lost");
        } finally {
            try { 
                // Spieler aus dem Spiel entfernen, wenn die verbindung getrennt wurde
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
            } catch (IOException ignored){}
        }
    }


    // der aktuelle zustand wird an alle clients gesendet, um player positions zu updaten

    private void broadcastState(boolean includeBlocks) {
        List<Player> all = state.getPlayers();

        List<Block> blocks = state.getBlocks();

        synchronized (Server.handlers) {
            for (ClientHandler h : Server.handlers) {
                try {

                    // !TODO Bloecke der Landschaft senden!

                    if (includeBlocks) {
                        h.out.println("LANDSCAPE_BLOCKS");
                        for (Block b : blocks) {
                            h.out.println(
                                    b.getX() + "," + b.getY() + "," +
                                            b.getWidth() + "," + b.getHeight() + "," +
                                            b.getColor().getRed() + "," + b.getColor().getGreen() + "," + b.getColor().getBlue()
                            );
                        }
                        h.out.println("END_LANDSCAPE_BLOCKS");
                    }




                    h.out.println("STATE_UPDATE");
                    for (Player p : all) {
                        h.out.println(
                                p.getId() + "|" +
                                        p.getUsername() + "|" +
                                        p.getX() + "," + p.getY()
                        );
                    }
                    h.out.println("END_UPDATE");
                } catch (Exception e) {
                    // Ignoriere Fehler beim Senden an einzelne Clients
                }
            }
        }
    }
}