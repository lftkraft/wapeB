package dev.azuyo.wapeB.api.events;

import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerUnpunishEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Punishment punishment;
    private final String executor;
    private boolean cancelled;

    public PlayerUnpunishEvent(Punishment punishment, String executor) {
        this.punishment = punishment;
        this.executor = executor;
        this.cancelled = false;
    }

    public Punishment getPunishment() {
        return punishment;
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
