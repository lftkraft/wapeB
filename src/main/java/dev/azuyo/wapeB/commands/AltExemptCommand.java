package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.PlayerDataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AltExemptCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    public AltExemptCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.altexempt")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.altexempt.usage", "&cUsage: /altexempt <player> [add/remove]"), null));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : args[0];
        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");

        boolean actionAdd = args.length < 2 || args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("true");

        if (actionAdd) {
            playerDataManager.setAltExempt(target.getUniqueId(), true, executorName);
            sender.sendMessage(MessageUtil.createComponent(
                    configManager.getString("messages.altexempt.added", "&aSuccessfully exempted %player% from alt-account checks.").replace("%player%", targetName),
                    null
            ));
        } else {
            playerDataManager.setAltExempt(target.getUniqueId(), false, executorName);
            sender.sendMessage(MessageUtil.createComponent(
                    configManager.getString("messages.altexempt.removed", "&aSuccessfully removed alt-account exemption for %player%.").replace("%player%", targetName),
                    null
            ));
        }

        return true;
    }
}
