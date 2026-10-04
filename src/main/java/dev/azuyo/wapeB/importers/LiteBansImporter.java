package dev.azuyo.wapeB.importers;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LiteBansImporter implements PunishmentImporter {

    private final WapeB plugin;

    public LiteBansImporter(WapeB plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "litebans";
    }

    @Override
    public String getDescription() {
        return "Imports bans, mutes, warnings and kicks from LiteBans (SQLite or MySQL).";
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
                    // Default to local LiteBans SQLite file
                    File sqliteFile = new File("plugins/LiteBans/litebans.sqlite");
                    if (options != null && options.containsKey("file")) {
                        sqliteFile = new File((String) options.get("file"));
                    }

                    if (!sqliteFile.exists()) {
                        result.addError("LiteBans database file not found at: " + sqliteFile.getAbsolutePath());
                        result.setDurationMillis(System.currentTimeMillis() - startTime);
                        future.complete(result);
                        return;
                    }
                    jdbcUrl = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
                }

                conn = DriverManager.getConnection(jdbcUrl, user, pass);

                importTable(conn, "{prefix}bans", Punishment.PunishmentType.BAN, Punishment.PunishmentType.TEMPBAN, Punishment.PunishmentType.IPBAN, Punishment.PunishmentType.TEMPIPMUTE, result);
                importTable(conn, "{prefix}mutes", Punishment.PunishmentType.MUTE, Punishment.PunishmentType.TEMPMUTE, Punishment.PunishmentType.IPMUTE, Punishment.PunishmentType.TEMPIPMUTE, result);
                importTable(conn, "{prefix}warnings", Punishment.PunishmentType.WARN, Punishment.PunishmentType.WARN, Punishment.PunishmentType.WARN, Punishment.PunishmentType.WARN, result);
                importTable(conn, "{prefix}kicks", Punishment.PunishmentType.KICK, Punishment.PunishmentType.KICK, Punishment.PunishmentType.KICK, Punishment.PunishmentType.KICK, result);

                result.addDetail("Successfully completed LiteBans import: " + result.getImportedCount() + " imported, " + result.getSkippedCount() + " skipped, " + result.getFailedCount() + " failed.");
            } catch (Exception e) {
                result.addError("Import exception: " + e.getMessage());
                plugin.getLogger().severe("LiteBans import error: " + e.getMessage());
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

    private void importTable(Connection conn, String rawTableName, Punishment.PunishmentType permType, Punishment.PunishmentType tempType, Punishment.PunishmentType ipPermType, Punishment.PunishmentType ipTempType, ImportResult result) {
        String[] possiblePrefixes = {"litebans_", "lb_", ""};
        String actualTable = null;

        for (String pfx : possiblePrefixes) {
            String candidate = rawTableName.replace("{prefix}", pfx);
            try (ResultSet rs = conn.getMetaData().getTables(null, null, candidate, null)) {
                if (rs.next()) {
                    actualTable = candidate;
                    break;
                }
            } catch (SQLException ignored) {}
        }

        if (actualTable == null) {
            result.addDetail("Table " + rawTableName + " not found in LiteBans database, skipping.");
            return;
        }

        String sql = "SELECT * FROM " + actualTable;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    String uuidStr = getStringSafe(rs, "uuid");
                    String ip = getStringSafe(rs, "ip");
                    String reason = getStringSafe(rs, "reason");
                    String staff = getStringSafe(rs, "banned_by_name");
                    if (staff == null || staff.isEmpty()) staff = "Console";

                    long time = getLongSafe(rs, "time");
                    if (time <= 0) time = System.currentTimeMillis();

                    long until = getLongSafe(rs, "until");
                    boolean active = getBooleanSafe(rs, "active", true);

                    long duration = -1;
                    if (until > 0 && until > time) {
                        duration = until - time;
                    }

                    boolean isIp = (uuidStr == null || uuidStr.isEmpty() || uuidStr.equals("#") || uuidStr.equals("null")) && (ip != null && !ip.isEmpty());

                    Punishment.PunishmentType finalType;
                    if (isIp) {
                        finalType = (duration > 0) ? ipTempType : ipPermType;
                    } else {
                        finalType = (duration > 0) ? tempType : permType;
                    }

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

                    String playerName = null;
                    if (playerUuid != null) {
                        playerName = Bukkit.getOfflinePlayer(playerUuid).getName();
                    }
                    if (playerName == null) playerName = isIp ? ip : "Unknown";

                    String serverScope = getStringSafe(rs, "server_scope");
                    if (serverScope == null || serverScope.isEmpty()) serverScope = "global";

                    String serverOrigin = getStringSafe(rs, "server_origin");
                    if (serverOrigin == null || serverOrigin.isEmpty()) serverOrigin = plugin.getConfigManager().getString("server-name", "Lobby");

                    Punishment p = new Punishment(
                            -1,
                            playerUuid != null ? playerUuid : UUID.nameUUIDFromBytes((playerName + time).getBytes()),
                            playerName,
                            ip != null ? ip : "N/A",
                            finalType,
                            reason != null ? reason : "Imported from LiteBans",
                            staff,
                            time,
                            duration,
                            active,
                            serverScope,
                            serverOrigin,
                            null
                    );

                    plugin.getDataManager().savePunishment(p);
                    result.incrementImported();
                } catch (Exception rowEx) {
                    result.incrementFailed();
                    result.addError("Row import error in " + actualTable + ": " + rowEx.getMessage());
                }
            }
        } catch (SQLException e) {
            result.addError("Failed reading table " + actualTable + ": " + e.getMessage());
        }
    }

    private String getStringSafe(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException e) { return null; }
    }

    private long getLongSafe(ResultSet rs, String col) {
        try { return rs.getLong(col); } catch (SQLException e) { return 0; }
    }

    private boolean getBooleanSafe(ResultSet rs, String col, boolean def) {
        try { return rs.getBoolean(col); } catch (SQLException e) { return def; }
    }
}
