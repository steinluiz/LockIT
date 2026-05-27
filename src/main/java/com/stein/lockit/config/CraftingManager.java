package com.stein.lockit.config;

import com.stein.lockit.LockIT;
import com.stein.lockit.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.io.File;
import java.util.List;

public class CraftingManager {
    private final LockIT plugin;
    private final File file;
    private FileConfiguration config;

    public CraftingManager(LockIT plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "craftings.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                plugin.saveResource("craftings.yml", false);
            } catch (IllegalArgumentException e) { }
        }
        config = YamlConfiguration.loadConfiguration(file);
        registerRecipes();
    }

    private void registerRecipes() {
        if (config == null) return;

        String[] keys = {"key", "universal_key", "lockpick", "lock_1", "lock_2", "lock_3", "lock_4", "lock_5", "lock_6"};

        for (String key : keys) {
            if (!config.contains(key + ".enabled") || !config.getBoolean(key + ".enabled")) continue;

            ItemStack resultItem = null;
            switch (key) {
                case "key": resultItem = ItemUtils.getKey("blank"); break;
                case "universal_key": resultItem = ItemUtils.getUniversalKey(); break;
                case "lockpick": resultItem = ItemUtils.getLockpick(); break;
                case "lock_1": resultItem = ItemUtils.getLock(1); break;
                case "lock_2": resultItem = ItemUtils.getLock(2); break;
                case "lock_3": resultItem = ItemUtils.getLock(3); break;
                case "lock_4": resultItem = ItemUtils.getLock(4); break;
                case "lock_5": resultItem = ItemUtils.getLock(5); break;
                case "lock_6": resultItem = ItemUtils.getLock(6); break;
            }

            if (resultItem != null) {
                resultItem.setAmount(config.getInt(key + ".amount", 1));
                NamespacedKey nsKey = new NamespacedKey(plugin, key + "_recipe");

                plugin.getServer().removeRecipe(nsKey);

                ShapedRecipe recipe = new ShapedRecipe(nsKey, resultItem);

                List<String> shapeList = config.getStringList(key + ".shape");
                if (shapeList.isEmpty()) continue;

                recipe.shape(shapeList.toArray(new String[0]));

                ConfigurationSection ingredients = config.getConfigurationSection(key + ".ingredients");
                if (ingredients != null) {
                    for (String charKey : ingredients.getKeys(false)) {
                        char c = charKey.charAt(0);
                        String matName = ingredients.getString(charKey);
                        Material mat = Material.getMaterial(matName);
                        if (mat != null) {
                            recipe.setIngredient(c, mat);
                        }
                    }
                }
                plugin.getServer().addRecipe(recipe);
            }
        }
    }
}