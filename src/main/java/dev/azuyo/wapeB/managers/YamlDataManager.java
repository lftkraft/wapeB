package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class YamlDataManager implements DataManager {

    private final WapeB plugin;
    private File punishmentsFile;
    private FileConfiguration punishmentsConfig;
    private final List<Punishment.PunishmentType> allBanTypes = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN,
            Punishment.PunishmentType.FREEZE_LOGOUT_BAN
    );

    public YamlDataManager(WapeB plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        punishmentsFile = new File(plugin.getDataFolder(), "punishments.yml");
        if (!punishmentsFile.exists()) {
            try {
                punishmentsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create punishments.yml!");
                e.printStackTrace();
            }
        }
        punishmentsConfig = YamlConfiguration.loadConfiguration(punishmentsFile);
    }

    private void save() {
        try {
            punishmentsConfig.save(punishmentsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save punishments.yml!");
            e.printStackTrace();
        }
    }

    @Override
    public int getNextId() {
        int id = 0;
        ConfigurationSection section = punishmentsConfig.getConfigurationSection("punishments");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int currentId = Integer.parseInt(key);
                    if (currentId > id) {
                        id = currentId;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return id + 1;
    }

    @Override
    public void savePunishment(Punishment punishment) {
        String path = "punishments." + punishment.getId();
        punishmentsConfig.set(path + ".playerUuid", punishment.getPlayerUuid() != null ? punishment.getPlayerUuid().toString() : null);
        punishmentsConfig.set(path + ".playerName", punishment.getPlayerName());
        punishmentsConfig.set(path + ".ipAddress", punishment.getIpAddress());
        punishmentsConfig.set(path + ".type", punishment.getType().toString());
        punishmentsConfig.set(path + ".reason", punishment.getReason());
        punishmentsConfig.set(path + ".executorName", punishment.getExecutorName());
        punishmentsConfig.set(path + ".date", punishment.getDate());
        punishmentsConfig.set(path + ".duration", punishment.getDuration());
        punishmentsConfig.set(path + ".end", punishment.getEnd());
        punishmentsConfig.set(path + ".active", punishment.isActive());
        save();
    }

    @Override
    public Punishment getPunishment(int id) {
        String path = "punishments." + id;
        if (punishmentsConfig.contains(path)) {
            return buildPunishment(path);
        }
        return null;
    }

    @Override
    public List<Punishment> getWarnings(UUID playerUuid) {
        List<Punishment> warnings = new ArrayList<>();
        ConfigurationSection section = punishmentsConfig.getConfigurationSection("punishments");
        if (section == null) return warnings;

        String expiryString = plugin.getConfigManager().getString("warning-expiry", "3d");
        long expiryMillis = expiryString.equals("0") ? -1 : TimeUtil.parseTime(expiryString);

        for (String id : section.getKeys(false)) {
            String path = "punishments." + id;
            String uuidString = punishmentsConfig.getString(path + ".playerUuid");
            if (uuidString != null && uuidString.equals(playerUuid.toString()) &&
                punishmentsConfig.getString(path + ".type").equals(Punishment.PunishmentType.WARN.toString())) {
                
                Punishment punishment = buildPunishment(path);
                if (punishment.isActive()) {
                    if (expiryMillis != -1 && (punishment.getDate() + expiryMillis) <= System.currentTimeMillis()) {
                        punishment.setActive(false);
                        savePunishment(punishment);
                    } else {
                        warnings.add(punishment);
                    }
                }
            }
        }
        return warnings;
    }

    @Override
    public List<Punishment> getHistory(UUID playerUuid) {
        List<Punishment> history = new ArrayList<>();
        ConfigurationSection section = punishmentsConfig.getConfigurationSection("punishments");
        if (section == null) return history;

        for (String id : section.getKeys(false)) {
            String path = "punishments." + id;
            String uuidString = punishmentsConfig.getString(path + ".playerUuid");
            if (uuidString != null && uuidString.equals(playerUuid.toString())) {
                history.add(buildPunishment(path));
            }
        }
        return history;
    }
    
    @Override
    public List<Punishment> getAllActiveBans() {
        List<Punishment> activeBans = new ArrayList<>();
        ConfigurationSection section = punishmentsConfig.getConfigurationSection("punishments");
        if (section == null) return activeBans;

        for (String id : section.getKeys(false)) {
            String path = "punishments." + id;
            Punishment punishment = buildPunishment(path);
            if (punishment != null && punishment.isActive() && allBanTypes.contains(punishment.getType())) {
                if (punishment.getDuration() == -1 || punishment.getEnd() > System.currentTimeMillis()) {
                    activeBans.add(punishment);
                } else {
                    punishment.setActive(false);
                    savePunishment(punishment);
                }
            }
        }
        return activeBans;
    }

    @Override
    public List<Punishment> getStaffHistory(String executorName) {
        List<Punishment> staffHistory = new ArrayList<>();
        ConfigurationSection section = punishmentsConfig.getConfigurationSection("punishments");
        if (section == null) return staffHistory;

        for (String id : section.getKeys(false)) {
            String path = "punishments." + id;
            if (punishmentsConfig.getString(path + ".executorName").equalsIgnoreCase(executorName)) {
                staffHistory.add(buildPunishment(path));
            }
        }
        return staffHistory;
    }

    @Override
    public List<String> getAltNamesByIp(String ipAddress, UUID excludeUuid) {
        Set<String> altNames = new HashSet<>();
        ConfigurationSection section = punishmentsConfig.getConfigurationSection("punishments");
        if (section == null || ipAddress == null) return new ArrayList<>();

        for (String id : section.getKeys(false)) {
            String path = "punishments." + id;
            String storedIp = punishmentsConfig.getString(path + ".ipAddress");
            String storedUuidString = punishmentsConfig.getString(path + ".playerUuid");

            if (ipAddress.equals(storedIp)) {
                UUID storedUuid = storedUuidString != null ? UUID.fromString(storedUuidString) : null;
                if (storedUuid != null && !storedUuid.equals(excludeUuid)) {
                    String playerName = punishmentsConfig.getString(path + ".playerName");
                    if (playerName != null) {
                        altNames.add(playerName);
                    }
                }
            }
        }
        return new ArrayList<>(altNames);
    }

    @Override
    public Punishment getActivePunishment(UUID playerUuid, String ipAddress, List<Punishment.PunishmentType> types) {
        return getActivePunishment(playerUuid, ipAddress, null, types);
    }

    @Override
    public Punishment getActivePunishment(UUID playerUuid, String ipAddress, List<UUID> altUuids, List<Punishment.PunishmentType> types) {
        ConfigurationSection section = punishmentsConfig.getConfigurationSection("punishments");
        if (section == null) return null;

        List<String> keys = new ArrayList<>(section.getKeys(false));
        for (int i = keys.size() - 1; i >= 0; i--) {
            String id = keys.get(i);
            String path = "punishments." + id;
            if (punishmentsConfig.getBoolean(path + ".active", false)) {
                String storedTypeStr = punishmentsConfig.getString(path + ".type");
                if (storedTypeStr == null) continue;
                Punishment.PunishmentType storedType = Punishment.PunishmentType.valueOf(storedTypeStr);
                
                if (types.contains(storedType)) {
                    String storedUuidStr = punishmentsConfig.getString(path + ".playerUuid");
                    UUID storedUuid = storedUuidStr != null ? UUID.fromString(storedUuidStr) : null;
                    String storedIp = punishmentsConfig.getString(path + ".ipAddress");

                    boolean uuidMatch = playerUuid != null && playerUuid.equals(storedUuid);
                    boolean ipMatch = ipAddress != null && !ipAddress.isEmpty() && ipAddress.equals(storedIp);
                    boolean altMatch = altUuids != null && storedUuid != null && altUuids.contains(storedUuid);

                    if (uuidMatch || ipMatch || altMatch) {
                        Punishment p = buildPunishment(path);
                        if (p.getDuration() == -1 || p.getEnd() > System.currentTimeMillis()) {
                            return p;
                        } else {
                            p.setActive(false);
                            savePunishment(p);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void removePunishment(int id) {
        punishmentsConfig.set("punishments." + id, null);
        save();
    }

    @Override
    public void deactivateAllBans() {
        ConfigurationSection section = punishmentsConfig.getConfigurationSection("punishments");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String path = "punishments." + id;
            Punishment.PunishmentType type = Punishment.PunishmentType.valueOf(punishmentsConfig.getString(path + ".type"));
            if (allBanTypes.contains(type)) {
                punishmentsConfig.set(path + ".active", false);
            }
        }
        save();
    }

    @Override
    public List<Punishment> getAllPunishments() {
        List<Punishment> history = new ArrayList<>();
        ConfigurationSection section = punishmentsConfig.getConfigurationSection("punishments");
        if (section == null) return history;

        for (String id : section.getKeys(false)) {
            String path = "punishments." + id;
            history.add(buildPunishment(path));
        }
        return history;
    }

    @Override
    public void setWebPassword(UUID uuid, String hashedPassword) {
        punishmentsConfig.set("web_users." + uuid.toString(), hashedPassword);
        save();
    }

    @Override
    public String getWebPassword(UUID uuid) {
        return punishmentsConfig.getString("web_users." + uuid.toString());
    }

    private Punishment buildPunishment(String path) {
        String id = path.substring(path.lastIndexOf('.') + 1);
        String uuidString = punishmentsConfig.getString(path + ".playerUuid");
        Punishment p = new Punishment(
                Integer.parseInt(id),
                uuidString != null ? UUID.fromString(uuidString) : null,
                punishmentsConfig.getString(path + ".playerName"),
                punishmentsConfig.getString(path + ".ipAddress"),
                Punishment.PunishmentType.valueOf(punishmentsConfig.getString(path + ".type")),
                punishmentsConfig.getString(path + ".reason"),
                punishmentsConfig.getString(path + ".executorName"),
                punishmentsConfig.getLong(path + ".date"),
                punishmentsConfig.getLong(path + ".duration")
        );
        p.setActive(punishmentsConfig.getBoolean(path + ".active", true));
        return p;
    }
}