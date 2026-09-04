package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.WebhookUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class WarnCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public WarnCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.warn")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.usage", "&cUsage: /warn <player> <reason> [-s]"), null));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
            return true;
        }

        boolean silent = args[args.length - 1].equalsIgnoreCase("-s");
        String reason = String.join(" ", silent ? Arrays.copyOfRange(args, 1, args.length - 1) : Arrays.copyOfRange(args, 1, args.length));

        if (reason.isEmpty()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.usage", "&cUsage: /warn <player> <reason> [-s]"), null));
            return true;
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");
        int punishmentId = dataManager.getNextId();

        Punishment punishment = new Punishment(punishmentId, target.getUniqueId(), target.getName(), Punishment.PunishmentType.WARN, reason, executorName, System.currentTimeMillis(), 0);
        dataManager.savePunishment(punishment);

        // Webhook
        WebhookUtil.sendPunishmentWebhook(punishment);

        if (target.isOnline()) {
            target.getPlayer().sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.player-warned", ""), punishment));
        }

        // Broadcast
        String broadcastMessageConfig = configManager.getString("messages.warn.broadcast", "%prefix% %executor% warned %player%.");
        if (silent) {
            String silentPrefix = configManager.getString("messages.warn.silent.prefix", "&7(Silent) ");
            Component silentBroadcast = MessageUtil.createComponent(silentPrefix + broadcastMessageConfig, punishment);
            Bukkit.broadcast(silentBroadcast, "wapeb.notify");
        } else {
            Component broadcast = MessageUtil.createComponent(broadcastMessageConfig, punishment);
            Bukkit.broadcast(broadcast);
        }

        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.success", "&aSuccessfully warned %player%."), punishment));

        return true;
    }
}