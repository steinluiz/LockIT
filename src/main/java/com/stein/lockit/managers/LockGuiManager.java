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
    /** Slots of the 5-slot hopper inventory. All other slots stay empty. */
    public static final int LOCK_SLOT = 1;
    public static final int TRAP_SLOT = 3;

    private final LockIT plugin;

    public LockGuiManager(LockIT plugin) {
        this.plugin = plugin;
    }

    public void open(Player p, Location blockLoc) {
        LockGuiHolder holder = new LockGuiHolder(blockLoc);
        Inventory inv = Bukkit.createInventory(holder, InventoryType.HOPPER,
                plugin.getResourcePackManager().lockGuiTitle(p, plugin.getMsg().getGuiTitle()));
        holder.setInventory(inv);

        // If the block already has a lock (and trap), show them in their slots so they can be taken out (removed).
        LockInfo info = plugin.getLockManager().getLock(blockLoc);
        if (info != null && info.lockItem != null) {
            inv.setItem(LOCK_SLOT, info.lockItem.clone());
            if (info.trapItem != null) inv.setItem(TRAP_SLOT, info.trapItem.clone());
        }

        p.openInventory(inv);
    }

    /** Whether the item may go in the given GUI slot. */
    private boolean fits(int slot, ItemStack item) {
        if (slot == LOCK_SLOT) return ItemUtils.isLock(item);
        if (slot == TRAP_SLOT) return ItemUtils.isTrap(item);
        return false;
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
            if (raw != LOCK_SLOT && raw != TRAP_SLOT) {
                e.setCancelled(true);
                return;
            }
            ClickType ct = e.getClick();
            if (ct == ClickType.NUMBER_KEY || ct == ClickType.SWAP_OFFHAND) {
                e.setCancelled(true);
                return;
            }
            ItemStack cursor = e.getCursor();
            if (cursor != null && !cursor.getType().isAir() && !fits(raw, cursor)) {
                e.setCancelled(true);
            }
            return;
        }

        // Click inside the player's inventory: allow shift-click of a lock/trap into its slot only.
        if (e.isShiftClick()) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            int slot = ItemUtils.isLock(clicked) ? LOCK_SLOT : ItemUtils.isTrap(clicked) ? TRAP_SLOT : -1;
            if (slot == -1) return;
            ItemStack existing = top.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                top.setItem(slot, clicked.clone());
                e.setCurrentItem(null);
            }
        }
    }

    public void handleDrag(InventoryDragEvent e) {
        int topSize = e.getView().getTopInventory().getSize();
        for (int raw : e.getRawSlots()) {
            if (raw >= topSize) continue;
            // Only a lock may go in the lock slot and a trap in the trap slot; every other GUI slot stays empty.
            if (!fits(raw, e.getOldCursor())) {
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
        ItemStack lock = top.getItem(LOCK_SLOT);
        ItemStack trap = top.getItem(TRAP_SLOT);
        top.clear();
        if (lock != null && lock.getType().isAir()) lock = null;
        if (trap != null && trap.getType().isAir()) trap = null;

        // The items shown when opening were clones of the installed ones: whatever is in the
        // slots now is what the block gets, and taking an item out removes it from the block.
        LockInfo info = plugin.getLockManager().getLock(loc);
        boolean hadTrap = info != null && info.trapItem != null;

        String code = ItemUtils.isLock(lock) ? ItemUtils.getNBT(lock, ItemUtils.KEY_ID_TAG) : null;
        if (code == null || code.equals("blank")) {
            if (info != null) {
                plugin.getLockManager().setLock(loc, null);
                plugin.getMsg().send(p, "lock_removed");
                p.playSound(loc, Sound.ENTITY_ITEM_BREAK, 1, 1);
            } else if (lock != null && ItemUtils.isLock(lock)) {
                plugin.getMsg().send(p, "lock_needs_key");
            } else if (trap != null) {
                plugin.getMsg().send(p, "trap_needs_lock");
            }
            // No valid lock: nothing stays in the block, give everything back.
            if (lock != null) returnToPlayer(p, lock);
            if (trap != null) returnToPlayer(p, trap);
            return;
        }

        ItemStack newTrap = trap == null ? null : trap.asOne();
        boolean sameLock = info != null && lock.isSimilar(info.lockItem);
        boolean sameTrap = hadTrap ? info.trapItem.isSimilar(newTrap) : newTrap == null;
        if (!sameLock || !sameTrap) {
            plugin.getLockManager().setLock(loc, lock.asOne(), newTrap);
        }

        if (info == null || !sameLock) {
            plugin.getMsg().send(p, "lock_installed");
            p.playSound(loc, Sound.BLOCK_ANVIL_USE, 1, 1);
        }
        if (newTrap != null && !sameTrap) {
            plugin.getMsg().send(p, "trap_installed");
            p.playSound(loc, Sound.BLOCK_TRIPWIRE_ATTACH, 1, 1);
        } else if (newTrap == null && hadTrap) {
            plugin.getMsg().send(p, "trap_removed");
        }

        if (lock.getAmount() > 1) returnToPlayer(p, lock.asQuantity(lock.getAmount() - 1));
        if (trap != null && trap.getAmount() > 1) returnToPlayer(p, trap.asQuantity(trap.getAmount() - 1));
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
