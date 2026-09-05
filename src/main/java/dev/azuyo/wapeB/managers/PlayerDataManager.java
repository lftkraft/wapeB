package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.AltInfo;
import dev.azuyo.wapeB.utils.IPUtil;
import dev.azuyo.wapeB.utils.IpHistoryRecord;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        playerDataConfig.set(playerUuid.toString() + ".lastSeen", System.currentTimeMillis());
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerUuid);
        if (op.getName() != null) {
            playerDataConfig.set(playerUuid.toString() + ".lastName", op.getName());
        }
        save();
    }

    public String getLastKnownIp(UUID playerUuid) {
        return playerDataConfig.getString(playerUuid.toString() + ".lastIp");
    }

    public void recordIpHistory(UUID playerUuid, String playerName, String ipAddress) {
        if (playerUuid == null || ipAddress == null) return;

        setLastKnownIp(playerUuid, ipAddress);

        String path = playerUuid.toString() + ".ipHistory";
        List<String> history = playerDataConfig.getStringList(path);

        long now = System.currentTimeMillis();
        String entry = ipAddress + "|" + now;

        // Check if IP is already in history to update timestamp
        boolean updated = false;
        List<String> updatedHistory = new ArrayList<>();
        for (String item : history) {
            if (item.startsWith(ipAddress + "|")) {
                updatedHistory.add(entry);
                updated = true;
            } else {
                updatedHistory.add(item);
            }
        }

        if (!updated) {
            updatedHistory.add(entry);
        }

        playerDataConfig.set(path, updatedHistory);
        save();
    }

    public List<IpHistoryRecord> getIpHistory(UUID playerUuid) {
        List<IpHistoryRecord> records = new ArrayList<>();
        if (playerUuid == null) return records;

        List<String> rawHistory = playerDataConfig.getStringList(playerUuid.toString() + ".ipHistory");
        for (String item : rawHistory) {
            String[] parts = item.split("\\|");
            if (parts.length == 2) {
                try {
                    String ip = parts[0];
                    long time = Long.parseLong(parts[1]);
                    records.add(new IpHistoryRecord(ip, time));
                } catch (NumberFormatException ignored) {}
            }
        }
        return records;
    }

    public boolean isAltExempt(UUID playerUuid) {
        if (playerUuid == null) return false;
        return playerDataConfig.getBoolean(playerUuid.toString() + ".exempt", false);
    }

    public void setAltExempt(UUID playerUuid, boolean exempt, String addedBy) {
        if (playerUuid == null) return;
        playerDataConfig.set(playerUuid.toString() + ".exempt", exempt);
        if (exempt) {
            playerDataConfig.set(playerUuid.toString() + ".exemptBy", addedBy != null ? addedBy : "Console");
            playerDataConfig.set(playerUuid.toString() + ".exemptDate", System.currentTimeMillis());
        } else {
            playerDataConfig.set(playerUuid.toString() + ".exemptBy", null);
            playerDataConfig.set(playerUuid.toString() + ".exemptDate", null);
        }
        save();
    }

    public List<UUID> getPlayersByIp(String ipAddress) {
        return getPlayersByIp(ipAddress, plugin.getConfigManager().getBoolean("alts.cidr-matching", true));
    }

    public List<UUID> getPlayersByIp(String ipAddress, boolean includeCidr) {
        Set<UUID> players = new HashSet<>();
        ConfigurationSection section = playerDataConfig.getConfigurationSection("");
        if (section == null || ipAddress == null) return new ArrayList<>(players);

        for (String uuidString : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                String storedIp = playerDataConfig.getString(uuidString + ".lastIp");
                if (storedIp != null) {
                    if (storedIp.equalsIgnoreCase(ipAddress)) {
                        players.add(uuid);
                    } else if (includeCidr && IPUtil.isIpInCidr(ipAddress, storedIp)) {
                        players.add(uuid);
                    }
                }

                // Check historical IPs
                List<String> rawHistory = playerDataConfig.getStringList(uuidString + ".ipHistory");
                for (String item : rawHistory) {
                    String[] parts = item.split("\\|");
                    if (parts.length >= 1) {
                        String histIp = parts[0];
                        if (histIp.equalsIgnoreCase(ipAddress)) {
                            players.add(uuid);
                        } else if (includeCidr && IPUtil.isIpInCidr(ipAddress, histIp)) {
                            players.add(uuid);
                        }
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return new ArrayList<>(players);
    }

    public List<AltInfo> getDetailedAlts(UUID targetUuid) {
        List<AltInfo> result = new ArrayList<>();
        if (targetUuid == null) return result;

        String targetIp = getLastKnownIp(targetUuid);
        if (targetIp == null || targetIp.isEmpty()) return result;

        boolean cidrEnabled = plugin.getConfigManager().getBoolean("alts.cidr-matching", true);

        ConfigurationSection section = playerDataConfig.getConfigurationSection("");
        if (section == null) return result;

        for (String uuidString : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                if (uuid.equals(targetUuid)) continue;

                String storedIp = playerDataConfig.getString(uuidString + ".lastIp");
                long lastSeen = playerDataConfig.getLong(uuidString + ".lastSeen", 0);
                String lastName = playerDataConfig.getString(uuidString + ".lastName", Bukkit.getOfflinePlayer(uuid).getName());

                AltInfo.MatchType matchType = null;

                if (storedIp != null && storedIp.equalsIgnoreCase(targetIp)) {
                    matchType = AltInfo.MatchType.EXACT_IP;
                } else if (storedIp != null && cidrEnabled && IPUtil.isIpInCidr(targetIp, storedIp)) {
                    matchType = AltInfo.MatchType.CIDR_SUBNET;
                } else {
                    // Check historical IPs
                    List<String> rawHistory = playerDataConfig.getStringList(uuidString + ".ipHistory");
                    for (String item : rawHistory) {
                        String[] parts = item.split("\\|");
                        if (parts.length >= 1) {
                            String histIp = parts[0];
                            if (histIp.equalsIgnoreCase(targetIp)) {
                                matchType = AltInfo.MatchType.EXACT_IP;
                                break;
                            } else if (cidrEnabled && IPUtil.isIpInCidr(targetIp, histIp)) {
                                matchType = AltInfo.MatchType.CIDR_SUBNET;
                            }
                        }
                    }
                }

                if (matchType != null) {
                    Punishment activeBan = plugin.getApi().getActiveBan(uuid);
                    Punishment activeMute = plugin.getApi().getActiveMute(uuid);
                    boolean isExempt = isAltExempt(uuid);

                    result.add(new AltInfo(uuid, lastName, storedIp, lastSeen, matchType, activeBan, activeMute, isExempt));
                }
            } catch (IllegalArgumentException ignored) {}
        }

        // Sort by last seen descending
        result.sort((a, b) -> Long.compare(b.getLastSeen(), a.getLastSeen()));
        return result;
    }
}
