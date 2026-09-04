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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public HistoryCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.history")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.history.usage", ""), null));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", ""), null));
            return true;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        List<Punishment> history = dataManager.getHistory(target.getUniqueId());
        Collections.reverse(history); // Show newest first

        if (history.isEmpty()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-history", ""), null, Collections.singletonMap("%player%", target.getName())));
            return true;
        }

        int pageSize = 5;
        int maxPage = (int) Math.ceil((double) history.size() / pageSize);
        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;

        Map<String, String> globalPlaceholders = new HashMap<>();
        globalPlaceholders.put("%player%", target.getName());
        globalPlaceholders.put("%page%", String.valueOf(page));
        globalPlaceholders.put("%max_page%", String.valueOf(maxPage));

        // Header
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.history.header", ""), null, globalPlaceholders));

        // Lines
        String lineFormat = configManager.getString("messages.history.line", "");
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, history.size());

        for (int i = start; i < end; i++) {
            Punishment p = history.get(i);
            sender.sendMessage(MessageUtil.createComponent(lineFormat, p, globalPlaceholders));
        }

        // Footer
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.history.footer", ""), null, globalPlaceholders));

        return true;
    }
}
