package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.ai.GroqApiClient;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SentinelManager {
    private final WapeB plugin;
    private final boolean enabled;
    private final String bypassPermission;
    private final boolean wordFilterEnabled;
    private final List<String> bannedWords;
    private final List<String> bannedPhrases;
    private final int autoMuteThreshold;
    private final String autoMuteDuration;
    private final String autoMuteReason;
    private final boolean spamFloodEnabled;
    private final int spamThreshold;
    private final long spamTimeframe;
    private final int floodThreshold;
    private final long floodTimeframe;
    private final boolean aiEnabled;
    private final String aiName;
    private final String aiMuteDuration;
    private final String aiMuteReasonPrefix;
    private final GroqApiClient groqApiClient;

    private final Map<UUID, Integer> violationCount = new HashMap<>();
    private final Map<UUID, List<Long>> messageHistory = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> lastMessages = new ConcurrentHashMap<>();

    public SentinelManager(WapeB plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfigManager().getConfig().getBoolean("sentinel.enabled", true);
        this.bypassPermission = plugin.getConfigManager().getConfig().getString("sentinel.bypass-permission", "wapeb.sentinel.bypass");
        
        this.wordFilterEnabled = plugin.getConfigManager().getConfig().getBoolean("sentinel.word-filter.enabled", true);
        this.bannedWords = plugin.getConfigManager().getConfig().getStringList("sentinel.word-filter.banned-words");
        this.bannedPhrases = plugin.getConfigManager().getConfig().getStringList("sentinel.word-filter.banned-phrases");
        this.autoMuteThreshold = plugin.getConfigManager().getConfig().getInt("sentinel.word-filter.auto-mute-threshold", 3);
        this.autoMuteDuration = plugin.getConfigManager().getConfig().getString("sentinel.word-filter.auto-mute-duration", "30m");
        this.autoMuteReason = plugin.getConfigManager().getConfig().getString("sentinel.word-filter.auto-mute-reason", "Chat helytelen használata");

        this.spamFloodEnabled = plugin.getConfigManager().getConfig().getBoolean("sentinel.spam-flood.enabled", true);
        this.spamThreshold = plugin.getConfigManager().getConfig().getInt("sentinel.spam-flood.spam-threshold", 5);
        this.spamTimeframe = parseDurationToMillis(plugin.getConfigManager().getConfig().getString("sentinel.spam-flood.spam-timeframe", "5s"));
        this.floodThreshold = plugin.getConfigManager().getConfig().getInt("sentinel.spam-flood.flood-threshold", 3);
        this.floodTimeframe = parseDurationToMillis(plugin.getConfigManager().getConfig().getString("sentinel.spam-flood.flood-timeframe", "10s"));

        this.aiEnabled = plugin.getConfigManager().getConfig().getBoolean("sentinel.ai.enabled", true);
        this.aiName = plugin.getConfigManager().getConfig().getString("sentinel.ai.name", "Sentinel AI");
        this.aiMuteDuration = plugin.getConfigManager().getConfig().getString("sentinel.ai.ai-mute-duration", "20m");
        this.aiMuteReasonPrefix = plugin.getConfigManager().getConfig().getString("sentinel.ai.ai-mute-reason-prefix", "Chat helytelen használata - ");
        
        this.groqApiClient = new GroqApiClient(plugin);
    }

    private long parseDurationToMillis(String duration) {
        if (duration == null || duration.isEmpty()) return 0;
        try {
            if (duration.endsWith("s")) return Long.parseLong(duration.replace("s", "")) * 1000;
            if (duration.endsWith("m")) return Long.parseLong(duration.replace("m", "")) * 60 * 1000;
            return Long.parseLong(duration) * 1000;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean hasBypass(Player player) {
        return player.hasPermission(bypassPermission);
    }

    public boolean checkMessage(Player player, String message) {
        if (!enabled || hasBypass(player)) {
            return false;
        }

        if (checkWordFilter(player, message)) {
            return true;
        }

        if (checkSpamFlood(player, message)) {
            return true;
        }

        if (aiEnabled) {
            groqApiClient.analyzeChatMessage(message).thenAccept(response -> {
                if (response.isShouldMute()) {
                    plugin.getLogger().info("AI detected a violation for " + player.getName() + ". Reason: " + response.getReason());
                    applyAIMute(player, response.getReason());
                }
            });
        }

        return false;
    }

    private boolean checkWordFilter(Player player, String message) {
        String lowerCaseMessage = message.toLowerCase();
        for (String word : bannedWords) {
            if (lowerCaseMessage.contains(word.toLowerCase())) {
                handleViolation(player, "word-filter-blocked");
                return true;
            }
        }
        for (String phrase : bannedPhrases) {
            if (lowerCaseMessage.contains(phrase.toLowerCase())) {
                handleViolation(player, "word-filter-blocked");
                return true;
            }
        }
        return false;
    }

    private boolean checkSpamFlood(Player player, String message) {
        if (!spamFloodEnabled) return false;
        
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Spam check (frequency)
        List<Long> times = messageHistory.computeIfAbsent(uuid, k -> new ArrayList<>());
        times.add(now);
        times.removeIf(t -> now - t > spamTimeframe);
        if (times.size() > spamThreshold) {
            handleViolation(player, "spam-blocked");
            return true;
        }

        // Flood check (repetition)
        List<String> messages = lastMessages.computeIfAbsent(uuid, k -> new ArrayList<>());
        messages.add(message);
        if (messages.size() > floodThreshold) {
            messages.remove(0);
            long count = messages.stream().filter(m -> m.equalsIgnoreCase(message)).count();
            if (count >= floodThreshold) {
                handleViolation(player, "flood-blocked");
                return true;
            }
        }

        return false;
    }

    private void handleViolation(Player player, String messageKey) {
        String msg = plugin.getConfigManager().getConfig().getString("sentinel.messages." + messageKey);
        player.sendMessage(MessageUtil.parse(MessageUtil.replacePlaceholders(msg, null)));

        UUID uuid = player.getUniqueId();
        int count = violationCount.getOrDefault(uuid, 0) + 1;
        violationCount.put(uuid, count);

        if (count >= autoMuteThreshold) {
            applyAutoMute(player);
            violationCount.put(uuid, 0);
        }
    }

    private void applyAutoMute(Player player) {
        long duration = TimeUtil.parseTime(autoMuteDuration);
        Punishment punishment = new Punishment(
                -1,
                player.getUniqueId(),
                player.getName(),
                player.getAddress().getAddress().getHostAddress(),
                Punishment.PunishmentType.SENTINEL_AUTO_MUTE,
                autoMuteReason,
                "Sentinel",
                System.currentTimeMillis(),
                duration
        );

        plugin.getDataManager().savePunishment(punishment);
        
        String broadcastMsg = plugin.getConfigManager().getConfig().getString("sentinel.messages.auto-mute-broadcast");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", player.getName());
        placeholders.put("%duration%", autoMuteDuration);
        placeholders.put("%reason%", autoMuteReason);
        
        Component broadcast = MessageUtil.createComponent(broadcastMsg, punishment, placeholders);
        plugin.getServer().broadcast(broadcast);
    }

    private void applyAIMute(Player player, String reason) {
        String fullReason = aiMuteReasonPrefix + reason;
        long duration = TimeUtil.parseTime(aiMuteDuration);
        Punishment punishment = new Punishment(
                -1,
                player.getUniqueId(),
                player.getName(),
                player.getAddress().getAddress().getHostAddress(),
                Punishment.PunishmentType.SENTINEL_AI_MUTE,
                fullReason,
                aiName,
                System.currentTimeMillis(),
                duration
        );

        plugin.getDataManager().savePunishment(punishment);
        
        String broadcastMsg = plugin.getConfigManager().getConfig().getString("sentinel.messages.ai-mute-broadcast");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", player.getName());
        placeholders.put("%sentinel_ai_name%", aiName);
        placeholders.put("%reason%", reason);
        placeholders.put("%duration%", aiMuteDuration);
        
        Component broadcast = MessageUtil.createComponent(broadcastMsg, punishment, placeholders);
        plugin.getServer().broadcast(broadcast);

        String playerMsg = plugin.getConfigManager().getConfig().getString("sentinel.messages.ai-muted");
        player.sendMessage(MessageUtil.createComponent(playerMsg, punishment, placeholders));
    }
}
