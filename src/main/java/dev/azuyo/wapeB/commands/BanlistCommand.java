package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BanlistCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public BanlistCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.banlist")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        List<Punishment> activeBans = dataManager.getAllActiveBans();

        if (activeBans.isEmpty()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-active-bans", ""), null));
            return true;
        }

        // Header
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("banlist.header", ""), null));

        // Lines
        String lineFormat = configManager.getString("banlist.line", "");
        for (Punishment ban : activeBans) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%type%", ban.getType().toString());
            sender.sendMessage(MessageUtil.createComponent(lineFormat, ban, placeholders));
        }

        // Footer
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("banlist.footer", ""), null));

        return true;
    }
}