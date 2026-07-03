package com.stein.lockit.listeners;

import com.stein.lockit.LockIT;
import com.stein.lockit.data.LockInfo;
import com.stein.lockit.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class InteractionListener implements Listener {
    private final LockIT plugin;

    public InteractionListener(LockIT plugin) {
        this.plugin = plugin;
    }

    private void closeIfOpen(Block b) {
        if (b.getBlockData() instanceof Openable) {
            Openable openable = (Openable) b.getBlockData();
            if (openable.isOpen()) {
                openable.setOpen(false);
                b.setBlockData(openable);
            }
        }
    }

    private Location getRealBlockLocation(Block b) {
        if (b.getBlockData() instanceof Bisected) {
            Bisected data = (Bisected) b.getBlockData();
            if (data.getHalf() == Bisected.Half.TOP) return b.getRelative(0, -1, 0).getLocation();
        }
        return b.getLocation();
    }

    private Block getDoubleChestPartner(Block b) {
        if (!(b.getState() instanceof org.bukkit.block.Chest)) return null;
        org.bukkit.inventory.InventoryHolder holder = ((org.bukkit.block.Chest) b.getState()).getInventory().getHolder();
        if (holder instanceof org.bukkit.block.DoubleChest) {
            org.bukkit.block.DoubleChest dc = (org.bukkit.block.DoubleChest) holder;
            Block left = ((org.bukkit.block.Chest) dc.getLeftSide()).getBlock();
            Block right = ((org.bukkit.block.Chest) dc.getRightSide()).getBlock();
            return b.equals(left) ? right : left;
        }
        return null;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();

        if (block.getType() == Material.SMITHING_TABLE) { handleKeyCreation(e); return; }
        if (block.getType() == Material.GRINDSTONE) { handleGrindstoneClear(e); return; }

        Material type = block.getType();
        if (isLockableBlock(type)) {
            Location loc = getRealBlockLocation(block);
            LockInfo info = LockIT.getInstance().getLockManager().getLock(loc);

            if (p.isSneaking() && info == null && isLockItem(hand)) {
                String keyId = ItemUtils.getNBT(hand, ItemUtils.KEY_ID_TAG);

                if (keyId != null && !keyId.equals("blank")) {
                    e.setCancelled(true);
                    ItemStack lockToInstall = hand.asOne();
                    LockIT.getInstance().getLockManager().setLock(loc, lockToInstall);
                    hand.subtract(1);
                    plugin.getMsg().send(p, "lock_installed");
                    p.playSound(loc, Sound.BLOCK_ANVIL_USE, 1, 1);
                    return;
                } else {
                    e.setCancelled(true);
                    plugin.getMsg().send(p, "lock_needs_key");
                    p.playSound(loc, Sound.BLOCK_LEVER_CLICK, 1, 0.5f);
                    return;
                }
            }

            if (info != null) {
                String lockId = ItemUtils.getNBT(info.lockItem, ItemUtils.KEY_ID_TAG);
                boolean playerHasKey = hasKey(p, lockId);
                boolean holdingKey = (playerHasKey && checkKeyItem(hand, lockId)) || isUniversalKey(hand);

                if (holdingKey) {
                    e.setCancelled(true);
                    boolean newState = !info.isLocked;
                    LockIT.getInstance().getLockManager().setLockState(loc, newState);
                    String msg = newState ? "state_locked" : "state_unlocked";
                    Sound sound = newState ? Sound.BLOCK_IRON_DOOR_CLOSE : Sound.BLOCK_IRON_DOOR_OPEN;
                    plugin.getMsg().sendActionBar(p, msg);
                    p.playSound(loc, sound, 0.5f, newState ? 0.5f : 2);
                    return;
                }

                if (info.isLocked) {
                    if (playerHasKey) {
                        plugin.getMsg().sendActionBar(p, "opened_key_inv");
                    } else {
                        e.setCancelled(true);
                        if (isLockpick(hand)) {
                            int level = 1;
                            if (info.lockItem.hasItemMeta()) {
                                level = info.lockItem.getItemMeta().getPersistentDataContainer().getOrDefault(ItemUtils.getNsKey(ItemUtils.LOCK_LEVEL_TAG), PersistentDataType.INTEGER, 1);
                            }

                            if (level >= 6) {
                                plugin.getMsg().send(p, "lock_unpickable");
                                p.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
                                return;
                            }

                            LockIT.getInstance().getLockpickManager().startGame(p, loc, level);
                            return;
                        }
                        plugin.getMsg().sendActionBar(p, "locked_msg");
                        p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
                        if (!type.name().contains("DOOR") && !type.name().contains("GATE")) {
                            p.playSound(loc, Sound.BLOCK_CHEST_LOCKED, 1, 1);
                        }
                    }
                }
            }
        }
    }


    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(this::isProtectedBlock);
        enforceLockedStateInRadius(e.getLocation(), 4);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(this::isProtectedBlock);
        enforceLockedStateInRadius(e.getBlock().getLocation(), 4);
    }

    @EventHandler
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent e) {

        if (e.getEntity().getType().name().equals("WIND_CHARGE") || e.getEntity().getType().name().equals("BREEZE_WIND_CHARGE")) {
            enforceLockedStateInRadius(e.getEntity().getLocation(), 4);
        }
    }

    private boolean isProtectedBlock(Block block) {
        if (isLockableBlock(block.getType())) {
            LockInfo info = LockIT.getInstance().getLockManager().getLock(getRealBlockLocation(block));
            if (info != null) return true;

            Block partner = getDoubleChestPartner(block);
            if (partner != null) {
                LockInfo partnerInfo = LockIT.getInstance().getLockManager().getLock(getRealBlockLocation(partner));
                if (partnerInfo != null) return true;
            }
        }

        Block blockAbove = block.getRelative(org.bukkit.block.BlockFace.UP);
        if (blockAbove.getType().name().contains("DOOR")) {
            Location locAbove = getRealBlockLocation(blockAbove);
            LockInfo infoAbove = LockIT.getInstance().getLockManager().getLock(locAbove);
            if (infoAbove != null) return true;
        }

        return false;
    }

    private void enforceLockedStateInRadius(Location center, int radius) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block b = center.clone().add(x, y, z).getBlock();

                        if (isLockableBlock(b.getType())) {
                            Location blockLoc = getRealBlockLocation(b);
                            LockInfo info = LockIT.getInstance().getLockManager().getLock(blockLoc);


                            if (info != null && info.isLocked) {
                                closeIfOpen(b);
                            }
                        }
                    }
                }
            }
        }, 1L);
    }

    // =====================================================================

    @EventHandler
    public void onEntityInteract(EntityInteractEvent e) {
        Block block = e.getBlock();
        if (block == null) return;
        if (isLockableBlock(block.getType())) {
            Location loc = getRealBlockLocation(block);
            LockInfo info = LockIT.getInstance().getLockManager().getLock(loc);
            if (info != null && info.isLocked) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        Block block = e.getBlock();
        if (isLockableBlock(block.getType())) {
            Location loc = getRealBlockLocation(block);
            LockInfo info = LockIT.getInstance().getLockManager().getLock(loc);
            if (info != null && info.isLocked) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onRedstoneInteract(BlockRedstoneEvent e) {
        Block block = e.getBlock();
        if (isLockableBlock(block.getType())) {
            Location loc = getRealBlockLocation(block);
            LockInfo info = LockIT.getInstance().getLockManager().getLock(loc);
            if (info != null && info.isLocked) e.setNewCurrent(0);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        if (e.getSource().getLocation() != null) {
            Block block = e.getSource().getLocation().getBlock();
            if (isLockableBlock(block.getType())) {
                Location loc = getRealBlockLocation(block);
                LockInfo info = LockIT.getInstance().getLockManager().getLock(loc);
                if (info != null && info.isLocked) e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block block : e.getBlocks()) {
            if (isProtectedBlock(block)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block block : e.getBlocks()) {
            if (isProtectedBlock(block)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private void handleGrindstoneClear(PlayerInteractEvent e) {
        ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
        String currentId = ItemUtils.getNBT(hand, ItemUtils.KEY_ID_TAG);
        if (currentId != null && !currentId.equals("blank")) {
            e.setCancelled(true);
            ItemMeta meta = hand.getItemMeta();
            meta.getPersistentDataContainer().remove(ItemUtils.getNsKey(ItemUtils.KEY_ID_TAG));
            meta.lore(null);
            hand.setItemMeta(meta);
            e.getPlayer().playSound(e.getClickedBlock().getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1, 1);
            plugin.getMsg().send(e.getPlayer(), "item_cleaned");
        }
    }

    private void handleKeyCreation(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        String keyMaterialName = LockIT.getInstance().getItemConfig().getConfig().getString("key.material", "NAME_TAG");
        if (item != null && item.getType().name().equals(keyMaterialName)) {
            String currentId = ItemUtils.getNBT(item, ItemUtils.KEY_ID_TAG);
            if (currentId == null || "blank".equals(currentId)) {
                e.setCancelled(true);
                String newId = UUID.randomUUID().toString().substring(0, 8);
                ItemMeta meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(ItemUtils.getNsKey(ItemUtils.KEY_ID_TAG), PersistentDataType.STRING, newId);
                meta.lore(java.util.Collections.singletonList(Component.text(newId).color(NamedTextColor.GRAY)));
                item.setItemMeta(meta);
                plugin.getMsg().send(e.getPlayer(), "key_forged", "%id%", newId);
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
            }
        }
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (isLockableBlock(type)) {
            Location loc = getRealBlockLocation(block);
            LockInfo info = LockIT.getInstance().getLockManager().getLock(loc);
            Player p = e.getPlayer();
            if (info != null) {
                String lockId = ItemUtils.getNBT(info.lockItem, ItemUtils.KEY_ID_TAG);
                ItemStack hand = p.getInventory().getItemInMainHand();

                boolean isAuthorized = checkKeyItem(hand, lockId) || isUniversalKey(hand);

                if (p.isSneaking() && isAuthorized) {
                    e.setCancelled(true);
                    LockIT.getInstance().getLockManager().setLock(loc, null);
                    java.util.HashMap<Integer, ItemStack> leftOver = p.getInventory().addItem(info.lockItem);
                    if (!leftOver.isEmpty()) p.getWorld().dropItemNaturally(p.getLocation(), info.lockItem);
                    plugin.getMsg().send(p, "lock_removed");
                    p.playSound(loc, Sound.ENTITY_ITEM_BREAK, 1, 1);
                    return;
                }
                if (p.getGameMode() != GameMode.CREATIVE) {
                    e.setCancelled(true);
                    plugin.getMsg().send(p, "must_be_sneaking");
                }
            }
        }
    }


    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (isProtectedBlock(e.getBlock())) {
            e.setCancelled(true);
            plugin.getMsg().send(e.getPlayer(), "locked_break_attempt");
        }
    }

    private boolean isLockableBlock(Material mat) {
        String name = mat.name();
        return name.contains("DOOR") || name.contains("CHEST") || name.contains("BARREL") || name.contains("SHULKER") || name.contains("GATE") || name.contains("TRAPDOOR");
    }

    private boolean isLockItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(ItemUtils.getNsKey(ItemUtils.LOCK_LEVEL_TAG), PersistentDataType.INTEGER);
    }

    private boolean isLockpick(ItemStack item) {
        return item != null && item.getType().name().equals(LockIT.getInstance().getItemConfig().getConfig().getString("lockpick.material", "STICK")) && item.hasItemMeta() && item.getItemMeta().getCustomModelData() == LockIT.getInstance().getItemConfig().getConfig().getInt("lockpick.custom_model_data", 2633);
    }

    private boolean hasKey(Player p, String lockId) {
        if (lockId == null) return true;
        if (checkKeyItem(p.getInventory().getItemInMainHand(), lockId) || isUniversalKey(p.getInventory().getItemInMainHand())) return true;
        for (ItemStack item : p.getInventory().getContents()) {
            if (checkKeyItem(item, lockId) || isUniversalKey(item)) return true;
        }
        return false;
    }

    private boolean isUniversalKey(ItemStack item) {
        return "true".equals(ItemUtils.getNBT(item, "universal"));
    }

    private boolean checkKeyItem(ItemStack item, String lockId) {
        if (item == null || item.getType() == Material.AIR) return false;
        String keyMat = LockIT.getInstance().getItemConfig().getConfig().getString("key.material", "NAME_TAG");
        if (!item.getType().name().equals(keyMat)) return false;
        String keyId = ItemUtils.getNBT(item, ItemUtils.KEY_ID_TAG);
        return lockId != null && lockId.equals(keyId);
    }
}