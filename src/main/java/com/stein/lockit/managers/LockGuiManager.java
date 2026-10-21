package com.stein.lockit.managers;

import com.stein.lockit.LockIT;
import com.stein.lockit.data.LockInfo;
import com.stein.lockit.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class LockGuiManager {
    /** Middle slot of the 5-slot hopper inventory. All other slots stay empty. */
    public static final int LOCK_SLOT = 2;

    private final LockIT plugin;

    public LockGuiManager(LockIT plugin) {
        this.plugin = plugin;
    }

    public void open(Player p, Location blockLoc) {
        LockGuiHolder holder = new LockGuiHolder(blockLoc);
        Inventory inv = Bukkit.createInventory(holder, InventoryType.HOPPER,
                plugin.getResourcePackManager().lockGuiTitle(p, plugin.getMsg().getGuiTitle()));
        holder.setInventory(inv);

        // If the block already has a lock, show it in the lock slot so it can be taken out (removed).
        LockInfo info = plugin.getLockManager().getLock(blockLoc);
        if (info != null && info.lockItem != null) {
            inv.setItem(LOCK_SLOT, info.lockItem.clone());
        }

        p.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        int raw = e.getRawSlot();
        boolean inTop = raw >= 0 && raw < top.getSize();

        if (e.getClick() == ClickType.DOUBLE_CLICK) {
            e.setCancelled(true);
            return;
        }

        if (inTop) {
            if (raw != LOCK_SLOT) {
                e.setCancelled(true);
                return;
            }
            ClickType ct = e.getClick();
            if (ct == ClickType.NUMBER_KEY || ct == ClickType.SWAP_OFFHAND) {
                e.setCancelled(true);
                return;
            }
            ItemStack cursor = e.getCursor();
            if (cursor != null && !cursor.getType().isAir() && !ItemUtils.isLock(cursor)) {
                e.setCancelled(true);
            }
            return;
        }

        // Click inside the player's inventory: allow shift-click of a lock into the lock slot only.
        if (e.isShiftClick()) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (ItemUtils.isLock(clicked)) {
                ItemStack existing = top.getItem(LOCK_SLOT);
                if (existing == null || existing.getType().isAir()) {
                    top.setItem(LOCK_SLOT, clicked.clone());
                    e.setCurrentItem(null);
                }
            }
        }
    }

    public void handleDrag(InventoryDragEvent e) {
        int topSize = e.getView().getTopInventory().getSize();
        for (int raw : e.getRawSlots()) {
            if (raw >= topSize) continue;
            // Only a lock may go in the lock slot; every other GUI slot stays empty.
            if (raw != LOCK_SLOT || !ItemUtils.isLock(e.getOldCursor())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    public void handleClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        Inventory top = e.getInventory();
        if (!(top.getHolder() instanceof LockGuiHolder)) return;

        Location loc = ((LockGuiHolder) top.getHolder()).getLocation();
        ItemStack inSlot = top.getItem(LOCK_SLOT);
        top.setItem(LOCK_SLOT, null);

        LockInfo info = plugin.getLockManager().getLock(loc);

        // Manage mode: the block already has a lock (it was shown in the lock slot).
        if (info != null) {
            if (inSlot == null || inSlot.getType().isAir()) {
                // Player took the lock out → remove it from the block. They keep the item they dragged out.
                plugin.getLockManager().setLock(loc, null);
                plugin.getMsg().send(p, "lock_removed");
                p.playSound(loc, Sound.ENTITY_ITEM_BREAK, 1, 1);
            }
            // Lock left in the slot → block stays locked; the shown item was a clone, discard it.
            return;
        }

        if (inSlot == null || inSlot.getType().isAir()) return;

        if (ItemUtils.isLock(inSlot)) {
            String keyId = ItemUtils.getNBT(inSlot, ItemUtils.KEY_ID_TAG);
            if (keyId != null && !keyId.equals("blank")) {
                plugin.getLockManager().setLock(loc, inSlot.asOne());
                plugin.getMsg().send(p, "lock_installed");
                p.playSound(loc, Sound.BLOCK_ANVIL_USE, 1, 1);

                if (inSlot.getAmount() > 1) {
                    ItemStack rest = inSlot.clone();
                    rest.setAmount(inSlot.getAmount() - 1);
                    returnToPlayer(p, rest);
                }
                return;
            }
            plugin.getMsg().send(p, "lock_needs_key");
        }

        // Nothing installed: give the item back.
        returnToPlayer(p, inSlot);
    }

    private void returnToPlayer(Player p, ItemStack item) {
        HashMap<Integer, ItemStack> leftOver = p.getInventory().addItem(item);
        if (!leftOver.isEmpty()) {
            for (ItemStack l : leftOver.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), l);
            }
        }
    }
}
