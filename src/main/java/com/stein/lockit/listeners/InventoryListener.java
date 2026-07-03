package com.stein.lockit.listeners;

import com.stein.lockit.LockIT;
import com.stein.lockit.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;

public class InventoryListener implements Listener {
    private final LockIT plugin;

    public InventoryListener(LockIT plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        ItemStack current = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        if (e.getView().getTopInventory().getHolder() instanceof com.stein.lockit.managers.LockGuiHolder) {
            LockIT.getInstance().getLockGuiManager().handleClick(e);
            return;
        }

        if (e.getView().getTitle().startsWith("Lockpicking")) {
            e.setCancelled(true);
            LockIT.getInstance().getLockpickManager().handleClick(p, e.getSlot(), e.getInventory());
            return;
        }

        if (handleKeyToLockTransfer(p, current, cursor)) {
            e.setCancelled(true);
            return;
        }

        if (handleKeyToKeyTransfer(p, current, cursor)) {
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof com.stein.lockit.managers.LockGuiHolder) {
            LockIT.getInstance().getLockGuiManager().handleClose(e);
            return;
        }

        if (e.getView().getTitle().startsWith("Lockpicking")) {
            LockIT.getInstance().getLockpickManager().quit((Player) e.getPlayer());
        }
    }

    private boolean handleKeyToKeyTransfer(Player p, ItemStack clickedItem, ItemStack cursorItem) {

        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) {
            return false;
        }

        if (clickedItem == null || clickedItem.getType().isAir() || !clickedItem.hasItemMeta()) return false;
        if (cursorItem == null || cursorItem.getType().isAir()) return false;

        String keyMat = LockIT.getInstance().getItemConfig().getConfig().getString("key.material", "NAME_TAG");

        if (!clickedItem.getType().name().equals(keyMat)) return false;
        if (!cursorItem.getType().name().equals(keyMat)) return false;

        String cursorId = ItemUtils.getNBT(cursorItem, ItemUtils.KEY_ID_TAG);
        String clickedId = ItemUtils.getNBT(clickedItem, ItemUtils.KEY_ID_TAG);

        if (cursorId == null || cursorId.equals("blank")) return false;

        if (clickedId != null && !clickedId.equals("blank")) return false;

        ItemMeta meta = clickedItem.getItemMeta();
        meta.getPersistentDataContainer().set(ItemUtils.getNsKey(ItemUtils.KEY_ID_TAG), PersistentDataType.STRING, cursorId);
        meta.lore(Collections.singletonList(Component.text(cursorId).color(NamedTextColor.GRAY)));
        clickedItem.setItemMeta(meta);

        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1.5f);
        return true;
    }

    private boolean handleKeyToLockTransfer(Player p, ItemStack clickedItem, ItemStack cursorItem) {
        if (clickedItem == null || clickedItem.getType().isAir() || !clickedItem.hasItemMeta()) {
            return false;
        }

        if (!clickedItem.getItemMeta().getPersistentDataContainer().has(ItemUtils.getNsKey(ItemUtils.LOCK_LEVEL_TAG), PersistentDataType.INTEGER)) {
            return false;
        }

        if (cursorItem == null || cursorItem.getType().isAir()) {
            return false;
        }

        String keyMat = LockIT.getInstance().getItemConfig().getConfig().getString("key.material", "NAME_TAG");

        if (cursorItem.getType().name().equals(keyMat)) {
            String keyId = ItemUtils.getNBT(cursorItem, ItemUtils.KEY_ID_TAG);
            if (keyId == null || keyId.equals("blank")) return false;

            // Valid transfer attempt, but not allowed in creative.
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) {
                plugin.getMsg().send(p, "no_creative_transfer");
                return true;
            }

            ItemMeta lockMeta = clickedItem.getItemMeta();
            lockMeta.getPersistentDataContainer().set(ItemUtils.getNsKey(ItemUtils.KEY_ID_TAG), PersistentDataType.STRING, keyId);
            lockMeta.lore(Collections.singletonList(Component.text(keyId).color(NamedTextColor.GRAY)));
            clickedItem.setItemMeta(lockMeta);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 2);
            return true;
        }
        return false;
    }
}