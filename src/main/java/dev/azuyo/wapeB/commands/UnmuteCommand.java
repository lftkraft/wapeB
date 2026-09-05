package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.managers.PlayerDataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class UnmuteCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private final List<Punishment.PunishmentType> muteTypes = Arrays.asList(
            Punishment.PunishmentType.MUTE,
            Punishment.PunishmentType.TEMPMUTE,
            Punishment.PunishmentType.IPMUTE,
            Punishment.PunishmentType.TEMPIPMUTE,
            Punishment.PunishmentType.SENTINEL_AUTO_MUTE,
            Punishment.PunishmentType.SENTINEL_AI_MUTE
    );
    private final List<Punishment.PunishmentType> ipMuteTypes = Arrays.asList(
            Punishment.PunishmentType.IPMUTE,
            Punishment.PunishmentType.TEMPIPMUTE
    );

    public UnmuteCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.unmute")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.unmute.usage", "&cUsage: /unmute <player/ip> [reason] [-s]"), null));
            return true;
        }

        String targetIdentifier = args[0];
        String targetIp = null;
        OfflinePlayer targetPlayer = null;
        Punishment activeMute = null;

        boolean isIp = IP_PATTERN.matcher(targetIdentifier).matches();

        if (isIp) {
            targetIp = targetIdentifier;
            activeMute = dataManager.getActivePunishment(null, targetIp, ipMuteTypes);
        } else {
            targetPlayer = Bukkit.getOfflinePlayer(targetIdentifier);
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
                return true;
            }

            activeMute = dataManager.getActivePunishment(targetPlayer.getUniqueId(), null, muteTypes);
            if (activeMute == null) {
                targetIp = playerDataManager.getLastKnownIp(targetPlayer.getUniqueId());
                if (targetIp != null) {
                    activeMute = dataManager.getActivePunishment(null, targetIp, ipMuteTypes);
                }
            }
        }
        
        if (activeMute == null) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.not-muted", "&cPlayer/IP is not muted."), null));
            return true;
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");
        boolean silent = args.length > 1 && args[args.length - 1].equalsIgnoreCase("-s");
        String reason = (args.length > 1 && !silent) ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Unmuted";
        if (silent && args.length > 2) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
        }
        if (reason.isEmpty()) reason = "Unmuted";

        boolean success = plugin.getApi().unmutePlayer(activeMute.getPlayerUuid(), reason, executorName);
        if (success) {
            Punishment unmutePunishment = new Punishment(
                activeMute.getId(), 
                activeMute.getPlayerUuid(), 
                activeMute.getPlayerName(), 
                activeMute.getIpAddress(), 
                activeMute.getType(), 
                reason, 
                executorName, 
                System.currentTimeMillis(), 
                0
            );
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.unmute.success", "&aSuccessfully unmuted %player%."), unmutePunishment));
        }

        return true;
    }
}
