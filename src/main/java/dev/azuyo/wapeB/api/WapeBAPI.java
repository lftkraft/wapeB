package dev.azuyo.wapeB.api;

import dev.azuyo.wapeB.utils.Punishment;

import java.util.List;
import java.util.UUID;

/**
 * Public Java API interface for wapeB plugin.
 */
public interface WapeBAPI {

    // --- Query Methods ---

    List<Punishment> getPunishments(UUID playerUuid);
    List<Punishment> getPunishments(String playerName);

    Punishment getActiveBan(UUID playerUuid);
    Punishment getActiveBan(String playerName);
    Punishment getActiveBanByIp(String ipAddress);

    Punishment getActiveMute(UUID playerUuid);
    Punishment getActiveMute(String playerName);
    Punishment getActiveMuteByIp(String ipAddress);

    Punishment getActiveBanForPlayerOrAlt(UUID playerUuid);
    Punishment getActiveBanForPlayerOrAlt(String playerName);

    Punishment getActiveMuteForPlayerOrAlt(UUID playerUuid);
    Punishment getActiveMuteForPlayerOrAlt(String playerName);

    List<Punishment> getWarnings(UUID playerUuid);
    List<Punishment> getWarnings(String playerName);

    List<Punishment> getHistory(UUID playerUuid);
    List<Punishment> getHistory(String playerName);

    List<Punishment> getStaffHistory(String executorName);

    boolean recordStaffAction(String staffName, String targetPlayer, Punishment.PunishmentType type, String reason, long duration);
    boolean recordStaffAction(String staffName, UUID targetUuid, String targetPlayer, Punishment.PunishmentType type, String reason, long duration);
    boolean recordStaffAction(String staffName, String executorDisplayName, String targetPlayer, Punishment.PunishmentType type, String reason, long duration);
    boolean addStaffHistoryEntry(String staffName, Punishment punishment);

    List<String> getAlts(UUID playerUuid);
    List<String> getAlts(String playerName);
    List<dev.azuyo.wapeB.utils.AltInfo> getDetailedAlts(UUID playerUuid);
    List<dev.azuyo.wapeB.utils.AltInfo> getDetailedAlts(String playerName);

    List<dev.azuyo.wapeB.utils.IpHistoryRecord> getIpHistory(UUID playerUuid);
    List<dev.azuyo.wapeB.utils.IpHistoryRecord> getIpHistory(String playerName);

    boolean isAltExempt(UUID playerUuid);
    boolean isAltExempt(String playerName);
    boolean setAltExempt(UUID playerUuid, boolean exempt, String addedBy);
    boolean setAltExempt(String playerName, boolean exempt, String addedBy);

    boolean isBanned(UUID playerUuid);
    boolean isBanned(String playerName);
    boolean isBannedByIp(String ipAddress);

    boolean isMuted(UUID playerUuid);
    boolean isMuted(String playerName);
    boolean isMutedByIp(String ipAddress);

    boolean isBannedForPlayerOrAlt(UUID playerUuid);
    boolean isBannedForPlayerOrAlt(String playerName);

    boolean isMutedForPlayerOrAlt(UUID playerUuid);
    boolean isMutedForPlayerOrAlt(String playerName);

    boolean isFrozen(UUID playerUuid);
    boolean isFrozen(String playerName);

    boolean isLockdownActive();
    String getLockdownReason();

    // --- Execution Methods ---

    boolean banPlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipBan);
    boolean banPlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipBan);

    boolean mutePlayer(UUID target, String reason, String executor, long duration, boolean silent, boolean ipMute);
    boolean mutePlayer(String targetName, String reason, String executor, long duration, boolean silent, boolean ipMute);

    boolean warnPlayer(UUID target, String reason, String executor, boolean silent);
    boolean warnPlayer(String targetName, String reason, String executor, boolean silent);

    boolean kickPlayer(UUID target, String reason, String executor, boolean silent);
    boolean kickPlayer(String targetName, String reason, String executor, boolean silent);

    boolean freezePlayer(UUID target, String reason, String executor);
    boolean freezePlayer(String targetName, String reason, String executor);

    boolean unfreezePlayer(UUID target, String executor);
    boolean unfreezePlayer(String targetName, String executor);

    boolean unbanPlayer(UUID target, String reason, String executor);
    boolean unbanPlayer(String targetName, String reason, String executor);

    boolean unmutePlayer(UUID target, String reason, String executor);
    boolean unmutePlayer(String targetName, String reason, String executor);

    boolean revokePunishment(int punishmentId, String executor);

    boolean setLockdown(boolean enabled, String reason);

    // --- CIDR Subnet & GeoIP API Methods ---

    boolean isCidrBanned(String ipOrCidr);
    Punishment getActiveCidrBan(String ipOrCidr);
    boolean banIpRange(String cidrOrRange, String reason, String executor, long duration, boolean silent);
    boolean unbanIpRange(String cidrOrRange, String reason, String executor);

    dev.azuyo.wapeB.utils.GeoIPUtil.GeoInfo getGeoInfo(String ipAddress);

    // --- Template API Methods ---

    dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate getTemplate(String category, String templateKey);
    java.util.Map<String, java.util.Map<String, dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate>> getAllTemplates();
    List<dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate> getTemplatesForCategory(String category);

    boolean punishWithTemplate(UUID target, String category, String templateKey, String executor, boolean silent);
    boolean punishWithTemplate(String targetName, String category, String templateKey, String executor, boolean silent);

    // --- Warn Action API Methods ---

    java.util.Map<Integer, String> getWarnActions();
    int getActiveWarnCount(UUID playerUuid);
    int getActiveWarnCount(String playerName);
    boolean triggerWarnActionCheck(UUID targetUuid);
    boolean triggerWarnActionCheck(String targetName);

    // --- Command Override & Alias Methods ---

    boolean registerCommandAlias(String originalCommand, String customAlias);
    List<String> getCommandAliases(String originalCommand);
}
