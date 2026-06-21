package com.stein.lockit.utils;

import com.stein.lockit.LockIT;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ConfigUtils {

    private ConfigUtils() {}

    /**
     * Merges any keys present in the bundled resource but missing from the user's file
     * back into the file on disk. Lets old servers gain new config keys without losing
     * their existing customizations.
     */
    public static void mergeDefaults(LockIT plugin, File file, FileConfiguration config, String resourceName) {
        InputStream defStream = plugin.getResource(resourceName);
        if (defStream == null) return;

        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defStream, StandardCharsets.UTF_8));
        config.setDefaults(defConfig);
        config.options().copyDefaults(true);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + resourceName + ": " + e.getMessage());
        }
    }
}
