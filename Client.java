import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private static final String SERVER_IP   = "192.168.178.33";
    private static final int    SERVER_PORT = 12345;



    // klasse nur für test und debug zwecke

    private String username;
    private String currentStatus = "";
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Hält den Status jedes Spielers (username → currentStatus)
    private final Map<String, String> playerStatuses = new HashMap<>();
    // Hält die Position jedes Spielers (username → [pX, pY])
    private final Map<String, double[]> playerPositions = new HashMap<>();

    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {

            socket = new Socket(SERVER_IP, SERVER_PORT);
            out    = new PrintWriter(socket.getOutputStream(), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            if ("ENTER_USERNAME".equals(in.readLine())) {
                System.out.print("Enter username: ");
                username = scanner.nextLine();
                out.println(username);
            }

            new Thread(this::listenServer).start();

            printState();

            while (true) {
                String status = scanner.nextLine();
                currentStatus = status;
                out.println("STATUS:" + status);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void listenServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if ("STATE_UPDATE".equals(line)) {
                    playerStatuses.clear();
                    playerPositions.clear();

                    while (!(line = in.readLine()).equals("END_UPDATE")) {
                        // Beispiel-Eintrag: "Olli|ready|50.0,100.0"
                        String[] parts = line.split("\\|", 3);
                        String user   = parts[0];               // Spielername
                        String status = parts[1];               // currentStatus
                        String coord  = parts.length > 2
                                ? parts[2]
                                : "0.0,0.0";        // pX,pY-String
                        String[] xy   = coord.split(",", 2);
                        double pX = Double.parseDouble(xy[0]);  // pX als double
                        double pY = Double.parseDouble(xy[1]);  // pY

                        // sichern in die maps
                        playerStatuses.put(user, status);
                        playerPositions.put(user, new double[]{pX, pY});

                        // wenn es own entry ist: update currentStatus
                        if (user.equals(username)) {
                            currentStatus = status;
                        }
                    }

                    printState();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ANSI-Escape-Code: Konsole leeren
    private void clearConsole() {
        System.out.print("\u001b[H\u001b[2J");
        System.out.flush();
    }


    private void printState() {
        clearConsole();
        System.out.println("-- DEBUGGING VERSION OF GAME SERVER --");
        System.out.println("Logged in as: " + username);
        System.out.println("Your currentState String: " + currentStatus);

        if (playerStatuses.size() <= 1) {
            System.out.println("You are the only one in this lobby!");
        } else {
            for (Map.Entry<String, String> entry : playerStatuses.entrySet()) {
                String user = entry.getKey();
                if (user.equals(username)) continue;

                String status = entry.getValue();
                double[] pos  = playerPositions.get(user);
                double pX = pos[0], pY = pos[1];

                System.out.println(
                        user
                                + " has currentState: " + status
                                + " | pX=" + pX
                                + " | pY=" + pY
                );
            }
        }

        System.out.println();
        System.out.print("Set new currentState to: ");
    }

    public static void main(String[] args) {
        new Client().start();
    }
}
