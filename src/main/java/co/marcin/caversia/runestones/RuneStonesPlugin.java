package co.marcin.caversia.runestones;

import co.marcin.caversia.runestones.bungee.BungeeListener;
import co.marcin.caversia.runestones.bungee.JoinListener;
import com.gotofinal.darkrise.economy.DarkRiseEconomy;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.Getter;
import me.travja.darkrise.core.bungee.BungeeUtil;
import me.travja.darkrise.core.item.DarkRiseItem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class RuneStonesPlugin extends JavaPlugin implements Listener {
    @Getter
    private final Map<UUID, Integer> markTimeMap = new HashMap<>();
    @Getter
    private final Map<UUID, Integer> teleportTimeMap = new HashMap<>();
    @Getter
    private final Collection<Runestone> runestones = new ArrayList<>();
    private final Collection<Runestone> runestoneSaveQueue = new ArrayList<>();
    @Getter
    private static RuneStonesPlugin instance;
    private DarkRiseEconomy economy;
//    private File storageDirectory;

    public static boolean IS_BUNGEE = false;
    public static String BUNGEE_ID;
    public static final String CHANNEL = "darkrise:runestones";

    @Override
    public void onEnable() {
        instance = this;
        IS_BUNGEE = RuneStonesPlugin.getInstance().getConfig().getBoolean("bungee");
        BUNGEE_ID = RuneStonesPlugin.getInstance().getConfig().getString("bungee_id");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        economy = getPlugin(DarkRiseEconomy.class);
//        storageDirectory = new File(getDataFolder(), "/data");
//        load();

        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }

//        if (!storageDirectory.exists() && !storageDirectory.mkdirs()) {
//            getLogger().severe("Could not create the storage directory");
//        }

        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, new BungeeListener());

        getLogger().info(String.format("v%s Enabled", getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
//        save();
        getLogger().info(String.format("v%s Disabled", getDescription().getVersion()));
    }

    public void teleport(Player player, Location location) {
        teleport(player, location, player.getInventory().getItemInMainHand());
    }

    public void teleport(Player player, Location location, ItemStack item) {
        teleportTimeMap.put(player.getUniqueId(), getUnixtime());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 14f, 1f);
        player.teleport(location);

        if (item.getAmount() == 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        new Message("teleported").send(player);
    }

    public String locToString(Location loc) {
        StringBuilder builder = new StringBuilder(loc.getWorld().getName());
        builder.append(",").append(loc.getX())
                .append(",").append(loc.getY())
                .append(",").append(loc.getZ())
                .append(",").append(loc.getYaw())
                .append(",").append(loc.getPitch());
        return builder.toString();
    }

    public Location stringToLoc(String locStr) {
        String[] args = locStr.split(",");
        try {
            World w = Bukkit.getWorld(args[0]);
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);
            float pitch = 0f;
            float yaw = 0f;
            if (args.length >= 6) {
                yaw = Float.parseFloat(args[4]);
                pitch = Float.parseFloat(args[5]);
            }

            return new Location(w, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if (event.getHand() != EquipmentSlot.HAND
                || event.getItem() == null) {
            return;
        }

        DarkRiseItem item = economy.getItems().getItemByStack(event.getItem());

        if (item == null) {
            return;
        }

        boolean marked = item.getId().equals(getConfig().getString("item.marked"));

        if (!marked && !item.getId().equals(getConfig().getString("item.unmarked"))) {
            return;
        }

        event.setCancelled(true);

        if (marked) { //Teleporting
            if (!player.hasPermission("runestones.use")) {
                new Message("noperm").send(player);
            }

            Optional<Runestone> runestone = getRunestone(event.getItem());

            if (!runestone.isPresent()) {
                return;
            }

            int lastTeleportTime = teleportTimeMap.getOrDefault(player.getUniqueId(), 0);
            int timeLeft = lastTeleportTime + getConfig().getInt("cooldown.teleport") - getUnixtime();
            if (timeLeft > 0) {
                new Message("wait.teleport").set(timeLeft).send(player);
                return;
            }

            if (!IS_BUNGEE || BUNGEE_ID.equals(runestone.get().getServer())) {
                teleport(player, runestone.get().getLocation(), event.getItem());
            } else { //They're not on the correct server. So we'll tp them to the correct one
                BungeeUtil.sendMessage(RuneStonesPlugin.CHANNEL, player, "RunestoneTP", runestone.get().getServer(), player.getName(), runestone.get().getLocString());
            }
        } else { //Marking
            if (!player.hasPermission("runestones.mark")) {
                new Message("noperm").send(player);
            }

            if (event.getItem().getAmount() != 1) {
                new Message("notone").send(player);
                return;
            }

            Integer lastMarkTime = markTimeMap.getOrDefault(player.getUniqueId(), 0);

            int timeLeft = lastMarkTime + getConfig().getInt("cooldown.mark") - getUnixtime();
            if (timeLeft > 0) {
                new Message("wait.mark").set(timeLeft).send(player);
                return;
            }

            new Message("marking").send(player);

            markTimeMap.put(player.getUniqueId(), getUnixtime());
            playMarkEffects(player);
            Location location = player.getLocation();
            getServer().getScheduler().runTaskLater(this, () -> {
                if (!location.getWorld().equals(player.getLocation().getWorld())
                        || location.distance(player.getLocation()) > 1) {
                    return;
                }

                Runestone runestone = new Runestone();
                runestone.setLocation(player.getLocation());

                if (IS_BUNGEE)
                    runestone.setServer(BUNGEE_ID);

                player.getInventory().setItemInMainHand(mark(event.getItem(), runestone));
                runestones.add(runestone);
                runestoneSaveQueue.add(runestone);
                new Message("marked").send(player);
            }, 20 * getConfig().getInt("warmup"));
        }
    }

    public static int getUnixtime() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public Optional<Runestone> getRunestone(UUID uuid) {
        return runestones.stream().filter(runestone -> runestone.getUuid().equals(uuid)).findFirst();
    }

    public Optional<Runestone> getRunestone(ItemStack itemStack) {
        NBTItem item = new NBTItem(itemStack);
        if (itemStack == null
                || !itemStack.hasItemMeta()
                || !itemStack.getItemMeta().hasLore() || !item.hasKey("isRunestone")) {
            return Optional.empty();
        }

        String uuidString = itemStack.getItemMeta().getLore().get(itemStack.getItemMeta().getLore().size() - 1);
        UUID uuid = invisibleDecrypt(uuidString);
        Runestone runestone = new Runestone(uuid, item);

        Optional<Runestone> opt = Optional.of(runestone);

        return opt;//getRunestone(uuid);
    }

    public ItemStack mark(ItemStack itemStack, Runestone runestone) {
        itemStack = economy.getItems().getItemById(getConfig().getString("item.marked")).getItem();
        ItemMeta meta = itemStack.getItemMeta();
        List<String> lore = meta.getLore();

        lore = lore.stream().map(s -> StringUtils.replace(s, "%W", String.valueOf(runestone.getLocation().getWorld().getName()))).collect(Collectors.toList());
        lore = lore.stream().map(s -> StringUtils.replace(s, "%X", String.valueOf(runestone.getLocation().getBlockX()))).collect(Collectors.toList());
        lore = lore.stream().map(s -> StringUtils.replace(s, "%Y", String.valueOf(runestone.getLocation().getBlockY()))).collect(Collectors.toList());
        lore = lore.stream().map(s -> StringUtils.replace(s, "%Z", String.valueOf(runestone.getLocation().getBlockZ()))).collect(Collectors.toList());

        lore.add(invisibleEncrypt(runestone.getUuid()));
        meta.setLore(lore);
        itemStack.setItemMeta(meta);

        NBTItem item = new NBTItem(itemStack);
        item.setBoolean("isRunestone", true);
        item.setString("server", runestone.getServer());
        item.setString("location", locToString(runestone.getLocation()));
        itemStack = item.getItem();
        return itemStack;
    }

//    public void load() {
//        File[] list = storageDirectory.listFiles();
//
//        if (list == null) {
//            return;
//        }
//
//        for (File file : list) {
//            ConfigurationSection configuration = YamlConfiguration.loadConfiguration(file);
//            String key = Iterables.getFirst(configuration.getKeys(false), null);
//            Validate.notNull(key);
//            configuration = configuration.getConfigurationSection(key);
//            runestones.add(new Runestone(configuration.getValues(true)));
//        }
//    }

//    public void save() {
//        for (Runestone runestone : runestoneSaveQueue) {
//            try {
//                File file = new File(storageDirectory, runestone.getUuid().toString());
//
//                if (!file.exists() && !file.createNewFile()) {
//                    getLogger().severe("Failed to create file: " + file.getPath());
//                }
//
//                YamlConfiguration configuration = new YamlConfiguration();
//                configuration.set(runestone.getUuid().toString(), runestone.serialize());
//                configuration.save(file);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        runestoneSaveQueue.clear();
//    }

    public static String invisibleEncrypt(UUID uuid) {
        StringBuilder builder = new StringBuilder();

        for (char c : uuid.toString().toCharArray()) {
            builder.append(ChatColor.COLOR_CHAR);
            builder.append(c);
        }

        return builder.toString();
    }

    public static UUID invisibleDecrypt(String string) {
        string = StringUtils.remove(string, ChatColor.COLOR_CHAR);

        try {
            return UUID.fromString(string);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void playMarkEffects(Player player) {
        Location location = player.getLocation();
        final int[] count = {
                getInstance().getConfig().getInt("warmup") * 20
        };

        Bukkit.getScheduler().runTask(getInstance(), new Runnable() {
            Random rand = new Random();
            int offset = getInstance().getConfig().getInt("effectradius");

            @Override
            public void run() {
                if (!location.getWorld().equals(player.getLocation().getWorld())
                        || location.distance(player.getLocation()) > 1) {
                    new Message("moved").send(player);
                    return;
                }

                for (int i = 0; i < 4; i++) {
                    player.getWorld().spawnParticle(Particle.SPELL_MOB,
                            player.getLocation().add(rand.nextDouble() * offset - offset, rand.nextDouble() * offset - offset, rand.nextDouble() * offset - offset),
                            0, 1d/*red*/, 0d/*green*/, 0d/*blue*/);
                    //ParticleEffect.SPELL_MOB.display(new ParticleEffect.OrdinaryColor(Color.RED), player.getLocation(), getInstance().getConfig().getInt("effectradius"));
                }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 14f, 1f);

                if (--count[0] > 0) {
                    Bukkit.getScheduler().runTaskLater(getInstance(), this, 1);
                }
            }
        });
    }
}
