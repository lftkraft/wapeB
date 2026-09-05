package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TemplateManager {

    public static class PunishmentTemplate {
        private final String key;
        private final String category;
        private final String reason;
        private final String duration;

        public PunishmentTemplate(String key, String category, String reason, String duration) {
            this.key = key;
            this.category = category;
            this.reason = reason;
            this.duration = duration != null ? duration : "perm";
        }

        public String getKey() { return key; }
        public String getCategory() { return category; }
        public String getReason() { return reason; }
        public String getDuration() { return duration; }
    }

    private final WapeB plugin;
    private File file;
    private FileConfiguration config;
    private final Map<String, Map<String, PunishmentTemplate>> templates = new HashMap<>();

    public TemplateManager(WapeB plugin) {
        this.plugin = plugin;
        loadTemplates();
    }

    public void loadTemplates() {
        this.templates.clear();
        this.file = new File(plugin.getDataFolder(), "templates.yml");
        if (!file.exists()) {
            plugin.saveResource("templates.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        for (String category : config.getKeys(false)) {
            if (config.isConfigurationSection(category)) {
                Map<String, PunishmentTemplate> categoryMap = new HashMap<>();
                Set<String> keys = config.getConfigurationSection(category).getKeys(false);
                for (String key : keys) {
                    String path = category + "." + key;
                    String reason = config.getString(path + ".reason", "No reason specified");
                    String duration = config.getString(path + ".duration", null);
                    categoryMap.put(key.toLowerCase(), new PunishmentTemplate(key, category, reason, duration));
                }
                templates.put(category.toLowerCase(), categoryMap);
            }
        }
    }

    public PunishmentTemplate getTemplate(String category, String templateKey) {
        if (category == null || templateKey == null) return null;
        String cleanKey = templateKey.startsWith("$") ? templateKey.substring(1) : templateKey;
        Map<String, PunishmentTemplate> categoryMap = templates.get(category.toLowerCase());
        if (categoryMap != null) {
            return categoryMap.get(cleanKey.toLowerCase());
        }
        return null;
    }

    public Map<String, Map<String, PunishmentTemplate>> getAllTemplates() {
        return templates;
    }

    public java.util.List<PunishmentTemplate> getTemplatesForCategory(String category) {
        if (category == null) return java.util.Collections.emptyList();
        Map<String, PunishmentTemplate> categoryMap = templates.get(category.toLowerCase());
        if (categoryMap != null) {
            return new java.util.ArrayList<>(categoryMap.values());
        }
        return java.util.Collections.emptyList();
    }
}
