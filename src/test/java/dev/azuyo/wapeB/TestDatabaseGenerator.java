package dev.azuyo.wapeB;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

public class TestDatabaseGenerator {

    public static void main(String[] args) {
        try {
            generateLiteBansDb("plugins/LiteBans/litebans.sqlite");
            generateLiteBansDb("test_samples/LiteBans/litebans.sqlite");

            generateAdvancedBanDb("plugins/AdvancedBan/saved.db");
            generateAdvancedBanDb("plugins/AdvancedBan/AdvancedBan.db");
            generateAdvancedBanDb("test_samples/AdvancedBan/AdvancedBan.db");

            System.out.println("Test databases successfully generated!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void generateLiteBansDb(String filePath) throws Exception {
        File file = new File(filePath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        if (file.exists()) file.delete();

        String url = "jdbc:sqlite:" + file.getAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // LiteBans bans table
            stmt.executeUpdate("CREATE TABLE litebans_bans (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "name VARCHAR(16), " +
                    "ip VARCHAR(45), " +
                    "reason TEXT, " +
                    "banned_by_name VARCHAR(16), " +
                    "banned_by_uuid VARCHAR(36), " +
                    "time BIGINT, " +
                    "until BIGINT, " +
                    "active INTEGER, " +
                    "server_scope VARCHAR(32), " +
                    "server_origin VARCHAR(32), " +
                    "silent INTEGER, " +
                    "ipban INTEGER)");

            // LiteBans mutes table
            stmt.executeUpdate("CREATE TABLE litebans_mutes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "name VARCHAR(16), " +
                    "ip VARCHAR(45), " +
                    "reason TEXT, " +
                    "banned_by_name VARCHAR(16), " +
                    "banned_by_uuid VARCHAR(36), " +
                    "time BIGINT, " +
                    "until BIGINT, " +
                    "active INTEGER, " +
                    "server_scope VARCHAR(32), " +
                    "server_origin VARCHAR(32), " +
                    "silent INTEGER, " +
                    "ipban INTEGER)");

            // LiteBans warnings table
            stmt.executeUpdate("CREATE TABLE litebans_warnings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "name VARCHAR(16), " +
                    "ip VARCHAR(45), " +
                    "reason TEXT, " +
                    "banned_by_name VARCHAR(16), " +
                    "banned_by_uuid VARCHAR(36), " +
                    "time BIGINT, " +
                    "until BIGINT, " +
                    "active INTEGER, " +
                    "server_scope VARCHAR(32), " +
                    "server_origin VARCHAR(32), " +
                    "silent INTEGER)");

            // LiteBans kicks table
            stmt.executeUpdate("CREATE TABLE litebans_kicks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "name VARCHAR(16), " +
                    "ip VARCHAR(45), " +
                    "reason TEXT, " +
                    "banned_by_name VARCHAR(16), " +
                    "banned_by_uuid VARCHAR(36), " +
                    "time BIGINT, " +
                    "until BIGINT, " +
                    "active INTEGER, " +
                    "server_scope VARCHAR(32), " +
                    "server_origin VARCHAR(32), " +
                    "silent INTEGER)");

            long now = System.currentTimeMillis();
            long day = 86400000L;

            // Insert sample bans
            stmt.executeUpdate(String.format(
                    "INSERT INTO litebans_bans (uuid, name, ip, reason, banned_by_name, time, until, active, server_scope, server_origin, silent, ipban) VALUES " +
                    "('%s', 'LiteBans_Csaló1', '1.2.3.4', 'Killaura / Fly hack', 'AdminLite', %d, %d, 1, 'global', 'Lobby', 0, 0), " +
                    "('%s', 'LiteBans_PermaBan', '5.6.7.8', 'Súlyos szabálysértés', 'Console', %d, -1, 1, 'global', 'Survival', 1, 0)",
                    UUID.randomUUID(), now - 10000, now + (7 * day),
                    UUID.randomUUID(), now - 50000
            ));

            // Insert sample mutes
            stmt.executeUpdate(String.format(
                    "INSERT INTO litebans_mutes (uuid, name, ip, reason, banned_by_name, time, until, active, server_scope, server_origin, silent, ipban) VALUES " +
                    "('%s', 'LiteBans_Spammer', '9.10.11.12', 'Folyamatos spam és flood', 'ModLite', %d, %d, 1, 'global', 'Lobby', 0, 0)",
                    UUID.randomUUID(), now - 5000, now + day
            ));

            // Insert sample warning
            stmt.executeUpdate(String.format(
                    "INSERT INTO litebans_warnings (uuid, name, ip, reason, banned_by_name, time, until, active, server_scope, server_origin, silent) VALUES " +
                    "('%s', 'LiteBans_Warned', '13.14.15.16', 'Káromkodás chaten', 'HelperLite', %d, -1, 1, 'global', 'Lobby', 0)",
                    UUID.randomUUID(), now - 2000
            ));
        }
    }

    private static void generateAdvancedBanDb(String filePath) throws Exception {
        File file = new File(filePath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        if (file.exists()) file.delete();

        String url = "jdbc:sqlite:" + file.getAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE TABLE Punishments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR(16), " +
                    "uuid VARCHAR(36), " +
                    "reason TEXT, " +
                    "operator VARCHAR(16), " +
                    "punishmentType VARCHAR(16), " +
                    "start BIGINT, " +
                    "end BIGINT, " +
                    "calculation VARCHAR(50))");

            long now = System.currentTimeMillis();
            long day = 86400000L;

            stmt.executeUpdate(String.format(
                    "INSERT INTO Punishments (name, uuid, reason, operator, punishmentType, start, end, calculation) VALUES " +
                    "('AdvBan_Xrayer', '%s', 'X-Ray gyémánt bányászat', 'AdvAdmin', 'BAN', %d, -1, ''), " +
                    "('AdvBan_TempMute', '%s', 'Toxikus viselkedés', 'AdvMod', 'TEMP_MUTE', %d, %d, '3d'), " +
                    "('AdvBan_WarnedUser', '%s', 'Capslock használata', 'AdvHelper', 'WARNING', %d, -1, '')",
                    UUID.randomUUID(), now - 20000,
                    UUID.randomUUID(), now - 15000, now + (3 * day),
                    UUID.randomUUID(), now - 5000
            ));
        }
    }
}
