package dev.azuyo.wapeB.listeners;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerLoginListener implements Listener {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final List<Punishment.PunishmentType> banTypes = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN,
            Punishment.PunishmentType.FREEZE_LOGOUT_BAN
    );

    public PlayerLoginListener(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        String playerName = player.getName();
        String playerIp = event.getAddress().getHostAddress();

        // --- Lockdown Check ---
        boolean lockdownEnabled = plugin.getConfigManager().getConfig().getBoolean("lockdown.enabled", false);
        String bypassPermission = plugin.getConfigManager().getString("lockdown.bypass-permission", "wapeb.lockdown.bypass");

        if (lockdownEnabled && !player.hasPermission(bypassPermission)) {
            List<String> kickReasonLines = plugin.getConfigManager().getStringList("lockdown.kick-reason");
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, MessageUtil.formatKickScreen(kickReasonLines, null));
            return;
        }

        // --- Ban Check ---
        Punishment activeBan = dataManager.getActivePunishment(playerUuid, playerIp, banTypes);
        if (activeBan != null) {
            if (activeBan.getDuration() != -1 && activeBan.getEnd() <= System.currentTimeMillis()) {
                activeBan.setActive(false);
                dataManager.savePunishment(activeBan);
            } else {
                // Create a new Punishment object for the kick screen with the correct player name
                Punishment kickPunishment = new Punishment(
                    activeBan.getId(), activeBan.getPlayerUuid(), playerName, activeBan.getIpAddress(),
                    activeBan.getType(), activeBan.getReason(), activeBan.getExecutorName(),
                    activeBan.getDate(), activeBan.getDuration()
                );
                
                List<String> kickScreenLines = plugin.getConfigManager().getStringList("messages.ban.kick-screen");
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, MessageUtil.formatKickScreen(kickScreenLines, kickPunishment));
                
                notifyPunishment(kickPunishment);
                return; 
            }
        }

        // --- Alts Check ---
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> altNames = dataManager.getAltNamesByIp(playerIp, playerUuid);
            if (!altNames.isEmpty()) {
                notifyAlts(playerName, playerIp, altNames);
            }
        });
    }

    private void notifyPunishment(Punishment punishment) {
        String permission = plugin.getConfigManager().getString("messages.punishment-notification.permission", "wapeb.notify.punishment");
        String message = plugin.getConfigManager().getString("messages.punishment-notification.ban-attempt", "%prefix% §c%player% §ftried to join but is banned for §c%duration%§f.");
        if (permission.isEmpty() || message.isEmpty()) return;

        Bukkit.getScheduler().runTask(plugin, () -> 
            Bukkit.getOnlinePlayers().stream()
                  .filter(staff -> staff.hasPermission(permission))
                  .forEach(staff -> staff.sendMessage(MessageUtil.createComponent(message, punishment)))
        );
    }

    private void notifyAlts(String playerName, String playerIp, List<String> altNames) {
        String permission = "wapeb.alts.notify";
        String message = plugin.getConfigManager().getString("alts.login-notification", "");
        if (message.isEmpty()) return;

        String altsList = String.join(", ", altNames);
        String altsListHover = String.join("\n", altNames);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", playerName);
        placeholders.put("%ip_address%", playerIp);
        placeholders.put("%alts_list%", altsList);
        placeholders.put("%alts_list_hover%", altsListHover); // This will be used by the hover event in MessageUtil

        Bukkit.getScheduler().runTask(plugin, () -> 
            Bukkit.getOnlinePlayers().stream()
                  .filter(staff -> staff.hasPermission(permission))
                  .forEach(staff -> staff.sendMessage(MessageUtil.createComponent(message, null, placeholders)))
        );
    }
}
