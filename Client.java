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

    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            // Verbindung zum Server aufbauen
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out    = new PrintWriter(socket.getOutputStream(), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 1) Login
            if ("ENTER_USERNAME".equals(in.readLine())) {
                System.out.print("Enter username: ");
                username = scanner.nextLine();
                out.println(username);
            }

            // 2) Listener-Thread starten
            new Thread(this::listenServer).start();

            // 3) Erste Anzeige
            printState(Collections.emptyList());

            // 4) Eingabe-Schleife: neuer Status
            while (true) {
                String status = scanner.nextLine();
                currentStatus = status;
                out.println("STATUS:" + status);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Liest STATE_UPDATE vom Server und aktualisiert die Anzeige */
    private void listenServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if ("STATE_UPDATE".equals(line)) {
                    List<String> entries = new ArrayList<>();
                    while (!(line = in.readLine()).equals("END_UPDATE")) {
                        entries.add(line);
                    }

                    // Liste aller anderen Spieler im Format "Name has currentState: ..."
                    List<String> others = new ArrayList<>();
                    for (String entry : entries) {
                        String[] parts = entry.split("\\|", 2);
                        String user = parts[0];
                        String stat = parts.length > 1 ? parts[1] : "";
                        if (!user.equals(username)) {
                            others.add(user + " has currentState: " + stat);
                        }
                    }

                    // Konsolen-Ausgabe aktualisieren
                    printState(others);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Konsole per ANSI-Escape-Sequenz leeren */
    private void clearConsole() {
        System.out.print("\u001b[H\u001b[2J");
        System.out.flush();
    }

    /** Status aller Spieler ausgeben und Eingabe-Prompt setzen */
    private void printState(List<String> others) {
        clearConsole();
        System.out.println("-- DEBUGGING VERSION OF GAME SERVER --");
        System.out.println("Logged in as: " + username);
        System.out.println("Your currentState String: " + currentStatus);
        if (others.isEmpty()) {
            System.out.println("You are the only one in this lobby!");
        } else {
            for (String s : others) {
                System.out.println(s);
            }
        }
        System.out.println();
        System.out.print("Set new currentState to: ");
    }

    public static void main(String[] args) {
        new Client().start();
    }
}