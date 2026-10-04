package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShadowMuteCommand implements CommandExecutor, TabCompleter {

    private final WapeB plugin;
    private final ConfigManager configManager;

    public ShadowMuteCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.shadowmute")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.shadowmute.usage", "&cUsage: /shadowmute <player> [time] [reason] [-s] [-ip]"), null));
            return true;
        }

        String targetName = args[0];
        List<String> arguments = new ArrayList<>(Arrays.asList(args).subList(1, args.length));

        boolean silent = arguments.remove("-s");
        boolean ipMute = arguments.remove("-ip");

        String proof = null;
        for (int i = arguments.size() - 1; i >= 0; i--) {
            String arg = arguments.get(i);
            if (arg.toLowerCase().startsWith("-proof:")) {
                proof = arg.substring(7);
                arguments.remove(i);
                break;
            } else if (arg.toLowerCase().startsWith("-proof=")) {
                proof = arg.substring(7);
                arguments.remove(i);
                break;
            }
        }

        String server = "global";
        for (int i = arguments.size() - 1; i >= 0; i--) {
            String arg = arguments.get(i);
            if (arg.toLowerCase().startsWith("-server:")) {
                server = arg.substring(8);
                arguments.remove(i);
                break;
            } else if (arg.toLowerCase().startsWith("-srv:")) {
                server = arg.substring(5);
                arguments.remove(i);
                break;
            } else if (arg.toLowerCase().startsWith("-server=")) {
                server = arg.substring(8);
                arguments.remove(i);
                break;
            } else if (arg.startsWith("-") && arg.length() > 1 && !arg.equalsIgnoreCase("-s") && !arg.equalsIgnoreCase("-ip") && !arg.matches("^-\\d+$")) {
                server = arg.substring(1);
                arguments.remove(i);
                break;
            }
        }

        long duration = -1;
        String reason = null;

        if (!arguments.isEmpty()) {
            String firstArg = arguments.get(0);
            dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate template = plugin.getTemplateManager().getTemplate("shadowmute", firstArg);
            if (template == null) {
                template = plugin.getTemplateManager().getTemplate("mute", firstArg);
            }
            if (template != null) {
                reason = template.getReason();
                if (template.getDuration() != null && !template.getDuration().equalsIgnoreCase("perm")) {
                    duration = TimeUtil.parseTime(template.getDuration());
                }
                if (template.isSilent()) {
                    silent = true;
                }
                arguments.remove(0);
            } else {
                long parsedTime = TimeUtil.parseTime(firstArg);
                if (parsedTime != -1) {
                    duration = parsedTime;
                    arguments.remove(0);
                }
            }
        }

        if (reason == null || reason.isEmpty()) {
            if (!arguments.isEmpty()) {
                String potentialTemplate = arguments.get(0);
                dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate template = plugin.getTemplateManager().getTemplate("shadowmute", potentialTemplate);
                if (template == null) {
                    template = plugin.getTemplateManager().getTemplate("mute", potentialTemplate);
                }
                if (template != null) {
                    reason = template.getReason();
                    if (duration == -1 && template.getDuration() != null && !template.getDuration().equalsIgnoreCase("perm")) {
                        duration = TimeUtil.parseTime(template.getDuration());
                    }
                    if (template.isSilent()) {
                        silent = true;
                    }
                } else {
                    reason = String.join(" ", arguments);
                }
            }
        }

        if (reason == null || reason.isEmpty()) {
            reason = configManager.getString("messages.shadowmute.default-reason", "Shadow muted.");
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");

        final String finalReason = reason;
        final long finalDuration = duration;
        final String finalServer = server;
        final String finalProof = proof;
        final boolean finalSilent = silent;
        final boolean finalIpMute = ipMute;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getApi().shadowMutePlayer(targetName, finalReason, executorName, finalDuration, finalSilent, finalIpMute, finalServer, finalServer, finalProof);
            if (success) {
                Punishment mute = plugin.getApi().getActiveShadowMute(targetName);
                if (mute == null) {
                    Punishment.PunishmentType pType = finalIpMute 
                            ? (finalDuration == -1 ? Punishment.PunishmentType.IPSHADOWMUTE : Punishment.PunishmentType.TEMPIPSHADOWMUTE)
                            : (finalDuration == -1 ? Punishment.PunishmentType.SHADOWMUTE : Punishment.PunishmentType.TEMPSHADOWMUTE);
                    mute = new Punishment(-1, target.getUniqueId(), targetName, null, pType, finalReason, executorName, finalServer, finalServer, finalProof, System.currentTimeMillis(), finalDuration);
                }
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.shadowmute.success", "&aSuccessfully shadow-muted %player%."), mute));
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("wapeb.shadowmute")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    players.add(p.getName());
                }
            }
            return players;
        } else if (args.length == 2) {
            List<String> suggestions = Arrays.asList("1h", "12h", "1d", "7d", "30d", "perm", "-s", "-ip", "-proof:", "-server:");
            List<String> result = new ArrayList<>();
            for (String s : suggestions) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) {
                    result.add(s);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
