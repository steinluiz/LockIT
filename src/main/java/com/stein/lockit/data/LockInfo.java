package com.stein.lockit.data;

import org.bukkit.inventory.ItemStack;

public class LockInfo {
    public ItemStack lockItem;
    public boolean isLocked;
    /** Trap installed together with the lock, or null. */
    public ItemStack trapItem;

    public LockInfo(ItemStack lockItem, boolean isLocked) {
        this(lockItem, isLocked, null);
    }

    public LockInfo(ItemStack lockItem, boolean isLocked, ItemStack trapItem) {
        this.lockItem = lockItem;
        this.isLocked = isLocked;
        this.trapItem = trapItem;
    }
}
