package co.marcin.caversia.runestones.bungee;

import co.marcin.caversia.runestones.RuneStonesPlugin;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

public class BungeeListener implements PluginMessageListener {

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(RuneStonesPlugin.CHANNEL))
            return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        UUID id = UUID.fromString(in.readUTF()); // We really don't need to do anything with this, aside from sending a response.
        String one = in.readUTF();
        if (one.equals("RunestoneTP")) {
            String pname = in.readUTF();
            String locStr = in.readUTF();

            Player target = Bukkit.getPlayer(pname);
            if (target.isOnline())
                RuneStonesPlugin.getInstance().teleport(target, RuneStonesPlugin.getInstance().stringToLoc(locStr));
            else
                JoinListener.addTPers(pname, RuneStonesPlugin.getInstance().stringToLoc(locStr));
        } else
            RuneStonesPlugin.getInstance().getLogger().warning("Unknown Bungee Message: " + one);
    }
}
