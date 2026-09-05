package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class KickCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;

    public KickCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.kick")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.kick.usage", "&cUsage: /kick <player> [reason] [-s]"), null));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
            return true;
        }

        boolean silent = args.length > 1 && args[args.length - 1].equalsIgnoreCase("-s");
        String[] reasonArgs = silent ? Arrays.copyOfRange(args, 1, args.length - 1) : Arrays.copyOfRange(args, 1, args.length);
        String reason = String.join(" ", reasonArgs);

        if (reason.isEmpty()) {
            reason = configManager.getString("messages.kick.default-reason", "You have been kicked.");
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");

        boolean success = plugin.getApi().kickPlayer(target.getUniqueId(), reason, executorName, silent);
        if (success) {
            Punishment p = new Punishment(0, target.getUniqueId(), target.getName(), Punishment.PunishmentType.KICK, reason, executorName, System.currentTimeMillis(), 0);
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.kick.success", "&aSuccessfully kicked %player%."), p));
        }

        return true;
    }
}