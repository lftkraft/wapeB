package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KickallCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;

    public KickallCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.kickall")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        String reason = String.join(" ", args);
        if (reason.isEmpty()) {
            reason = configManager.getString("messages.kickall.default-reason", "All players kicked.");
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");
        
        // Create a temporary punishment object for message formatting
        Punishment tempPunishment = new Punishment(0, null, "All Players", Punishment.PunishmentType.KICK, reason, executorName, System.currentTimeMillis(), 0);

        Component kickMessage = MessageUtil.formatKickScreen(configManager.getStringList("messages.kickall.kick-screen"), tempPunishment);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("wapeb.kickall.bypass")) {
                player.kick(kickMessage);
            }
        }

        String broadcastMessage = configManager.getString("messages.kickall.broadcast", "");
        if (!broadcastMessage.isEmpty()) {
            Bukkit.broadcast(MessageUtil.createComponent(broadcastMessage, tempPunishment));
        }
        
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.kickall.success", ""), tempPunishment));

        return true;
    }
}