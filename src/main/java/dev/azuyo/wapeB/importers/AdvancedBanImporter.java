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
                } else {
                    File sqliteFile = new File("plugins/AdvancedBan/saved.db");
                    if (options != null && options.containsKey("file")) {
                        sqliteFile = new File((String) options.get("file"));
                    }

                    if (!sqliteFile.exists()) {
                        result.addError("AdvancedBan database file not found at: " + sqliteFile.getAbsolutePath());
                        result.setDurationMillis(System.currentTimeMillis() - startTime);
                        future.complete(result);
                        return;
                    }
                    jdbcUrl = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
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
