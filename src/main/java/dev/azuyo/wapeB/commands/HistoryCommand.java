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

        String targetNameInput = args[0];
        String argPage = args.length > 1 ? args[1] : null;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetNameInput);
            List<Punishment> history = plugin.getApi().getHistory(targetNameInput);

            if (history.isEmpty() && !target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", ""), null));
                return;
            }

            int page = 1;
            if (argPage != null) {
                try {
                    page = Integer.parseInt(argPage);
                } catch (NumberFormatException ignored) {}
            }

            history.sort((a, b) -> Integer.compare(b.getId(), a.getId())); // Always show newest punishments first

            if (history.isEmpty()) {
                String targetDisplayName = target.getName() != null ? target.getName() : targetNameInput;
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-history", ""), null, Collections.singletonMap("%player%", targetDisplayName)));
                return;
            }

            int pageSize = 5;
            int maxPage = (int) Math.ceil((double) history.size() / pageSize);
            if (page < 1) page = 1;
            if (page > maxPage) page = maxPage;

            Map<String, String> globalPlaceholders = new HashMap<>();
            globalPlaceholders.put("%player%", target.getName() != null ? target.getName() : targetNameInput);
            globalPlaceholders.put("%page%", String.valueOf(page));
            globalPlaceholders.put("%max_page%", String.valueOf(maxPage));
            globalPlaceholders.put("%total%", String.valueOf(history.size()));

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
        });

        return true;
    }
}
