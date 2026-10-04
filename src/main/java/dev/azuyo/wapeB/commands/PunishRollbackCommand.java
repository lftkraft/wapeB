package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class PunishRollbackCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public PunishRollbackCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.rollback")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.rollback-usage", ""), null));
            return true;
        }

        String argId = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int id = Integer.parseInt(argId);
                Punishment p = dataManager.getPunishment(id);

                if (p == null) {
                    sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", ""), null));
                    return;
                }

                p.setActive(false);
                dataManager.savePunishment(p);

                // Webhook for rollback
                WebhookUtil.sendRollbackWebhook(p);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%punishment_id%", String.valueOf(p.getId()));
                placeholders.put("%type%", p.getType().toString());
                placeholders.put("%player%", p.getPlayerName());

                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.rollback-success", ""), null, placeholders));

            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", ""), null));
            }
        });

        return true;
    }
}