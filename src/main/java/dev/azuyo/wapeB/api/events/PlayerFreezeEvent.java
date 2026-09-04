package dev.azuyo.wapeB.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerFreezeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String reason;
    private final String executor;
    private boolean cancelled;

    public PlayerFreezeEvent(Player player, String reason, String executor) {
        this.player = player;
        this.reason = reason;
        this.executor = executor;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public String getReason() {
        return reason;
    }

    public String getExecutor() {
        return executor;
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
