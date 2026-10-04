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
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandManager {

    private final WapeB plugin;
    private final Map<String, List<String>> customAliases = new ConcurrentHashMap<>();
    private final List<Command> registeredDynamicCommands = new CopyOnWriteArrayList<>();
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

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands() {
        if (commandMap == null) return null;
        try {
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            return (Map<String, Command>) knownCommandsField.get(commandMap);
        } catch (Exception e1) {
            try {
                Method getKnownCommandsMethod = commandMap.getClass().getMethod("getKnownCommands");
                return (Map<String, Command>) getKnownCommandsMethod.invoke(commandMap);
            } catch (Exception e2) {
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

        String lowerAlias = alias.toLowerCase().trim();
        customAliases.computeIfAbsent(originalCommandName.toLowerCase(), k -> new ArrayList<>()).add(lowerAlias);

        PluginCommand directCommand = plugin.getCommand(lowerAlias);
        if (directCommand != null) {
            // Already declared and registered directly via plugin.yml
            return true;
        }

        Map<String, Command> knownCommands = getKnownCommands();
        if (knownCommands != null) {
            Command existing = knownCommands.remove(lowerAlias);
            if (existing != null) {
                existing.unregister(commandMap);
            }
            Command existingPrefixed = knownCommands.remove(plugin.getName().toLowerCase() + ":" + lowerAlias);
            if (existingPrefixed != null) {
                existingPrefixed.unregister(commandMap);
            }
        }

        Command dynamicCommand = new Command(lowerAlias) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                WapeB instance = WapeB.getInstance();
                if (instance == null || !instance.isEnabled()) {
                    return false;
                }
                PluginCommand target = instance.getCommand(originalCommandName);
                if (target != null && target.getPlugin().isEnabled()) {
                    return target.execute(sender, commandLabel, args);
                }
                return false;
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                WapeB instance = WapeB.getInstance();
                if (instance == null || !instance.isEnabled()) {
                    return Collections.emptyList();
                }
                PluginCommand target = instance.getCommand(originalCommandName);
                if (target != null && target.getPlugin().isEnabled()) {
                    return target.tabComplete(sender, alias, args);
                }
                return Collections.emptyList();
            }
        };

        PluginCommand originalCommand = plugin.getCommand(originalCommandName);
        if (originalCommand != null) {
            dynamicCommand.setDescription(originalCommand.getDescription());
            dynamicCommand.setPermission(originalCommand.getPermission());
            dynamicCommand.setUsage(originalCommand.getUsage());
        }

        registeredDynamicCommands.add(dynamicCommand);
        commandMap.register(plugin.getName().toLowerCase(), dynamicCommand);
        if (knownCommands != null) {
            knownCommands.put(lowerAlias, dynamicCommand);
            knownCommands.put(plugin.getName().toLowerCase() + ":" + lowerAlias, dynamicCommand);
        }
        plugin.getLogger().info("Registered custom command alias /" + alias + " -> /" + originalCommandName);
        return true;
    }

    public void unregisterAll() {
        if (commandMap == null) return;
        Map<String, Command> knownCommands = getKnownCommands();
        for (Command cmd : registeredDynamicCommands) {
            cmd.unregister(commandMap);
            if (knownCommands != null) {
                knownCommands.remove(cmd.getName().toLowerCase());
                knownCommands.remove(plugin.getName().toLowerCase() + ":" + cmd.getName().toLowerCase());
                for (String alias : cmd.getAliases()) {
                    knownCommands.remove(alias.toLowerCase());
                    knownCommands.remove(plugin.getName().toLowerCase() + ":" + alias.toLowerCase());
                }
            }
        }
        registeredDynamicCommands.clear();
        customAliases.clear();
    }

    public List<String> getAliases(String originalCommandName) {
        return customAliases.getOrDefault(originalCommandName.toLowerCase(), Collections.emptyList());
    }
}
