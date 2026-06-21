package com.stein.lockit.managers;

import com.stein.lockit.LockIT;
import com.stein.lockit.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LockpickManager {
    private final LockIT plugin;
    private final Map<UUID, GameSession> sessions = new HashMap<>();
    private final Random random = new Random();

    public LockpickManager(LockIT plugin) {
        this.plugin = plugin;
    }

    public void startGame(Player p, Location blockLoc, int level) {
        int pins = plugin.getLockpickConfig().getPins(level);
        int lives = plugin.getLockpickConfig().getLives(level);

        GameSession session = new GameSession(blockLoc, pins, lives);
        sessions.put(p.getUniqueId(), session);

        Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, Component.text("Lockpicking (Lvl " + level + ")"));
        for (int i = 0; i < pins; i++) {
            inv.setItem(9 + i, new ItemStack(Material.IRON_INGOT));
        }

        p.openInventory(inv);
        plugin.getMsg().send(p, "lp_start");
    }

    public void handleClick(Player p, int slot, Inventory inv) {
        GameSession session = sessions.get(p.getUniqueId());
        if (session == null) return;

        if (!ItemUtils.hasLockpick(p)) {
            plugin.getMsg().send(p, "lp_no_items");
            return;
        }

        if (slot < 9 || slot > 17 || inv.getItem(slot) == null || inv.getItem(slot).getType() == Material.AIR) {
            return;
        }

        int correctSlot = session.sequence.get(session.currentStage);

        if (slot == correctSlot) {
            session.currentStage++;
            ItemStack pin = inv.getItem(slot);
            inv.setItem(slot, null);

            int destSlot = (random.nextBoolean()) ? slot - 9 : slot + 9;
            inv.setItem(destSlot, pin);

            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1.5f + (session.currentStage * 0.1f));

            if (session.currentStage >= session.sequence.size()) {
                p.closeInventory();
                plugin.getMsg().send(p, "lp_win");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

                plugin.getLockManager().setLockState(session.blockLoc, false);

                sessions.remove(p.getUniqueId());
            }

        } else {
            boolean lockpickRemoved = damageLockpick(p);
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 0.5f);
            session.lives--;

            // Break message only when a lockpick actually breaks.
            if (lockpickRemoved) {
                plugin.getMsg().send(p, "lp_break", "%lives%", String.valueOf(session.lives));
            }

            // Fail message is separate: it fires when the lockpicking attempt is lost.
            if (session.lives <= 0) {
                p.closeInventory();
                plugin.getMsg().send(p, "lp_fail");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                sessions.remove(p.getUniqueId());
            }
        }
    }

    // Returns true only when a lockpick item is actually consumed/removed from the player.
    private boolean damageLockpick(Player p) {
        if (!plugin.getLockpickConfig().isDurabilityEnabled()) {
            p.getInventory().removeItem(ItemUtils.getLockpick());
            return true;
        }

        ItemStack lp = ItemUtils.findLockpick(p);
        if (lp == null) return false;

        int dur = ItemUtils.getDurability(lp) - 1;
        if (dur <= 0) {
            lp.setAmount(lp.getAmount() - 1);
            if (lp.getAmount() > 0) {
                ItemUtils.setDurability(lp, plugin.getLockpickConfig().getDurability());
            }
            return true;
        } else {
            ItemUtils.setDurability(lp, dur);
            return false;
        }
    }

    public void quit(Player p) {
        sessions.remove(p.getUniqueId());
    }

    private class GameSession {
        Location blockLoc;
        List<Integer> sequence = new ArrayList<>();
        int currentStage = 0;
        int lives;

        public GameSession(Location loc, int pinCount, int initialLives) {
            this.blockLoc = loc;
            this.lives = initialLives;
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < pinCount; i++) {
                slots.add(9 + i);
            }
            Collections.shuffle(slots);
            this.sequence = slots;
        }
    }
}