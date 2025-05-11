import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Server.GameState gameState;
    private final UUID playerId = UUID.randomUUID();
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, Server.GameState gameState) {
        this.clientSocket = socket;
        this.gameState   = gameState;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // 1) Login: Username abfragen
            out.println("ENTER_USERNAME");
            String username = in.readLine();
            gameState.addPlayer(playerId, username);

            // 2) Sofort erster Broadcast
            broadcastState();

            // 3) Fortlaufend Status-Updates verarbeiten
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("STATUS:")) {
                    String status = message.substring("STATUS:".length());
                    gameState.updateStatus(playerId, status);
                    broadcastState();
                } else {
                    System.out.println("Unknown message: " + message);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); }
            catch (IOException ignored) {}
        }
    }

    /** Sendet an alle verbundenen Clients den vollständigen Lobby-Status */
    private void broadcastState() {
        List<Server.Player> players = gameState.getPlayers();
        synchronized (Server.handlers) {
            for (ClientHandler handler : Server.handlers) {
                handler.out.println("STATE_UPDATE");
                for (Server.Player p : players) {
                    handler.out.println(p.getUsername() + "|" + p.getCurrentStatus());
                }
                handler.out.println("END_UPDATE");
            }
        }
        System.out.println("Broadcasted state to all clients");
    }
}
