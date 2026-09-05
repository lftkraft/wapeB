package dev.azuyo.wapeB.utils;

import java.util.UUID;

public class AltInfo {

    public enum MatchType {
        EXACT_IP,
        CIDR_SUBNET
    }

    private final UUID uuid;
    private final String playerName;
    private final String lastIp;
    private final long lastSeen;
    private final MatchType matchType;
    private final Punishment activeBan;
    private final Punishment activeMute;
    private final boolean isExempt;

    public AltInfo(UUID uuid, String playerName, String lastIp, long lastSeen, MatchType matchType, Punishment activeBan, Punishment activeMute, boolean isExempt) {
        this.uuid = uuid;
        this.playerName = playerName != null ? playerName : "Unknown";
        this.lastIp = lastIp;
        this.lastSeen = lastSeen;
        this.matchType = matchType != null ? matchType : MatchType.EXACT_IP;
        this.activeBan = activeBan;
        this.activeMute = activeMute;
        this.isExempt = isExempt;
    }

    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }
    public String getLastIp() { return lastIp; }
    public long getLastSeen() { return lastSeen; }
    public MatchType getMatchType() { return matchType; }
    public Punishment getActiveBan() { return activeBan; }
    public Punishment getActiveMute() { return activeMute; }
    public boolean isExempt() { return isExempt; }
    public boolean isBanned() { return activeBan != null; }
    public boolean isMuted() { return activeMute != null; }
}
