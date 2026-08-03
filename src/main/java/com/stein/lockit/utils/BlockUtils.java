package com.stein.lockit.utils;

import com.stein.lockit.LockIT;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.inventory.InventoryHolder;

public final class BlockUtils {

    private BlockUtils() {}

    /** When true, a double door is secured by a single lock shared by both wings. */
    public static boolean isDoubleDoorSingleLock() {
        return LockIT.getInstance().getConfig().getBoolean("double-door-single-lock", true);
    }

    /** Lower half of a bisected block (door, tall block); the block itself otherwise. */
    public static Block getBaseBlock(Block b) {
        BlockData data = b.getBlockData();
        if (data instanceof Bisected && ((Bisected) data).getHalf() == Bisected.Half.TOP) {
            return b.getRelative(0, -1, 0);
        }
        return b;
    }

    /**
     * The other wing of a double door, or null when this block is not part of one.
     * Two doors are a pair when they share a facing and their hinges sit on the
     * outer sides — exactly what vanilla's hinge flip produces when a door is
     * placed next to another one. Trapdoors are a different block data type and
     * never match.
     */
    public static Block getDoubleDoorPartner(Block b) {
        Block base = getBaseBlock(b);
        BlockData data = base.getBlockData();
        if (!(data instanceof Door)) return null;
        Door door = (Door) data;

        // Hinge on the left => the partner sits clockwise from the facing, and vice versa.
        BlockFace side = door.getHinge() == Door.Hinge.LEFT
                ? rotateClockwise(door.getFacing())
                : rotateCounterClockwise(door.getFacing());
        if (side == null) return null;

        Block other = base.getRelative(side);
        BlockData otherData = other.getBlockData();
        if (!(otherData instanceof Door)) return null;
        Door otherDoor = (Door) otherData;

        if (otherDoor.getHalf() != Bisected.Half.BOTTOM) return null;
        if (otherDoor.getFacing() != door.getFacing()) return null;
        if (otherDoor.getHinge() == door.getHinge()) return null;
        return other;
    }

    /**
     * Location the lock of this block is stored under. Halves of a door, of a
     * double chest, and (when enabled) the two wings of a double door all resolve
     * to the same location so they share one lock.
     */
    public static Location getLockLocation(Block b) {
        Block base = getBaseBlock(b);

        // Double chest: both halves resolve to the left side.
        if (base.getState() instanceof Chest) {
            InventoryHolder holder = ((Chest) base.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) {
                return ((Chest) ((DoubleChest) holder).getLeftSide()).getBlock().getLocation();
            }
        }

        if (isDoubleDoorSingleLock()) {
            Block partner = getDoubleDoorPartner(base);
            if (partner != null) {
                Location own = base.getLocation();
                Location other = partner.getLocation();
                // Keep a lock where it already lives: data written while the option was
                // off, or the wing the player installed it from.
                if (LockIT.getInstance().getLockManager().getLock(own) != null) return own;
                if (LockIT.getInstance().getLockManager().getLock(other) != null) return other;
                return lowest(own, other);
            }
        }

        return base.getLocation();
    }

    /** Deterministic pick so both wings agree on one location regardless of hinge state. */
    private static Location lowest(Location a, Location b) {
        if (a.getBlockX() != b.getBlockX()) return a.getBlockX() < b.getBlockX() ? a : b;
        if (a.getBlockZ() != b.getBlockZ()) return a.getBlockZ() < b.getBlockZ() ? a : b;
        return a;
    }

    private static BlockFace rotateClockwise(BlockFace face) {
        switch (face) {
            case NORTH: return BlockFace.EAST;
            case EAST: return BlockFace.SOUTH;
            case SOUTH: return BlockFace.WEST;
            case WEST: return BlockFace.NORTH;
            default: return null;
        }
    }

    private static BlockFace rotateCounterClockwise(BlockFace face) {
        switch (face) {
            case NORTH: return BlockFace.WEST;
            case WEST: return BlockFace.SOUTH;
            case SOUTH: return BlockFace.EAST;
            case EAST: return BlockFace.NORTH;
            default: return null;
        }
    }
}
