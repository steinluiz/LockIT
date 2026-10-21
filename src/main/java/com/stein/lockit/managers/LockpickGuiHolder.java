package com.stein.lockit.managers;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker holder for the lockpicking minigame, so it can be recognised without
 * relying on the title (which is a texture glyph when the resource pack is loaded).
 */
public class LockpickGuiHolder implements InventoryHolder {
    private Inventory inventory;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
