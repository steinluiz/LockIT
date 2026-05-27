package com.stein.lockit.utils;

import com.stein.lockit.LockIT;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker implements Listener {

    private final LockIT plugin;
    private final String projectId;
    private boolean isUpdateAvailable = false;
    private String latestVersion = "";

    public UpdateChecker(LockIT plugin, String projectId) {
        this.plugin = plugin;
        this.projectId = projectId;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/" + projectId + "/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Stein/LockIT/" + plugin.getDescription().getVersion());

                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();

                    if (!versions.isEmpty()) {
                        latestVersion = versions.get(0).getAsJsonObject().get("version_number").getAsString();
                        String currentVersion = plugin.getDescription().getVersion();

                        if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                            isUpdateAvailable = true;
                            plugin.getLogger().warning("========================================");
                            plugin.getLogger().warning("A new version of LockIT is available!");
                            plugin.getLogger().warning("Current version: " + currentVersion);
                            plugin.getLogger().warning("Latest version: " + latestVersion);
                            plugin.getLogger().warning("Download it at: https://modrinth.com/plugin/" + projectId);
                            plugin.getLogger().warning("========================================");
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates on Modrinth.");
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (isUpdateAvailable && (p.isOp() || p.hasPermission("lockit.admin"))) {

            Component message = Component.text("\n[LockIT] A new update is available! (" + latestVersion + ")\n")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("Click here to download it.")
                            .color(NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.openUrl("https://modrinth.com/plugin/" + projectId)))
                    .append(Component.text("\n"));

            p.sendMessage(message);
        }
    }
}