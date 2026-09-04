package dev.azuyo.wapeB.utils;

import java.util.UUID;

public class Punishment {

    public enum PunishmentType {
        BAN, TEMPBAN, IPBAN, TEMPIPBAN,
        MUTE, TEMPMUTE, IPMUTE, TEMPIPMUTE,
        WARN, KICK,
        FREEZE_LOGOUT_BAN,
        SENTINEL_AUTO_MUTE, // New punishment type for automatic mutes
        SENTINEL_AI_MUTE // New punishment type for AI-based mutes
    }

    private int id;
    private final UUID playerUuid;
    private final String playerName;
    private final String ipAddress;
    private final PunishmentType type;
    private final String reason;
    private final String executorName;
    private final long date;
    private final long duration;
    private final long end;
    private boolean active;

    // Constructors
    public Punishment(int id, UUID playerUuid, String playerName, PunishmentType type, String reason, String executorName, long date, long duration) {
        this(id, playerUuid, playerName, null, type, reason, executorName, date, duration);
    }

    public Punishment(int id, UUID playerUuid, String playerName, String ipAddress, PunishmentType type, String reason, String executorName, long date, long duration) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.ipAddress = ipAddress;
        this.type = type;
        this.reason = reason;
        this.executorName = executorName;
        this.date = date;
        this.duration = duration;
        this.end = (duration == -1) ? -1 : date + duration;
        this.active = true;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getIpAddress() { return ipAddress; }
    public PunishmentType getType() { return type; }
    public String getReason() { return reason; }
    public String getExecutorName() { return executorName; }
    public long getDate() { return date; }
    public long getDuration() { return duration; }
    public long getEnd() { return end; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}