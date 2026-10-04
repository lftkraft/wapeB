package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateManager {

    public static class PunishmentTemplate {
        private final String key;
        private final String category;
        private final String reason;
        private final String duration;
        private final boolean silent;
        private final String shortcut;

        public PunishmentTemplate(String key, String category, String reason, String duration) {
            this(key, category, reason, duration, false, null);
        }

        public PunishmentTemplate(String key, String category, String reason, String duration, boolean silent, String shortcut) {
            this.key = key;
            this.category = category;
            this.reason = reason;
            this.duration = duration != null ? duration : "perm";
            this.silent = silent;
            this.shortcut = shortcut;
        }

        public String getKey() { return key; }
        public String getCategory() { return category; }
        public String getReason() { return reason; }
        public String getDuration() { return duration; }
        public boolean isSilent() { return silent; }
        public String getShortcut() { return shortcut; }
    }

    private final WapeB plugin;
    private File file;
    private FileConfiguration config;
    private final Map<String, Map<String, PunishmentTemplate>> templates = new ConcurrentHashMap<>();
    private final Map<String, PunishmentTemplate> shortcutMap = new ConcurrentHashMap<>();

    public TemplateManager(WapeB plugin) {
        this.plugin = plugin;
        loadTemplates();
    }

    public synchronized void loadTemplates() {
        this.templates.clear();
        this.shortcutMap.clear();
        this.file = new File(plugin.getDataFolder(), "templates.yml");
        if (!file.exists()) {
            plugin.saveResource("templates.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        for (String category : config.getKeys(false)) {
            if (config.isConfigurationSection(category)) {
                Map<String, PunishmentTemplate> categoryMap = new ConcurrentHashMap<>();
                Set<String> keys = config.getConfigurationSection(category).getKeys(false);
                for (String key : keys) {
                    String path = category + "." + key;
                    String reason = config.getString(path + ".reason", "No reason specified");
                    String duration = config.getString(path + ".duration", null);
                    boolean silent = config.getBoolean(path + ".silent", false);
                    String shortcut = config.getString(path + ".shortcut", null);

                    PunishmentTemplate template = new PunishmentTemplate(key, category, reason, duration, silent, shortcut);
                    categoryMap.put(key.toLowerCase(), template);

                    if (shortcut != null && !shortcut.isEmpty()) {
                        shortcutMap.put(shortcut.toLowerCase(), template);
                    }
                    // Also map by $key and #key
                    shortcutMap.put("$" + key.toLowerCase(), template);
                    shortcutMap.put("#" + key.toLowerCase(), template);
                }
                templates.put(category.toLowerCase(), categoryMap);
            }
        }
    }

    public PunishmentTemplate getTemplate(String category, String templateKey) {
        if (templateKey == null) return null;

        // Check shortcut map first if starts with # or $
        if (templateKey.startsWith("#") || templateKey.startsWith("$")) {
            PunishmentTemplate pt = shortcutMap.get(templateKey.toLowerCase());
            if (pt != null) return pt;
        }

        if (category == null) {
            return findTemplate(templateKey);
        }

        String cleanKey = templateKey.startsWith("$") || templateKey.startsWith("#") ? templateKey.substring(1) : templateKey;
        Map<String, PunishmentTemplate> categoryMap = templates.get(category.toLowerCase());
        if (categoryMap != null) {
            PunishmentTemplate pt = categoryMap.get(cleanKey.toLowerCase());
            if (pt != null) return pt;
        }

        return shortcutMap.get(templateKey.toLowerCase());
    }

    public PunishmentTemplate findTemplate(String keyOrShortcut) {
        if (keyOrShortcut == null) return null;
        PunishmentTemplate pt = shortcutMap.get(keyOrShortcut.toLowerCase());
        if (pt != null) return pt;

        String clean = keyOrShortcut.startsWith("#") || keyOrShortcut.startsWith("$") ? keyOrShortcut.substring(1).toLowerCase() : keyOrShortcut.toLowerCase();

        for (Map<String, PunishmentTemplate> cat : templates.values()) {
            if (cat.containsKey(clean)) {
                return cat.get(clean);
            }
        }
        return null;
    }

    public Map<String, Map<String, PunishmentTemplate>> getAllTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    public List<PunishmentTemplate> getTemplatesForCategory(String category) {
        if (category == null) return Collections.emptyList();
        Map<String, PunishmentTemplate> categoryMap = templates.get(category.toLowerCase());
        if (categoryMap != null) {
            return new ArrayList<>(categoryMap.values());
        }
        return Collections.emptyList();
    }

    public synchronized boolean saveTemplate(String category, String key, String reason, String duration, boolean silent, String shortcut) {
        if (category == null || key == null) return false;
        String path = category.toLowerCase() + "." + key.toLowerCase();
        config.set(path + ".reason", reason != null ? reason : "No reason specified");
        config.set(path + ".duration", duration);
        config.set(path + ".silent", silent);
        if (shortcut != null && !shortcut.isEmpty()) {
            config.set(path + ".shortcut", shortcut);
        }
        try {
            config.save(file);
            loadTemplates();
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save template: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean deleteTemplate(String category, String key) {
        if (category == null || key == null) return false;
        String path = category.toLowerCase() + "." + key.toLowerCase();
        if (!config.contains(path)) return false;
        config.set(path, null);
        try {
            config.save(file);
            loadTemplates();
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to delete template: " + e.getMessage());
            return false;
        }
    }
}
