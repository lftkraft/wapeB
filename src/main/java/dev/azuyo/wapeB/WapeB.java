package dev.azuyo.wapeB;

import dev.azuyo.wapeB.api.WapeBAPI;
import dev.azuyo.wapeB.api.WapeBAPIImpl;
import dev.azuyo.wapeB.commands.*;
import dev.azuyo.wapeB.importers.ImportManager;
import dev.azuyo.wapeB.listeners.*;
import dev.azuyo.wapeB.managers.*;
import dev.azuyo.wapeB.utils.TimeUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class WapeB extends JavaPlugin {

    private static WapeB instance;
    private static WapeBAPI api;
    private DataManager dataManager;
    private ConfigManager configManager;
    private LockdownManager lockdownManager;
    private PlayerDataManager playerDataManager;
    private FreezeManager freezeManager;
    private WebAPIManager webAPIManager;
    private SentinelManager sentinelManager;
    private CommandManager commandManager;
    private TemplateManager templateManager;
    private PluginMessageManager pluginMessageManager;
    private ChatSnapshotManager chatSnapshotManager;
    private ImportManager importManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize Template Manager
        templateManager = new TemplateManager(this);

        // Initialize Chat Snapshot Manager
        chatSnapshotManager = new ChatSnapshotManager(this);

        // Initialize Plugin Message Manager for Velocity proxy communication
        pluginMessageManager = new PluginMessageManager(this);

        // Initialize TimeUtil from config
        ConfigurationSection timeSection = configManager.getConfigurationSection("time-formats");
        if (timeSection != null) {
            TimeUtil.init(
                timeSection.getString("permanent"),
                timeSection.getString("year"),
                timeSection.getString("week"),
                timeSection.getString("day"),
                timeSection.getString("hour"),
                timeSection.getString("minute"),
                timeSection.getString("second")
            );
        }

        // Initialize data manager based on config
        String storageMethod = configManager.getString("storage-method", "sqlite");
        if (storageMethod.equalsIgnoreCase("mysql") || storageMethod.equalsIgnoreCase("mariadb")) {
            dataManager = new MysqlDataManager(this);
        } else if (storageMethod.equalsIgnoreCase("sqlite")) {
            dataManager = new SqliteDataManager(this);
        } else {
            dataManager = new YamlDataManager(this);
        }

        // Initialize Lockdown Manager
        lockdownManager = new LockdownManager(this);

        // Initialize Player Data Manager
        playerDataManager = new PlayerDataManager(this);

        // Initialize Import Manager
        importManager = new ImportManager(this);

        // Initialize Freeze Manager
        freezeManager = new FreezeManager(this);

        // Initialize Web API Manager
        webAPIManager = new WebAPIManager(this);

        // Initialize Sentinel Manager
        sentinelManager = new SentinelManager(this);

        // Initialize Command Manager & API
        commandManager = new CommandManager(this);
        api = new WapeBAPIImpl(this, commandManager);

        // Register commands and listeners
        registerCommands();
        registerListeners();

        getLogger().info("wapeB has been enabled!");
    }

    @Override
    public void onDisable() {
        if (webAPIManager != null) {
            webAPIManager.stopServer();
        }
        if (dataManager != null) {
            dataManager.close();
        }
        getLogger().info("wapeB has been disabled!");
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("ban")).setExecutor(new BanCommand(this));
        Objects.requireNonNull(getCommand("banip")).setExecutor(new BanIpCommand(this));
        Objects.requireNonNull(getCommand("kick")).setExecutor(new KickCommand(this));
        Objects.requireNonNull(getCommand("kickall")).setExecutor(new KickallCommand(this));
        Objects.requireNonNull(getCommand("mute")).setExecutor(new MuteCommand(this));
        Objects.requireNonNull(getCommand("muteip")).setExecutor(new MuteIpCommand(this));
        ShadowMuteCommand shadowMuteCommand = new ShadowMuteCommand(this);
        if (getCommand("shadowmute") != null) {
            Objects.requireNonNull(getCommand("shadowmute")).setExecutor(shadowMuteCommand);
            Objects.requireNonNull(getCommand("shadowmute")).setTabCompleter(shadowMuteCommand);
        }
        if (getCommand("smute") != null) {
            Objects.requireNonNull(getCommand("smute")).setExecutor(shadowMuteCommand);
            Objects.requireNonNull(getCommand("smute")).setTabCompleter(shadowMuteCommand);
        }
        if (getCommand("tempshadowmute") != null) {
            Objects.requireNonNull(getCommand("tempshadowmute")).setExecutor(shadowMuteCommand);
            Objects.requireNonNull(getCommand("tempshadowmute")).setTabCompleter(shadowMuteCommand);
        }
        if (getCommand("tsmute") != null) {
            Objects.requireNonNull(getCommand("tsmute")).setExecutor(shadowMuteCommand);
            Objects.requireNonNull(getCommand("tsmute")).setTabCompleter(shadowMuteCommand);
        }
        ShadowMuteIpCommand shadowMuteIpCommand = new ShadowMuteIpCommand(this);
        if (getCommand("shadowmuteip") != null) {
            Objects.requireNonNull(getCommand("shadowmuteip")).setExecutor(shadowMuteIpCommand);
            Objects.requireNonNull(getCommand("shadowmuteip")).setTabCompleter(shadowMuteIpCommand);
        }
        if (getCommand("smuteip") != null) {
            Objects.requireNonNull(getCommand("smuteip")).setExecutor(shadowMuteIpCommand);
            Objects.requireNonNull(getCommand("smuteip")).setTabCompleter(shadowMuteIpCommand);
        }
        if (getCommand("tempshadowmuteip") != null) {
            Objects.requireNonNull(getCommand("tempshadowmuteip")).setExecutor(shadowMuteIpCommand);
            Objects.requireNonNull(getCommand("tempshadowmuteip")).setTabCompleter(shadowMuteIpCommand);
        }
        Objects.requireNonNull(getCommand("unban")).setExecutor(new UnbanCommand(this));
        Objects.requireNonNull(getCommand("unmute")).setExecutor(new UnmuteCommand(this));
        Objects.requireNonNull(getCommand("warn")).setExecutor(new WarnCommand(this));
        Objects.requireNonNull(getCommand("unwarn")).setExecutor(new UnwarnCommand(this));
        Objects.requireNonNull(getCommand("warnings")).setExecutor(new WarningsCommand(this));
        Objects.requireNonNull(getCommand("history")).setExecutor(new HistoryCommand(this));
        Objects.requireNonNull(getCommand("checkban")).setExecutor(new CheckBanCommand(this));
        Objects.requireNonNull(getCommand("checkmute")).setExecutor(new CheckMuteCommand(this));
        Objects.requireNonNull(getCommand("freeze")).setExecutor(new FreezeCommand(this));
        Objects.requireNonNull(getCommand("unfreeze")).setExecutor(new FreezeCommand(this)); 
        Objects.requireNonNull(getCommand("alts")).setExecutor(new AltsCommand(this));
        Objects.requireNonNull(getCommand("altexempt")).setExecutor(new AltExemptCommand(this));
        Objects.requireNonNull(getCommand("banlist")).setExecutor(new BanlistCommand(this));
        Objects.requireNonNull(getCommand("staffhistory")).setExecutor(new StaffHistoryCommand(this));
        Objects.requireNonNull(getCommand("lockdown")).setExecutor(new LockdownCommand(this));
        Objects.requireNonNull(getCommand("wapeb")).setExecutor(new WapebCommand(this));
        Objects.requireNonNull(getCommand("globalunban")).setExecutor(new GlobalUnbanCommand(this));
        Objects.requireNonNull(getCommand("punish")).setExecutor(new PunishCommand(this));
        Objects.requireNonNull(getCommand("punish-rollback")).setExecutor(new PunishRollbackCommand(this));
        ProofCommand proofCommand = new ProofCommand(this);
        Objects.requireNonNull(getCommand("punish-proof")).setExecutor(proofCommand);
        Objects.requireNonNull(getCommand("punish-proof")).setTabCompleter(proofCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
    }

    public static WapeB getInstance() {
        return instance;
    }

    public static WapeBAPI getApi() {
        return api;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LockdownManager getLockdownManager() {
        return lockdownManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public FileConfiguration getConfigFile() {
        return configManager.getConfig();
    }

    public FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public WebAPIManager getWebAPIManager() {
        return webAPIManager;
    }

    public SentinelManager getSentinelManager() {
        return sentinelManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    public PluginMessageManager getPluginMessageManager() {
        return pluginMessageManager;
    }

    public ChatSnapshotManager getChatSnapshotManager() {
        return chatSnapshotManager;
    }

    public ImportManager getImportManager() {
        return importManager;
    }
}
