package dev.azuyo.wapeB.api.events;

import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PlayerPunishEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final String playerName;
    private final String ipAddress;
    private final Punishment.PunishmentType type;
    private String reason;
    private String executor;
    private String activeServer;
    private String server;
    private long duration;
    private boolean silent;
    private boolean cancelled;

    public PlayerPunishEvent(UUID playerUuid, String playerName, String ipAddress, Punishment.PunishmentType type, String reason, String executor, long duration, boolean silent) {
        this(playerUuid, playerName, ipAddress, type, reason, executor, "global", "global", duration, silent);
    }

    public PlayerPunishEvent(UUID playerUuid, String playerName, String ipAddress, Punishment.PunishmentType type, String reason, String executor, String activeServer, long duration, boolean silent) {
        this(playerUuid, playerName, ipAddress, type, reason, executor, activeServer, "global", duration, silent);
    }

    public PlayerPunishEvent(UUID playerUuid, String playerName, String ipAddress, Punishment.PunishmentType type, String reason, String executor, String activeServer, String server, long duration, boolean silent) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.ipAddress = ipAddress;
        this.type = type;
        this.reason = reason;
        this.executor = executor;
        this.activeServer = (activeServer != null && !activeServer.trim().isEmpty()) ? activeServer : "global";
        this.server = (server != null && !server.trim().isEmpty()) ? server : "global";
        this.duration = duration;
        this.silent = silent;
        this.cancelled = false;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Punishment.PunishmentType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public String getActiveServer() {
        return activeServer != null ? activeServer : "global";
    }

    public void setActiveServer(String activeServer) {
        this.activeServer = (activeServer != null && !activeServer.trim().isEmpty()) ? activeServer : "global";
    }

    public String getServer() {
        return server != null ? server : "global";
    }

    public void setServer(String server) {
        this.server = (server != null && !server.trim().isEmpty()) ? server : "global";
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
