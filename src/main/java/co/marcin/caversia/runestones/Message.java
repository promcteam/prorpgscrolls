package co.marcin.caversia.runestones;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class Message {
    private final String path;
    private final List<Object> variables = new ArrayList<>();

    public Message(String path) {
        this.path = path;
    }

    public Message set(Object o) {
        variables.add(o);
        return this;
    }

    public void send(CommandSender sender) {
        sender.sendMessage(compose());
    }

    protected String compose() {
        String string = RuneStonesPlugin.getInstance().getConfig().getString("messages." + path);

        for(int i = 0; i < variables.size(); i++) {
            string = StringUtils.replaceOnce(string, "%" + i, variables.get(i).toString());
        }

        return ChatColor.translateAlternateColorCodes('&', string);
    }
}
