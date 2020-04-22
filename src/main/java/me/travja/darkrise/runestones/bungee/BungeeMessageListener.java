package me.travja.darkrise.runestones.bungee;

import co.marcin.caversia.runestones.Runestone;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

public class BungeeMessageListener implements Listener {

    @EventHandler
    public void message(PluginMessageEvent event) {
        if (!event.getTag().equals(RunestonesBungee.CHANNEL))
            return;

        byte[] data = event.getData();

        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        byte[] msgbytes = new byte[data.length];
        in.readFully(msgbytes);

        DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
        try {
            if (msgin.available() == 0)
                return;

            String idstr = msgin.readUTF();
            UUID id = UUID.fromString(idstr);
            String server = msgin.readUTF();
            String command = msgin.readUTF();

            if(command.equals("RunestoneTP")) {
                String serverDest = msgin.readUTF();
                ServerInfo info = RunestonesBungee.getInstance().getProxy().getServerInfo(serverDest);
                ProxiedPlayer player = RunestonesBungee.getInstance().getProxy().getPlayer(msgin.readUTF());
                if(player == null || !player.isConnected())
                    return;
                if(info == null) {
                    player.sendMessage(new TextComponent(ChatColor.RED + "Could not teleport."));
                    return;
                }

                player.connect(info);

                info.sendData(RunestonesBungee.CHANNEL, new BungeeMessageData("BUNGEE", "RunestoneTP", player.getName(), msgin.readUTF()/*Location*/).toByteArray());
            } else {
                RunestonesBungee.getInstance().getLogger().info("Received unknown command: " + command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
