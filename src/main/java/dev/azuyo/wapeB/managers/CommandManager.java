package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandManager {

    private final WapeB plugin;
    private final Map<String, List<String>> customAliases = new ConcurrentHashMap<>();
    private CommandMap commandMap;

    public CommandManager(WapeB plugin) {
        this.plugin = plugin;
        this.commandMap = getCommandMap();
        loadConfigAliases();
    }

    private CommandMap getCommandMap() {
        try {
            Method getCommandMapMethod = Bukkit.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) getCommandMapMethod.invoke(Bukkit.getServer());
        } catch (Exception e1) {
            try {
                Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                return (CommandMap) commandMapField.get(Bukkit.getServer());
            } catch (Exception e2) {
                plugin.getLogger().severe("Failed to retrieve Bukkit CommandMap: " + e2.getMessage());
                return null;
            }
        }
    }

    public void loadConfigAliases() {
        ConfigurationSection section = plugin.getConfigManager().getConfig().getConfigurationSection("command-overrides");
        if (section == null) return;

        for (String cmd : section.getKeys(false)) {
            List<String> aliases = section.getStringList(cmd);
            for (String alias : aliases) {
                registerAlias(cmd, alias);
            }
        }
    }

    public boolean registerAlias(String originalCommandName, String alias) {
        if (commandMap == null) return false;
        
        PluginCommand originalCommand = plugin.getCommand(originalCommandName);
        if (originalCommand == null) return false;

        String lowerAlias = alias.toLowerCase().trim();
        customAliases.computeIfAbsent(originalCommandName.toLowerCase(), k -> new ArrayList<>()).add(lowerAlias);

        Command dynamicCommand = new Command(lowerAlias) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return originalCommand.execute(sender, commandLabel, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                return originalCommand.tabComplete(sender, alias, args);
            }
        };

        dynamicCommand.setDescription(originalCommand.getDescription());
        dynamicCommand.setPermission(originalCommand.getPermission());
        dynamicCommand.setUsage(originalCommand.getUsage());

        commandMap.register(plugin.getName().toLowerCase(), dynamicCommand);
        plugin.getLogger().info("Registered custom command alias /" + alias + " -> /" + originalCommandName);
        return true;
    }

    public List<String> getAliases(String originalCommandName) {
        return customAliases.getOrDefault(originalCommandName.toLowerCase(), Collections.emptyList());
    }
}
