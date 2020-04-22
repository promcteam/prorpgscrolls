package me.travja.darkrise.runestones.bungee;

import net.md_5.bungee.api.plugin.Plugin;

public class RunestonesBungee extends Plugin {

    public static final String CHANNEL = "darkrise:runestones";

    private static RunestonesBungee instance;

    @Override
    public void onLoad() {
        instance = this;
        getProxy().registerChannel(CHANNEL);
    }

    @Override
    public void onEnable() {

        getProxy().getPluginManager().registerListener(this, new BungeeMessageListener());
        getLogger().info("Runestones-Bungee enabled!");
    }


    public static RunestonesBungee getInstance() {
        return instance;
    }
}
