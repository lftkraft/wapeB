package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FreezeManager {

    private final WapeB plugin;
    private final Set<UUID> frozenPlayers;

    public FreezeManager(WapeB plugin) {
        this.plugin = plugin;
        this.frozenPlayers = new HashSet<>();
    }

    public void freezePlayer(Player player) {
        frozenPlayers.add(player.getUniqueId());
    }

    public void unfreezePlayer(Player player) {
        frozenPlayers.remove(player.getUniqueId());
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }
}
