package dev.azuyo.wapeB.utils;

import java.util.UUID;

public class Punishment {

    public enum PunishmentType {
        BAN, TEMPBAN, IPBAN, TEMPIPBAN,
        MUTE, TEMPMUTE, IPMUTE, TEMPIPMUTE,
        SHADOWMUTE, TEMPSHADOWMUTE, IPSHADOWMUTE, TEMPIPSHADOWMUTE,
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
    private String activeServer; // The server scope where the punishment is active (e.g. "global", "szerver2", "szerver1,szerver2")
    private String server;       // The server where the punishment took place (origin/source server, e.g. "szerver1")
    private String proof;        // Proof / evidence URL (e.g. screenshot, video link)
    private final long date;
    private final long duration;
    private final long end;
    private boolean active;

    // Constructors
    public Punishment(int id, UUID playerUuid, String playerName, PunishmentType type, String reason, String executorName, long date, long duration) {
        this(id, playerUuid, playerName, null, type, reason, executorName, "global", "global", date, duration);
    }

    public Punishment(int id, UUID playerUuid, String playerName, PunishmentType type, String reason, String executorName, String activeServer, long date, long duration) {
        this(id, playerUuid, playerName, null, type, reason, executorName, activeServer, "global", date, duration);
    }

    public Punishment(int id, UUID playerUuid, String playerName, PunishmentType type, String reason, String executorName, String activeServer, String server, long date, long duration) {
        this(id, playerUuid, playerName, null, type, reason, executorName, activeServer, server, date, duration);
    }

    public Punishment(int id, UUID playerUuid, String playerName, String ipAddress, PunishmentType type, String reason, String executorName, long date, long duration) {
        this(id, playerUuid, playerName, ipAddress, type, reason, executorName, "global", "global", date, duration);
    }

    public Punishment(int id, UUID playerUuid, String playerName, String ipAddress, PunishmentType type, String reason, String executorName, String activeServer, long date, long duration) {
        this(id, playerUuid, playerName, ipAddress, type, reason, executorName, activeServer, "global", date, duration);
    }

    public Punishment(int id, UUID playerUuid, String playerName, String ipAddress, PunishmentType type, String reason, String executorName, String activeServer, String server, long date, long duration) {
        this(id, playerUuid, playerName, ipAddress, type, reason, executorName, activeServer, server, null, date, duration);
    }

    public Punishment(int id, UUID playerUuid, String playerName, String ipAddress, PunishmentType type, String reason, String executorName, String activeServer, String server, String proof, long date, long duration) {
        this(id, playerUuid, playerName, ipAddress, type, reason, executorName, activeServer, server, proof, date, duration, true);
    }

    public Punishment(int id, UUID playerUuid, String playerName, String ipAddress, PunishmentType type, String reason, String executorName, String activeServer, String server, String proof, long date, long duration, boolean active) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.ipAddress = ipAddress;
        this.type = type;
        this.reason = reason;
        this.executorName = executorName;
        this.activeServer = (activeServer != null && !activeServer.trim().isEmpty()) ? activeServer : "global";
        this.server = (server != null && !server.trim().isEmpty()) ? server : "global";
        this.proof = proof;
        this.date = date;
        this.duration = duration;
        this.end = (duration == -1) ? -1 : date + duration;
        this.active = active;
    }

    public Punishment(int id, UUID playerUuid, String playerName, String ipAddress, PunishmentType type, String reason, String executorName, long date, long duration, boolean active, String activeServer, String server, String proof) {
        this(id, playerUuid, playerName, ipAddress, type, reason, executorName, activeServer, server, proof, date, duration, active);
    }

    public boolean isAppliesTo(String currentServerName) {
        if (activeServer == null || activeServer.trim().isEmpty()) return true;
        if (activeServer.equalsIgnoreCase("global") || activeServer.equalsIgnoreCase("all") || activeServer.equalsIgnoreCase("*")) return true;
        if (currentServerName == null) return false;
        for (String s : activeServer.split(",")) {
            if (s.trim().equalsIgnoreCase(currentServerName.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean isIpPunishment() {
        return type == PunishmentType.IPBAN 
            || type == PunishmentType.TEMPIPBAN 
            || type == PunishmentType.IPMUTE 
            || type == PunishmentType.TEMPIPMUTE 
            || type == PunishmentType.IPSHADOWMUTE 
            || type == PunishmentType.TEMPIPSHADOWMUTE;
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
    public String getActiveServer() { return activeServer != null ? activeServer : "global"; }
    public void setActiveServer(String activeServer) { this.activeServer = (activeServer != null && !activeServer.trim().isEmpty()) ? activeServer : "global"; }
    public String getServer() { return server != null ? server : "global"; }
    public void setServer(String server) { this.server = (server != null && !server.trim().isEmpty()) ? server : "global"; }
    public String getProof() { return proof; }
    public void setProof(String proof) { this.proof = proof; }
    public boolean hasProof() { return proof != null && !proof.trim().isEmpty(); }
    public long getDate() { return date; }
    public long getDuration() { return duration; }
    public long getEnd() { return end; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}