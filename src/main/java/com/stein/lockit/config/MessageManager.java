package com.stein.lockit.config;

import com.stein.lockit.LockIT;
import com.stein.lockit.utils.ConfigUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class MessageManager {
    private final LockIT plugin;
    private final File file;
    private FileConfiguration config;

    private final String PREFIX = "&8[&6LockIT&8] &r";

    public MessageManager(LockIT plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                plugin.saveResource("messages.yml", false);
            } catch (IllegalArgumentException e) { }
        }
        config = YamlConfiguration.loadConfiguration(file);
        ConfigUtils.mergeDefaults(plugin, file, config, "messages.yml");
    }

    private String getRaw(String key) {
        if (config == null) return null;
        String val = config.getString(key);
        if (val == null || val.isEmpty()) return null;
        return val;
    }

    public boolean has(String key) {
        return getRaw(key) != null;
    }

    private boolean useActionBar() {
        return plugin.getConfig().getBoolean("action-bar", false);
    }

    public void send(CommandSender sender, String key) {
        String msg = getRaw(key);
        if (msg == null) return;
        if (useActionBar() && sender instanceof Player) {
            ((Player) sender).sendActionBar(Component.text(ChatColor.translateAlternateColorCodes('&', msg)));
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + msg));
        }
    }

    public void send(CommandSender sender, String key, String placeholder, String replacement) {
        String msg = getRaw(key);
        if (msg == null) return;
        if (useActionBar() && sender instanceof Player) {
            String finalMsg = msg.replace(placeholder, replacement);
            ((Player) sender).sendActionBar(Component.text(ChatColor.translateAlternateColorCodes('&', finalMsg)));
        } else {
            String finalMsg = (PREFIX + msg).replace(placeholder, replacement);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', finalMsg));
        }
    }

    public void sendActionBar(Player p, String key) {
        String msg = getRaw(key);
        if (msg != null) {
            p.sendActionBar(Component.text(ChatColor.translateAlternateColorCodes('&', msg)));
        }
    }
}