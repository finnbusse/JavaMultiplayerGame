import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    private final GameState gameState = new GameState();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    // Liste aller Handler für Broadcasts
    static final List<ClientHandler> handlers = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        new Server().start();
    }

    public void start() {
        System.out.println("Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, gameState);
                handlers.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Hält den Zustand aller Spieler */
    static class GameState {
        private final ConcurrentMap<UUID, Player> players = new ConcurrentHashMap<>();

        public void addPlayer(UUID id, String username) {
            Player p = new Player(id, username);
            players.put(id, p);
            System.out.println("Added player: " + username + " (" + id + ")");
        }

        public void updateStatus(UUID id, String status) {
            Player p = players.get(id);
            if (p != null) {
                p.setCurrentStatus(status);
                System.out.println("Updated " + p.getUsername() + " → " + status);
            }
        }

        public List<Player> getPlayers() {
            return new ArrayList<>(players.values());
        }
    }
}
