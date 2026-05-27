package com.stein.lockit;

import com.stein.lockit.commands.LockCommand;
import com.stein.lockit.config.CraftingManager;
import com.stein.lockit.config.ItemConfigManager;
import com.stein.lockit.config.LockpickConfigManager;
import com.stein.lockit.config.MessageManager;
import com.stein.lockit.listeners.InteractionListener;
import com.stein.lockit.listeners.InventoryListener;
import com.stein.lockit.managers.LockManager;
import com.stein.lockit.managers.LockpickManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;


public final class LockIT extends JavaPlugin {

    private static LockIT instance;
    private LockManager lockManager;
    private MessageManager messageManager;
    private ItemConfigManager itemConfigManager;
    private LockpickConfigManager lockpickConfigManager;
    private LockpickManager lockpickManager;
    private CraftingManager craftingManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        this.messageManager = new MessageManager(this);
        this.itemConfigManager = new ItemConfigManager(this);
        this.lockpickConfigManager = new LockpickConfigManager(this);
        this.lockManager = new LockManager(this);
        this.lockpickManager = new LockpickManager(this);
        this.craftingManager = new CraftingManager(this);

        PluginCommand cmd = getCommand("lockit");
        if (cmd != null) {
            cmd.setExecutor(new LockCommand(this));
        }

        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        getLogger().info("LockIT loaded successfully!");

        com.stein.lockit.utils.UpdateChecker updateChecker = new com.stein.lockit.utils.UpdateChecker(this, "lock-it");
        updateChecker.checkForUpdates();
        getServer().getPluginManager().registerEvents(updateChecker, this);
    }

    @Override
    public void onDisable() {
        if (lockManager != null) lockManager.saveAll();
    }

    public static LockIT getInstance() { return instance; }
    public LockManager getLockManager() { return lockManager; }
    public MessageManager getMsg() { return messageManager; }
    public ItemConfigManager getItemConfig() { return itemConfigManager; }
    public LockpickConfigManager getLockpickConfig() { return lockpickConfigManager; }
    public LockpickManager getLockpickManager() { return lockpickManager; }
}