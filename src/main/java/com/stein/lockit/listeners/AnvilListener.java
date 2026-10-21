package com.stein.lockit.listeners;

import com.stein.lockit.LockIT;
import com.stein.lockit.config.ItemConfigManager;
import com.stein.lockit.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.ArrayList;
import java.util.List;

/**
 * Master keys: a coded key in the first anvil slot plus a coded key in the second
 * gives the first key with the codes of both. The second key is consumed.
 */
public class AnvilListener implements Listener {
    private final LockIT plugin;

    public AnvilListener(LockIT plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        if (!plugin.getConfig().getBoolean("master-key.enabled", true)) return;

        AnvilInventory inv = e.getInventory();
        ItemStack base = inv.getFirstItem();
        ItemStack addition = inv.getSecondItem();
        if (!ItemUtils.isKey(base) || !ItemUtils.isKey(addition)) return;

        List<String> baseCodes = ItemUtils.getKeyCodes(base);
        List<String> addCodes = ItemUtils.getKeyCodes(addition);
        if (baseCodes.isEmpty() || addCodes.isEmpty()) return;

        // The anvil uses up the whole first slot, so only allow a single key there.
        if (base.getAmount() != 1) {
            e.setResult(null);
            return;
        }

        List<String> merged = new ArrayList<>(baseCodes);
        for (String code : addCodes) {
            if (!merged.contains(code)) merged.add(code);
        }
        int maxCodes = plugin.getConfig().getInt("master-key.max-codes", 0);
        if (merged.size() == baseCodes.size() || (maxCodes > 0 && merged.size() > maxCodes)) {
            e.setResult(null);
            return;
        }

        ItemStack result = base.clone();
        ItemUtils.setKeyCodes(result, merged);
        applyMasterLook(result, base, e.getView().getRenameText());
        e.setResult(result);

        // Name tag + name tag isn't a vanilla anvil recipe, so the server resets the cost to
        // "denied" right after this event. Apply ours on the next tick, if the result is still ours.
        int cost = Math.max(1, plugin.getConfig().getInt("master-key.xp-cost", 5));
        AnvilView view = e.getView();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!result.equals(inv.getResult())) return;
            view.setMaximumRepairCost(Math.max(40, cost + 1));
            view.setRepairItemCountCost(1);
            view.setRepairCost(cost);
        });
    }

    private void applyMasterLook(ItemStack result, ItemStack base, String renameText) {
        FileConfiguration items = plugin.getItemConfig().getConfig();
        ItemMeta meta = result.getItemMeta();

        if (items.contains("master_key.custom_model_data")) {
            meta.setCustomModelData(items.getInt("master_key.custom_model_data"));
        }

        String currentName = plainName(base);
        String defaultKeyName = plain(ItemConfigManager.colorize(items.getString("key.name", "Key")));
        if (renameText != null && !renameText.isBlank() && !isSameName(renameText, currentName)) {
            // The player typed a new name in the anvil.
            meta.displayName(Component.text(renameText));
        } else if (isSameName(defaultKeyName, currentName) && items.contains("master_key.name")) {
            // Still a plain key: give it the master key name.
            meta.displayName(ItemConfigManager.colorize(items.getString("master_key.name")));
        }
        result.setItemMeta(meta);
    }

    private static String plainName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() ? plain(meta.displayName()) : "";
    }

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    // Keys made before 1.6 have raw color codes in their name ("§fKey"), and the client
    // drops the "§" in the anvil text box ("fKey"). Treat those as the same name as "Key".
    private static boolean isSameName(String typed, String current) {
        return typed.equals(current)
                || typed.equals(current.replace("§", ""))
                || typed.equals(current.replaceAll("§.", ""));
    }
}
