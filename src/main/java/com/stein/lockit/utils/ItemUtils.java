package com.stein.lockit.utils;

import com.stein.lockit.LockIT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;

public class ItemUtils {
    public static final String KEY_ID_TAG = "keyid";
    public static final String LOCK_LEVEL_TAG = "lock_level";
    public static final String DURABILITY_TAG = "lockpick_durability";

    public static NamespacedKey getNsKey(String key) {
        return new NamespacedKey(LockIT.getInstance(), key);
    }

    public static ItemStack getLock(int level) {
        ItemStack item = LockIT.getInstance().getItemConfig().getLockFromConfig(level);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(getNsKey(LOCK_LEVEL_TAG), PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getKey(String id) {
        ItemStack item = LockIT.getInstance().getItemConfig().getItemFromConfig("key");
        if (id != null && !id.equals("blank")) {
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(getNsKey(KEY_ID_TAG), PersistentDataType.STRING, id);
            meta.lore(Collections.singletonList(Component.text(id).color(NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        } else {
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(getNsKey(KEY_ID_TAG), PersistentDataType.STRING, "blank");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getUniversalKey() {
        ItemStack item = LockIT.getInstance().getItemConfig().getItemFromConfig("universal_key");
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(getNsKey("universal"), PersistentDataType.STRING, "true");
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getLockpick() {
        return LockIT.getInstance().getItemConfig().getItemFromConfig("lockpick");
    }

    public static boolean hasLockpick(Player p) {
        return findLockpick(p) != null;
    }

    public static ItemStack findLockpick(Player p) {
        ItemStack ref = getLockpick();
        for (ItemStack i : p.getInventory().getContents()) {
            if (i != null && i.getType() == ref.getType()) {
                if (i.hasItemMeta() && i.getItemMeta().hasCustomModelData()) {
                    if (i.getItemMeta().getCustomModelData() == ref.getItemMeta().getCustomModelData()) {
                        return i;
                    }
                }
            }
        }
        return null;
    }

    public static int getDurability(ItemStack item) {
        int def = LockIT.getInstance().getLockpickConfig().getDurability();
        if (item == null || !item.hasItemMeta()) return def;
        Integer v = item.getItemMeta().getPersistentDataContainer()
                .get(getNsKey(DURABILITY_TAG), PersistentDataType.INTEGER);
        return v == null ? def : v;
    }

    public static void setDurability(ItemStack item, int value) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(getNsKey(DURABILITY_TAG), PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }

    public static boolean isLock(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(getNsKey(LOCK_LEVEL_TAG), PersistentDataType.INTEGER);
    }

    public static String getNBT(ItemStack item, String tag) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(getNsKey(tag), PersistentDataType.STRING);
    }
}