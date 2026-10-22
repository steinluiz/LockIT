package com.stein.lockit.config;

import com.stein.lockit.LockIT;
import com.stein.lockit.data.TrapType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class TrapConfigManager {
    private final LockIT plugin;
    private final File file;
    private FileConfiguration config;

    public TrapConfigManager(LockIT plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "traps.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                plugin.saveResource("traps.yml", false);
            } catch (IllegalArgumentException e) { }
        }
        config = YamlConfiguration.loadConfiguration(file);
        com.stein.lockit.utils.ConfigUtils.mergeDefaults(plugin, file, config, "traps.yml");
    }

    /** How many times the trap can go off. 0 = never breaks. */
    public int getDurability(TrapType type) {
        return Math.max(0, config.getInt(type.getId() + ".durability", 3));
    }

    /** Whether the robber is thrown out of the lockpicking menu when the trap goes off. */
    public boolean kicks(TrapType type) {
        return config.getBoolean(type.getId() + ".kick", type != TrapType.SPIKE);
    }

    public int getInkBlindnessSeconds() {
        return Math.max(0, config.getInt("ink_trap.blindness-seconds", 5));
    }

    public int getInkGlowingSeconds() {
        return Math.max(0, config.getInt("ink_trap.glowing-seconds", 10));
    }

    public int getSoundAlertRadius() {
        return Math.max(0, config.getInt("sound_trap.alert-radius", 32));
    }

    public double getSpikeDamage() {
        return Math.max(0, config.getDouble("spike_trap.damage", 4.0));
    }
}
