// File: Client.java

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    // Server-Adresse und Port
    private static final String SERVER_IP   = "127.0.0.1";
    private static final int    SERVER_PORT = 12345;

    private String username;
    private String currentStatus = "";
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // *** Neu: Hält den Status jedes Spielers (username → currentStatus) ***
    private final Map<String, String> playerStatuses = new HashMap<>();
    // *** Neu: Hält die Position jedes Spielers (username → [pX, pY]) ***
    private final Map<String, double[]> playerPositions = new HashMap<>();

    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            // 1) Verbindung aufbauen
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out    = new PrintWriter(socket.getOutputStream(), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 2) Login: Username anfordern
            if ("ENTER_USERNAME".equals(in.readLine())) {
                System.out.print("Enter username: ");
                username = scanner.nextLine();
                out.println(username);
            }

            // 3) Thread, der Updates vom Server liest und Maps füllt
            new Thread(this::listenServer).start();

            // 4) Erste Anzeige (noch ohne echte Daten)
            printState();

            // 5) Eingabe-Schleife: neuen Status senden
            while (true) {
                String status = scanner.nextLine();
                currentStatus = status;
                out.println("STATUS:" + status);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Liest STATE_UPDATE-Blöcke vom Server und pflegt:
     *   • playerStatuses  (username → currentStatus)
     *   • playerPositions (username → [pX, pY])
     *
     * Ablauf:
     * 1) auf "STATE_UPDATE" warten
     * 2) Maps leeren
     * 3) Einträge lesen bis "END_UPDATE"
     * 4) Für jeden Eintrag:
     *      • Auftrennen in username, status, coord (pX,pY)
     *      • coord parsen in double pX, pY
     *      • Einträge in Maps speichern
     *      • Bei eigenem username: currentStatus aktualisieren
     * 5) Nach dem Einlesen Anzeige neu zeichnen
     */
    private void listenServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if ("STATE_UPDATE".equals(line)) {
                    // 2) Maps zurücksetzen
                    playerStatuses.clear();
                    playerPositions.clear();

                    // 3) Lies alle Einträge im Block
                    while (!(line = in.readLine()).equals("END_UPDATE")) {
                        // Beispiel-Eintrag: "Alice|ready|50.0,100.0"
                        String[] parts = line.split("\\|", 3);
                        String user   = parts[0];               // Spielername
                        String status = parts[1];               // currentStatus
                        String coord  = parts.length > 2
                                ? parts[2]
                                : "0.0,0.0";        // pX,pY-String
                        String[] xy   = coord.split(",", 2);
                        double pX = Double.parseDouble(xy[0]);  // X-Koordinate
                        double pY = Double.parseDouble(xy[1]);  // Y-Koordinate

                        // 4a) Speichere in die Maps
                        playerStatuses.put(user, status);
                        playerPositions.put(user, new double[]{pX, pY});

                        // 4b) Wenn es unsere eigene entry ist, update currentStatus
                        if (user.equals(username)) {
                            currentStatus = status;
                        }
                    }

                    // 5) Anzeige aller Spieler neu zeichnen
                    printState();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** ANSI-Escape-Code: Konsole leeren */
    private void clearConsole() {
        System.out.print("\u001b[H\u001b[2J");
        System.out.flush();
    }

    /**
     * Gibt Header, eigenen Status und alle anderen Spieler mit Status & Position aus.
     * Nutzt die Maps, um pX und pY als doubles zu halten.
     */
    private void printState() {
        clearConsole();
        System.out.println("-- DEBUGGING VERSION OF GAME SERVER --");
        System.out.println("Logged in as: " + username);
        System.out.println("Your currentState String: " + currentStatus);

        // Prüfen, ob noch andere Spieler da sind
        if (playerStatuses.size() <= 1) {
            System.out.println("You are the only one in this lobby!");
        } else {
            // Für jeden Eintrag in playerStatuses
            for (Map.Entry<String, String> entry : playerStatuses.entrySet()) {
                String user = entry.getKey();
                // eigenen Eintrag überspringen
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
