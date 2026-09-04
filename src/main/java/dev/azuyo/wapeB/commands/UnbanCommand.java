package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.managers.PlayerDataManager; // Hozzáadva
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class UnbanCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager; // Hozzáadva
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private final List<Punishment.PunishmentType> banTypes = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN,
            Punishment.PunishmentType.FREEZE_LOGOUT_BAN
    );
    private final List<Punishment.PunishmentType> ipBanTypes = Arrays.asList( // Hozzáadva
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN
    );

    public UnbanCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
        this.playerDataManager = plugin.getPlayerDataManager(); // Hozzáadva
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.unban")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.unban.usage", "&cUsage: /unban <player/ip> [reason] [-s]"), null));
            return true;
        }

        String targetIdentifier = args[0];
        String targetIp = null;
        OfflinePlayer targetPlayer = null;
        Punishment activeBan = null;

        boolean isIp = IP_PATTERN.matcher(targetIdentifier).matches();

        if (isIp) {
            targetIp = targetIdentifier;
            activeBan = dataManager.getActivePunishment(null, targetIp, ipBanTypes);
        } else {
            targetPlayer = Bukkit.getOfflinePlayer(targetIdentifier);
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
                return true;
            }

            // Try to unban by player UUID first
            activeBan = dataManager.getActivePunishment(targetPlayer.getUniqueId(), null, banTypes);

            // If no direct player ban, try to find an IP ban associated with the player's last known IP
            if (activeBan == null) {
                targetIp = playerDataManager.getLastKnownIp(targetPlayer.getUniqueId());
                if (targetIp != null) {
                    activeBan = dataManager.getActivePunishment(null, targetIp, ipBanTypes);
                }
            }
        }
        
        if (activeBan == null) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.not-banned", "&cPlayer/IP is not banned."), null));
            return true;
        }

        activeBan.setActive(false);
        dataManager.savePunishment(activeBan);

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");
        boolean silent = args.length > 1 && args[args.length - 1].equalsIgnoreCase("-s");
        String reason = (args.length > 1 && !silent) ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Unbanned";
        if (silent && args.length > 2) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
        }
        if (reason.isEmpty()) reason = "Unbanned";


        Punishment unbanPunishment = new Punishment(
            activeBan.getId(), 
            activeBan.getPlayerUuid(), 
            activeBan.getPlayerName(), 
            activeBan.getIpAddress(), // Keep the original IP address from the ban
            activeBan.getType(), 
            reason, 
            executorName, 
            System.currentTimeMillis(), 
            0 // Duration 0 for unban
        );

        // Broadcast
        String broadcastMessageConfig = configManager.getString("messages.unban.broadcast", "%prefix% %executor% unbanned %player%.");
        if (silent) {
            String silentPrefix = configManager.getString("messages.unban.silent.prefix", "&7(Silent) ");
            Component silentBroadcast = MessageUtil.createComponent(silentPrefix + broadcastMessageConfig, unbanPunishment);
            Bukkit.broadcast(silentBroadcast, "wapeb.notify");
        } else {
            Component broadcast = MessageUtil.createComponent(broadcastMessageConfig, unbanPunishment);
            Bukkit.broadcast(broadcast);
        }

        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.unban.success", "&aSuccessfully unbanned %player%."), unbanPunishment));

        return true;
    }
}
