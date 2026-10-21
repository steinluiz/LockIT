package com.stein.lockit.managers;

import com.stein.lockit.LockIT;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

/**
 * Sends the LockIT resource pack on join and tracks who has it loaded.
 * Menus only use the textured titles for players that actually loaded the pack.
 */
public class ResourcePackManager implements Listener {
    private static final Key GUI_FONT = Key.key("lockit", "gui");
    // Glyphs from assets/lockit/font/gui.json: a -8 px space (cancels the title's x offset) + the background.
    private static final String LOCK_GUI = "\uF808\uE000";
    private static final String LOCKPICK_GUI = "\uF808\uE001";

    private final LockIT plugin;
    private final Set<UUID> loaded = new HashSet<>();

    public ResourcePackManager(LockIT plugin) {
        this.plugin = plugin;
    }

    private ConfigurationSection config() {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("resource-pack");
        return s != null ? s : plugin.getConfig().createSection("resource-pack");
    }

    public boolean isEnabled() {
        return config().getBoolean("enabled", true) && !config().getString("url", "").isBlank();
    }

    public boolean hasTextures(Player p) {
        return isEnabled() && loaded.contains(p.getUniqueId());
    }

    public void send(Player p) {
        if (!isEnabled()) return;
        ConfigurationSection c = config();
        try {
            UUID id = UUID.fromString(c.getString("id", ""));
            byte[] hash = HexFormat.of().parseHex(c.getString("sha1", "").trim());
            p.setResourcePack(id, c.getString("url"), hash, parsePrompt(c.getString("prompt", "")), c.getBoolean("required", true));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid resource-pack id or sha1 in config.yml: " + ex.getMessage());
        }
    }

    private Component parsePrompt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.trim().startsWith("{")) {
            try {
                return GsonComponentSerializer.gson().deserialize(raw);
            } catch (RuntimeException ignored) { }
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    public Component lockGuiTitle(Player p, String fallback) {
        if (hasTextures(p)) return Component.text(LOCK_GUI, NamedTextColor.WHITE).font(GUI_FONT);
        return LegacyComponentSerializer.legacySection().deserialize(fallback);
    }

    public Component lockpickGuiTitle(Player p, Component fallback) {
        if (hasTextures(p)) return Component.text(LOCKPICK_GUI, NamedTextColor.WHITE).font(GUI_FONT);
        return fallback;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        send(e.getPlayer());
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent e) {
        // Ignore other packs (e.g. one from server.properties or another plugin).
        if (!config().getString("id", "").equalsIgnoreCase(String.valueOf(e.getID()))) return;
        UUID player = e.getPlayer().getUniqueId();
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> loaded.add(player);
            case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> loaded.remove(player);
            default -> { }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        loaded.remove(e.getPlayer().getUniqueId());
    }
}
