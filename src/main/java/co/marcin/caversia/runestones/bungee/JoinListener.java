package co.marcin.caversia.runestones.bungee;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;

public class JoinListener implements Listener {

    private static HashMap<String, Location> toJoin = new HashMap<>();

    protected static void addTPers(String player, Location toTP) {
        toJoin.put(player, toTP);
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!toJoin.containsKey(player.getName())) return;

        player.teleport(toJoin.get(player.getName()));
    }

}
