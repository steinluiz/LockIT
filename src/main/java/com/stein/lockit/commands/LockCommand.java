package com.stein.lockit.commands;

import com.stein.lockit.LockIT;
import com.stein.lockit.utils.ItemUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LockCommand implements CommandExecutor, TabCompleter {
    private final LockIT plugin;

    public LockCommand(LockIT plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMsg().send(sender, "only_players");
            return true;
        }

        if (!sender.hasPermission("lockit.admin")) {
            plugin.getMsg().send(sender, "no_permission");
            return true;
        }

        Player p = (Player) sender;
        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "universalkey": p.getInventory().addItem(ItemUtils.getUniversalKey()); break;
            case "key": p.getInventory().addItem(ItemUtils.getKey(null)); break;
            case "customkey":
                if (args.length > 1) p.getInventory().addItem(ItemUtils.getKey(args[1]));
                else plugin.getMsg().send(p, "cmd_usage_key"); break;
            case "lockpick": p.getInventory().addItem(ItemUtils.getLockpick()); break;
            case "reload":
                plugin.reloadConfigs();
                plugin.getMsg().send(p, "reload_success");
                break;
            case "lock":
                if (args.length > 1) {
                    try {
                        int level = Integer.parseInt(args[1]);
                        if (level < 1 || level > 6) throw new NumberFormatException();
                        p.getInventory().addItem(ItemUtils.getLock(level));
                    } catch (NumberFormatException e) { plugin.getMsg().send(p, "level_error"); }
                } else plugin.getMsg().send(p, "cmd_usage_lock"); break;
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lockit.admin")) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("universalkey");
            list.add("key");
            list.add("customkey");
            list.add("lock");
            list.add("lockpick");
            list.add("reload");
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("lock")) {
            for(int i=1; i<=6; i++) list.add(String.valueOf(i));
        }
        return list;
    }
}