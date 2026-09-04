package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

public class UnwarnCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public UnwarnCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.unwarn")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.unwarn.usage", "&cUsage: /unwarn <player> <id> [-s]"), null));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
            return true;
        }

        int punishmentId;
        try {
            punishmentId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", "&cInvalid ID."), null));
            return true;
        }

        Punishment punishment = dataManager.getPunishment(punishmentId);

        if (punishment == null || !punishment.getPlayerUuid().equals(target.getUniqueId()) || punishment.getType() != Punishment.PunishmentType.WARN) {
            String notFoundMsg = configManager.getString("messages.unwarn.not-found", "%prefix% <red>Warning with ID #%punishment_id% not found for this player.");
            sender.sendMessage(MessageUtil.createComponent(notFoundMsg, null, Collections.singletonMap("%punishment_id%", String.valueOf(punishmentId))));
            return true;
        }

        dataManager.removePunishment(punishmentId);

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");
        boolean silent = args.length > 2 && args[args.length - 1].equalsIgnoreCase("-s");
        
        // Creating a temporary punishment object with WARN type to avoid NPE and show correct info in messages
        Punishment tempPunishment = new Punishment(punishmentId, target.getUniqueId(), target.getName(), Punishment.PunishmentType.WARN, punishment.getReason(), executorName, System.currentTimeMillis(), 0);

        // Broadcast
        String broadcastMessageConfig = configManager.getString("messages.unwarn.broadcast", "%prefix% %executor% removed a warning from %player%.");
        if (silent) {
            String silentPrefix = configManager.getString("messages.unwarn.silent.prefix", "<gray>(Silent) ");
            Bukkit.broadcast(MessageUtil.createComponent(silentPrefix + broadcastMessageConfig, tempPunishment), "wapeb.notify");
        } else {
            Bukkit.broadcast(MessageUtil.createComponent(broadcastMessageConfig, tempPunishment));
        }

        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.unwarn.success", "&aSuccessfully removed warning %punishment_id% from %player%."), tempPunishment));

        return true;
    }
}
