package com.stein.lockit.managers;

import com.stein.lockit.LockIT;
import com.stein.lockit.config.TrapConfigManager;
import com.stein.lockit.data.LockInfo;
import com.stein.lockit.data.TrapType;
import com.stein.lockit.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Sets off the trap installed in a lock when a robber clicks a wrong pin. */
public class TrapManager {
    private final LockIT plugin;

    public TrapManager(LockIT plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets off the trap of the lock at the location, if any.
     * @return true if the robber must be thrown out of the lockpicking menu
     */
    public boolean trigger(Player robber, Location loc) {
        LockInfo info = plugin.getLockManager().getLock(loc);
        if (info == null || info.trapItem == null) return false;
        TrapType type = ItemUtils.getTrapType(info.trapItem);
        if (type == null) return false;

        TrapConfigManager cfg = plugin.getTrapConfig();
        switch (type) {
            case INK -> {
                int ticks = cfg.getInkBlindnessSeconds() * 20;
                if (ticks > 0) robber.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0));
                int glowTicks = cfg.getInkGlowingSeconds() * 20;
                if (glowTicks > 0) robber.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowTicks, 0));
                robber.getWorld().spawnParticle(Particle.SQUID_INK, robber.getEyeLocation(), 40, 0.3, 0.3, 0.3, 0.05);
                robber.getWorld().playSound(robber.getLocation(), Sound.ENTITY_SQUID_SQUIRT, 1, 1);
                plugin.getMsg().send(robber, "trap_ink");
            }
            case SOUND -> {
                // Volume 4 can be heard about 64 blocks away.
                loc.getWorld().playSound(loc, Sound.BLOCK_BELL_USE, 4, 1);
                loc.getWorld().playSound(loc, Sound.EVENT_RAID_HORN, 4, 1);
                plugin.getMsg().send(robber, "trap_sound");

                int radius = cfg.getSoundAlertRadius();
                if (radius > 0) {
                    String pos = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
                    for (Player other : loc.getWorld().getPlayers()) {
                        if (other != robber && other.getLocation().distanceSquared(loc) <= (double) radius * radius) {
                            plugin.getMsg().send(other, "trap_sound_alert", "%pos%", pos);
                        }
                    }
                }
            }
            case SPIKE -> {
                double damage = cfg.getSpikeDamage();
                if (damage > 0) robber.damage(damage);
                robber.getWorld().playSound(robber.getLocation(), Sound.BLOCK_POINTED_DRIPSTONE_LAND, 1, 0.8f);
                robber.getWorld().playSound(robber.getLocation(), Sound.ENCHANT_THORNS_HIT, 1, 1);
                plugin.getMsg().send(robber, "trap_spike");
            }
        }

        wear(robber, loc, info.trapItem);
        return cfg.kicks(type);
    }

    /** Uses up one activation; the trap breaks when none are left. */
    private void wear(Player robber, Location loc, ItemStack trap) {
        int uses = ItemUtils.getTrapUses(trap);
        if (uses == 0) return; // never breaks

        if (uses <= 1) {
            plugin.getLockManager().setTrap(loc, null);
            loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1, 1);
            plugin.getMsg().send(robber, "trap_broken");
        } else {
            ItemStack worn = trap.clone();
            ItemUtils.setTrapUses(worn, uses - 1);
            plugin.getLockManager().setTrap(loc, worn);
        }
    }
}
