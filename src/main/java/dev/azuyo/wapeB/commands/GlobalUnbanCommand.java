package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class GlobalUnbanCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public GlobalUnbanCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.console-only", "<red>This command can only be used from the console!"), null));
            return true;
        }

        dataManager.deactivateAllBans();
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.global-unban-success", "<green>All active bans have been successfully removed (Global Unban)."), null));

        return true;
    }
}