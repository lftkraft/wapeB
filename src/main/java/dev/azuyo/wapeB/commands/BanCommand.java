package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.managers.PlayerDataManager;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BanCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final PlayerDataManager playerDataManager;
    private final ConfigManager configManager;
    private final List<Punishment.PunishmentType> banTypes = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN
    );

    public BanCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.playerDataManager = plugin.getPlayerDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.ban")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.ban.usage", "&cUsage: /ban <player> [time] [reason] [-s] [-ip]"), null));
            return true;
        }

        // --- Argument Parsing ---
        String targetName = args[0];
        List<String> arguments = new ArrayList<>(Arrays.asList(args).subList(1, args.length));

        boolean silent = arguments.remove("-s");
        boolean ipBan = arguments.remove("-ip");

        long duration = -1;
        String reason;

        if (!arguments.isEmpty()) {
            long parsedTime = TimeUtil.parseTime(arguments.get(0));
            if (parsedTime != -1) {
                duration = parsedTime;
                arguments.remove(0);
            }
        }
        
        reason = String.join(" ", arguments);
        if (reason.isEmpty()) {
            reason = configManager.getString("messages.ban.default-reason", "The Ban Hammer has spoken!");
        }

        // --- Target and IP Resolution ---
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
             sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
             return true;
        }

        UUID targetUuid = target.getUniqueId();
        String targetIp = null;

        if (target.isOnline()) {
            Player onlineTarget = (Player) target;
            if (onlineTarget.getAddress() != null) {
                targetIp = onlineTarget.getAddress().getAddress().getHostAddress();
            }
        } else {
            targetIp = playerDataManager.getLastKnownIp(targetUuid);
        }

        if (ipBan && targetIp == null) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.ban.no-ip-found", "&cCould not find a last known IP for that player."), null));
            return true;
        }

        // --- Punishment Logic ---
        // Deactivate existing active bans for this player
        Punishment existingBan = dataManager.getActivePunishment(targetUuid, targetIp, banTypes);
        if (existingBan != null) {
            existingBan.setActive(false);
            dataManager.savePunishment(existingBan);
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");
        int punishmentId = dataManager.getNextId();

        Punishment.PunishmentType type;
        if (ipBan) {
            type = (duration == -1) ? Punishment.PunishmentType.IPBAN : Punishment.PunishmentType.TEMPIPBAN;
        } else {
            type = (duration == -1) ? Punishment.PunishmentType.BAN : Punishment.PunishmentType.TEMPBAN;
        }

        Punishment punishment = new Punishment(punishmentId, targetUuid, target.getName(), targetIp, type, reason, executorName, System.currentTimeMillis(), duration);
        dataManager.savePunishment(punishment);

        // --- Webhook ---
        WebhookUtil.sendPunishmentWebhook(punishment);

        // --- Kicking and Broadcasting ---
        if (target.isOnline()) {
            Player onlineTarget = (Player) target;
            // Use a Bukkit scheduler task to kick the player on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                Component kickMessage = MessageUtil.formatKickScreen(configManager.getStringList("messages.ban.kick-screen"), punishment);
                onlineTarget.kick(kickMessage);
            });
        }

        String broadcastMessageConfig = configManager.getString("messages.ban.broadcast", "%prefix% %executor% banned %player%.");
        if (silent) {
            String silentPrefix = configManager.getString("messages.ban.silent.prefix", "&7(Silent) ");
            Component silentBroadcast = MessageUtil.createComponent(silentPrefix + broadcastMessageConfig, punishment);
            Bukkit.broadcast(silentBroadcast, "wapeb.notify");
        } else {
            Component broadcast = MessageUtil.createComponent(broadcastMessageConfig, punishment);
            Bukkit.broadcast(broadcast);
        }

        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.ban.success", "&aSuccessfully banned %player%."), punishment));

        return true;
    }
}