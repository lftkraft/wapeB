package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
import dev.azuyo.wapeB.utils.WebhookUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class MuteCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final List<Punishment.PunishmentType> muteTypes = Arrays.asList(
            Punishment.PunishmentType.MUTE,
            Punishment.PunishmentType.TEMPMUTE,
            Punishment.PunishmentType.IPMUTE,
            Punishment.PunishmentType.TEMPIPMUTE
    );

    public MuteCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.mute")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.mute.usage", "&cUsage: /mute <player> [time] [reason] [-s]"), null));
            return true;
        }

        String targetName = args[0];
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
            reason = "";
        }

        if (reason.isEmpty()) {
            reason = configManager.getString("messages.mute.default-reason", "You have been muted.");
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
            return true;
        }

        // Deactivate existing active mutes for this player
        Punishment existingMute = dataManager.getActivePunishment(target.getUniqueId(), null, muteTypes);
        if (existingMute != null) {
            existingMute.setActive(false);
            dataManager.savePunishment(existingMute);
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");
        int punishmentId = dataManager.getNextId();
        Punishment.PunishmentType type = (duration == -1) ? Punishment.PunishmentType.MUTE : Punishment.PunishmentType.TEMPMUTE;

        Punishment punishment = new Punishment(punishmentId, target.getUniqueId(), target.getName(), type, reason, executorName, System.currentTimeMillis(), duration);
        dataManager.savePunishment(punishment);

        // Webhook
        WebhookUtil.sendPunishmentWebhook(punishment);

        // Broadcast
        String broadcastMessageConfig = configManager.getString("messages.mute.broadcast", "%prefix% %executor% muted %player%.");
        if (silent) {
            String silentPrefix = configManager.getString("messages.mute.silent.prefix", "&7(Silent) ");
            Component silentBroadcast = MessageUtil.createComponent(silentPrefix + broadcastMessageConfig, punishment);
            Bukkit.broadcast(silentBroadcast, "wapeb.notify");
        } else {
            Component broadcast = MessageUtil.createComponent(broadcastMessageConfig, punishment);
            Bukkit.broadcast(broadcast);
        }

        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.mute.success", "&aSuccessfully muted %player%."), punishment));

        return true;
    }
}