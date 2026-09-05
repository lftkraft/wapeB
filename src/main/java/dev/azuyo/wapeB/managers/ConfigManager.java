package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private static final String[] BUNDLED_LANGUAGES = {
        "en", "hu", "de", "fr", "es", "pt", "ru", "ro", "da", "sv", "custom"
    };

    private final WapeB plugin;
    private FileConfiguration config;
    private File configFile;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public ConfigManager(WapeB plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        // 1. Ensure messages directory exists and save bundled language files
        File messagesDir = new File(plugin.getDataFolder(), "messages");
        if (!messagesDir.exists()) {
            messagesDir.mkdirs();
        }

        for (String lang : BUNDLED_LANGUAGES) {
            File langFile = new File(plugin.getDataFolder(), "messages/" + lang + ".yml");
            if (!langFile.exists()) {
                try {
                    plugin.saveResource("messages/" + lang + ".yml", false);
                } catch (Exception ignored) {
                }
            }
        }

        // 2. Load main config.yml
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // 3. Load active language file
        loadMessagesConfig();
    }

    private void loadMessagesConfig() {
        String lang = config != null ? config.getString("language", "en") : "en";
        messagesFile = new File(plugin.getDataFolder(), "messages/" + lang + ".yml");
        if (!messagesFile.exists()) {
            plugin.getLogger().warning("Language file 'messages/" + lang + ".yml' not found. Falling back to 'messages/en.yml'.");
            messagesFile = new File(plugin.getDataFolder(), "messages/en.yml");
        }

        if (messagesFile.exists()) {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        } else {
            messagesConfig = new YamlConfiguration();
        }

        // Load en.yml as default fallback for missing keys
        File defaultEnFile = new File(plugin.getDataFolder(), "messages/en.yml");
        if (defaultEnFile.exists() && !messagesFile.equals(defaultEnFile)) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultEnFile);
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadMessagesConfig();
        plugin.getLogger().info("Configuration and language messages reloaded.");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml!");
            e.printStackTrace();
        }
    }

    public String getString(String path, String defaultValue) {
        if (messagesConfig != null && (messagesConfig.isString(path) || messagesConfig.contains(path))) {
            String val = messagesConfig.getString(path);
            if (val != null) {
                return val;
            }
        }
        return config != null ? config.getString(path, defaultValue) : defaultValue;
    }

    public String getString(String path) {
        return getString(path, null);
    }

    public List<String> getStringList(String path) {
        if (messagesConfig != null && (messagesConfig.isList(path) || messagesConfig.contains(path))) {
            List<String> list = messagesConfig.getStringList(path);
            if (list != null && !list.isEmpty()) {
                return list;
            }
        }
        return config != null ? config.getStringList(path) : Collections.emptyList();
    }

    public int getInt(String path, int defaultValue) {
        if (messagesConfig != null && messagesConfig.isInt(path)) {
            return messagesConfig.getInt(path, defaultValue);
        }
        return config != null ? config.getInt(path, defaultValue) : defaultValue;
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        if (messagesConfig != null && messagesConfig.isBoolean(path)) {
            return messagesConfig.getBoolean(path, defaultValue);
        }
        return config != null ? config.getBoolean(path, defaultValue) : defaultValue;
    }

    public ConfigurationSection getConfigurationSection(String path) {
        if (messagesConfig != null && (messagesConfig.isConfigurationSection(path) || messagesConfig.contains(path))) {
            ConfigurationSection section = messagesConfig.getConfigurationSection(path);
            if (section != null) {
                return section;
            }
        }
        return config != null ? config.getConfigurationSection(path) : null;
    }
}