package com.stein.lockit.utils;

import com.stein.lockit.LockIT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class ItemUtils {
    public static final String KEY_ID_TAG = "keyid";
    public static final String LOCK_LEVEL_TAG = "lock_level";
    public static final String DURABILITY_TAG = "lockpick_durability";
    // A key can hold several codes (master key); they are stored comma-separated in KEY_ID_TAG.
    public static final String CODE_SEPARATOR = ",";

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
            setKeyCodes(item, parseCodes(id));
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

    public static boolean isKey(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String keyMat = LockIT.getInstance().getItemConfig().getConfig().getString("key.material", "NAME_TAG");
        return item.getType().name().equals(keyMat) && !"true".equals(getNBT(item, "universal"));
    }

    public static List<String> parseCodes(String raw) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        if (raw == null) return new ArrayList<>();
        for (String code : raw.split(CODE_SEPARATOR)) {
            code = code.trim();
            if (!code.isEmpty() && !code.equals("blank")) codes.add(code);
        }
        return new ArrayList<>(codes);
    }

    /** All codes on a key; empty for blank keys or non-keys. */
    public static List<String> getKeyCodes(ItemStack item) {
        return parseCodes(getNBT(item, KEY_ID_TAG));
    }

    public static boolean keyHasCode(ItemStack item, String code) {
        return code != null && getKeyCodes(item).contains(code);
    }

    /** Writes the codes to the key and lists one code per lore line. */
    public static void setKeyCodes(ItemStack item, List<String> codes) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(getNsKey(KEY_ID_TAG), PersistentDataType.STRING, String.join(CODE_SEPARATOR, codes));
        List<Component> lore = new ArrayList<>();
        for (String code : codes) lore.add(Component.text(code).color(NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
    }
}
