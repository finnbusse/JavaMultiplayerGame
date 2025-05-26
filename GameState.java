import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.awt.Color;

public class GameState {

    private final ConcurrentMap<UUID, Player> players = new ConcurrentHashMap<>();

    private final ArrayList<Block> blocks = new ArrayList<>();

    //private final Ball ball;


    public GameState() {
        //ball = new Ball(155,155,50);

    }

    // game state verwaltet die einzelnen Player objekte, zugeordnet zur UUID in einer ConcurrentMap


    public void addPlayer(UUID id, String username, double x, double y) {
        players.put(id, new Player(id, username, x, y));
        System.out.println("Added " + username + " (" + id + ")");
    }

    public void updatePosition(UUID id, double x, double y) {
        Player p = players.get(id);
        if (p != null) {
            p.setPosition(x, y);
        }
    }

    public void removePlayer(UUID id) {
        Player removed = players.remove(id);
        if (removed != null) {
            System.out.println("Removed " + removed.getUsername() + " (" + id + ")");
        }
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players.values());
    }

    public List<Block> getBlocks() {
        return new ArrayList<>(blocks);
    }
}