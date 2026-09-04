package dev.azuyo.wapeB.listeners;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.managers.FreezeManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
import dev.azuyo.wapeB.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;
import java.util.List;

public class PlayerQuitListener implements Listener {

    private final WapeB plugin;
    private final FreezeManager freezeManager;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final List<Punishment.PunishmentType> banTypes = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN,
            Punishment.PunishmentType.FREEZE_LOGOUT_BAN
    );

    public PlayerQuitListener(WapeB plugin) {
        this.plugin = plugin;
        this.freezeManager = plugin.getFreezeManager();
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (freezeManager.isFrozen(player)) {
            freezeManager.unfreezePlayer(player); // Unfreeze the player first

            // Ban the player
            String banReason = configManager.getString("freeze.ban-reason", "Logout during freeze.");
            long banDuration = TimeUtil.parseTime(configManager.getString("freeze.ban-duration", "7d"));

            // Deactivate existing active bans for this player
            Punishment existingBan = dataManager.getActivePunishment(player.getUniqueId(), null, banTypes);
            if (existingBan != null) {
                existingBan.setActive(false);
                dataManager.savePunishment(existingBan);
            }

            int punishmentId = dataManager.getNextId();
            String ipAddress = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
            Punishment punishment = new Punishment(punishmentId, player.getUniqueId(), player.getName(), ipAddress, Punishment.PunishmentType.FREEZE_LOGOUT_BAN, banReason, "Console", System.currentTimeMillis(), banDuration);
            dataManager.savePunishment(punishment);

            // Send Webhook
            WebhookUtil.sendPunishmentWebhook(punishment);

            // Broadcast the ban
            String broadcastMessage = configManager.getString("freeze.messages.logout-ban-broadcast", "%prefix% %player% was banned for logging out while frozen.");
            Bukkit.broadcast(MessageUtil.createComponent(broadcastMessage, punishment));
        }
    }
}