package dev.azuyo.wapeB.managers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
import dev.azuyo.wapeB.utils.WebhookUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebAPIManager {

    private final WapeB plugin;
    private HttpServer server;
    private boolean enabled;
    private int port;
    private String apiKey;
    private final Gson gson = new Gson();
    
    private final Map<String, LoginRequest> pendingLogins = new ConcurrentHashMap<>();
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    private static class LoginRequest {
        String code;
        long timestamp;
        LoginRequest(String code) {
            this.code = code;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class Session {
        UUID uuid;
        long lastAccess;
        Session(UUID uuid) {
            this.uuid = uuid;
            this.lastAccess = System.currentTimeMillis();
        }
    }

    public WebAPIManager(WapeB plugin) {
        this.plugin = plugin;
        loadConfig();
        startCleanupTask();
    }

    public void loadConfig() {
        ConfigurationSection webApiSection = plugin.getConfigManager().getConfig().getConfigurationSection("web-api");
        if (webApiSection == null) {
            this.enabled = false;
            return;
        }
        this.enabled = webApiSection.getBoolean("enabled", false);
        this.port = webApiSection.getInt("port", 8080);
        this.apiKey = webApiSection.getString("api-key", "YOUR_SECURE_API_KEY_HERE");

        if (this.enabled) {
            startServer();
        } else {
            stopServer();
        }
    }

    private void startServer() {
        if (server != null) stopServer();
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            
            server.createContext("/api/login/request", new LoginRequestHandler());
            server.createContext("/api/login/verify", new LoginVerifyHandler());
            server.createContext("/api/login/password", new PasswordLoginHandler());
            server.createContext("/api/user/info", new UserInfoHandler());
            server.createContext("/api/user/set-password", new SetPasswordHandler());
            server.createContext("/api/stats", new StatsHandler());
            server.createContext("/api/lockdown", new LockdownHandler());
            server.createContext("/api/player/profile", new PlayerProfileHandler());
            server.createContext("/api/player/punishments", new PlayerPunishmentsHandler());
            server.createContext("/api/player/checkban", new CheckBanHandler());
            server.createContext("/api/player/checkmute", new CheckMuteHandler());
            server.createContext("/api/commands/list", new CommandsListHandler());
            server.createContext("/api/punish/execute", new PunishExecuteHandler());
            server.createContext("/api/punish/remove", new PunishRemoveHandler());
            server.createContext("/api/punish/active", new ActivePunishmentsHandler());

            server.start();
            plugin.getLogger().info("Web API server started on port " + port);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start Web API server: " + e.getMessage());
        }
    }

    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            pendingLogins.values().removeIf(req -> now - req.timestamp > TimeUnit.MINUTES.toMillis(5));
            activeSessions.values().removeIf(sess -> now - sess.lastAccess > TimeUnit.HOURS.toMillis(2));
        }, 1200L, 1200L);
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private boolean isAuthenticated(HttpExchange exchange) {
        String providedApiKey = exchange.getRequestHeaders().getFirst("X-API-Key");
        if (providedApiKey == null || !apiKey.equals(providedApiKey)) {
            sendResponse(exchange, 401, "{\"error\": \"Invalid API Key\"}");
            return false;
        }
        return true;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error sending HTTP response: " + e.getMessage());
        }
    }

    public void clearAllSessions() {
        pendingLogins.clear();
        activeSessions.clear();
    }

    private boolean checkPermission(UUID uuid, String permission) {
        try {
            return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) return player.hasPermission(permission);
                
                try {
                    LuckPerms lp = LuckPermsProvider.get();
                    User user = lp.getUserManager().loadUser(uuid).join();
                    if (user != null) {
                        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
                    }
                } catch (Exception ignored) {}
                return false;
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = password + salt;
            byte[] hash = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String getAdminName(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    // --- API Handlers ---

    class LoginRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            String playerName = params.get("player");
            
            Player player = Bukkit.getPlayer(playerName);
            if (player == null || !player.isOnline()) {
                sendResponse(exchange, 404, "{\"error\": \"Player not online\"}");
                return;
            }
            
            String code = String.format("%06d", new Random().nextInt(999999));
            pendingLogins.put(player.getName(), new LoginRequest(code));
            
            String message = plugin.getConfigManager().getString("web-api.messages.login-code", "%prefix% <white>A bejelentkezési kódod: <gradient:#FF00D9:#B300FF><bold>%code%</bold></gradient>");
            player.sendMessage(MessageUtil.createComponent(message.replace("%code%", code), null));
            sendResponse(exchange, 200, "{\"success\": true}");
        }
    }

    class LoginVerifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            String playerName = params.get("player");
            String code = params.get("code");
            
            LoginRequest req = pendingLogins.get(playerName);
            if (req != null && req.code.equals(code)) {
                pendingLogins.remove(playerName);
                String sessionId = UUID.randomUUID().toString();
                OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
                activeSessions.put(sessionId, new Session(op.getUniqueId()));
                sendResponse(exchange, 200, "{\"success\": true, \"session\": \"" + sessionId + "\", \"uuid\": \"" + op.getUniqueId() + "\"}");
            } else {
                sendResponse(exchange, 401, "{\"error\": \"Invalid code\"}");
            }
        }
    }

    class PasswordLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            String playerName = params.get("player");
            String password = params.get("password");
            
            if (playerName == null || password == null) {
                sendResponse(exchange, 400, "{\"error\": \"Missing parameters\"}");
                return;
            }

            OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
            UUID uuid = op.getUniqueId();
            String storedData = plugin.getDataManager().getWebPassword(uuid);
            
            if (storedData != null && storedData.contains(":")) {
                String[] parts = storedData.split(":");
                String salt = parts[0];
                String hash = parts[1];
                
                if (hash.equals(hashPassword(password, salt))) {
                    String sessionId = UUID.randomUUID().toString();
                    activeSessions.put(sessionId, new Session(uuid));
                    sendResponse(exchange, 200, "{\"success\": true, \"session\": \"" + sessionId + "\", \"uuid\": \"" + uuid + "\"}");
                    return;
                }
            }
            sendResponse(exchange, 401, "{\"error\": \"Invalid credentials\"}");
        }
    }

    class SetPasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            String uuidStr = params.get("uuid");
            String newPassword = params.get("password");
            
            if (uuidStr == null || newPassword == null) {
                sendResponse(exchange, 400, "{\"error\": \"Missing uuid or password\"}");
                return;
            }

            UUID uuid = UUID.fromString(uuidStr);
            byte[] saltBytes = new byte[16];
            new SecureRandom().nextBytes(saltBytes);
            String salt = Base64.getEncoder().encodeToString(saltBytes);
            
            String hashed = hashPassword(newPassword, salt);
            plugin.getDataManager().setWebPassword(uuid, salt + ":" + hashed);
            sendResponse(exchange, 200, "{\"success\": true}");
        }
    }

    class UserInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            String uuidStr = params.get("uuid");
            if (uuidStr == null) {
                sendResponse(exchange, 400, "{\"error\": \"Missing uuid\"}");
                return;
            }
            
            UUID uuid = UUID.fromString(uuidStr);
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            String rank = getPlayerRank(uuid);
            boolean hasPassword = plugin.getDataManager().getWebPassword(uuid) != null;
            
            JsonObject resp = new JsonObject();
            resp.addProperty("name", name);
            resp.addProperty("rank", rank);
            resp.addProperty("uuid", uuid.toString());
            resp.addProperty("hasPassword", hasPassword);
            sendResponse(exchange, 200, resp.toString());
        }
    }

    class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            List<Punishment> all = plugin.getDataManager().getAllPunishments();
            long now = System.currentTimeMillis();
            long dayAgo = now - (24 * 60 * 60 * 1000);
            int bans = 0, mutes = 0, warns = 0;
            int[] hourlyStats = new int[24];
            
            for (Punishment p : all) {
                if (p.getDate() > dayAgo) {
                    if (p.getType().name().contains("BAN")) bans++;
                    else if (p.getType().name().contains("MUTE")) mutes++;
                    else if (p.getType() == Punishment.PunishmentType.WARN) warns++;
                    
                    int hourIndex = (int) ((p.getDate() - dayAgo) / (3600 * 1000));
                    if (hourIndex >= 0 && hourIndex < 24) hourlyStats[hourIndex]++;
                }
            }
            
            JsonObject stats = new JsonObject();
            stats.addProperty("bans", bans);
            stats.addProperty("mutes", mutes);
            stats.addProperty("warnings", warns);
            JsonArray graphData = new JsonArray();
            for (int count : hourlyStats) graphData.add(count);
            stats.add("graph", graphData);
            sendResponse(exchange, 200, stats.toString());
        }
    }

    class LockdownHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            LockdownManager lm = plugin.getLockdownManager();
            
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
                String action = params.get("action");
                String reason = params.get("reason");
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if ("on".equalsIgnoreCase(action)) {
                        lm.setLockdownEnabled(true);
                        if (reason != null && !reason.isEmpty()) lm.setLockdownReason(reason);
                    } else {
                        lm.setLockdownEnabled(false);
                    }
                });
                sendResponse(exchange, 200, "{\"success\": true}");
            } else {
                JsonObject resp = new JsonObject();
                resp.addProperty("enabled", lm.isLockdownEnabled());
                resp.addProperty("reason", lm.getLockdownReason());
                sendResponse(exchange, 200, resp.toString());
            }
        }
    }

    class PlayerProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            String targetName = params.get("player");
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sendResponse(exchange, 404, "{\"error\": \"Player not found\"}");
                return;
            }
            
            UUID uuid = target.getUniqueId();
            List<Punishment> history = plugin.getDataManager().getHistory(uuid);
            
            JsonObject resp = new JsonObject();
            resp.addProperty("name", target.getName());
            resp.addProperty("uuid", uuid.toString());
            resp.addProperty("online", target.isOnline());
            resp.addProperty("rank", getPlayerRank(uuid));
            
            JsonArray historyArr = new JsonArray();
            for (Punishment p : history) {
                JsonObject po = new JsonObject();
                po.addProperty("id", p.getId());
                po.addProperty("type", p.getType().name());
                po.addProperty("reason", p.getReason());
                po.addProperty("executor", p.getExecutorName());
                po.addProperty("date", p.getDate());
                po.addProperty("active", p.isActive());
                historyArr.add(po);
            }
            resp.add("history", historyArr);
            sendResponse(exchange, 200, resp.toString());
        }
    }

    class PunishExecuteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            String adminUuidStr = params.get("admin_uuid");
            String targetName = params.get("target");
            String typeStr = params.get("type");
            String reason = params.get("reason");
            String durationStr = params.get("duration");
            boolean silent = "true".equalsIgnoreCase(params.get("silent"));
            
            if (adminUuidStr == null) {
                sendResponse(exchange, 400, "{\"error\": \"Missing admin uuid\"}");
                return;
            }
            UUID adminUuid = UUID.fromString(adminUuidStr);
            
            Punishment.PunishmentType type = Punishment.PunishmentType.valueOf(typeStr.toUpperCase());
            String perm = "wapeb." + type.name().toLowerCase();
            if (!checkPermission(adminUuid, perm)) {
                sendResponse(exchange, 403, "{\"error\": \"No permission: " + perm + "\"}");
                return;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            long duration = (durationStr == null || durationStr.isEmpty() || durationStr.equals("-1")) ? -1 : TimeUtil.parseTime(durationStr);
            String targetIp = target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : plugin.getPlayerDataManager().getLastKnownIp(target.getUniqueId());
            
            Punishment p = new Punishment(plugin.getDataManager().getNextId(), target.getUniqueId(), target.getName(), targetIp, type, reason, getAdminName(adminUuid), System.currentTimeMillis(), duration);
            
            // KICK esetén alapból inaktív legyen
            if (type == Punishment.PunishmentType.KICK) {
                p.setActive(false);
            }
            
            plugin.getDataManager().savePunishment(p);
            WebhookUtil.sendPunishmentWebhook(p);
            
            String typeKey = type.name().toLowerCase();
            if (typeKey.contains("ban")) typeKey = "ban";
            else if (typeKey.contains("mute")) typeKey = "mute";
            else if (typeKey.contains("warn")) typeKey = "warn";
            
            final String finalTypeKey = typeKey;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isOnline() && (type == Punishment.PunishmentType.BAN || type == Punishment.PunishmentType.TEMPBAN || type == Punishment.PunishmentType.KICK)) {
                    ((Player)target).kick(MessageUtil.formatKickScreen(plugin.getConfigManager().getStringList("messages." + finalTypeKey + ".kick-screen"), p));
                }
                String broadcastMsg = plugin.getConfigManager().getString("messages." + finalTypeKey + ".broadcast", "");
                if (!broadcastMsg.isEmpty()) {
                    if (silent) Bukkit.broadcast(MessageUtil.createComponent(plugin.getConfigManager().getString("messages." + finalTypeKey + ".silent.prefix", "&7(Silent) ") + broadcastMsg, p), "wapeb.notify");
                    else Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, p));
                }
            });
            sendResponse(exchange, 200, "{\"success\": true}");
        }
    }

    class PunishRemoveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            int id = Integer.parseInt(params.get("id"));
            UUID adminUuid = UUID.fromString(params.get("admin_uuid"));
            
            if (!checkPermission(adminUuid, "wapeb.unban")) {
                sendResponse(exchange, 403, "{\"error\": \"No permission\"}");
                return;
            }
            
            Punishment p = plugin.getDataManager().getPunishment(id);
            if (p != null) {
                p.setActive(false);
                plugin.getDataManager().savePunishment(p);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String typeKey = p.getType().name().toLowerCase();
                    String unKey = "unban";
                    if (typeKey.contains("ban")) unKey = "unban";
                    else if (typeKey.contains("mute")) unKey = "unmute";
                    else if (typeKey.contains("warn")) unKey = "unwarn";

                    String broadcastMsg = plugin.getConfigManager().getString("messages." + unKey + ".broadcast", "");
                    if (!broadcastMsg.isEmpty()) {
                        Punishment temp = new Punishment(p.getId(), p.getPlayerUuid(), p.getPlayerName(), p.getType(), p.getReason(), getAdminName(adminUuid), p.getDate(), p.getDuration());
                        Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, temp));
                    }
                });
                sendResponse(exchange, 200, "{\"success\": true}");
            } else {
                sendResponse(exchange, 404, "{\"error\": \"Punishment not found\"}");
            }
        }
    }

    class ActivePunishmentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            List<Punishment> all = plugin.getDataManager().getAllPunishments();
            JsonArray arr = new JsonArray();
            for (Punishment p : all) {
                // Csak azokat listázzuk ki, amik tényleg aktívak (nem Kick és nem manuálisan deaktivált)
                if (p.isActive()) {
                    JsonObject po = new JsonObject();
                    po.addProperty("id", p.getId());
                    po.addProperty("target", p.getPlayerName());
                    po.addProperty("type", p.getType().name());
                    po.addProperty("reason", p.getReason());
                    po.addProperty("executor", p.getExecutorName());
                    po.addProperty("date", p.getDate());
                    po.addProperty("uuid", p.getPlayerUuid() != null ? p.getPlayerUuid().toString() : "");
                    arr.add(po);
                }
            }
            sendResponse(exchange, 200, arr.toString());
        }
    }

    class PlayerPunishmentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            String playerName = params.get("player");
            if (playerName == null || playerName.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\": \"Missing player parameter\"}");
                return;
            }

            List<Punishment> punishments = plugin.getApi().getPunishments(playerName);
            JsonArray arr = new JsonArray();
            for (Punishment p : punishments) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", p.getId());
                obj.addProperty("player", p.getPlayerName());
                obj.addProperty("uuid", p.getPlayerUuid() != null ? p.getPlayerUuid().toString() : "");
                obj.addProperty("type", p.getType().name());
                obj.addProperty("reason", p.getReason());
                obj.addProperty("executor", p.getExecutorName());
                obj.addProperty("date", p.getDate());
                obj.addProperty("duration", p.getDuration());
                obj.addProperty("active", p.isActive());
                arr.add(obj);
            }
            sendResponse(exchange, 200, arr.toString());
        }
    }

    class CheckBanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            String playerName = params.get("player");
            if (playerName == null || playerName.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\": \"Missing player parameter\"}");
                return;
            }

            Punishment activeBan = plugin.getApi().getActiveBan(playerName);
            if (activeBan == null) {
                sendResponse(exchange, 200, "{\"banned\": false}");
            } else {
                JsonObject obj = new JsonObject();
                obj.addProperty("banned", true);
                obj.addProperty("id", activeBan.getId());
                obj.addProperty("player", activeBan.getPlayerName());
                obj.addProperty("type", activeBan.getType().name());
                obj.addProperty("reason", activeBan.getReason());
                obj.addProperty("executor", activeBan.getExecutorName());
                obj.addProperty("date", activeBan.getDate());
                obj.addProperty("end", activeBan.getEnd());
                sendResponse(exchange, 200, obj.toString());
            }
        }
    }

    class CheckMuteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
            String playerName = params.get("player");
            if (playerName == null || playerName.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\": \"Missing player parameter\"}");
                return;
            }

            Punishment activeMute = plugin.getApi().getActiveMute(playerName);
            if (activeMute == null) {
                sendResponse(exchange, 200, "{\"muted\": false}");
            } else {
                JsonObject obj = new JsonObject();
                obj.addProperty("muted", true);
                obj.addProperty("id", activeMute.getId());
                obj.addProperty("player", activeMute.getPlayerName());
                obj.addProperty("type", activeMute.getType().name());
                obj.addProperty("reason", activeMute.getReason());
                obj.addProperty("executor", activeMute.getExecutorName());
                obj.addProperty("date", activeMute.getDate());
                obj.addProperty("end", activeMute.getEnd());
                sendResponse(exchange, 200, obj.toString());
            }
        }
    }

    class CommandsListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isAuthenticated(exchange)) return;
            JsonObject resp = new JsonObject();
            JsonArray commandsArr = new JsonArray();

            String[] pluginCommands = new String[]{
                "ban", "banip", "kick", "kickall", "mute", "muteip", "unban", "unmute",
                "warn", "unwarn", "warnings", "history", "checkban", "checkmute",
                "freeze", "unfreeze", "alts", "banlist", "staffhistory", "lockdown",
                "wapeb", "globalunban", "punish", "punish-rollback"
            };

            for (String cmdName : pluginCommands) {
                JsonObject cmdObj = new JsonObject();
                cmdObj.addProperty("command", cmdName);
                List<String> customAliases = plugin.getCommandManager().getAliases(cmdName);
                JsonArray aliasesArr = new JsonArray();
                for (String alias : customAliases) {
                    aliasesArr.add(alias);
                }
                cmdObj.add("customAliases", aliasesArr);
                commandsArr.add(cmdObj);
            }
            resp.add("commands", commandsArr);
            sendResponse(exchange, 200, resp.toString());
        }
    }

    private String getPlayerRank(UUID uuid) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(uuid);
            if (user != null) return user.getPrimaryGroup();
            user = lp.getUserManager().loadUser(uuid).join();
            if (user != null) return user.getPrimaryGroup();
        } catch (Exception ignored) {}
        return "default";
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            try {
                String key = entry[0];
                String value = entry.length > 1 ? URLDecoder.decode(entry[1], StandardCharsets.UTF_8.toString()) : "";
                result.put(key, value);
            } catch (Exception e) {
                if (entry.length > 1) result.put(entry[0], entry[1]);
            }
        }
        return result;
    }
}