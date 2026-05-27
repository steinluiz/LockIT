package com.stein.lockit.config;

import com.stein.lockit.LockIT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor; // Adicionado import que faltava
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ItemConfigManager {
    private final LockIT plugin;
    private final File file;
    private FileConfiguration config;

    public ItemConfigManager(LockIT plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "items.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                plugin.saveResource("items.yml", false);
            } catch (IllegalArgumentException e) { }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    // --- CORREÇÃO: Getter público para acessar a config ---
    public FileConfiguration getConfig() {
        return config;
    }
    // -----------------------------------------------------

    public ItemStack getItemFromConfig(String key) {
        String matName = config.getString(key + ".material", "STONE");
        Material mat = Material.getMaterial(matName);
        if (mat == null) mat = Material.STONE;
        String name = ChatColor.translateAlternateColorCodes('&', config.getString(key + ".name", "Item Error"));
        int cmd = config.getInt(key + ".custom_model_data", 0);

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.setCustomModelData(cmd);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getLockFromConfig(int level) {
        String key = "lock_" + level;
        boolean hasConfig = config.contains(key);

        String matName = hasConfig ? config.getString(key + ".material", "IRON_NUGGET") : "IRON_NUGGET";
        Material mat = Material.getMaterial(matName);
        if (mat == null) mat = Material.IRON_NUGGET;

        String defaultName = "&rLock Level " + level;
        if (level == 6) defaultName = "&5Unpickable Lock";

        String name = ChatColor.translateAlternateColorCodes('&',
                hasConfig ? config.getString(key + ".name", defaultName) : defaultName
        );

        int cmd;
        if (hasConfig && config.contains(key + ".custom_model_data")) {
            cmd = config.getInt(key + ".custom_model_data");
        } else {
            cmd = 99 + (level - 1);
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.setCustomModelData(cmd);
            item.setItemMeta(meta);
        }
        return item;
    }
}