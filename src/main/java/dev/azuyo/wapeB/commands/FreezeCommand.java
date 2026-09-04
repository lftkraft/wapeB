package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.FreezeManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FreezeCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;
    private final FreezeManager freezeManager;

    public FreezeCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.freezeManager = plugin.getFreezeManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("freeze.usage", "Usage: /freeze <player> [reason] or /unfreeze <player>"), null));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", ""), null));
            return true;
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");

        if (command.getName().equalsIgnoreCase("freeze")) {
            if (!sender.hasPermission("wapeb.freeze")) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
                return true;
            }
            if (freezeManager.isFrozen(target)) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("freeze.messages.already-frozen", ""), null));
                return true;
            }

            freezeManager.freezePlayer(target);
            String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No reason specified.";
            
            // Send freeze screen message using the correct method
            Punishment tempPunishment = new Punishment(0, target.getUniqueId(), target.getName(), Punishment.PunishmentType.KICK, reason, executorName, System.currentTimeMillis(), 0);
            Component freezeMessage = MessageUtil.formatKickScreen(configManager.getStringList("freeze.messages.freeze-screen"), tempPunishment);
            target.sendMessage(freezeMessage);

            String frozenSuccessMessage = configManager.getString("freeze.messages.frozen-success", "%prefix% §aPlayer %player% has been frozen.");
            sender.sendMessage(MessageUtil.createComponent(frozenSuccessMessage.replace("%player%", target.getName()), null));

        } else if (command.getName().equalsIgnoreCase("unfreeze")) {
            if (!sender.hasPermission("wapeb.unfreeze")) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
                return true;
            }
            if (!freezeManager.isFrozen(target)) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("freeze.messages.not-frozen", ""), null));
                return true;
            }

            freezeManager.unfreezePlayer(target);
            String unfrozenSuccessMessage = configManager.getString("freeze.messages.unfrozen-success", "%prefix% §aPlayer %player% has been unfrozen.");
            sender.sendMessage(MessageUtil.createComponent(unfrozenSuccessMessage.replace("%player%", target.getName()), null));
            
            String unfrozenMessage = configManager.getString("freeze.messages.unfrozen", "%prefix% §aYou have been unfrozen by %executor%.");
            target.sendMessage(MessageUtil.createComponent(unfrozenMessage.replace("%executor%", executorName), null));
        }
        return true;
    }
}
