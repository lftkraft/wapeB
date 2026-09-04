package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDataManager {

    private final WapeB plugin;
    private File playerDataFile;
    private FileConfiguration playerDataConfig;

    public PlayerDataManager(WapeB plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml!");
                e.printStackTrace();
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    private void save() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml!");
            e.printStackTrace();
        }
    }

    public void setLastKnownIp(UUID playerUuid, String ipAddress) {
        playerDataConfig.set(playerUuid.toString() + ".lastIp", ipAddress);
        save();
    }

    public String getLastKnownIp(UUID playerUuid) {
        return playerDataConfig.getString(playerUuid.toString() + ".lastIp");
    }

    public List<UUID> getPlayersByIp(String ipAddress) {
        List<UUID> players = new ArrayList<>();
        ConfigurationSection section = playerDataConfig.getConfigurationSection(""); // Root section
        if (section == null) return players;

        for (String uuidString : section.getKeys(false)) {
            String storedIp = playerDataConfig.getString(uuidString + ".lastIp");
            if (storedIp != null && storedIp.equals(ipAddress)) {
                players.add(UUID.fromString(uuidString));
            }
        }
        return players;
    }
}
