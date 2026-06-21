package com.stein.lockit.managers;

import com.stein.lockit.LockIT;
import com.stein.lockit.data.LockInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LockManager {
    private final LockIT plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<Location, LockInfo> locks = new HashMap<>();

    public LockManager(LockIT plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
    }

    public void loadData() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        try {
            if (!dataFile.exists()) dataFile.createNewFile();
        } catch (IOException e) { e.printStackTrace(); }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("locks")) {
            var section = dataConfig.getConfigurationSection("locks");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    ItemStack item = dataConfig.getItemStack("locks." + key + ".item");
                    boolean locked = dataConfig.getBoolean("locks." + key + ".locked");
                    Location loc = stringToLoc(key);
                    if (loc != null) locks.put(loc, new LockInfo(item, locked));
                }
            }
        }
    }

    public void saveAll() {
        for (Map.Entry<Location, LockInfo> entry : locks.entrySet()) {
            String key = locToString(entry.getKey());
            dataConfig.set("locks." + key + ".item", entry.getValue().lockItem);
            dataConfig.set("locks." + key + ".locked", entry.getValue().isLocked);
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public LockInfo getLock(Location loc) { return locks.get(loc); }

    public Map<Location, LockInfo> getLocks() { return locks; }

    public void setLock(Location loc, ItemStack item) {
        if (item == null) {
            locks.remove(loc);
            dataConfig.set("locks." + locToString(loc), null);
        } else {
            locks.put(loc, new LockInfo(item, true));
        }
        saveAll();
    }

    public void setLockState(Location loc, boolean locked) {
        LockInfo info = locks.get(loc);
        if (info != null) {
            info.isLocked = locked;
            saveAll();
        }
    }

    private String locToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location stringToLoc(String str) {
        try {
            String[] parts = str.split(",");
            if (parts.length != 4) return null;
            return new Location(Bukkit.getWorld(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception e) { return null; }
    }
}