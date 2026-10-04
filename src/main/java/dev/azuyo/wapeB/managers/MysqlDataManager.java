package dev.azuyo.wapeB.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class MysqlDataManager implements DataManager {

    private final WapeB plugin;
    private HikariDataSource dataSource;
    private final List<Punishment.PunishmentType> allBanTypes = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN,
            Punishment.PunishmentType.FREEZE_LOGOUT_BAN
    );

    private int lastKnownMaxId = -1;
    private final Set<Integer> locallyCreatedIds = Collections.synchronizedSet(new HashSet<>());

    public MysqlDataManager(WapeB plugin) {
        this.plugin = plugin;
        connect();
        createTables();
        initMaxId();
        startSyncTask();
    }

    private void initMaxId() {
        if (dataSource == null) return;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(id), 0) FROM punishments");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                this.lastKnownMaxId = rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not initialize max punishment ID: " + e.getMessage());
        }
    }

    private void startSyncTask() {
        // Poll every 1 second (20 ticks) asynchronously for cross-server syncing
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::syncNewPunishments, 20L, 20L);
    }

    private synchronized void connect() {
        String host = plugin.getConfigManager().getString("mysql.host", "127.0.0.1");
        if (host.equalsIgnoreCase("localhost")) {
            host = "127.0.0.1";
        }
        int port = plugin.getConfigManager().getInt("mysql.port", 3306);
        String database = plugin.getConfigManager().getString("mysql.database", "tesztszero");
        String username = plugin.getConfigManager().getString("mysql.username", "root");
        String password = plugin.getConfigManager().getString("mysql.password", "");
        int poolSize = plugin.getConfigManager().getInt("mysql.pool-size", 10);
        int timeout = plugin.getConfigManager().getInt("mysql.connection-timeout", 5000);
        boolean useSSL = plugin.getConfigManager().getBoolean("mysql.use-ssl", false);

        HikariConfig config = new HikariConfig();
        
        // Auto-detect JDBC driver (MariaDB driver supports both MariaDB and MySQL seamlessly)
        String jdbcUrl;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            jdbcUrl = "jdbc:mariadb://" + host + ":" + port + "/" + database + 
                    "?autoReconnect=true" +
                    "&characterEncoding=utf8" +
                    "&permitMysqlScheme=true";
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ignored) {
            }
            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + 
                    "?useSSL=" + useSSL + 
                    "&allowPublicKeyRetrieval=true" +
                    "&autoReconnect=true" +
                    "&characterEncoding=utf8" +
                    "&serverTimezone=UTC";
        }

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        if (password != null && !password.trim().isEmpty()) {
            config.setPassword(password);
        } else {
            config.setPassword((String) null);
        }
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(Math.min(2, poolSize));
        config.setConnectionTimeout(timeout);
        config.setPoolName("wapeB-HikariPool");

        // High-performance properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        try {
            plugin.getLogger().info("Connecting to Database: " + jdbcUrl + " (User: " + username + ")");
            this.dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Successfully connected to the MySQL/MariaDB database (HikariCP pool: " + poolSize + ").");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not connect to the MySQL database!");
            e.printStackTrace();
        }
    }

    private void createTables() {
        if (dataSource == null) return;

        String sqlPunishments = "CREATE TABLE IF NOT EXISTS punishments (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "playerUuid VARCHAR(36)," +
                "playerName VARCHAR(32)," +
                "ipAddress VARCHAR(64)," +
                "type VARCHAR(32) NOT NULL," +
                "reason TEXT," +
                "executorName VARCHAR(64) NOT NULL," +
                "active_server VARCHAR(255) DEFAULT 'global'," +
                "server VARCHAR(64) DEFAULT 'global'," +
                "proof TEXT," +
                "date BIGINT NOT NULL," +
                "duration BIGINT NOT NULL," +
                "end BIGINT NOT NULL," +
                "active BOOLEAN NOT NULL," +
                "INDEX idx_uuid (playerUuid)," +
                "INDEX idx_name (playerName)," +
                "INDEX idx_ip (ipAddress)," +
                "INDEX idx_active_server (active_server)," +
                "INDEX idx_server (server)," +
                "INDEX idx_active_type (active, type)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sqlWebUsers = "CREATE TABLE IF NOT EXISTS web_users (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "password VARCHAR(255)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlPunishments);
            stmt.execute(sqlWebUsers);
            
            // Auto-migration for existing tables
            try {
                stmt.execute("ALTER TABLE punishments ADD COLUMN server VARCHAR(64) DEFAULT 'global'");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("ALTER TABLE punishments ADD COLUMN active_server VARCHAR(255) DEFAULT 'global'");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("ALTER TABLE punishments ADD COLUMN proof TEXT");
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create MySQL tables!");
            e.printStackTrace();
        }
    }

    @Override
    public void savePunishment(Punishment punishment) {
        if (dataSource == null) return;

        boolean isUpdate = (punishment.getId() != 0 && getPunishment(punishment.getId()) != null);
        String sql;
        if (isUpdate) {
            sql = "UPDATE punishments SET active = ?, playerUuid = ?, playerName = ?, ipAddress = ?, type = ?, reason = ?, executorName = ?, active_server = ?, server = ?, proof = ?, date = ?, duration = ?, end = ? WHERE id = ?";
        } else {
            sql = "INSERT INTO punishments(playerUuid, playerName, ipAddress, type, reason, executorName, active_server, server, proof, date, duration, end, active) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            if (isUpdate) {
                pstmt.setBoolean(1, punishment.isActive());
                pstmt.setString(2, punishment.getPlayerUuid() != null ? punishment.getPlayerUuid().toString() : null);
                pstmt.setString(3, punishment.getPlayerName());
                pstmt.setString(4, punishment.getIpAddress());
                pstmt.setString(5, punishment.getType().toString());
                pstmt.setString(6, punishment.getReason());
                pstmt.setString(7, punishment.getExecutorName());
                pstmt.setString(8, punishment.getActiveServer() != null ? punishment.getActiveServer() : "global");
                pstmt.setString(9, punishment.getServer() != null ? punishment.getServer() : "global");
                pstmt.setString(10, punishment.getProof());
                pstmt.setLong(11, punishment.getDate());
                pstmt.setLong(12, punishment.getDuration());
                pstmt.setLong(13, punishment.getEnd());
                pstmt.setInt(14, punishment.getId());
            } else {
                pstmt.setString(1, punishment.getPlayerUuid() != null ? punishment.getPlayerUuid().toString() : null);
                pstmt.setString(2, punishment.getPlayerName());
                pstmt.setString(3, punishment.getIpAddress());
                pstmt.setString(4, punishment.getType().toString());
                pstmt.setString(5, punishment.getReason());
                pstmt.setString(6, punishment.getExecutorName());
                pstmt.setString(7, punishment.getActiveServer() != null ? punishment.getActiveServer() : "global");
                pstmt.setString(8, punishment.getServer() != null ? punishment.getServer() : "global");
                pstmt.setString(9, punishment.getProof());
                pstmt.setLong(10, punishment.getDate());
                pstmt.setLong(11, punishment.getDuration());
                pstmt.setLong(12, punishment.getEnd());
                pstmt.setBoolean(13, punishment.isActive());
            }

            pstmt.executeUpdate();

            if (!isUpdate) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        punishment.setId(generatedId);
                        locallyCreatedIds.add(generatedId);
                        this.lastKnownMaxId = Math.max(this.lastKnownMaxId, generatedId);
                    }
                }
            } else {
                locallyCreatedIds.add(punishment.getId());
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save punishment to MySQL!");
            e.printStackTrace();
        }
    }

    @Override
    public Punishment getPunishment(int id) {
        if (dataSource == null) return null;

        String sql = "SELECT * FROM punishments WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return buildPunishmentFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get punishment " + id + " from MySQL!");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Punishment> getWarnings(UUID playerUuid) {
        List<Punishment> warnings = new ArrayList<>();
        if (dataSource == null || playerUuid == null) return warnings;

        String sql = "SELECT * FROM punishments WHERE playerUuid = ? AND type = 'WARN' AND active = 1";
        String expiryString = plugin.getConfigManager().getString("warning-expiry", "3d");
        long expiryMillis = expiryString.equals("0") ? -1 : TimeUtil.parseTime(expiryString);
        long now = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Punishment punishment = buildPunishmentFromResultSet(rs);
                    if (expiryMillis != -1 && (punishment.getDate() + expiryMillis) <= now) {
                        punishment.setActive(false);
                        savePunishment(punishment);
                    } else {
                        warnings.add(punishment);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get warnings for " + playerUuid + " from MySQL!");
            e.printStackTrace();
        }
        return warnings;
    }

    @Override
    public List<Punishment> getHistory(UUID playerUuid) {
        List<Punishment> history = new ArrayList<>();
        if (dataSource == null || playerUuid == null) return history;

        String sql = "SELECT * FROM punishments WHERE playerUuid = ? ORDER BY id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(buildPunishmentFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get history for " + playerUuid + " from MySQL!");
            e.printStackTrace();
        }
        return history;
    }

    @Override
    public List<Punishment> getAllActiveBans() {
        List<Punishment> activeBans = new ArrayList<>();
        if (dataSource == null) return activeBans;

        String currentServer = plugin.getConfigManager().getString("server-name", "Lobby");
        String types = allBanTypes.stream()
                .map(type -> "'" + type.toString() + "'")
                .collect(Collectors.joining(","));
        String sql = "SELECT * FROM punishments WHERE active = 1 AND type IN (" + types + ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Punishment punishment = buildPunishmentFromResultSet(rs);
                    if (!punishment.isAppliesTo(currentServer)) {
                        continue;
                    }
                    if (punishment.getDuration() == -1 || punishment.getEnd() > System.currentTimeMillis()) {
                        activeBans.add(punishment);
                    } else {
                        punishment.setActive(false);
                        savePunishment(punishment);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get all active bans from MySQL!");
            e.printStackTrace();
        }
        return activeBans;
    }

    @Override
    public List<Punishment> getStaffHistory(String executorName) {
        List<Punishment> staffHistory = new ArrayList<>();
        if (dataSource == null || executorName == null || executorName.trim().isEmpty()) return staffHistory;

        String sql = "SELECT * FROM punishments WHERE LOWER(executorName) = LOWER(?) OR LOWER(executorName) LIKE LOWER(CONCAT(?, ' - %')) OR LOWER(executorName) LIKE LOWER(CONCAT(?, ' (%)')) ORDER BY id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, executorName);
            pstmt.setString(2, executorName);
            pstmt.setString(3, executorName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    staffHistory.add(buildPunishmentFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get staff history for " + executorName + " from MySQL!");
            e.printStackTrace();
        }
        return staffHistory;
    }

    @Override
    public List<String> getAltNamesByIp(String ipAddress, UUID excludeUuid) {
        Set<String> altNames = new HashSet<>();
        if (dataSource == null || ipAddress == null) return new ArrayList<>();

        String sql = "SELECT DISTINCT playerName FROM punishments WHERE ipAddress = ? AND (playerUuid != ? OR playerUuid IS NULL)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, excludeUuid != null ? excludeUuid.toString() : "");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("playerName");
                    if (name != null && !name.isEmpty()) {
                        altNames.add(name);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get alt names from MySQL!");
            e.printStackTrace();
        }
        return new ArrayList<>(altNames);
    }

    @Override
    public Punishment getActivePunishment(UUID playerUuid, String ipAddress, List<Punishment.PunishmentType> types) {
        return getActivePunishment(playerUuid, null, ipAddress, null, types);
    }

    @Override
    public Punishment getActivePunishment(UUID playerUuid, String ipAddress, List<UUID> altUuids, List<Punishment.PunishmentType> types) {
        return getActivePunishment(playerUuid, null, ipAddress, altUuids, types);
    }

    @Override
    public Punishment getActivePunishment(UUID playerUuid, String playerName, String ipAddress, List<UUID> altUuids, List<Punishment.PunishmentType> types) {
        if (dataSource == null) return null;
        if (playerUuid == null && (playerName == null || playerName.isEmpty()) && (ipAddress == null || ipAddress.isEmpty()) && (altUuids == null || altUuids.isEmpty())) {
            return null;
        }

        String currentServer = plugin.getConfigManager().getString("server-name", "Lobby");

        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM punishments WHERE active = 1 AND (");
        List<String> conditions = new ArrayList<>();
        if (playerUuid != null) {
            conditions.add("playerUuid = ?");
        }
        if (playerName != null && !playerName.isEmpty()) {
            conditions.add("LOWER(playerName) = LOWER(?)");
        }
        if (ipAddress != null && !ipAddress.isEmpty()) {
            conditions.add("ipAddress = ?");
        }
        if (altUuids != null && !altUuids.isEmpty()) {
            for (int i = 0; i < altUuids.size(); i++) {
                conditions.add("playerUuid = ?");
            }
        }
        sqlBuilder.append(String.join(" OR ", conditions));
        sqlBuilder.append(") AND type IN (");
        sqlBuilder.append(types.stream().map(t -> "?").collect(Collectors.joining(",")));
        sqlBuilder.append(") ORDER BY id DESC");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            
            int paramIndex = 1;
            if (playerUuid != null) {
                pstmt.setString(paramIndex++, playerUuid.toString());
            }
            if (playerName != null && !playerName.isEmpty()) {
                pstmt.setString(paramIndex++, playerName);
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

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Punishment p = buildPunishmentFromResultSet(rs);
                    if (!p.isAppliesTo(currentServer)) {
                        continue;
                    }

                    boolean uuidMatch = playerUuid != null && playerUuid.equals(p.getPlayerUuid());
                    boolean nameMatch = playerName != null && !playerName.isEmpty() && p.getPlayerName() != null && playerName.equalsIgnoreCase(p.getPlayerName());
                    boolean altMatch = altUuids != null && p.getPlayerUuid() != null && altUuids.contains(p.getPlayerUuid());
                    boolean ipMatch = p.isIpPunishment() && ipAddress != null && !ipAddress.isEmpty() && p.getIpAddress() != null && (ipAddress.equalsIgnoreCase(p.getIpAddress()) || dev.azuyo.wapeB.utils.IPUtil.isIpInCidr(ipAddress, p.getIpAddress()));

                    if (!uuidMatch && !nameMatch && !altMatch && !ipMatch) {
                        continue;
                    }

                    if (p.getDuration() == -1 || p.getEnd() > System.currentTimeMillis()) {
                        return p;
                    } else {
                        p.setActive(false);
                        savePunishment(p);
                    }
                }
            }

            // Fallback CIDR check for subnets (e.g. 192.168.1.0/24)
            if (ipAddress != null && !ipAddress.isEmpty()) {
                String cidrSql = "SELECT * FROM punishments WHERE active = 1 AND ipAddress IS NOT NULL AND ipAddress LIKE '%/%' AND type IN (" +
                        types.stream().map(t -> "'" + t.toString() + "'").collect(Collectors.joining(",")) + ")";
                try (PreparedStatement cidrStmt = conn.prepareStatement(cidrSql)) {
                    try (ResultSet cidrRs = cidrStmt.executeQuery()) {
                        while (cidrRs.next()) {
                            Punishment p = buildPunishmentFromResultSet(cidrRs);
                            if (!p.isAppliesTo(currentServer)) {
                                continue;
                            }
                            if (!p.isIpPunishment()) {
                                continue;
                            }
                            if (dev.azuyo.wapeB.utils.IPUtil.isIpInCidr(ipAddress, p.getIpAddress())) {
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
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get active punishment from MySQL!");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void removePunishment(int id) {
        if (dataSource == null) return;

        String sql = "DELETE FROM punishments WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not remove punishment " + id + " from MySQL!");
            e.printStackTrace();
        }
    }

    @Override
    public int getNextId() {
        if (dataSource == null) return 1;

        String sql = "SELECT MAX(id) FROM punishments";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        } catch (SQLException ignored) {
        }
        return 1;
    }

    @Override
    public void deactivateAllBans() {
        if (dataSource == null) return;

        String types = allBanTypes.stream()
                .map(type -> "'" + type.toString() + "'")
                .collect(Collectors.joining(","));
        String sql = "UPDATE punishments SET active = 0 WHERE type IN (" + types + ")";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not deactivate all bans in MySQL!");
            e.printStackTrace();
        }
    }

    @Override
    public List<Punishment> getAllPunishments() {
        List<Punishment> all = new ArrayList<>();
        if (dataSource == null) return all;

        String sql = "SELECT * FROM punishments ORDER BY id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                all.add(buildPunishmentFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get all punishments from MySQL!");
        }
        return all;
    }

    @Override
    public void setWebPassword(UUID uuid, String hashedPassword) {
        if (dataSource == null || uuid == null) return;

        String sql = "INSERT INTO web_users (uuid, password) VALUES (?, ?) ON DUPLICATE KEY UPDATE password = VALUES(password)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not set web password in MySQL!");
            e.printStackTrace();
        }
    }

    @Override
    public String getWebPassword(UUID uuid) {
        if (dataSource == null || uuid == null) return null;

        String sql = "SELECT password FROM web_users WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get web password from MySQL!");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("MySQL HikariCP connection pool has been closed.");
        }
    }

    private Punishment buildPunishmentFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String uuidString = rs.getString("playerUuid");
        UUID playerUuid = (uuidString != null && !uuidString.isEmpty()) ? UUID.fromString(uuidString) : null;
        String playerName = rs.getString("playerName");
        String ipAddress = rs.getString("ipAddress");
        Punishment.PunishmentType type = Punishment.PunishmentType.valueOf(rs.getString("type"));
        String reason = rs.getString("reason");
        String executorName = rs.getString("executorName");
        String activeServer = "global";
        try {
            activeServer = rs.getString("active_server");
            if (activeServer == null || activeServer.isEmpty()) activeServer = "global";
        } catch (SQLException ignored) {}
        String server = "global";
        try {
            server = rs.getString("server");
            if (server == null || server.isEmpty()) server = "global";
        } catch (SQLException ignored) {}
        String proof = null;
        try {
            proof = rs.getString("proof");
        } catch (SQLException ignored) {}
        long date = rs.getLong("date");
        long duration = rs.getLong("duration");
        boolean active = rs.getBoolean("active");

        Punishment p = new Punishment(id, playerUuid, playerName, ipAddress, type, reason, executorName, activeServer, server, proof, date, duration);
        p.setActive(active);
        return p;
    }

    private void syncNewPunishments() {
        if (dataSource == null || dataSource.isClosed() || lastKnownMaxId < 0) return;

        String sql = "SELECT * FROM punishments WHERE id > ? ORDER BY id ASC";
        List<Punishment> newPunishments = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lastKnownMaxId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Punishment p = buildPunishmentFromResultSet(rs);
                    if (p != null) {
                        newPunishments.add(p);
                        if (p.getId() > lastKnownMaxId) {
                            this.lastKnownMaxId = p.getId();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error polling punishments from MySQL: " + e.getMessage());
        }

        if (!newPunishments.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String currentServer = plugin.getConfigManager().getString("server-name", "Lobby");
                boolean showRemoteBroadcast = plugin.getConfigManager().getBoolean("broadcast.show-remote-punishments", true);

                for (Punishment p : newPunishments) {
                    if (locallyCreatedIds.contains(p.getId())) {
                        locallyCreatedIds.remove(p.getId());
                        continue;
                    }

                    String uniqueKey = (p.getPlayerName() != null ? p.getPlayerName() : "") + ":" + p.getType().name() + ":" + p.getReason() + ":" + p.getExecutorName();
                    if (PluginMessageManager.isProcessed(p.getId(), uniqueKey)) {
                        continue;
                    }
                    PluginMessageManager.markProcessed(p.getId(), uniqueKey);

                    boolean appliesToThisServer = p.isAppliesTo(currentServer);

                    // 1. Kick online player if ban/kick applies to this server
                    if (appliesToThisServer && p.isActive() && (p.getType() == Punishment.PunishmentType.BAN || p.getType() == Punishment.PunishmentType.TEMPBAN || p.getType() == Punishment.PunishmentType.IPBAN || p.getType() == Punishment.PunishmentType.TEMPIPBAN || p.getType() == Punishment.PunishmentType.KICK)) {
                        Player online = p.getPlayerUuid() != null ? Bukkit.getPlayer(p.getPlayerUuid()) : Bukkit.getPlayer(p.getPlayerName());
                        if (online != null && online.isOnline()) {
                            String kickKey = p.getType() == Punishment.PunishmentType.KICK ? "messages.kick.kick-screen" : "messages.ban.kick-screen";
                            online.kick(MessageUtil.formatKickScreen(plugin.getConfigManager().getStringList(kickKey), p));
                        }
                    }

                    // 2. Broadcast to players and console on this server
                    if (appliesToThisServer || showRemoteBroadcast) {
                        String msgKey = getBroadcastKeyForType(p.getType());
                        String broadcastMsg = plugin.getConfigManager().getString(msgKey, "%prefix% %executor% punished %player%.");
                        Component comp = MessageUtil.createComponent(broadcastMsg, p);
                        Bukkit.broadcast(comp);
                    }
                }
            });
        }
    }

    private String getBroadcastKeyForType(Punishment.PunishmentType type) {
        if (type == null) return "messages.ban.broadcast";
        switch (type) {
            case BAN:
            case TEMPBAN:
            case FREEZE_LOGOUT_BAN:
                return "messages.ban.broadcast";
            case IPBAN:
            case TEMPIPBAN:
                return "messages.banip.broadcast";
            case MUTE:
            case TEMPMUTE:
            case SENTINEL_AUTO_MUTE:
            case SENTINEL_AI_MUTE:
                return "messages.mute.broadcast";
            case IPMUTE:
            case TEMPIPMUTE:
                return "messages.muteip.broadcast";
            case WARN:
                return "messages.warn.broadcast";
            case KICK:
                return "messages.kick.broadcast";
            default:
                return "messages.ban.broadcast";
        }
    }
}
