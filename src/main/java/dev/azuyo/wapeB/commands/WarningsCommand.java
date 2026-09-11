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

import java.util.Collections;
import java.util.List;

public class WarningsCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public WarningsCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.warnings")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warnings.usage", ""), null));
            return true;
        }

        String targetNameInput = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetNameInput);
        List<Punishment> warnings = plugin.getApi().getWarnings(targetNameInput);

        if (warnings.isEmpty() && !target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", ""), null));
            return true;
        }

        if (warnings.isEmpty()) {
            String targetDisplayName = target.getName() != null ? target.getName() : targetNameInput;
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-warnings", ""), null, Collections.singletonMap("%player%", targetDisplayName)));
            return true;
        }

        // Header
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warnings.header", ""), warnings.get(0)));

        // Lines
        String lineFormat = configManager.getString("messages.warnings.line", "");
        for (Punishment warning : warnings) {
            sender.sendMessage(MessageUtil.createComponent(lineFormat, warning));
        }

        // Footer
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warnings.footer", ""), warnings.get(0)));

        return true;
    }
}