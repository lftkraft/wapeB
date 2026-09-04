package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.managers.PlayerDataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
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

public class BanIpCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private final List<Punishment.PunishmentType> ipBanTypes = Arrays.asList(
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN
    );

    public BanIpCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.banip")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.banip.usage", "&cUsage: /banip <player/ip> [time] [reason] [-s]"), null));
            return true;
        }

        String targetIdentifier = args[0];
        String targetIp = null;
        OfflinePlayer targetPlayer = null;
        String finalTargetName = targetIdentifier;

        if (IP_PATTERN.matcher(targetIdentifier).matches()) {
            targetIp = targetIdentifier;
        } else {
            targetPlayer = Bukkit.getOfflinePlayer(targetIdentifier);
            
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
                return true;
            }

            finalTargetName = targetPlayer.getName();
            if (targetPlayer.isOnline()) {
                targetIp = targetPlayer.getPlayer().getAddress().getAddress().getHostAddress();
            } else {
                targetIp = playerDataManager.getLastKnownIp(targetPlayer.getUniqueId());
            }
        }
        
        if (targetIp == null) {
            String noIpMessage = configManager.getString("messages.no-ip-history", "&cNo recorded IP address found for offline player %player%.")
                                             .replace("%player%", finalTargetName);
            sender.sendMessage(MessageUtil.createComponent(noIpMessage, null));
            return true;
        }

        boolean silent = args.length > 1 && args[args.length - 1].equalsIgnoreCase("-s");
        String[] reasonArgs = silent ? Arrays.copyOfRange(args, 1, args.length - 1) : Arrays.copyOfRange(args, 1, args.length);
        long duration = -1;
        String reason;

        if (reasonArgs.length > 0) {
            long parsedTime = TimeUtil.parseTime(reasonArgs[0]);
            if (parsedTime != -1) {
                duration = parsedTime;
                reason = String.join(" ", Arrays.copyOfRange(reasonArgs, 1, reasonArgs.length));
            } else {
                reason = String.join(" ", reasonArgs);
            }
        } else {
            reason = configManager.getString("messages.ban.default-reason", "The Ban Hammer has spoken!");
        }
        if (reason.isEmpty()) {
            reason = configManager.getString("messages.ban.default-reason", "The Ban Hammer has spoken!");
        }

        Punishment existingIpBan = dataManager.getActivePunishment(null, targetIp, ipBanTypes);
        if (existingIpBan != null) {
            existingIpBan.setActive(false);
            dataManager.savePunishment(existingIpBan);
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");
        int punishmentId = dataManager.getNextId();
        Punishment.PunishmentType type = (duration == -1) ? Punishment.PunishmentType.IPBAN : Punishment.PunishmentType.TEMPIPBAN;

        Punishment punishment = new Punishment(punishmentId, targetPlayer != null ? targetPlayer.getUniqueId() : null, finalTargetName, targetIp, type, reason, executorName, System.currentTimeMillis(), duration);
        dataManager.savePunishment(punishment);

        // KICK ALL PLAYERS ON THE BANNED IP
        final String finalTargetIp = targetIp;
        Component kickMessage = MessageUtil.formatKickScreen(configManager.getStringList("messages.ban.kick-screen"), punishment);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getAddress().getAddress().getHostAddress().equals(finalTargetIp)) {
                    p.kick(kickMessage);
                }
            }
        });

        String broadcastMessageConfig = configManager.getString("messages.banip.broadcast", "%prefix% %executor% IP-banned %player%.");
        if (silent) {
            String silentPrefix = configManager.getString("messages.banip.silent.prefix", "&7(Silent) ");
            Component silentBroadcast = MessageUtil.createComponent(silentPrefix + broadcastMessageConfig, punishment);
            Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("wapeb.notify")).forEach(p -> p.sendMessage(silentBroadcast));
        } else {
            Component broadcast = MessageUtil.createComponent(broadcastMessageConfig, punishment);
            Bukkit.broadcast(broadcast);
        }

        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.banip.success", "&aSuccessfully IP-banned %player%."), punishment));

        return true;
    }
}
