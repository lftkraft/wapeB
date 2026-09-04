package dev.azuyo.wapeB.listeners;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.managers.SentinelManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerChatListener implements Listener {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final SentinelManager sentinelManager;
    private final List<Punishment.PunishmentType> muteTypes = Arrays.asList(
            Punishment.PunishmentType.MUTE,
            Punishment.PunishmentType.TEMPMUTE,
            Punishment.PunishmentType.IPMUTE,
            Punishment.PunishmentType.TEMPIPMUTE,
            Punishment.PunishmentType.SENTINEL_AUTO_MUTE,
            Punishment.PunishmentType.SENTINEL_AI_MUTE
    );

    // No cooldown for the player's own mute message as requested.
    
    // Cooldown for staff mute notifications (1 minute)
    private final Map<UUID, Long> lastMuteStaffAlertTime = new HashMap<>();
    private static final long STAFF_ALERT_COOLDOWN_MILLIS = 60 * 1000; // 1 minute

    public PlayerChatListener(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.sentinelManager = plugin.getSentinelManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerIp = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A";

        // --- Mute Check FIRST ---
        Punishment activeMute = dataManager.getActivePunishment(player.getUniqueId(), playerIp, muteTypes);

        if (activeMute != null) {
            if (activeMute.getDuration() != -1 && activeMute.getEnd() <= System.currentTimeMillis()) {
                activeMute.setActive(false);
                dataManager.savePunishment(activeMute);
            } else {
                event.setCancelled(true);

                // Send mute message to player ALWAYS
                String mutedMessage = plugin.getConfigManager().getString("messages.mute.player-is-muted", "%prefix% §cYou are currently muted! \\n§cReason: %reason% \\n§cExpires in: %duration%");
                player.sendMessage(MessageUtil.createComponent(mutedMessage, activeMute));
                
                // Notify staff about mute attempt (with cooldown)
                notifyStaff(activeMute);
                return; // Don't process the message further
            }
        }

        // --- Sentinel Check ---
        if (sentinelManager.checkMessage(player, event.getMessage())) {
            event.setCancelled(true);
        }
    }

    private void notifyStaff(Punishment punishment) {
        long currentTime = System.currentTimeMillis();
        UUID playerUuid = punishment.getPlayerUuid();
        
        // 1 minute cooldown for staff notifications
        if (lastMuteStaffAlertTime.containsKey(playerUuid) &&
            currentTime - lastMuteStaffAlertTime.get(playerUuid) < STAFF_ALERT_COOLDOWN_MILLIS) {
            return;
        }

        String permission = plugin.getConfigManager().getString("messages.punishment-notification.permission", "wapeb.notify.punishment");
        String message = plugin.getConfigManager().getString("messages.punishment-notification.mute-attempt", "%prefix% §e%player% §ftried to chat while muted for §e%duration%§f.");
        if (permission.isEmpty() || message.isEmpty()) {
            return;
        }

        lastMuteStaffAlertTime.put(playerUuid, currentTime);

        // The punishment object already contains the player's name.
        Bukkit.getOnlinePlayers().stream()
              .filter(staff -> staff.hasPermission(permission))
              .forEach(staff -> staff.sendMessage(MessageUtil.createComponent(message, punishment)));
    }
}
