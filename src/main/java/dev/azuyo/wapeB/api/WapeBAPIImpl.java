package dev.azuyo.wapeB.api;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.api.events.*;
import dev.azuyo.wapeB.managers.*;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class WapeBAPIImpl implements WapeBAPI {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final PlayerDataManager playerDataManager;
    private final ConfigManager configManager;
    private final FreezeManager freezeManager;
    private final LockdownManager lockdownManager;
    private final CommandManager commandManager;

    private static final List<Punishment.PunishmentType> BAN_TYPES = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN
    );

    private static final List<Punishment.PunishmentType> MUTE_TYPES = Arrays.asList(
            Punishment.PunishmentType.MUTE,
            Punishment.PunishmentType.TEMPMUTE,
            Punishment.PunishmentType.IPMUTE,
            Punishment.PunishmentType.TEMPIPMUTE,
            Punishment.PunishmentType.SENTINEL_AUTO_MUTE,
            Punishment.PunishmentType.SENTINEL_AI_MUTE
    );

    private static final List<Punishment.PunishmentType> SHADOW_MUTE_TYPES = Arrays.asList(
            Punishment.PunishmentType.SHADOWMUTE,
            Punishment.PunishmentType.TEMPSHADOWMUTE,
            Punishment.PunishmentType.IPSHADOWMUTE,
            Punishment.PunishmentType.TEMPIPSHADOWMUTE
    );

    public WapeBAPIImpl(WapeB plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.playerDataManager = plugin.getPlayerDataManager();
        this.configManager = plugin.getConfigManager();
        this.freezeManager = plugin.getFreezeManager();
        this.lockdownManager = plugin.getLockdownManager();
        this.commandManager = commandManager;
    }

    // --- Query Methods ---

    @Override
    public List<Punishment> getPunishments(UUID playerUuid) {
        return dataManager.getHistory(playerUuid);
    }

    @Override
    public List<Punishment> getPunishments(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getPunishments(op.getUniqueId());
    }

    @Override
    public Punishment getActiveBan(UUID playerUuid) {
        return getActiveBanForPlayerOrAlt(playerUuid);
    }

    @Override
    public Punishment getActiveBan(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getActiveBanForPlayerOrAlt(op.getUniqueId());
    }

    @Override
    public Punishment getActiveBanByIp(String ipAddress) {
        List<UUID> alts = (ipAddress != null && !ipAddress.isEmpty()) ? playerDataManager.getPlayersByIp(ipAddress) : null;
        return dataManager.getActivePunishment(null, ipAddress, alts, BAN_TYPES);
    }

    @Override
    public Punishment getActiveBanForPlayerOrAlt(UUID playerUuid) {
        if (playerUuid == null) return null;
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerUuid);
        String ip = (op.isOnline() && op.getPlayer() != null && op.getPlayer().getAddress() != null)
                ? op.getPlayer().getAddress().getAddress().getHostAddress()
                : playerDataManager.getLastKnownIp(playerUuid);
        List<UUID> alts = (ip != null && !ip.isEmpty()) ? playerDataManager.getPlayersByIp(ip) : null;
        return dataManager.getActivePunishment(playerUuid, ip, alts, BAN_TYPES);
    }

    @Override
    public Punishment getActiveBanForPlayerOrAlt(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getActiveBanForPlayerOrAlt(op.getUniqueId());
    }

    @Override
    public Punishment getActiveMute(UUID playerUuid) {
        return getActiveMuteForPlayerOrAlt(playerUuid);
    }

    @Override
    public Punishment getActiveMute(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getActiveMuteForPlayerOrAlt(op.getUniqueId());
    }

    @Override
    public Punishment getActiveMuteByIp(String ipAddress) {
        List<UUID> alts = (ipAddress != null && !ipAddress.isEmpty()) ? playerDataManager.getPlayersByIp(ipAddress) : null;
        return dataManager.getActivePunishment(null, ipAddress, alts, MUTE_TYPES);
    }

    @Override
    public Punishment getActiveMuteForPlayerOrAlt(UUID playerUuid) {
        if (playerUuid == null) return null;
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerUuid);
        String ip = (op.isOnline() && op.getPlayer() != null && op.getPlayer().getAddress() != null)
                ? op.getPlayer().getAddress().getAddress().getHostAddress()
                : playerDataManager.getLastKnownIp(playerUuid);
        List<UUID> alts = (ip != null && !ip.isEmpty()) ? playerDataManager.getPlayersByIp(ip) : null;
        return dataManager.getActivePunishment(playerUuid, ip, alts, MUTE_TYPES);
    }

    @Override
    public Punishment getActiveMuteForPlayerOrAlt(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getActiveMuteForPlayerOrAlt(op.getUniqueId());
    }

    @Override
    public Punishment getActiveShadowMute(UUID playerUuid) {
        return getActiveShadowMuteForPlayerOrAlt(playerUuid);
    }

    @Override
    public Punishment getActiveShadowMute(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getActiveShadowMuteForPlayerOrAlt(op.getUniqueId());
    }

    @Override
    public Punishment getActiveShadowMuteByIp(String ipAddress) {
        List<UUID> alts = (ipAddress != null && !ipAddress.isEmpty()) ? playerDataManager.getPlayersByIp(ipAddress) : null;
        return dataManager.getActivePunishment(null, ipAddress, alts, SHADOW_MUTE_TYPES);
    }

    @Override
    public Punishment getActiveShadowMuteForPlayerOrAlt(UUID playerUuid) {
        if (playerUuid == null) return null;
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerUuid);
        String ip = (op.isOnline() && op.getPlayer() != null && op.getPlayer().getAddress() != null)
                ? op.getPlayer().getAddress().getAddress().getHostAddress()
                : playerDataManager.getLastKnownIp(playerUuid);
        List<UUID> alts = (ip != null && !ip.isEmpty()) ? playerDataManager.getPlayersByIp(ip) : null;
        return dataManager.getActivePunishment(playerUuid, ip, alts, SHADOW_MUTE_TYPES);
    }

    @Override
    public Punishment getActiveShadowMuteForPlayerOrAlt(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getActiveShadowMuteForPlayerOrAlt(op.getUniqueId());
    }

    @Override
    public List<Punishment> getWarnings(UUID playerUuid) {
        return dataManager.getWarnings(playerUuid);
    }

    @Override
    public List<Punishment> getWarnings(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getWarnings(op.getUniqueId());
    }

    @Override
    public List<Punishment> getHistory(UUID playerUuid) {
        return dataManager.getHistory(playerUuid);
    }

    @Override
    public List<Punishment> getHistory(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getHistory(op.getUniqueId());
    }

    @Override
    public List<Punishment> getStaffHistory(String executorName) {
        if (executorName == null || executorName.trim().isEmpty()) return Collections.emptyList();
        return dataManager.getStaffHistory(executorName);
    }

    @Override
    public boolean recordStaffAction(String staffName, String targetPlayer, Punishment.PunishmentType type, String reason, long duration) {
        if (staffName == null || staffName.trim().isEmpty() || targetPlayer == null || targetPlayer.trim().isEmpty()) {
            return false;
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetPlayer);
        return recordStaffAction(staffName, op.getUniqueId(), targetPlayer, type, reason, duration);
    }

    @Override
    public boolean recordStaffAction(String staffName, UUID targetUuid, String targetPlayer, Punishment.PunishmentType type, String reason, long duration) {
        return recordStaffAction(staffName, staffName, targetUuid, targetPlayer, type, reason, duration);
    }

    @Override
    public boolean recordStaffAction(String staffName, String executorDisplayName, String targetPlayer, Punishment.PunishmentType type, String reason, long duration) {
        if (staffName == null || staffName.trim().isEmpty() || targetPlayer == null || targetPlayer.trim().isEmpty()) {
            return false;
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetPlayer);
        return recordStaffAction(staffName, executorDisplayName, op.getUniqueId(), targetPlayer, type, reason, duration);
    }

    private boolean recordStaffAction(String staffName, String executorDisplayName, UUID targetUuid, String targetPlayer, Punishment.PunishmentType type, String reason, long duration) {
        if (staffName == null || staffName.trim().isEmpty() || targetPlayer == null || targetPlayer.trim().isEmpty()) {
            return false;
        }
        String ip = (targetUuid != null) ? playerDataManager.getLastKnownIp(targetUuid) : null;
        String finalExecutor = (executorDisplayName != null && !executorDisplayName.trim().isEmpty()) ? executorDisplayName : staffName;

        Punishment p = new Punishment(
                dataManager.getNextId(),
                targetUuid,
                targetPlayer,
                ip,
                type != null ? type : Punishment.PunishmentType.WARN,
                reason != null ? reason : "N/A",
                finalExecutor,
                System.currentTimeMillis(),
                duration
        );
        dataManager.savePunishment(p);
        return true;
    }

    @Override
    public boolean addStaffHistoryEntry(String staffName, Punishment punishment) {
        if (staffName == null || staffName.trim().isEmpty() || punishment == null) {
            return false;
        }
        String finalExecutor = (punishment.getExecutorName() != null && !punishment.getExecutorName().trim().isEmpty())
                ? punishment.getExecutorName()
                : staffName;

        Punishment entry = new Punishment(
                punishment.getId() <= 0 ? dataManager.getNextId() : punishment.getId(),
                punishment.getPlayerUuid(),
                punishment.getPlayerName(),
                punishment.getIpAddress(),
                punishment.getType(),
                punishment.getReason(),
                finalExecutor,
                punishment.getDate() <= 0 ? System.currentTimeMillis() : punishment.getDate(),
                punishment.getDuration()
        );
        dataManager.savePunishment(entry);
        return true;
    }

    @Override
    public List<String> getAlts(UUID playerUuid) {
        String ip = playerDataManager.getLastKnownIp(playerUuid);
        if (ip == null) return Collections.emptyList();
        return dataManager.getAltNamesByIp(ip, playerUuid);
    }

    @Override
    public List<String> getAlts(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getAlts(op.getUniqueId());
    }

    @Override
    public List<dev.azuyo.wapeB.utils.AltInfo> getDetailedAlts(UUID playerUuid) {
        return playerDataManager.getDetailedAlts(playerUuid);
    }

    @Override
    public List<dev.azuyo.wapeB.utils.AltInfo> getDetailedAlts(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getDetailedAlts(op.getUniqueId());
    }

    @Override
    public List<dev.azuyo.wapeB.utils.IpHistoryRecord> getIpHistory(UUID playerUuid) {
        return playerDataManager.getIpHistory(playerUuid);
    }

    @Override
    public List<dev.azuyo.wapeB.utils.IpHistoryRecord> getIpHistory(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getIpHistory(op.getUniqueId());
    }

    @Override
    public boolean isAltExempt(UUID playerUuid) {
        return playerDataManager.isAltExempt(playerUuid);
    }

    @Override
    public boolean isAltExempt(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return isAltExempt(op.getUniqueId());
    }

    @Override
    public boolean setAltExempt(UUID playerUuid, boolean exempt, String addedBy) {
        playerDataManager.setAltExempt(playerUuid, exempt, addedBy);
        return true;
    }

    @Override
    public boolean setAltExempt(String playerName, boolean exempt, String addedBy) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return setAltExempt(op.getUniqueId(), exempt, addedBy);
    }

    @Override
    public List<dev.azuyo.wapeB.utils.AltExemptInfo> getAllAltExempts() {
        return playerDataManager.getAllAltExempts();
    }

    @Override
    public dev.azuyo.wapeB.utils.AltExemptInfo getAltExemptDetails(UUID playerUuid) {
        return playerDataManager.getAltExemptDetails(playerUuid);
    }

    @Override
    public dev.azuyo.wapeB.utils.AltExemptInfo getAltExemptDetails(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getAltExemptDetails(op.getUniqueId());
    }


    @Override
    public boolean isBanned(UUID playerUuid) {
        return getActiveBan(playerUuid) != null;
    }

    @Override
    public boolean isBanned(String playerName) {
        return getActiveBan(playerName) != null;
    }

    @Override
    public boolean isBannedByIp(String ipAddress) {
        return getActiveBanByIp(ipAddress) != null;
    }

    @Override
    public boolean isBannedForPlayerOrAlt(UUID playerUuid) {
        return getActiveBanForPlayerOrAlt(playerUuid) != null;
    }

    @Override
    public boolean isBannedForPlayerOrAlt(String playerName) {
        return getActiveBanForPlayerOrAlt(playerName) != null;
    }

    @Override
    public boolean isMuted(UUID playerUuid) {
        return getActiveMute(playerUuid) != null;
    }

    @Override
    public boolean isMuted(String playerName) {
        return getActiveMute(playerName) != null;
    }

    @Override
    public boolean isMutedByIp(String ipAddress) {
        return getActiveMuteByIp(ipAddress) != null;
    }

    @Override
    public boolean isMutedForPlayerOrAlt(UUID playerUuid) {
        return getActiveMuteForPlayerOrAlt(playerUuid) != null;
    }

    @Override
    public boolean isMutedForPlayerOrAlt(String playerName) {
        return getActiveMuteForPlayerOrAlt(playerName) != null;
    }

    @Override
    public boolean isShadowMuted(UUID playerUuid) {
        return getActiveShadowMute(playerUuid) != null;
    }

    @Override
    public boolean isShadowMuted(String playerName) {
        return getActiveShadowMute(playerName) != null;
    }

    @Override
    public boolean isShadowMutedByIp(String ipAddress) {
        return getActiveShadowMuteByIp(ipAddress) != null;
    }

    @Override
    public boolean isShadowMutedForPlayerOrAlt(UUID playerUuid) {
        return getActiveShadowMuteForPlayerOrAlt(playerUuid) != null;
    }

    @Override
    public boolean isShadowMutedForPlayerOrAlt(String playerName) {
        return getActiveShadowMuteForPlayerOrAlt(playerName) != null;
    }

    @Override
    public boolean isFrozen(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        return player != null && freezeManager.isFrozen(player);
    }

    @Override
    public boolean isFrozen(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        return player != null && freezeManager.isFrozen(player);
    }

    @Override
    public boolean isLockdownActive() {
        return lockdownManager.isLockdownEnabled();
    }

    @Override
    public String getLockdownReason() {
        return lockdownManager.getLockdownReason();
    }

    // --- Execution Methods ---

    @Override
    public boolean banPlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipBan) {
        String currentServer = configManager.getString("server-name", "Lobby");
        return banPlayer(target, reason, executor, duration, silent, ipBan, "global", currentServer);
    }

    @Override
    public boolean banPlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipBan) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        String currentServer = configManager.getString("server-name", "Lobby");
        return banPlayer(op.getUniqueId(), reason, executor, duration, silent, ipBan, "global", currentServer);
    }

    @Override
    public boolean banPlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipBan, String activeServer) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        String currentServer = configManager.getString("server-name", "Lobby");
        return banPlayer(op.getUniqueId(), reason, executor, duration, silent, ipBan, activeServer, currentServer);
    }

    @Override
    public boolean banPlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipBan, String activeServer) {
        String currentServer = configManager.getString("server-name", "Lobby");
        return banPlayer(target, reason, executor, duration, silent, ipBan, activeServer, currentServer);
    }

    @Override
    public boolean banPlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipBan, String activeServer, String server) {
        return banPlayer(targetName, reason, executor, duration, silent, ipBan, activeServer, server, null);
    }

    @Override
    public boolean banPlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipBan, String activeServer, String server) {
        return banPlayer(target, reason, executor, duration, silent, ipBan, activeServer, server, null);
    }

    @Override
    public boolean banPlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipBan, String activeServer, String server, String proof) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        return banPlayer(op.getUniqueId(), reason, executor, duration, silent, ipBan, activeServer, server, proof);
    }

    @Override
    public boolean banPlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipBan, String activeServer, String server, String proof) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        String targetName = op.getName() != null ? op.getName() : target.toString();
        String targetIp = op.isOnline() && ((Player)op).getAddress() != null 
                ? ((Player)op).getAddress().getAddress().getHostAddress() 
                : playerDataManager.getLastKnownIp(target);

        Punishment.PunishmentType type;
        if (ipBan) {
            type = (duration == -1) ? Punishment.PunishmentType.IPBAN : Punishment.PunishmentType.TEMPIPBAN;
        } else {
            type = (duration == -1) ? Punishment.PunishmentType.BAN : Punishment.PunishmentType.TEMPBAN;
        }

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent FIRED for " + targetName + " | Original Executor: '" + executor + "' | Type: " + type + " | ActiveServer: " + activeServer + " | Server: " + server + " | Proof: " + proof);

        PlayerPunishEvent event = new PlayerPunishEvent(target, targetName, targetIp, type, reason, executor, activeServer, server, proof, duration, silent);
        Bukkit.getPluginManager().callEvent(event);

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent PROCESSED for " + targetName + " | Final Executor: '" + event.getExecutor() + "' | Cancelled: " + event.isCancelled());

        if (event.isCancelled()) return false;

        // Deactivate existing ban
        Punishment existingBan = dataManager.getActivePunishment(target, targetIp, BAN_TYPES);
        if (existingBan != null) {
            existingBan.setActive(false);
            dataManager.savePunishment(existingBan);
        }

        Punishment p = new Punishment(dataManager.getNextId(), target, targetName, targetIp, type, event.getReason(), event.getExecutor(), event.getActiveServer(), event.getServer(), event.getProof(), System.currentTimeMillis(), event.getDuration());
        dataManager.savePunishment(p);
        WebhookUtil.sendPunishmentWebhook(p);

        if (plugin.getChatSnapshotManager() != null) {
            dev.azuyo.wapeB.utils.ChatSnapshot snapshot = event.getChatSnapshot() != null ? event.getChatSnapshot() : plugin.getChatSnapshotManager().captureSnapshot(p);
            if (snapshot != null) {
                plugin.getChatSnapshotManager().saveSnapshotAsync(snapshot);
            }
        }

        String broadcastMsg = configManager.getString("messages.ban.broadcast", "%prefix% %executor% banned %player%.");
        if (plugin.getPluginMessageManager() != null) {
            plugin.getPluginMessageManager().sendPunishmentBroadcast(type.name(), p, event.isSilent(), broadcastMsg);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String currentServer = configManager.getString("server-name", "Lobby");
            boolean appliesToThisServer = p.isAppliesTo(currentServer);

            if (op.isOnline() && appliesToThisServer) {
                ((Player)op).kick(MessageUtil.formatKickScreen(configManager.getStringList("messages.ban.kick-screen"), p));
            }

            boolean showRemoteBroadcast = configManager.getBoolean("broadcast.show-remote-punishments", true);
            if (appliesToThisServer || showRemoteBroadcast) {
                if (event.isSilent()) {
                    Bukkit.broadcast(MessageUtil.createComponent(configManager.getString("messages.ban.silent.prefix", "&7(Silent) ") + broadcastMsg, p), "wapeb.notify");
                } else {
                    Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, p));
                }
            }
        });

        return true;
    }

    @Override
    public boolean mutePlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipMute) {
        String currentServer = configManager.getString("server-name", "Lobby");
        return mutePlayer(target, reason, executor, duration, silent, ipMute, "global", currentServer);
    }

    @Override
    public boolean mutePlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipMute) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        String currentServer = configManager.getString("server-name", "Lobby");
        return mutePlayer(op.getUniqueId(), reason, executor, duration, silent, ipMute, "global", currentServer);
    }

    @Override
    public boolean mutePlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        String currentServer = configManager.getString("server-name", "Lobby");
        return mutePlayer(op.getUniqueId(), reason, executor, duration, silent, ipMute, activeServer, currentServer);
    }

    @Override
    public boolean mutePlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer) {
        String currentServer = configManager.getString("server-name", "Lobby");
        return mutePlayer(target, reason, executor, duration, silent, ipMute, activeServer, currentServer);
    }

    @Override
    public boolean mutePlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer, String server) {
        return mutePlayer(targetName, reason, executor, duration, silent, ipMute, activeServer, server, null);
    }

    @Override
    public boolean mutePlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer, String server) {
        return mutePlayer(target, reason, executor, duration, silent, ipMute, activeServer, server, null);
    }

    @Override
    public boolean mutePlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer, String server, String proof) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        return mutePlayer(op.getUniqueId(), reason, executor, duration, silent, ipMute, activeServer, server, proof);
    }

    @Override
    public boolean mutePlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer, String server, String proof) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        String targetName = op.getName() != null ? op.getName() : target.toString();
        String targetIp = op.isOnline() && ((Player)op).getAddress() != null 
                ? ((Player)op).getAddress().getAddress().getHostAddress() 
                : playerDataManager.getLastKnownIp(target);

        Punishment.PunishmentType type;
        if (ipMute) {
            type = (duration == -1) ? Punishment.PunishmentType.IPMUTE : Punishment.PunishmentType.TEMPIPMUTE;
        } else {
            type = (duration == -1) ? Punishment.PunishmentType.MUTE : Punishment.PunishmentType.TEMPMUTE;
        }

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent FIRED for " + targetName + " | Original Executor: '" + executor + "' | Type: " + type + " | ActiveServer: " + activeServer + " | Server: " + server + " | Proof: " + proof);

        PlayerPunishEvent event = new PlayerPunishEvent(target, targetName, targetIp, type, reason, executor, activeServer, server, proof, duration, silent);
        Bukkit.getPluginManager().callEvent(event);

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent PROCESSED for " + targetName + " | Final Executor: '" + event.getExecutor() + "' | Cancelled: " + event.isCancelled());
        if (event.isCancelled()) return false;

        Punishment existingMute = dataManager.getActivePunishment(target, targetIp, MUTE_TYPES);
        if (existingMute != null) {
            existingMute.setActive(false);
            dataManager.savePunishment(existingMute);
        }

        Punishment p = new Punishment(dataManager.getNextId(), target, targetName, targetIp, type, event.getReason(), event.getExecutor(), event.getActiveServer(), event.getServer(), event.getProof(), System.currentTimeMillis(), event.getDuration());
        dataManager.savePunishment(p);
        WebhookUtil.sendPunishmentWebhook(p);

        if (plugin.getChatSnapshotManager() != null) {
            dev.azuyo.wapeB.utils.ChatSnapshot snapshot = event.getChatSnapshot() != null ? event.getChatSnapshot() : plugin.getChatSnapshotManager().captureSnapshot(p);
            if (snapshot != null) {
                plugin.getChatSnapshotManager().saveSnapshotAsync(snapshot);
            }
        }

        String broadcastMsg = configManager.getString("messages.mute.broadcast", "%prefix% %executor% muted %player%.");
        if (plugin.getPluginMessageManager() != null) {
            plugin.getPluginMessageManager().sendPunishmentBroadcast(type.name(), p, event.isSilent(), broadcastMsg);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String currentServer = configManager.getString("server-name", "Lobby");
            boolean appliesToThisServer = p.isAppliesTo(currentServer);

            boolean showRemoteBroadcast = configManager.getBoolean("broadcast.show-remote-punishments", true);
            if (appliesToThisServer || showRemoteBroadcast) {
                if (event.isSilent()) {
                    Bukkit.broadcast(MessageUtil.createComponent(configManager.getString("messages.mute.silent.prefix", "&7(Silent) ") + broadcastMsg, p), "wapeb.notify");
                } else {
                    Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, p));
                }
            }
        });

        return true;
    }

    @Override
    public boolean shadowMutePlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipMute) {
        String currentServer = configManager.getString("server-name", "Lobby");
        return shadowMutePlayer(target, reason, executor, duration, silent, ipMute, "global", currentServer);
    }

    @Override
    public boolean shadowMutePlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipMute) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        String currentServer = configManager.getString("server-name", "Lobby");
        return shadowMutePlayer(op.getUniqueId(), reason, executor, duration, silent, ipMute, "global", currentServer);
    }

    @Override
    public boolean shadowMutePlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        String currentServer = configManager.getString("server-name", "Lobby");
        return shadowMutePlayer(op.getUniqueId(), reason, executor, duration, silent, ipMute, activeServer, currentServer);
    }

    @Override
    public boolean shadowMutePlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer) {
        String currentServer = configManager.getString("server-name", "Lobby");
        return shadowMutePlayer(target, reason, executor, duration, silent, ipMute, activeServer, currentServer);
    }

    @Override
    public boolean shadowMutePlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer, String server) {
        return shadowMutePlayer(targetName, reason, executor, duration, silent, ipMute, activeServer, server, null);
    }

    @Override
    public boolean shadowMutePlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer, String server) {
        return shadowMutePlayer(target, reason, executor, duration, silent, ipMute, activeServer, server, null);
    }

    @Override
    public boolean shadowMutePlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer, String server, String proof) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        return shadowMutePlayer(op.getUniqueId(), reason, executor, duration, silent, ipMute, activeServer, server, proof);
    }

    @Override
    public boolean shadowMutePlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipMute, String activeServer, String server, String proof) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        String targetName = op.getName() != null ? op.getName() : target.toString();
        String targetIp = op.isOnline() && ((Player)op).getAddress() != null 
                ? ((Player)op).getAddress().getAddress().getHostAddress() 
                : playerDataManager.getLastKnownIp(target);

        Punishment.PunishmentType type;
        if (ipMute) {
            type = (duration == -1) ? Punishment.PunishmentType.IPSHADOWMUTE : Punishment.PunishmentType.TEMPIPSHADOWMUTE;
        } else {
            type = (duration == -1) ? Punishment.PunishmentType.SHADOWMUTE : Punishment.PunishmentType.TEMPSHADOWMUTE;
        }

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent FIRED for " + targetName + " | Original Executor: '" + executor + "' | Type: " + type + " | ActiveServer: " + activeServer + " | Server: " + server + " | Proof: " + proof);

        PlayerPunishEvent event = new PlayerPunishEvent(target, targetName, targetIp, type, reason, executor, activeServer, server, proof, duration, silent);
        Bukkit.getPluginManager().callEvent(event);

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent PROCESSED for " + targetName + " | Final Executor: '" + event.getExecutor() + "' | Cancelled: " + event.isCancelled());
        if (event.isCancelled()) return false;

        Punishment existingShadowMute = dataManager.getActivePunishment(target, targetIp, SHADOW_MUTE_TYPES);
        if (existingShadowMute != null) {
            existingShadowMute.setActive(false);
            dataManager.savePunishment(existingShadowMute);
        }

        Punishment p = new Punishment(dataManager.getNextId(), target, targetName, targetIp, type, event.getReason(), event.getExecutor(), event.getActiveServer(), event.getServer(), event.getProof(), System.currentTimeMillis(), event.getDuration());
        dataManager.savePunishment(p);
        WebhookUtil.sendPunishmentWebhook(p);

        if (plugin.getChatSnapshotManager() != null) {
            dev.azuyo.wapeB.utils.ChatSnapshot snapshot = event.getChatSnapshot() != null ? event.getChatSnapshot() : plugin.getChatSnapshotManager().captureSnapshot(p);
            if (snapshot != null) {
                plugin.getChatSnapshotManager().saveSnapshotAsync(snapshot);
            }
        }

        String broadcastMsg = configManager.getString("messages.shadowmute.broadcast", "%prefix% %executor% shadow-muted %player%.");
        if (plugin.getPluginMessageManager() != null) {
            plugin.getPluginMessageManager().sendPunishmentBroadcast(type.name(), p, event.isSilent(), broadcastMsg);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String currentServer = configManager.getString("server-name", "Lobby");
            boolean appliesToThisServer = p.isAppliesTo(currentServer);

            boolean showRemoteBroadcast = configManager.getBoolean("broadcast.show-remote-punishments", true);
            if (appliesToThisServer || showRemoteBroadcast) {
                String staffPerm = configManager.getString("messages.punishment-notification.permission", "wapeb.notify.punishment");
                if (event.isSilent()) {
                    Bukkit.broadcast(MessageUtil.createComponent(configManager.getString("messages.shadowmute.silent.prefix", "&7(Silent) ") + broadcastMsg, p), "wapeb.notify");
                } else {
                    Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, p), staffPerm);
                }
            }
        });

        return true;
    }

    @Override
    public boolean warnPlayer(UUID target, String reason, String executor, boolean silent) {
        String currentServer = configManager.getString("server-name", "Lobby");
        return warnPlayer(target, reason, executor, silent, "global", currentServer);
    }

    @Override
    public boolean warnPlayer(String targetName, String reason, String executor, boolean silent) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        String currentServer = configManager.getString("server-name", "Lobby");
        return warnPlayer(op.getUniqueId(), reason, executor, silent, "global", currentServer);
    }

    @Override
    public boolean warnPlayer(String targetName, String reason, String executor, boolean silent, String activeServer) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        String currentServer = configManager.getString("server-name", "Lobby");
        return warnPlayer(op.getUniqueId(), reason, executor, silent, activeServer, currentServer);
    }

    @Override
    public boolean warnPlayer(UUID target, String reason, String executor, boolean silent, String activeServer) {
        String currentServer = configManager.getString("server-name", "Lobby");
        return warnPlayer(target, reason, executor, silent, activeServer, currentServer);
    }

    @Override
    public boolean warnPlayer(String targetName, String reason, String executor, boolean silent, String activeServer, String server) {
        return warnPlayer(targetName, reason, executor, silent, activeServer, server, null);
    }

    @Override
    public boolean warnPlayer(UUID target, String reason, String executor, boolean silent, String activeServer, String server) {
        return warnPlayer(target, reason, executor, silent, activeServer, server, null);
    }

    @Override
    public boolean warnPlayer(String targetName, String reason, String executor, boolean silent, String activeServer, String server, String proof) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        return warnPlayer(op.getUniqueId(), reason, executor, silent, activeServer, server, proof);
    }

    @Override
    public boolean warnPlayer(UUID target, String reason, String executor, boolean silent, String activeServer, String server, String proof) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        String targetName = op.getName() != null ? op.getName() : target.toString();

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent FIRED for " + targetName + " | Original Executor: '" + executor + "' | Type: WARN | ActiveServer: " + activeServer + " | Server: " + server + " | Proof: " + proof);

        PlayerPunishEvent event = new PlayerPunishEvent(target, targetName, null, Punishment.PunishmentType.WARN, reason, executor, activeServer, server, proof, -1, silent);
        Bukkit.getPluginManager().callEvent(event);

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent PROCESSED for " + targetName + " | Final Executor: '" + event.getExecutor() + "' | Cancelled: " + event.isCancelled());

        if (event.isCancelled()) return false;

        Punishment p = new Punishment(dataManager.getNextId(), target, targetName, null, Punishment.PunishmentType.WARN, event.getReason(), event.getExecutor(), event.getActiveServer(), event.getServer(), event.getProof(), System.currentTimeMillis(), -1);
        dataManager.savePunishment(p);
        WebhookUtil.sendPunishmentWebhook(p);

        if (plugin.getChatSnapshotManager() != null) {
            dev.azuyo.wapeB.utils.ChatSnapshot snapshot = event.getChatSnapshot() != null ? event.getChatSnapshot() : plugin.getChatSnapshotManager().captureSnapshot(p);
            if (snapshot != null) {
                plugin.getChatSnapshotManager().saveSnapshotAsync(snapshot);
            }
        }

        checkWarnActions(target, targetName);

        String broadcastMsg = configManager.getString("messages.warn.broadcast", "%prefix% %executor% warned %player%.");
        if (plugin.getPluginMessageManager() != null) {
            plugin.getPluginMessageManager().sendPunishmentBroadcast("WARN", p, event.isSilent(), broadcastMsg);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String currentServer = configManager.getString("server-name", "Lobby");
            boolean appliesToThisServer = p.isAppliesTo(currentServer);

            if (op.isOnline() && appliesToThisServer) {
                ((Player)op).sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.target-notify", "&cYou have been warned for: %reason%"), p));
            }
            boolean showRemoteBroadcast = configManager.getBoolean("broadcast.show-remote-punishments", true);
            if (appliesToThisServer || showRemoteBroadcast) {
                if (event.isSilent()) {
                    Bukkit.broadcast(MessageUtil.createComponent(configManager.getString("messages.warn.silent.prefix", "&7(Silent) ") + broadcastMsg, p), "wapeb.notify");
                } else {
                    Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, p));
                }
            }
        });

        return true;
    }

    @Override
    public boolean kickPlayer(UUID target, String reason, String executor, boolean silent) {
        String currentServer = configManager.getString("server-name", "Lobby");
        return kickPlayer(target, reason, executor, silent, "global", currentServer);
    }

    @Override
    public boolean kickPlayer(String targetName, String reason, String executor, boolean silent) {
        Player player = Bukkit.getPlayer(targetName);
        if (player == null) return false;
        String currentServer = configManager.getString("server-name", "Lobby");
        return kickPlayer(player.getUniqueId(), reason, executor, silent, "global", currentServer);
    }

    @Override
    public boolean kickPlayer(String targetName, String reason, String executor, boolean silent, String activeServer) {
        Player player = Bukkit.getPlayer(targetName);
        if (player == null) return false;
        String currentServer = configManager.getString("server-name", "Lobby");
        return kickPlayer(player.getUniqueId(), reason, executor, silent, activeServer, currentServer);
    }

    @Override
    public boolean kickPlayer(UUID target, String reason, String executor, boolean silent, String activeServer) {
        String currentServer = configManager.getString("server-name", "Lobby");
        return kickPlayer(target, reason, executor, silent, activeServer, currentServer);
    }

    @Override
    public boolean kickPlayer(String targetName, String reason, String executor, boolean silent, String activeServer, String server) {
        return kickPlayer(targetName, reason, executor, silent, activeServer, server, null);
    }

    @Override
    public boolean kickPlayer(UUID target, String reason, String executor, boolean silent, String activeServer, String server) {
        return kickPlayer(target, reason, executor, silent, activeServer, server, null);
    }

    @Override
    public boolean kickPlayer(String targetName, String reason, String executor, boolean silent, String activeServer, String server, String proof) {
        Player player = Bukkit.getPlayer(targetName);
        if (player == null) return false;
        return kickPlayer(player.getUniqueId(), reason, executor, silent, activeServer, server, proof);
    }

    @Override
    public boolean kickPlayer(UUID target, String reason, String executor, boolean silent, String activeServer, String server, String proof) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        if (!op.isOnline()) return false;

        Player onlineTarget = (Player) op;

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent FIRED for " + onlineTarget.getName() + " | Original Executor: '" + executor + "' | Type: KICK | ActiveServer: " + activeServer + " | Server: " + server + " | Proof: " + proof);

        PlayerPunishEvent event = new PlayerPunishEvent(target, onlineTarget.getName(), null, Punishment.PunishmentType.KICK, reason, executor, activeServer, server, proof, -1, silent);
        Bukkit.getPluginManager().callEvent(event);

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent PROCESSED for " + onlineTarget.getName() + " | Final Executor: '" + event.getExecutor() + "' | Cancelled: " + event.isCancelled());

        if (event.isCancelled()) return false;

        Punishment p = new Punishment(dataManager.getNextId(), target, onlineTarget.getName(), null, Punishment.PunishmentType.KICK, event.getReason(), event.getExecutor(), event.getActiveServer(), event.getServer(), event.getProof(), System.currentTimeMillis(), -1);
        p.setActive(false);
        dataManager.savePunishment(p);
        WebhookUtil.sendPunishmentWebhook(p);

        if (plugin.getChatSnapshotManager() != null) {
            dev.azuyo.wapeB.utils.ChatSnapshot snapshot = event.getChatSnapshot() != null ? event.getChatSnapshot() : plugin.getChatSnapshotManager().captureSnapshot(p);
            if (snapshot != null) {
                plugin.getChatSnapshotManager().saveSnapshotAsync(snapshot);
            }
        }

        String broadcastMsg = configManager.getString("messages.kick.broadcast", "%prefix% %executor% kicked %player%.");
        if (plugin.getPluginMessageManager() != null) {
            plugin.getPluginMessageManager().sendPunishmentBroadcast("KICK", p, event.isSilent(), broadcastMsg);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String currentServer = configManager.getString("server-name", "Lobby");
            boolean appliesToThisServer = p.isAppliesTo(currentServer);

            if (appliesToThisServer) {
                onlineTarget.kick(MessageUtil.formatKickScreen(configManager.getStringList("messages.kick.kick-screen"), p));
            }
            boolean showRemoteBroadcast = configManager.getBoolean("broadcast.show-remote-punishments", true);
            if (appliesToThisServer || showRemoteBroadcast) {
                if (event.isSilent()) {
                    Bukkit.broadcast(MessageUtil.createComponent(configManager.getString("messages.kick.silent.prefix", "&7(Silent) ") + broadcastMsg, p), "wapeb.notify");
                } else {
                    Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, p));
                }
            }
        });

        return true;
    }

    @Override
    public boolean freezePlayer(UUID target, String reason, String executor) {
        Player player = Bukkit.getPlayer(target);
        if (player == null) return false;

        PlayerFreezeEvent event = new PlayerFreezeEvent(player, reason, executor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        freezeManager.freezePlayer(player);
        player.sendMessage(MessageUtil.createComponent(configManager.getString("messages.freeze.frozen-target", "&cYou have been frozen!"), null));
        return true;
    }

    @Override
    public boolean freezePlayer(String targetName, String reason, String executor) {
        Player player = Bukkit.getPlayer(targetName);
        if (player == null) return false;
        return freezePlayer(player.getUniqueId(), reason, executor);
    }

    @Override
    public boolean unfreezePlayer(UUID target, String executor) {
        Player player = Bukkit.getPlayer(target);
        if (player == null) return false;

        PlayerUnfreezeEvent event = new PlayerUnfreezeEvent(player, executor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        freezeManager.unfreezePlayer(player);
        player.sendMessage(MessageUtil.createComponent(configManager.getString("messages.freeze.unfrozen-target", "&aYou have been unfrozen."), null));
        return true;
    }

    @Override
    public boolean unfreezePlayer(String targetName, String executor) {
        Player player = Bukkit.getPlayer(targetName);
        if (player == null) return false;
        return unfreezePlayer(player.getUniqueId(), executor);
    }

    @Override
    public boolean unbanPlayer(UUID target, String reason, String executor) {
        if (target == null) return false;
        String ip = playerDataManager.getLastKnownIp(target);
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        String name = op != null ? op.getName() : null;
        List<UUID> alts = (ip != null && !ip.isEmpty()) ? playerDataManager.getPlayersByIp(ip) : null;
        Punishment activeBan = dataManager.getActivePunishment(target, name, ip, alts, BAN_TYPES);
        return unbanPunishment(activeBan, reason, executor);
    }

    @Override
    public boolean unbanPlayer(String targetName, String reason, String executor) {
        if (targetName == null || targetName.isEmpty()) return false;
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = op != null ? op.getUniqueId() : null;
        String ip = targetUuid != null ? playerDataManager.getLastKnownIp(targetUuid) : null;
        List<UUID> alts = (ip != null && !ip.isEmpty()) ? playerDataManager.getPlayersByIp(ip) : null;
        Punishment activeBan = dataManager.getActivePunishment(targetUuid, targetName, ip, alts, BAN_TYPES);
        return unbanPunishment(activeBan, reason, executor);
    }

    private boolean unbanPunishment(Punishment activeBan, String reason, String executor) {
        if (activeBan == null) return false;

        PlayerUnpunishEvent event = new PlayerUnpunishEvent(activeBan, executor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        activeBan.setActive(false);
        dataManager.savePunishment(activeBan);

        String broadcastMsg = configManager.getString("messages.unban.broadcast", "%prefix% %executor% unbanned %player%.");
        Punishment temp = new Punishment(activeBan.getId(), activeBan.getPlayerUuid(), activeBan.getPlayerName(), activeBan.getIpAddress(), activeBan.getType(), reason, executor, activeBan.getServer(), activeBan.getDate(), activeBan.getDuration());

        if (plugin.getPluginMessageManager() != null && !broadcastMsg.isEmpty()) {
            plugin.getPluginMessageManager().sendPunishmentBroadcast("UNBAN", temp, false, broadcastMsg);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String currentServer = configManager.getString("server-name", "Lobby");
            boolean appliesToThisServer = activeBan.getServer() == null || activeBan.getServer().equalsIgnoreCase("global") || activeBan.getServer().equalsIgnoreCase("all") || activeBan.getServer().equalsIgnoreCase(currentServer);

            boolean showRemoteBroadcast = configManager.getBoolean("broadcast.show-remote-punishments", true);
            if (appliesToThisServer || showRemoteBroadcast) {
                if (!broadcastMsg.isEmpty()) {
                    Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, temp));
                }
            }
        });

        return true;
    }

    @Override
    public boolean unmutePlayer(UUID target, String reason, String executor) {
        if (target == null) return false;
        String ip = playerDataManager.getLastKnownIp(target);
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        String name = op != null ? op.getName() : null;
        List<UUID> alts = (ip != null && !ip.isEmpty()) ? playerDataManager.getPlayersByIp(ip) : null;
        Punishment activeMute = dataManager.getActivePunishment(target, name, ip, alts, MUTE_TYPES);
        if (activeMute == null) {
            activeMute = dataManager.getActivePunishment(target, name, ip, alts, SHADOW_MUTE_TYPES);
            if (activeMute != null) {
                return unshadowMutePunishment(activeMute, reason, executor);
            }
        }
        return unmutePunishment(activeMute, reason, executor);
    }

    @Override
    public boolean unmutePlayer(String targetName, String reason, String executor) {
        if (targetName == null || targetName.isEmpty()) return false;
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = op != null ? op.getUniqueId() : null;
        String ip = targetUuid != null ? playerDataManager.getLastKnownIp(targetUuid) : null;
        List<UUID> alts = (ip != null && !ip.isEmpty()) ? playerDataManager.getPlayersByIp(ip) : null;
        Punishment activeMute = dataManager.getActivePunishment(targetUuid, targetName, ip, alts, MUTE_TYPES);
        if (activeMute == null) {
            activeMute = dataManager.getActivePunishment(targetUuid, targetName, ip, alts, SHADOW_MUTE_TYPES);
            if (activeMute != null) {
                return unshadowMutePunishment(activeMute, reason, executor);
            }
        }
        return unmutePunishment(activeMute, reason, executor);
    }

    private boolean unmutePunishment(Punishment activeMute, String reason, String executor) {
        if (activeMute == null) return false;

        PlayerUnpunishEvent event = new PlayerUnpunishEvent(activeMute, executor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        activeMute.setActive(false);
        dataManager.savePunishment(activeMute);

        String broadcastMsg = configManager.getString("messages.unmute.broadcast", "%prefix% %executor% unmuted %player%.");
        Punishment temp = new Punishment(activeMute.getId(), activeMute.getPlayerUuid(), activeMute.getPlayerName(), activeMute.getIpAddress(), activeMute.getType(), reason, executor, activeMute.getServer(), activeMute.getDate(), activeMute.getDuration());

        if (plugin.getPluginMessageManager() != null && !broadcastMsg.isEmpty()) {
            plugin.getPluginMessageManager().sendPunishmentBroadcast("UNMUTE", temp, false, broadcastMsg);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String currentServer = configManager.getString("server-name", "Lobby");
            boolean appliesToThisServer = activeMute.getServer() == null || activeMute.getServer().equalsIgnoreCase("global") || activeMute.getServer().equalsIgnoreCase("all") || activeMute.getServer().equalsIgnoreCase(currentServer);

            boolean showRemoteBroadcast = configManager.getBoolean("broadcast.show-remote-punishments", true);
            if (appliesToThisServer || showRemoteBroadcast) {
                if (!broadcastMsg.isEmpty()) {
                    Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, temp));
                }
            }
        });

        return true;
    }

    @Override
    public boolean unshadowMutePlayer(UUID target, String reason, String executor) {
        if (target == null) return false;
        String ip = playerDataManager.getLastKnownIp(target);
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        String name = op != null ? op.getName() : null;
        List<UUID> alts = (ip != null && !ip.isEmpty()) ? playerDataManager.getPlayersByIp(ip) : null;
        Punishment activeMute = dataManager.getActivePunishment(target, name, ip, alts, SHADOW_MUTE_TYPES);
        return unshadowMutePunishment(activeMute, reason, executor);
    }

    @Override
    public boolean unshadowMutePlayer(String targetName, String reason, String executor) {
        if (targetName == null || targetName.isEmpty()) return false;
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = op != null ? op.getUniqueId() : null;
        String ip = targetUuid != null ? playerDataManager.getLastKnownIp(targetUuid) : null;
        List<UUID> alts = (ip != null && !ip.isEmpty()) ? playerDataManager.getPlayersByIp(ip) : null;
        Punishment activeMute = dataManager.getActivePunishment(targetUuid, targetName, ip, alts, SHADOW_MUTE_TYPES);
        return unshadowMutePunishment(activeMute, reason, executor);
    }

    private boolean unshadowMutePunishment(Punishment activeMute, String reason, String executor) {
        if (activeMute == null) return false;

        PlayerUnpunishEvent event = new PlayerUnpunishEvent(activeMute, executor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        activeMute.setActive(false);
        dataManager.savePunishment(activeMute);

        String broadcastMsg = configManager.getString("messages.unshadowmute.broadcast", "%prefix% %executor% un-shadowmuted %player%.");
        Punishment temp = new Punishment(activeMute.getId(), activeMute.getPlayerUuid(), activeMute.getPlayerName(), activeMute.getIpAddress(), activeMute.getType(), reason, executor, activeMute.getServer(), activeMute.getDate(), activeMute.getDuration());

        if (plugin.getPluginMessageManager() != null && !broadcastMsg.isEmpty()) {
            plugin.getPluginMessageManager().sendPunishmentBroadcast("UNSHADOWMUTE", temp, false, broadcastMsg);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String currentServer = configManager.getString("server-name", "Lobby");
            boolean appliesToThisServer = activeMute.getServer() == null || activeMute.getServer().equalsIgnoreCase("global") || activeMute.getServer().equalsIgnoreCase("all") || activeMute.getServer().equalsIgnoreCase(currentServer);

            boolean showRemoteBroadcast = configManager.getBoolean("broadcast.show-remote-punishments", true);
            if (appliesToThisServer || showRemoteBroadcast) {
                if (!broadcastMsg.isEmpty()) {
                    String staffPerm = configManager.getString("messages.punishment-notification.permission", "wapeb.notify.punishment");
                    Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, temp), staffPerm);
                }
            }
        });

        return true;
    }

    @Override
    public boolean revokePunishment(int punishmentId, String executor) {
        Punishment p = dataManager.getPunishment(punishmentId);
        if (p == null) return false;

        PlayerUnpunishEvent event = new PlayerUnpunishEvent(p, executor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        p.setActive(false);
        dataManager.savePunishment(p);
        return true;
    }

    @Override
    public String getProof(int punishmentId) {
        Punishment p = dataManager.getPunishment(punishmentId);
        return (p != null) ? p.getProof() : null;
    }

    @Override
    public boolean setProof(int punishmentId, String proofUrl) {
        Punishment p = dataManager.getPunishment(punishmentId);
        if (p == null) return false;
        p.setProof(proofUrl);
        dataManager.savePunishment(p);
        return true;
    }

    @Override
    public boolean removeProof(int punishmentId) {
        Punishment p = dataManager.getPunishment(punishmentId);
        if (p == null || !p.hasProof()) return false;
        p.setProof(null);
        dataManager.savePunishment(p);
        return true;
    }

    @Override
    public boolean setLockdown(boolean enabled, String reason) {
        LockdownToggleEvent event = new LockdownToggleEvent(enabled, reason);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        lockdownManager.setLockdownEnabled(enabled);
        if (reason != null && !reason.isEmpty()) {
            lockdownManager.setLockdownReason(event.getReason());
        }
        return true;
    }

    private void checkWarnActions(UUID targetUuid, String targetName) {
        if (!configManager.getBoolean("warn-actions.enabled", true)) return;

        org.bukkit.configuration.ConfigurationSection actionsSection = configManager.getConfigurationSection("warn-actions.actions");
        if (actionsSection == null) return;

        List<Punishment> activeWarns = getWarnings(targetUuid);
        int activeCount = activeWarns.size();

        String commandTemplate = actionsSection.getString(String.valueOf(activeCount));
        if (commandTemplate != null && !commandTemplate.trim().isEmpty()) {
            String cmd = commandTemplate.replace("%player%", targetName).replace("%count%", String.valueOf(activeCount));
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("[wapeB Warn-Action] Threshold reached for " + targetName + " (" + activeCount + " warns). Running: /" + cmd);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            });
        }
    }

    // --- CIDR Subnet & GeoIP API Methods ---

    @Override
    public boolean isCidrBanned(String ipOrCidr) {
        return getActiveCidrBan(ipOrCidr) != null;
    }

    @Override
    public Punishment getActiveCidrBan(String ipOrCidr) {
        String normalized = dev.azuyo.wapeB.utils.IPUtil.normalizeCidr(ipOrCidr);
        return dataManager.getActivePunishment(null, normalized, BAN_TYPES);
    }

    @Override
    public boolean banIpRange(String cidrOrRange, String reason, String executor, long duration, boolean silent) {
        String cidr = dev.azuyo.wapeB.utils.IPUtil.normalizeCidr(cidrOrRange);
        if (!dev.azuyo.wapeB.utils.IPUtil.isValidIpOrCidr(cidr)) return false;

        Punishment.PunishmentType type = (duration == -1) ? Punishment.PunishmentType.IPBAN : Punishment.PunishmentType.TEMPIPBAN;
        Punishment p = new Punishment(dataManager.getNextId(), null, "IP-Range", cidr, type, reason, executor, System.currentTimeMillis(), duration);
        dataManager.savePunishment(p);
        WebhookUtil.sendPunishmentWebhook(p);
        return true;
    }

    @Override
    public boolean unbanIpRange(String cidrOrRange, String reason, String executor) {
        String cidr = dev.azuyo.wapeB.utils.IPUtil.normalizeCidr(cidrOrRange);
        Punishment activeBan = dataManager.getActivePunishment(null, cidr, BAN_TYPES);
        if (activeBan != null) {
            return unbanPlayer((UUID) null, reason, executor);
        }
        return false;
    }

    @Override
    public dev.azuyo.wapeB.utils.GeoIPUtil.GeoInfo getGeoInfo(String ipAddress) {
        return dev.azuyo.wapeB.utils.GeoIPUtil.getGeoInfo(ipAddress);
    }

    // --- Template API Methods ---

    @Override
    public dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate getTemplate(String category, String templateKey) {
        return plugin.getTemplateManager().getTemplate(category, templateKey);
    }

    @Override
    public Map<String, Map<String, dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate>> getAllTemplates() {
        return plugin.getTemplateManager().getAllTemplates();
    }

    @Override
    public List<dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate> getTemplatesForCategory(String category) {
        return plugin.getTemplateManager().getTemplatesForCategory(category);
    }

    @Override
    public boolean punishWithTemplate(UUID target, String category, String templateKey, String executor, boolean silent) {
        dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate t = getTemplate(category, templateKey);
        if (t == null) return false;
        long duration = t.getDuration().equalsIgnoreCase("perm") ? -1 : dev.azuyo.wapeB.utils.TimeUtil.parseTime(t.getDuration());
        String cat = category.toLowerCase();
        if (cat.contains("ban")) {
            return banPlayer(target, t.getReason(), executor, duration, silent, cat.contains("ip"));
        } else if (cat.contains("mute")) {
            return mutePlayer(target, t.getReason(), executor, duration, silent, cat.contains("ip"));
        } else if (cat.contains("warn")) {
            return warnPlayer(target, t.getReason(), executor, silent);
        } else if (cat.contains("kick")) {
            return kickPlayer(target, t.getReason(), executor, silent);
        }
        return false;
    }

    @Override
    public dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate findTemplate(String keyOrShortcut) {
        return plugin.getTemplateManager().findTemplate(keyOrShortcut);
    }

    @Override
    public boolean saveTemplate(String category, String key, String reason, String duration, boolean silent, String shortcut) {
        return plugin.getTemplateManager().saveTemplate(category, key, reason, duration, silent, shortcut);
    }

    @Override
    public boolean deleteTemplate(String category, String key) {
        return plugin.getTemplateManager().deleteTemplate(category, key);
    }

    @Override
    public boolean punishWithTemplate(String targetName, String category, String templateKey, String executor, boolean silent) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        return punishWithTemplate(op.getUniqueId(), category, templateKey, executor, silent);
    }

    // --- Importer API Methods ---

    @Override
    public void registerImporter(dev.azuyo.wapeB.importers.PunishmentImporter importer) {
        plugin.getImportManager().registerImporter(importer);
    }

    @Override
    public dev.azuyo.wapeB.importers.PunishmentImporter getImporter(String name) {
        return plugin.getImportManager().getImporter(name);
    }

    @Override
    public List<dev.azuyo.wapeB.importers.PunishmentImporter> getRegisteredImporters() {
        return plugin.getImportManager().getRegisteredImporters();
    }

    @Override
    public java.util.concurrent.CompletableFuture<dev.azuyo.wapeB.importers.ImportResult> executeImport(String importerName, Map<String, Object> options) {
        return plugin.getImportManager().executeImport(importerName, options);
    }

    @Override
    public java.util.concurrent.CompletableFuture<dev.azuyo.wapeB.importers.ImportResult> importPunishments(List<Punishment> punishments) {
        return plugin.getImportManager().importBatch(punishments);
    }

    // --- Warn Action API Methods ---

    @Override
    public Map<Integer, String> getWarnActions() {
        Map<Integer, String> result = new HashMap<>();
        org.bukkit.configuration.ConfigurationSection sec = configManager.getConfigurationSection("warn-actions.actions");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    int threshold = Integer.parseInt(key);
                    result.put(threshold, sec.getString(key));
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    @Override
    public int getActiveWarnCount(UUID playerUuid) {
        return getWarnings(playerUuid).size();
    }

    @Override
    public int getActiveWarnCount(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getActiveWarnCount(op.getUniqueId());
    }

    @Override
    public boolean triggerWarnActionCheck(UUID targetUuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
        String name = op.getName() != null ? op.getName() : targetUuid.toString();
        checkWarnActions(targetUuid, name);
        return true;
    }

    @Override
    public boolean triggerWarnActionCheck(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return triggerWarnActionCheck(op.getUniqueId());
    }

    // --- Command Override & Alias Methods ---

    @Override
    public boolean registerCommandAlias(String originalCommand, String customAlias) {
        return commandManager.registerAlias(originalCommand, customAlias);
    }

    @Override
    public List<String> getCommandAliases(String originalCommand) {
        return commandManager.getAliases(originalCommand);
    }

    // --- Chat Snapshot API Methods ---

    @Override
    public dev.azuyo.wapeB.utils.ChatSnapshot getChatSnapshot(int punishmentId) {
        if (plugin.getChatSnapshotManager() == null) return null;
        return plugin.getChatSnapshotManager().getChatSnapshot(punishmentId);
    }

    @Override
    public boolean hasChatSnapshot(int punishmentId) {
        if (plugin.getChatSnapshotManager() == null) return false;
        return plugin.getChatSnapshotManager().hasChatSnapshot(punishmentId);
    }

    @Override
    public boolean deleteChatSnapshot(int punishmentId) {
        if (plugin.getChatSnapshotManager() == null) return false;
        return plugin.getChatSnapshotManager().deleteChatSnapshot(punishmentId);
    }

    @Override
    public List<dev.azuyo.wapeB.utils.ChatMessage> getRecentChat(UUID playerUuid, int limit) {
        if (plugin.getChatSnapshotManager() == null) return java.util.Collections.emptyList();
        return plugin.getChatSnapshotManager().getRecentChat(playerUuid, limit);
    }

    @Override
    public dev.azuyo.wapeB.utils.ChatSnapshot captureChatSnapshot(UUID playerUuid, int limit) {
        if (plugin.getChatSnapshotManager() == null) return null;
        return plugin.getChatSnapshotManager().captureSnapshotForPlayer(playerUuid, limit);
    }
}
