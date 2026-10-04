package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
            } else {
                // Auto-sync missing keys in existing bundled language files
                syncLanguageFile(lang, langFile);
            }
        }

        // 2. Load main config.yml
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        syncConfigDefaults(configFile, config, "config.yml");

        // 3. Load active language file
        loadMessagesConfig();
    }

    private void syncLanguageFile(String lang, File file) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            boolean synced = syncConfigDefaults(file, yaml, "messages/" + lang + ".yml");
            if (synced && messagesFile != null && messagesFile.equals(file)) {
                this.messagesConfig = yaml;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not sync language file messages/" + lang + ".yml: " + e.getMessage());
        }
    }

    private boolean syncConfigDefaults(File targetFile, FileConfiguration targetConfig, String resourcePath) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return false;

            YamlConfiguration defaultYaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            Set<String> defaultKeys = defaultYaml.getKeys(true);
            boolean modified = false;
            int addedKeys = 0;

            for (String key : defaultKeys) {
                if (!targetConfig.contains(key)) {
                    targetConfig.set(key, defaultYaml.get(key));
                    modified = true;
                    addedKeys++;
                }
            }

            if (modified) {
                targetConfig.save(targetFile);
                plugin.getLogger().info("Automatically synchronized " + addedKeys + " missing configuration/message key(s) in " + targetFile.getName());
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to auto-sync defaults for " + targetFile.getName() + ": " + e.getMessage());
        }
        return false;
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
            syncConfigDefaults(messagesFile, messagesConfig, "messages/" + lang + ".yml");
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
        syncConfigDefaults(configFile, config, "config.yml");
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
        if (messagesConfig != null && messagesConfig.contains(path)) {
            String val = messagesConfig.getString(path);
            if (val != null && !val.isEmpty()) {
                return val;
            }
        }
        if (config != null && config.contains(path)) {
            String val = config.getString(path);
            if (val != null && !val.isEmpty()) {
                return val;
            }
        }
        if (defaultValue != null && !defaultValue.isEmpty()) {
            return defaultValue;
        }
        return "<red>Missing message: " + path + "</red>";
    }

    public String getString(String path) {
        return getString(path, null);
    }

    public List<String> getStringList(String path) {
        if (messagesConfig != null && messagesConfig.contains(path)) {
            List<String> list = messagesConfig.getStringList(path);
            if (list != null && !list.isEmpty()) {
                return list;
            }
        }
        if (config != null && config.contains(path)) {
            List<String> list = config.getStringList(path);
            if (list != null && !list.isEmpty()) {
                return list;
            }
        }
        return Collections.singletonList("<red>Missing message: " + path + "</red>");
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