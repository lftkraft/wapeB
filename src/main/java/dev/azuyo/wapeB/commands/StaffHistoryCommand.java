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

public class StaffHistoryCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public StaffHistoryCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.staffhistory")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("staffhistory.usage", ""), null));
            return true;
        }

        String staffName = args[0];
        List<Punishment> staffHistory = dataManager.getStaffHistory(staffName);

        if (staffHistory.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%executor%", staffName);
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-staff-history", ""), null, placeholders));
            return true;
        }

        // Header
        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("%executor%", staffName);
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("staffhistory.header", ""), null, headerPlaceholders));

        // Lines
        String lineFormat = configManager.getString("staffhistory.line", "");
        for (Punishment p : staffHistory) {
            Map<String, String> pPlaceholders = new HashMap<>();
            pPlaceholders.put("%type%", p.getType().toString());
            sender.sendMessage(MessageUtil.createComponent(lineFormat, p, pPlaceholders));
        }

        // Footer
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("staffhistory.footer", ""), null));

        return true;
    }
}