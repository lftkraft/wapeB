package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.Punishment;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SqliteDataManager implements DataManager {

    private final WapeB plugin;
    private Connection connection;
    private final List<Punishment.PunishmentType> allBanTypes = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN,
            Punishment.PunishmentType.FREEZE_LOGOUT_BAN
    );

    public SqliteDataManager(WapeB plugin) {
        this.plugin = plugin;
        connect();
        createTable();
    }

    private synchronized void connect() {
        File dbFile = new File(plugin.getDataFolder(), "punishments.db");
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("Successfully connected to the SQLite database.");
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Could not connect to the SQLite database!");
            e.printStackTrace();
        }
    }

    private synchronized void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS punishments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "playerUuid TEXT," +
                "playerName TEXT," +
                "ipAddress TEXT," +
                "type TEXT NOT NULL," +
                "reason TEXT," +
                "executorName TEXT NOT NULL," +
                "date INTEGER NOT NULL," +
                "duration INTEGER NOT NULL," +
                "end INTEGER NOT NULL," +
                "active BOOLEAN NOT NULL" +
                ");";

        String webUsersSql = "CREATE TABLE IF NOT EXISTS web_users (" +
                "uuid TEXT PRIMARY KEY," +
                "password TEXT" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute(webUsersSql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables!");
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void savePunishment(Punishment punishment) {
        boolean isUpdate = (punishment.getId() != 0 && getPunishment(punishment.getId()) != null);
        String sql;
        if (isUpdate) {
            sql = "UPDATE punishments SET active = ?, playerUuid = ?, playerName = ?, ipAddress = ?, type = ?, reason = ?, executorName = ?, date = ?, duration = ?, end = ? WHERE id = ?";
        } else {
            sql = "INSERT INTO punishments(playerUuid, playerName, ipAddress, type, reason, executorName, date, duration, end, active) VALUES(?,?,?,?,?,?,?,?,?,?)";
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (isUpdate) { // UPDATE
                pstmt.setBoolean(1, punishment.isActive());
                pstmt.setString(2, punishment.getPlayerUuid() != null ? punishment.getPlayerUuid().toString() : null);
                pstmt.setString(3, punishment.getPlayerName());
                pstmt.setString(4, punishment.getIpAddress());
                pstmt.setString(5, punishment.getType().toString());
                pstmt.setString(6, punishment.getReason());
                pstmt.setString(7, punishment.getExecutorName());
                pstmt.setLong(8, punishment.getDate());
                pstmt.setLong(9, punishment.getDuration());
                pstmt.setLong(10, punishment.getEnd());
                pstmt.setInt(11, punishment.getId());
            } else { // INSERT
                pstmt.setString(1, punishment.getPlayerUuid() != null ? punishment.getPlayerUuid().toString() : null);
                pstmt.setString(2, punishment.getPlayerName());
                pstmt.setString(3, punishment.getIpAddress());
                pstmt.setString(4, punishment.getType().toString());
                pstmt.setString(5, punishment.getReason());
                pstmt.setString(6, punishment.getExecutorName());
                pstmt.setLong(7, punishment.getDate());
                pstmt.setLong(8, punishment.getDuration());
                pstmt.setLong(9, punishment.getEnd());
                pstmt.setBoolean(10, punishment.isActive());
            }
            
            pstmt.executeUpdate();

            if (!isUpdate) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        punishment.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save punishment to SQLite!");
            e.printStackTrace();
        }
    }

    @Override
    public synchronized Punishment getPunishment(int id) {
        String sql = "SELECT * FROM punishments WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return buildPunishmentFromResultSet(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get punishment " + id + " from SQLite!");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public synchronized List<Punishment> getWarnings(UUID playerUuid) {
        List<Punishment> warnings = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE playerUuid = ? AND type = 'WARN'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                warnings.add(buildPunishmentFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get warnings for " + playerUuid + " from SQLite!");
            e.printStackTrace();
        }
        return warnings;
    }

    @Override
    public synchronized List<Punishment> getHistory(UUID playerUuid) {
        List<Punishment> history = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE playerUuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                history.add(buildPunishmentFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get history for " + playerUuid + " from SQLite!");
            e.printStackTrace();
        }
        return history;
    }

    @Override
    public synchronized List<Punishment> getAllActiveBans() {
        List<Punishment> activeBans = new ArrayList<>();
        String types = allBanTypes.stream()
                .map(type -> "'" + type.toString() + "'")
                .collect(Collectors.joining(","));
        String sql = "SELECT * FROM punishments WHERE active = 1 AND type IN (" + types + ")";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Punishment punishment = buildPunishmentFromResultSet(rs);
                if (punishment.getDuration() == -1 || punishment.getEnd() > System.currentTimeMillis()) {
                    activeBans.add(punishment);
                } else {
                    punishment.setActive(false);
                    savePunishment(punishment);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get all active bans from SQLite!");
            e.printStackTrace();
        }
        return activeBans;
    }

    @Override
    public synchronized List<Punishment> getStaffHistory(String executorName) {
        List<Punishment> staffHistory = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE executorName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, executorName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                staffHistory.add(buildPunishmentFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get staff history for " + executorName + " from SQLite!");
            e.printStackTrace();
        }
        return staffHistory;
    }
    
    @Override
    public synchronized List<String> getAltNamesByIp(String ipAddress, UUID excludeUuid) {
        Set<String> altNames = new HashSet<>();
        String sql = "SELECT DISTINCT playerName FROM punishments WHERE ipAddress = ? AND playerUuid != ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, excludeUuid.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                altNames.add(rs.getString("playerName"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get alt names from SQLite!");
            e.printStackTrace();
        }
        return new ArrayList<>(altNames);
    }

    @Override
    public synchronized Punishment getActivePunishment(UUID playerUuid, String ipAddress, List<Punishment.PunishmentType> types) {
        return getActivePunishment(playerUuid, ipAddress, null, types);
    }

    @Override
    public synchronized Punishment getActivePunishment(UUID playerUuid, String ipAddress, List<UUID> altUuids, List<Punishment.PunishmentType> types) {
        if (playerUuid == null && (ipAddress == null || ipAddress.isEmpty()) && (altUuids == null || altUuids.isEmpty())) {
            return null;
        }

        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM punishments WHERE active = 1 AND (");
        List<String> conditions = new ArrayList<>();
        if (playerUuid != null) {
            conditions.add("playerUuid = ?");
        }
        if (ipAddress != null && !ipAddress.isEmpty()) {
            conditions.add("ipAddress = ?");
        }
        if (altUuids != null && !altUuids.isEmpty()) {
            for (UUID altUuid : altUuids) {
                conditions.add("playerUuid = ?");
            }
        }
        sqlBuilder.append(String.join(" OR ", conditions));
        sqlBuilder.append(") AND type IN (");
        sqlBuilder.append(types.stream().map(t -> "?").collect(Collectors.joining(",")));
        sqlBuilder.append(") ORDER BY id DESC");

        try (PreparedStatement pstmt = connection.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            if (playerUuid != null) {
                pstmt.setString(paramIndex++, playerUuid.toString());
            }
            if (ipAddress != null && !ipAddress.isEmpty()) {
                pstmt.setString(paramIndex++, ipAddress);
            }
            if (altUuids != null && !altUuids.isEmpty()) {
                for (UUID altUuid : altUuids) {
                    pstmt.setString(paramIndex++, altUuid.toString());
                }
            }
            for (Punishment.PunishmentType type : types) {
                pstmt.setString(paramIndex++, type.toString());
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Punishment p = buildPunishmentFromResultSet(rs);
                if (p.getDuration() == -1 || p.getEnd() > System.currentTimeMillis()) {
                    return p;
                } else {
                    p.setActive(false);
                    savePunishment(p);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get active punishment from SQLite!");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public synchronized void removePunishment(int id) {
        String sql = "DELETE FROM punishments WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not remove punishment " + id + " from SQLite!");
            e.printStackTrace();
        }
    }

    @Override
    public synchronized int getNextId() {
        String sql = "SELECT MAX(id) FROM punishments";
        try (Statement stmt = connection.createStatement()) {
             ResultSet rs = stmt.executeQuery(sql);
             if (rs.next()) {
                 int max = rs.getInt(1);
                 return max + 1;
             }
        } catch (SQLException e) {
            // Table might be empty
        }
        return 1;
    }

    @Override
    public synchronized void deactivateAllBans() {
        String types = allBanTypes.stream()
                .map(type -> "'" + type.toString() + "'")
                .collect(Collectors.joining(","));
        String sql = "UPDATE punishments SET active = 0 WHERE type IN (" + types + ")";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not deactivate all bans in SQLite!");
            e.printStackTrace();
        }
    }

    @Override
    public synchronized List<Punishment> getAllPunishments() {
        List<Punishment> all = new ArrayList<>();
        String sql = "SELECT * FROM punishments";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                all.add(buildPunishmentFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get all punishments from SQLite!");
        }
        return all;
    }

    @Override
    public synchronized void setWebPassword(UUID uuid, String hashedPassword) {
        String sql = "INSERT OR REPLACE INTO web_users (uuid, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not set web password in SQLite!");
            e.printStackTrace();
        }
    }

    @Override
    public synchronized String getWebPassword(UUID uuid) {
        String sql = "SELECT password FROM web_users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get web password from SQLite!");
            e.printStackTrace();
        }
        return null;
    }

    private Punishment buildPunishmentFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String uuidString = rs.getString("playerUuid");
        UUID playerUuid = (uuidString != null) ? UUID.fromString(uuidString) : null;
        String playerName = rs.getString("playerName");
        String ipAddress = rs.getString("ipAddress");
        Punishment.PunishmentType type = Punishment.PunishmentType.valueOf(rs.getString("type"));
        String reason = rs.getString("reason");
        String executorName = rs.getString("executorName");
        long date = rs.getLong("date");
        long duration = rs.getLong("duration");
        boolean active = rs.getBoolean("active");

        Punishment p = new Punishment(id, playerUuid, playerName, ipAddress, type, reason, executorName, date, duration);
        p.setActive(active);
        return p;
    }
}