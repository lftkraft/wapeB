package dev.azuyo.wapeB.importers;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AdvancedBanImporter implements PunishmentImporter {

    private final WapeB plugin;

    public AdvancedBanImporter(WapeB plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "advancedban";
    }

    @Override
    public String getDescription() {
        return "Imports bans, mutes, warnings and kicks from AdvancedBan (SQLite or MySQL).";
    }

    @Override
    public CompletableFuture<ImportResult> executeImport(Map<String, Object> options) {
        CompletableFuture<ImportResult> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long startTime = System.currentTimeMillis();
            ImportResult result = new ImportResult(getName(), true);

            Connection conn = null;
            try {
                String jdbcUrl = null;
                String user = null;
                String pass = null;

                if (options != null && options.containsKey("jdbcUrl")) {
                    jdbcUrl = (String) options.get("jdbcUrl");
                    user = (String) options.get("user");
                    pass = (String) options.get("password");
                } else if (options != null && options.containsKey("file") && (((String)options.get("file")).startsWith("jdbc:") || ((String)options.get("file")).startsWith("mysql://"))) {
                    String raw = (String) options.get("file");
                    jdbcUrl = raw.startsWith("mysql://") ? "jdbc:" + raw : raw;
                } else {
                    File customFile = (options != null && options.containsKey("file")) ? new File((String) options.get("file")) : null;
                    File configFile = new File("plugins/AdvancedBan/config.yml");
                    File sqliteFile = customFile != null ? customFile : new File("plugins/AdvancedBan/saved.db");
                    if (!sqliteFile.exists() && customFile == null) {
                        sqliteFile = new File("plugins/AdvancedBan/AdvancedBan.db");
                    }

                    // Check config.yml first if present to detect MySQL configuration
                    if (configFile.exists() && (customFile == null || customFile.getName().endsWith(".yml"))) {
                        try {
                            org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
                            if (cfg.getBoolean("MySQL.Use", false) || cfg.getBoolean("MySQL.Enable", false)) {
                                String ip = cfg.getString("MySQL.IP", "localhost");
                                int port = cfg.getInt("MySQL.Port", 3306);
                                String db = cfg.getString("MySQL.DB-Name", "AdvancedBan");
                                user = cfg.getString("MySQL.User", "root");
                                pass = cfg.getString("MySQL.Password", "");
                                jdbcUrl = "jdbc:mysql://" + ip + ":" + port + "/" + db + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                                result.addDetail("Detected AdvancedBan MySQL configuration from config.yml (" + ip + ":" + port + "/" + db + ").");
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Could not parse AdvancedBan config.yml: " + e.getMessage());
                        }
                    }

                    if (jdbcUrl == null) {
                        if (!sqliteFile.exists()) {
                            result.addError("AdvancedBan database file not found at: " + sqliteFile.getAbsolutePath() + " and no MySQL settings in config.yml.");
                            result.setDurationMillis(System.currentTimeMillis() - startTime);
                            future.complete(result);
                            return;
                        }
                        jdbcUrl = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
                        result.addDetail("Using AdvancedBan SQLite file: " + sqliteFile.getName());
                    }
                }

                conn = DriverManager.getConnection(jdbcUrl, user, pass);

                String[] tableNames = {"Punishments", "punishments", "AdvancedBan_Punishments"};
                String actualTable = null;
                for (String t : tableNames) {
                    try (ResultSet rs = conn.getMetaData().getTables(null, null, t, null)) {
                        if (rs.next()) {
                            actualTable = t;
                            break;
                        }
                    } catch (SQLException ignored) {}
                }

                if (actualTable == null) {
                    result.addError("Could not find Punishments table in AdvancedBan database.");
                    result.setDurationMillis(System.currentTimeMillis() - startTime);
                    future.complete(result);
                    return;
                }

                String sql = "SELECT * FROM " + actualTable;
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        try {
                            String name = rs.getString("name");
                            String uuidStr = rs.getString("uuid");
                            String reason = rs.getString("reason");
                            String operator = rs.getString("operator");
                            String typeStr = rs.getString("punishmentType");
                            long start = rs.getLong("start");
                            long end = rs.getLong("end");

                            if (operator == null || operator.isEmpty()) operator = "Console";
                            if (start <= 0) start = System.currentTimeMillis();

                            long duration = -1;
                            if (end > 0 && end > start) {
                                duration = end - start;
                            }

                            boolean active = (end == -1) || (end > System.currentTimeMillis());

                            Punishment.PunishmentType type = mapAdvancedBanType(typeStr, duration);

                            UUID playerUuid = null;
                            if (uuidStr != null && !uuidStr.isEmpty()) {
                                try {
                                    if (uuidStr.contains("-")) {
                                        playerUuid = UUID.fromString(uuidStr);
                                    } else if (uuidStr.length() == 32) {
                                        playerUuid = UUID.fromString(uuidStr.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5"));
                                    }
                                } catch (IllegalArgumentException ignored) {}
                            }

                            if (playerUuid == null && name != null) {
                                playerUuid = UUID.nameUUIDFromBytes((name + start).getBytes());
                            }

                            String serverOrigin = plugin.getConfigManager().getString("server-name", "Lobby");

                            Punishment p = new Punishment(
                                    -1,
                                    playerUuid != null ? playerUuid : UUID.randomUUID(),
                                    name != null ? name : "Unknown",
                                    "N/A",
                                    type,
                                    reason != null ? reason : "Imported from AdvancedBan",
                                    operator,
                                    start,
                                    duration,
                                    active,
                                    "global",
                                    serverOrigin,
                                    null
                            );

                            plugin.getDataManager().savePunishment(p);
                            result.incrementImported();
                        } catch (Exception rowEx) {
                            result.incrementFailed();
                            result.addError("Row import error: " + rowEx.getMessage());
                        }
                    }
                }

                result.addDetail("Successfully completed AdvancedBan import: " + result.getImportedCount() + " imported.");
            } catch (Exception e) {
                result.addError("AdvancedBan import exception: " + e.getMessage());
            } finally {
                if (conn != null) {
                    try { conn.close(); } catch (SQLException ignored) {}
                }
                result.setDurationMillis(System.currentTimeMillis() - startTime);
                future.complete(result);
            }
        });

        return future;
    }

    private Punishment.PunishmentType mapAdvancedBanType(String abType, long duration) {
        if (abType == null) return Punishment.PunishmentType.BAN;
        String t = abType.toUpperCase();
        if (t.contains("IP_BAN") || t.contains("IPBAN")) {
            return (duration > 0) ? Punishment.PunishmentType.TEMPIPMUTE : Punishment.PunishmentType.IPBAN;
        }
        if (t.contains("BAN")) {
            return (duration > 0) ? Punishment.PunishmentType.TEMPBAN : Punishment.PunishmentType.BAN;
        }
        if (t.contains("MUTE")) {
            return (duration > 0) ? Punishment.PunishmentType.TEMPMUTE : Punishment.PunishmentType.MUTE;
        }
        if (t.contains("WARN")) {
            return Punishment.PunishmentType.WARN;
        }
        if (t.contains("KICK")) {
            return Punishment.PunishmentType.KICK;
        }
        return (duration > 0) ? Punishment.PunishmentType.TEMPBAN : Punishment.PunishmentType.BAN;
    }
}
