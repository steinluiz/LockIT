package com.stein.lockit.config;

import com.stein.lockit.LockIT;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LockpickConfigManager {
    private final LockIT plugin;
    private final File file;
    private FileConfiguration config;

    public LockpickConfigManager(LockIT plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lockpick.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                plugin.saveResource("lockpick.yml", false);
            } catch (IllegalArgumentException e) { }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public int getPins(int level) {
        if (config == null) return 3;
        int pins = config.getInt("level_" + level + ".pins", 3);
        if (pins > 9) pins = 9;
        if (pins < 1) pins = 1;
        return pins;
    }

    public int getLives(int level) {
        if (config == null) return 3;
        return config.getInt("level_" + level + ".lives", 3);
    }
}