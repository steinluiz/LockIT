package com.stein.lockit.managers;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker holder for the lock-install GUI. Stores the block location the lock
 * will be installed on so the GUI can be identified and resolved on click/close.
 */
public class LockGuiHolder implements InventoryHolder {
    private final Location location;
    private Inventory inventory;

    public LockGuiHolder(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
