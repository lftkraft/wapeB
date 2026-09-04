package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.LockdownManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;

public class LockdownCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;
    private final LockdownManager lockdownManager;

    public LockdownCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.lockdownManager = plugin.getLockdownManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.lockdown")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        if (args.length == 0) {
            // Display current status
            String statusMessage = configManager.getString("lockdown.messages.status", "%prefix% <white>Lockdown status: <gradient:#FF00D9:#B300FF>%status% <white>Reason: <gradient:#FF00D9:#B300FF>%reason%");
            statusMessage = statusMessage.replace("%status%", lockdownManager.isLockdownEnabled() ? "Enabled" : "Disabled")
                                         .replace("%reason%", lockdownManager.getLockdownReason());
            sender.sendMessage(MessageUtil.createComponent(statusMessage, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            String newReason = null;
            if (args.length > 1) {
                newReason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                lockdownManager.setLockdownReason(newReason);
            }
            
            lockdownManager.setLockdownEnabled(true);
            
            if (newReason != null) {
                String reasonSetMessage = configManager.getString("lockdown.messages.reason-set", "%prefix% <green>Lockdown reason set to: <white>%reason%");
                sender.sendMessage(MessageUtil.createComponent(reasonSetMessage, null, Collections.singletonMap("%reason%", newReason)));
            } else {
                String enabledMessage = configManager.getString("lockdown.messages.enabled", "%prefix% <red>Server lockdown has been enabled.");
                sender.sendMessage(MessageUtil.createComponent(enabledMessage, null));
            }
            
            // Kick all players without bypass permission
            Component kickMessage = MessageUtil.formatKickScreen(lockdownManager.getKickScreen(), new Punishment(0, null, "All Players", Punishment.PunishmentType.KICK, lockdownManager.getLockdownReason(), sender.getName(), System.currentTimeMillis(), 0));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission(configManager.getString("lockdown.bypass-permission", "wapeb.lockdown.bypass"))) {
                    player.kick(kickMessage);
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            lockdownManager.setLockdownEnabled(false);
            String disabledMessage = configManager.getString("lockdown.messages.disabled", "%prefix% <green>Server lockdown has been disabled.");
            sender.sendMessage(MessageUtil.createComponent(disabledMessage, null));
            return true;
        }

        if (args[0].equalsIgnoreCase("reason")) {
            if (args.length < 2) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("lockdown.messages.usage", "%prefix% <white>Usage: <red>/lockdown <on|off|reason> [reason]"), null));
                return true;
            }
            String newReason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            lockdownManager.setLockdownReason(newReason);
            String reasonSetMessage = configManager.getString("lockdown.messages.reason-set", "%prefix% <green>Lockdown reason set to: <white>%reason%");
            sender.sendMessage(MessageUtil.createComponent(reasonSetMessage, null, Collections.singletonMap("%reason%", newReason)));
            return true;
        }

        sender.sendMessage(MessageUtil.createComponent(configManager.getString("lockdown.messages.usage", "%prefix% <white>Usage: <red>/lockdown <on|off|reason> [reason]"), null));
        return true;
    }
}