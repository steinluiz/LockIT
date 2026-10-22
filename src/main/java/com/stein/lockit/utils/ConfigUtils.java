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

    




    public static void mergeDefaults(LockIT plugin, File file, FileConfiguration config, String resourceName) {
        InputStream defStream = plugin.getResource(resourceName);
        if (defStream == null) return;

        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defStream, StandardCharsets.UTF_8));
        config.setDefaults(defConfig);
        // Copy missing keys into the loaded config itself: lookups with an explicit fallback
        // (getString(path, def)) ignore the defaults, so new options would only apply after a restart.
        for (String key : defConfig.getKeys(true)) {
            if (!defConfig.isConfigurationSection(key) && !config.isSet(key)) {
                config.set(key, defConfig.get(key));
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + resourceName + ": " + e.getMessage());
        }
    }
}
