package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class LockdownManager {

    private final WapeB plugin;
    private boolean lockdownEnabled;
    private String lockdownReason;
    private List<String> kickScreen;

    public LockdownManager(WapeB plugin) {
        this.plugin = plugin;
        loadLockdownState();
    }

    public void loadLockdownState() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        this.lockdownEnabled = config.getBoolean("lockdown.enabled", false);
        this.lockdownReason = config.getString("lockdown.reason", "The server is currently under lockdown.");
        this.kickScreen = config.getStringList("lockdown.kick-reason");
    }

    public void saveLockdownState() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        config.set("lockdown.enabled", this.lockdownEnabled);
        config.set("lockdown.reason", this.lockdownReason);
        plugin.getConfigManager().saveConfig(); // Use ConfigManager's saveConfig
    }

    public boolean isLockdownEnabled() {
        return lockdownEnabled;
    }

    public void setLockdownEnabled(boolean lockdownEnabled) {
        this.lockdownEnabled = lockdownEnabled;
        saveLockdownState();
    }

    public String getLockdownReason() {
        return lockdownReason;
    }

    public void setLockdownReason(String lockdownReason) {
        this.lockdownReason = lockdownReason;
        saveLockdownState();
    }

    public List<String> getKickScreen() {
        return kickScreen;
    }
}
