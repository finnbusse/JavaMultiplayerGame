import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    static final List<ClientHandler> handlers =
            Collections.synchronizedList(new ArrayList<>());
    private static final int PORT = 12345;
    private final GameState state = new GameState();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.printLocalIPs();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // lokale ipv4s finden und in die console printen -> für client zum verbindenn
    private void printLocalIPs() throws SocketException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        while (nets.hasMoreElements()) {
            NetworkInterface ni = nets.nextElement();
            if (ni.isLoopback() || !ni.isUp()) continue;
            for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                if (addr instanceof Inet4Address) {
                    System.out.println("Listening on "
                            + addr.getHostAddress() + ":" + PORT);
                }
            }
        }
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Started, waiting for clients...");
            while (true) {
                Socket client = serverSocket.accept();
                ClientHandler h = new ClientHandler(client, state);
                handlers.add(h);
                pool.execute(h);
            }
        }
    }
}
