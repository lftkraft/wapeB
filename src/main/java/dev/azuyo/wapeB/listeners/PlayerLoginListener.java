package dev.azuyo.wapeB.listeners;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.managers.PlayerDataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerLoginListener implements Listener {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final PlayerDataManager playerDataManager;
    private final List<Punishment.PunishmentType> banTypes = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN,
            Punishment.PunishmentType.FREEZE_LOGOUT_BAN
    );

    // Cooldown for staff ban notifications (1 minute)
    private final Map<UUID, Long> lastBanAlertTime = new HashMap<>();
    private static final long BAN_ALERT_COOLDOWN_MILLIS = 60 * 1000; // 1 minute

    public PlayerLoginListener(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        String playerName = player.getName();
        String playerIp = event.getAddress().getHostAddress();

        // --- Update Player Data & History ---
        playerDataManager.recordIpHistory(playerUuid, playerName, playerIp);

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
                    activeBan.getServer(),
                    activeBan.getDate(), activeBan.getDuration()
                );
                
                List<String> kickScreenLines = plugin.getConfigManager().getStringList("messages.ban.kick-screen");
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, MessageUtil.formatKickScreen(kickScreenLines, kickPunishment));
                
                notifyPunishment(activeBan, playerName, playerUuid);
                return; 
            }
        }

        // --- Alts Check ---
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean alertEnabled = plugin.getConfigManager().getBoolean("alts.alert-on-join.enabled", true);
            if (!alertEnabled) return;

            List<dev.azuyo.wapeB.utils.AltInfo> detailedAlts = playerDataManager.getDetailedAlts(playerUuid);
            if (detailedAlts.isEmpty()) return;

            boolean onlyIfPunished = plugin.getConfigManager().getBoolean("alts.alert-on-join.only-if-punished", false);

            List<String> allAltNames = new ArrayList<>();
            List<String> bannedAltNames = new ArrayList<>();
            List<String> mutedAltNames = new ArrayList<>();
            List<String> hoverLines = new ArrayList<>();

            for (dev.azuyo.wapeB.utils.AltInfo alt : detailedAlts) {
                String altName = alt.getPlayerName() != null ? alt.getPlayerName() : alt.getUuid().toString();
                allAltNames.add(altName);

                String statusTag;
                if (alt.isBanned()) {
                    bannedAltNames.add(altName);
                    statusTag = "<red>[BANNED]</red>";
                } else if (alt.isMuted()) {
                    mutedAltNames.add(altName);
                    statusTag = "<yellow>[MUTED]</yellow>";
                } else {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(alt.getUuid());
                    if (op.isOnline()) {
                        statusTag = "<green>[ONLINE]</green>";
                    } else {
                        statusTag = "<gray>[CLEAN]</gray>";
                    }
                }
                hoverLines.add("<gray>- </gray>" + statusTag + " <white>" + altName + "</white>");
            }

            if (onlyIfPunished && bannedAltNames.isEmpty() && mutedAltNames.isEmpty()) {
                return;
            }

            String permission = plugin.getConfigManager().getString("alts.alert-on-join.permission", "wapeb.alts.notify");
            String message = plugin.getConfigManager().getString("messages.alts.login-notification", "");
            if (message.isEmpty()) {
                message = plugin.getConfigManager().getString("alts.login-notification", "");
            }
            if (message.isEmpty()) {
                message = "<gradient:#ff9900:#ff5500>[wapeB]</gradient> <yellow>Figyelem:</yellow> <white>%player%</white> <gray>belépett! <click:run_command:'/alts %player%'><hover:show_text:'<gray>Kattints az altok listázásához:\n%alts_hover%'><gold>[Altok: %total_alts_count% | Kitiltott: %banned_alts_count% | Némított: %muted_alts_count%]</gold></hover></click></gray>";
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%player%", playerName);
            placeholders.put("%ip%", playerIp);
            placeholders.put("%ip_address%", playerIp);
            placeholders.put("%total_alts_count%", String.valueOf(detailedAlts.size()));
            placeholders.put("%alt_count%", String.valueOf(detailedAlts.size()));
            placeholders.put("%banned_alts_count%", String.valueOf(bannedAltNames.size()));
            placeholders.put("%muted_alts_count%", String.valueOf(mutedAltNames.size()));
            placeholders.put("%banned_alts%", String.join(", ", bannedAltNames));
            placeholders.put("%muted_alts%", String.join(", ", mutedAltNames));
            placeholders.put("%alts%", String.join(", ", allAltNames));
            placeholders.put("%alts_list%", String.join(", ", allAltNames));
            placeholders.put("%alts_hover%", String.join("\n", hoverLines));
            placeholders.put("%alts_list_hover%", String.join("\n", hoverLines));

            final String finalMessage = message;
            Bukkit.getScheduler().runTask(plugin, () -> 
                Bukkit.getOnlinePlayers().stream()
                      .filter(staff -> staff.hasPermission(permission) || staff.hasPermission("wapeb.notify"))
                      .forEach(staff -> staff.sendMessage(MessageUtil.createComponent(finalMessage, null, placeholders)))
            );
        });
    }

    private void notifyPunishment(Punishment punishment, String playerName, UUID playerUuid) {
        // Check cooldown for ban alert
        long currentTime = System.currentTimeMillis();
        if (lastBanAlertTime.containsKey(playerUuid) &&
            currentTime - lastBanAlertTime.get(playerUuid) < BAN_ALERT_COOLDOWN_MILLIS) {
            return; // Cooldown is active, do not send notification
        }
        
        String permission = plugin.getConfigManager().getString("messages.punishment-notification.permission", "wapeb.notify.punishment");
        String message = plugin.getConfigManager().getString("messages.punishment-notification.ban-attempt", "%prefix% §c%player% §ftried to join but is banned for §c%duration%§f.");
        if (permission.isEmpty() || message.isEmpty()) return;

        // Update the timestamp and notify staff
        lastBanAlertTime.put(playerUuid, currentTime);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", playerName);

        Bukkit.getScheduler().runTask(plugin, () -> 
            Bukkit.getOnlinePlayers().stream()
                  .filter(staff -> staff.hasPermission(permission))
                  .forEach(staff -> staff.sendMessage(MessageUtil.createComponent(message, punishment, placeholders)))
        );
    }
}