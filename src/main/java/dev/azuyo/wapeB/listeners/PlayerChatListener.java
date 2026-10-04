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

    private final List<Punishment.PunishmentType> shadowMuteTypes = Arrays.asList(
            Punishment.PunishmentType.SHADOWMUTE,
            Punishment.PunishmentType.TEMPSHADOWMUTE,
            Punishment.PunishmentType.IPSHADOWMUTE,
            Punishment.PunishmentType.TEMPIPSHADOWMUTE
    );

    // Cooldown for staff mute notifications (1 minute)
    private final Map<UUID, Long> lastMuteStaffAlertTime = new HashMap<>();
    private final Map<UUID, Long> lastShadowMuteStaffAlertTime = new HashMap<>();
    private static final long STAFF_ALERT_COOLDOWN_MILLIS = 10 * 1000; // 10 seconds for shadow-mute alerts

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

        // --- Shadow-Mute Check ---
        Punishment activeShadowMute = dataManager.getActivePunishment(player.getUniqueId(), playerIp, shadowMuteTypes);
        if (activeShadowMute != null) {
            if (activeShadowMute.getDuration() != -1 && activeShadowMute.getEnd() <= System.currentTimeMillis()) {
                activeShadowMute.setActive(false);
                dataManager.savePunishment(activeShadowMute);
            } else {
                // Keep the chat message visible ONLY to the shadow-muted player
                event.getRecipients().clear();
                event.getRecipients().add(player);

                // Notify staff about shadowmute attempt
                notifyShadowMuteStaff(activeShadowMute, event.getMessage());
                return; // Do not process with Sentinel or other handlers
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
            currentTime - lastMuteStaffAlertTime.get(playerUuid) < 60 * 1000) {
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

    private void notifyShadowMuteStaff(Punishment punishment, String chatMessage) {
        long currentTime = System.currentTimeMillis();
        UUID playerUuid = punishment.getPlayerUuid();

        if (lastShadowMuteStaffAlertTime.containsKey(playerUuid) &&
            currentTime - lastShadowMuteStaffAlertTime.get(playerUuid) < STAFF_ALERT_COOLDOWN_MILLIS) {
            return;
        }

        String permission = plugin.getConfigManager().getString("messages.punishment-notification.shadowmute-permission", "wapeb.shadowmute.notify");
        String message = plugin.getConfigManager().getString("messages.punishment-notification.shadowmute-attempt",
                "%prefix% <#ff5555>%player%</#ff5555> <gray>(ShadowMute) beszélt:</gray> <white>%message%</white>");
        if (message.isEmpty()) return;

        lastShadowMuteStaffAlertTime.put(playerUuid, currentTime);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%message%", chatMessage);
        placeholders.put("%player%", punishment.getPlayerName() != null ? punishment.getPlayerName() : "Unknown");
        placeholders.put("%reason%", punishment.getReason() != null ? punishment.getReason() : "");

        Bukkit.getOnlinePlayers().stream()
              .filter(staff -> staff.hasPermission(permission) || staff.hasPermission("wapeb.notify.punishment") || staff.hasPermission("wapeb.notify"))
              .forEach(staff -> staff.sendMessage(MessageUtil.createComponent(message, punishment, placeholders)));
    }
}
