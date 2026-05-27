package com.stein.lockit.data;

import org.bukkit.inventory.ItemStack;

public class LockInfo {
    public ItemStack lockItem;
    public boolean isLocked;

    public LockInfo(ItemStack lockItem, boolean isLocked) {
        this.lockItem = lockItem;
        this.isLocked = isLocked;
    }
}