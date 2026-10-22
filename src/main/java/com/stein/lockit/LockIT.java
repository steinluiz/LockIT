package com.stein.lockit;

import com.stein.lockit.commands.LockCommand;
import com.stein.lockit.config.CraftingManager;
import com.stein.lockit.config.ItemConfigManager;
import com.stein.lockit.config.LockpickConfigManager;
import com.stein.lockit.config.MessageManager;
import com.stein.lockit.config.TrapConfigManager;
import com.stein.lockit.listeners.AnvilListener;
import com.stein.lockit.listeners.InteractionListener;
import com.stein.lockit.listeners.InventoryListener;
import com.stein.lockit.managers.LockManager;
import com.stein.lockit.managers.LockpickManager;
import com.stein.lockit.managers.ResourcePackManager;
import com.stein.lockit.managers.TrapManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;


public final class LockIT extends JavaPlugin {

    private static LockIT instance;
    private LockManager lockManager;
    private MessageManager messageManager;
    private ItemConfigManager itemConfigManager;
    private LockpickConfigManager lockpickConfigManager;
    private TrapConfigManager trapConfigManager;
    private TrapManager trapManager;
    private LockpickManager lockpickManager;
    private CraftingManager craftingManager;
    private com.stein.lockit.managers.LockGuiManager lockGuiManager;
    private ResourcePackManager resourcePackManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        setupMainConfig();

        this.messageManager = new MessageManager(this);
        this.itemConfigManager = new ItemConfigManager(this);
        this.lockpickConfigManager = new LockpickConfigManager(this);
        this.trapConfigManager = new TrapConfigManager(this);
        this.trapManager = new TrapManager(this);
        this.lockManager = new LockManager(this);
        this.lockpickManager = new LockpickManager(this);
        this.craftingManager = new CraftingManager(this);
        this.lockGuiManager = new com.stein.lockit.managers.LockGuiManager(this);
        this.resourcePackManager = new ResourcePackManager(this);

        PluginCommand cmd = getCommand("lockit");
        if (cmd != null) {
            cmd.setExecutor(new LockCommand(this));
        }

        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
        getServer().getPluginManager().registerEvents(resourcePackManager, this);

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
    public TrapConfigManager getTrapConfig() { return trapConfigManager; }
    public TrapManager getTrapManager() { return trapManager; }
    public LockpickManager getLockpickManager() { return lockpickManager; }
    public CraftingManager getCraftingManager() { return craftingManager; }
    public com.stein.lockit.managers.LockGuiManager getLockGuiManager() { return lockGuiManager; }
    public ResourcePackManager getResourcePackManager() { return resourcePackManager; }

    // Resource packs shipped by older LockIT versions; configs still using one get the current pack.
    private static final java.util.Set<String> OLD_PACK_SHA1S = java.util.Set.of(
            "633b450eb4155f5c73fd89376b3fdc857f0c847a",
            "93f87a83dc6c6b25b34628191e90e9c8464cf40a",
            "8fc78b21b00d868ed47b962db849ec5ffd144bce",
            "d0b3f9514d7609ea4139d117c48098a21cd05d4a");

    private void setupMainConfig() {
        saveDefaultConfig();      
        reloadConfig();           
        getConfig().options().copyDefaults(true);
        upgradeResourcePack();
        saveConfig();             
    }

    private void upgradeResourcePack() {
        String sha1 = getConfig().getString("resource-pack.sha1", "").trim().toLowerCase();
        if (!OLD_PACK_SHA1S.contains(sha1) || getConfig().getDefaults() == null) return;
        for (String key : new String[]{"url", "sha1", "id"}) {
            getConfig().set("resource-pack." + key, getConfig().getDefaults().getString("resource-pack." + key));
        }
        getLogger().info("Updated resource-pack in config.yml to the LockIT " + getPluginMeta().getVersion() + " pack.");
    }

    public void reloadConfigs() {
        setupMainConfig();
        messageManager.load();
        itemConfigManager.load();
        lockpickConfigManager.load();
        trapConfigManager.load();
        craftingManager.load();
    }
}