package co.marcin.caversia.runestones;

import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Runestone implements ConfigurationSerializable {
    @Getter
    private final UUID uuid;
    @Getter
    @Setter
    private Location location;
    @Getter
    @Setter
    private String locString;
    @Getter
    @Setter
    private String server;

    public Runestone(Map<String, Object> map) {
        Validate.notNull(map);
        Validate.notEmpty(map);
        uuid = UUID.fromString((String) map.get("uuid"));

        World world = Bukkit.getWorld((String) map.get("world"));
        if (world == null) {
            RuneStonesPlugin.getInstance().getLogger().severe("Invalid world: " + map.get("world"));
            return;
        }

        server = (String) map.get("server");

        location = Location.deserialize(map);
    }

    public Runestone(UUID id, NBTItem item) {
        setServer(item.getString("server"));
        setLocString(item.getString("location"));
        setLocation(RuneStonesPlugin.getInstance().stringToLoc(locString));
        uuid = id;
    }

    public Runestone() {
        uuid = UUID.randomUUID();
    }

    @Override
    public Map<String, Object> serialize() {
        Validate.notNull(getLocation());
        Map<String, Object> map = new HashMap<>();

        map.put("uuid", getUuid().toString());
        map.put("server", server);
        map.putAll(getLocation().serialize());

        return map;
    }
}
