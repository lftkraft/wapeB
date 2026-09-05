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

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent FIRED for " + targetName + " | Original Executor: '" + executor + "' | Type: " + type);

        PlayerPunishEvent event = new PlayerPunishEvent(target, targetName, targetIp, type, reason, executor, duration, silent);
        Bukkit.getPluginManager().callEvent(event);

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent PROCESSED for " + targetName + " | Final Executor: '" + event.getExecutor() + "' | Cancelled: " + event.isCancelled());

        if (event.isCancelled()) return false;

        // Deactivate existing ban
        Punishment existingBan = dataManager.getActivePunishment(target, targetIp, BAN_TYPES);
        if (existingBan != null) {
            existingBan.setActive(false);
            dataManager.savePunishment(existingBan);
        }

        Punishment p = new Punishment(dataManager.getNextId(), target, targetName, targetIp, type, event.getReason(), event.getExecutor(), System.currentTimeMillis(), event.getDuration());
        dataManager.savePunishment(p);
        WebhookUtil.sendPunishmentWebhook(p);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (op.isOnline()) {
                ((Player)op).kick(MessageUtil.formatKickScreen(configManager.getStringList("messages.ban.kick-screen"), p));
            }
            String broadcastMsg = configManager.getString("messages.ban.broadcast", "%prefix% %executor% banned %player%.");
            if (event.isSilent()) {
                Bukkit.broadcast(MessageUtil.createComponent(configManager.getString("messages.ban.silent.prefix", "&7(Silent) ") + broadcastMsg, p), "wapeb.notify");
            } else {
                Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, p));
            }
        });

        return true;
    }

    @Override
    public boolean banPlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipBan) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        return banPlayer(op.getUniqueId(), reason, executor, duration, silent, ipBan);
    }

    @Override
    public boolean mutePlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipMute) {
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

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent FIRED for " + targetName + " | Original Executor: '" + executor + "' | Type: " + type);

        PlayerPunishEvent event = new PlayerPunishEvent(target, targetName, targetIp, type, reason, executor, duration, silent);
        Bukkit.getPluginManager().callEvent(event);

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent PROCESSED for " + targetName + " | Final Executor: '" + event.getExecutor() + "' | Cancelled: " + event.isCancelled());
        if (event.isCancelled()) return false;

        Punishment existingMute = dataManager.getActivePunishment(target, targetIp, MUTE_TYPES);
        if (existingMute != null) {
            existingMute.setActive(false);
            dataManager.savePunishment(existingMute);
        }

        Punishment p = new Punishment(dataManager.getNextId(), target, targetName, targetIp, type, event.getReason(), event.getExecutor(), System.currentTimeMillis(), event.getDuration());
        dataManager.savePunishment(p);
        WebhookUtil.sendPunishmentWebhook(p);

        Bukkit.getScheduler().runTask(plugin, () -> {
            String broadcastMsg = configManager.getString("messages.mute.broadcast", "%prefix% %executor% muted %player%.");
            if (event.isSilent()) {
                Bukkit.broadcast(MessageUtil.createComponent(configManager.getString("messages.mute.silent.prefix", "&7(Silent) ") + broadcastMsg, p), "wapeb.notify");
            } else {
                Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, p));
            }
        });

        return true;
    }

    @Override
    public boolean mutePlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipMute) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        return mutePlayer(op.getUniqueId(), reason, executor, duration, silent, ipMute);
    }

    @Override
    public boolean warnPlayer(UUID target, String reason, String executor, boolean silent) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        String targetName = op.getName() != null ? op.getName() : target.toString();

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent FIRED for " + targetName + " | Original Executor: '" + executor + "' | Type: WARN");

        PlayerPunishEvent event = new PlayerPunishEvent(target, targetName, null, Punishment.PunishmentType.WARN, reason, executor, -1, silent);
        Bukkit.getPluginManager().callEvent(event);

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent PROCESSED for " + targetName + " | Final Executor: '" + event.getExecutor() + "' | Cancelled: " + event.isCancelled());

        if (event.isCancelled()) return false;

        Punishment p = new Punishment(dataManager.getNextId(), target, targetName, Punishment.PunishmentType.WARN, event.getReason(), event.getExecutor(), System.currentTimeMillis(), -1);
        dataManager.savePunishment(p);
        WebhookUtil.sendPunishmentWebhook(p);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (op.isOnline()) {
                ((Player)op).sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.target-notify", "&cYou have been warned for: %reason%"), p));
            }
            String broadcastMsg = configManager.getString("messages.warn.broadcast", "%prefix% %executor% warned %player%.");
            if (event.isSilent()) {
                Bukkit.broadcast(MessageUtil.createComponent(configManager.getString("messages.warn.silent.prefix", "&7(Silent) ") + broadcastMsg, p), "wapeb.notify");
            } else {
                Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, p));
            }
        });

        return true;
    }

    @Override
    public boolean warnPlayer(String targetName, String reason, String executor, boolean silent) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        return warnPlayer(op.getUniqueId(), reason, executor, silent);
    }

    @Override
    public boolean kickPlayer(UUID target, String reason, String executor, boolean silent) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        if (!op.isOnline()) return false;

        Player onlineTarget = (Player) op;

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent FIRED for " + onlineTarget.getName() + " | Original Executor: '" + executor + "' | Type: KICK");

        PlayerPunishEvent event = new PlayerPunishEvent(target, onlineTarget.getName(), null, Punishment.PunishmentType.KICK, reason, executor, -1, silent);
        Bukkit.getPluginManager().callEvent(event);

        plugin.getLogger().info("[wapeB Debug] PlayerPunishEvent PROCESSED for " + onlineTarget.getName() + " | Final Executor: '" + event.getExecutor() + "' | Cancelled: " + event.isCancelled());

        if (event.isCancelled()) return false;

        Punishment p = new Punishment(dataManager.getNextId(), target, onlineTarget.getName(), Punishment.PunishmentType.KICK, event.getReason(), event.getExecutor(), System.currentTimeMillis(), -1);
        p.setActive(false);
        dataManager.savePunishment(p);
        WebhookUtil.sendPunishmentWebhook(p);

        Bukkit.getScheduler().runTask(plugin, () -> {
            onlineTarget.kick(MessageUtil.formatKickScreen(configManager.getStringList("messages.kick.kick-screen"), p));
            String broadcastMsg = configManager.getString("messages.kick.broadcast", "%prefix% %executor% kicked %player%.");
            if (event.isSilent()) {
                Bukkit.broadcast(MessageUtil.createComponent(configManager.getString("messages.kick.silent.prefix", "&7(Silent) ") + broadcastMsg, p), "wapeb.notify");
            } else {
                Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, p));
            }
        });

        return true;
    }

    @Override
    public boolean kickPlayer(String targetName, String reason, String executor, boolean silent) {
        Player player = Bukkit.getPlayer(targetName);
        if (player == null) return false;
        return kickPlayer(player.getUniqueId(), reason, executor, silent);
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
        String ip = playerDataManager.getLastKnownIp(target);
        Punishment activeBan = dataManager.getActivePunishment(target, ip, BAN_TYPES);
        if (activeBan == null) return false;

        PlayerUnpunishEvent event = new PlayerUnpunishEvent(activeBan, executor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        activeBan.setActive(false);
        dataManager.savePunishment(activeBan);

        Bukkit.getScheduler().runTask(plugin, () -> {
            String broadcastMsg = configManager.getString("messages.unban.broadcast", "%prefix% %executor% unbanned %player%.");
            if (!broadcastMsg.isEmpty()) {
                Punishment temp = new Punishment(activeBan.getId(), activeBan.getPlayerUuid(), activeBan.getPlayerName(), activeBan.getType(), reason, executor, activeBan.getDate(), activeBan.getDuration());
                Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, temp));
            }
        });

        return true;
    }

    @Override
    public boolean unbanPlayer(String targetName, String reason, String executor) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        return unbanPlayer(op.getUniqueId(), reason, executor);
    }

    @Override
    public boolean unmutePlayer(UUID target, String reason, String executor) {
        String ip = playerDataManager.getLastKnownIp(target);
        Punishment activeMute = dataManager.getActivePunishment(target, ip, MUTE_TYPES);
        if (activeMute == null) return false;

        PlayerUnpunishEvent event = new PlayerUnpunishEvent(activeMute, executor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        activeMute.setActive(false);
        dataManager.savePunishment(activeMute);

        Bukkit.getScheduler().runTask(plugin, () -> {
            String broadcastMsg = configManager.getString("messages.unmute.broadcast", "%prefix% %executor% unmuted %player%.");
            if (!broadcastMsg.isEmpty()) {
                Punishment temp = new Punishment(activeMute.getId(), activeMute.getPlayerUuid(), activeMute.getPlayerName(), activeMute.getType(), reason, executor, activeMute.getDate(), activeMute.getDuration());
                Bukkit.broadcast(MessageUtil.createComponent(broadcastMsg, temp));
            }
        });

        return true;
    }

    @Override
    public boolean unmutePlayer(String targetName, String reason, String executor) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        return unmutePlayer(op.getUniqueId(), reason, executor);
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

    // --- Command Override & Alias Methods ---

    @Override
    public boolean registerCommandAlias(String originalCommand, String customAlias) {
        return commandManager.registerAlias(originalCommand, customAlias);
    }

    @Override
    public List<String> getCommandAliases(String originalCommand) {
        return commandManager.getAliases(originalCommand);
    }
}
