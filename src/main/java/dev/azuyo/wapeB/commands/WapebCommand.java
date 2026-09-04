package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class WapebCommand implements CommandExecutor {

    private final WapeB plugin;

    public WapebCommand(WapeB plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("wapeb.reload")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                    return true;
                }
                plugin.getConfigManager().reloadConfig();
                if (plugin.getWebAPIManager() != null) {
                    plugin.getWebAPIManager().loadConfig();
                }

                // Re-initialize TimeUtil after reload
                ConfigurationSection timeSection = plugin.getConfigManager().getConfigurationSection("time-formats");
                if (timeSection != null) {
                    TimeUtil.init(
                        timeSection.getString("permanent"),
                        timeSection.getString("year"),
                        timeSection.getString("week"),
                        timeSection.getString("day"),
                        timeSection.getString("hour"),
                        timeSection.getString("minute"),
                        timeSection.getString("second")
                    );
                }

                sender.sendMessage(Component.text("wapeB configuration has been reloaded.", NamedTextColor.GREEN));
                return true;
            } else if (args[0].equalsIgnoreCase("unlink")) {
                if (!sender.hasPermission("wapeb.unlink")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                    return true;
                }
                
                plugin.getWebAPIManager().clearAllSessions();
                String message = plugin.getConfigManager().getString("web-api.messages.unlink-success", "%prefix% <green>Minden aktív webes munkamenet lezárva.");
                sender.sendMessage(MessageUtil.createComponent(message, null));
                return true;
            }
        }

        sender.sendMessage(Component.text("wapeB Punishments v1.0.0 by Azuyo", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Usage: /wapeb <reload|unlink>", NamedTextColor.GRAY));
        return true;
    }
}