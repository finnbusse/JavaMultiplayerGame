import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    private final GameState gameState = new GameState();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    public static final List<ClientHandler> handlers =
            Collections.synchronizedList(new ArrayList<>());

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket, gameState);
            handlers.add(handler);
            pool.execute(handler);
        }
    }

    public static void main(String[] args) {
        try {
            new Server().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Verwaltung aller Spieler und ihres Status */
    static class GameState {
        private final ConcurrentMap<UUID, Player> players = new ConcurrentHashMap<>();

        public void addPlayer(UUID id, String username) {
            Player p = new Player(id, username);
            players.put(id, p);
            System.out.println("Added player " + username + " (" + id + ")");
        }

        public void updateStatus(UUID id, String status) {
            Player p = players.get(id);
            if (p != null) {
                p.setCurrentStatus(status);
                System.out.println("Updated status for " + p.getUsername() + ": " + status);
            }
        }

        public List<Player> getPlayers() {
            return new ArrayList<>(players.values());
        }
    }

//    /** Repräsentiert einen Spieler; beliebig erweiterbar */
    static class Player {
        private final UUID id;
        private final String username;
        private String currentStatus = "";

        public Player(UUID id, String username) {
            this.id = id;
            this.username = username;
        }

        public UUID getId() { return id; }
        public String getUsername() { return username; }
        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String s) { this.currentStatus = s; }
    }
}
