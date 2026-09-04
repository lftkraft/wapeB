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

    List<Punishment> getWarnings(UUID playerUuid);
    List<Punishment> getWarnings(String playerName);

    List<Punishment> getHistory(UUID playerUuid);
    List<Punishment> getHistory(String playerName);

    List<String> getAlts(UUID playerUuid);
    List<String> getAlts(String playerName);

    boolean isBanned(UUID playerUuid);
    boolean isBanned(String playerName);
    boolean isBannedByIp(String ipAddress);

    boolean isMuted(UUID playerUuid);
    boolean isMuted(String playerName);
    boolean isMutedByIp(String ipAddress);

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

    // --- Command Override & Alias Methods ---

    boolean registerCommandAlias(String originalCommand, String customAlias);
    List<String> getCommandAliases(String originalCommand);
}
