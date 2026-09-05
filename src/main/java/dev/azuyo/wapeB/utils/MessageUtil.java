package dev.azuyo.wapeB.utils;

import dev.azuyo.wapeB.WapeB;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private static final Pattern HOVER_TEXT_PATTERN = Pattern.compile("%hovertext (.*?)[ ]?%(.*?)%%");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Overloaded method for convenience, without custom placeholders.
     */
    public static Component createComponent(String text, Punishment punishment) {
        return createComponent(text, punishment, Collections.emptyMap());
    }

    /**
     * Creates a Component from a string, parsing placeholders, MiniMessage tags, and legacy color codes.
     * This is the main method to be used for sending messages.
     */
    public static Component createComponent(String text, Punishment punishment, Map<String, String> customPlaceholders) {
        if (text == null) return Component.empty();

        // First, replace all standard and custom placeholders.
        String replacedText = replacePlaceholders(text, punishment, customPlaceholders);

        // Handle the custom %hovertext ...% logic (legacy support)
        TextComponent.Builder builder = Component.text();
        Matcher matcher = HOVER_TEXT_PATTERN.matcher(replacedText);
        int lastEnd = 0;

        while (matcher.find()) {
            builder.append(parse(replacedText.substring(lastEnd, matcher.start())));

            String mainText = matcher.group(1);
            String hoverContent = matcher.group(2).replace("\\n", "\n");

            Component hoverComponent = createComponent(hoverContent, punishment, customPlaceholders);
            builder.append(parse(mainText).hoverEvent(HoverEvent.showText(hoverComponent)));
            
            lastEnd = matcher.end();
        }

        builder.append(parse(replacedText.substring(lastEnd)));
        return builder.build();
    }

    /**
     * Parses a string into a Component, supporting both MiniMessage and Legacy color codes (&).
     * Now with safety try-catch for MiniMessage errors.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        try {
            // If it looks like MiniMessage (contains < and >)
            if (text.contains("<") && text.contains(">")) {
                return MINI_MESSAGE.deserialize(text);
            }
        } catch (Exception e) {
            WapeB.getInstance().getLogger().warning("MiniMessage parse error for text: " + text + " | Error: " + e.getMessage());
            // Fallback to plain text if MiniMessage fails
            return Component.text(text);
        }

        // Fallback to legacy ampersand (&)
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /**
     * Overloaded method for convenience, without custom placeholders.
     */
    public static String replacePlaceholders(String text, Punishment punishment) {
        return replacePlaceholders(text, punishment, Collections.emptyMap());
    }

    public static String replacePlaceholders(String text, Punishment punishment, Map<String, String> customPlaceholders) {
        if (text == null) return "";

        String result = text.replace("%prefix%", WapeB.getInstance().getConfigManager().getString("prefix", ""));

        if (punishment != null && punishment.getType() != null) {
            String typeName = WapeB.getInstance().getConfigManager().getString("punishment-types." + punishment.getType().name(), punishment.getType().name());

            long remainingMillis;
            if (punishment.getDuration() == -1 || punishment.getEnd() == -1) {
                remainingMillis = -1;
            } else {
                remainingMillis = Math.max(0L, punishment.getEnd() - System.currentTimeMillis());
            }

            String remainingStr = TimeUtil.formatDuration(remainingMillis);
            String detailedRemainingStr = TimeUtil.formatDetailedDuration(remainingMillis);
            String originalStr = TimeUtil.formatDuration(punishment.getDuration());
            String detailedOriginalStr = TimeUtil.formatDetailedDuration(punishment.getDuration());

            result = result
                    .replace("%duration%", remainingStr)
                    .replace("%remaining%", remainingStr)
                    .replace("%remaining_duration%", remainingStr)
                    .replace("%time_left%", remainingStr)
                    .replace("%expires_in%", remainingStr)
                    .replace("%detailed_duration%", detailedRemainingStr)
                    .replace("%detailed_remaining%", detailedRemainingStr)
                    .replace("%original_duration%", originalStr)
                    .replace("%total_duration%", originalStr)
                    .replace("%detailed_original_duration%", detailedOriginalStr)
                    .replace("%player%", punishment.getPlayerName() != null ? punishment.getPlayerName() : "N/A")
                    .replace("%executor%", punishment.getExecutorName() != null ? punishment.getExecutorName() : "N/A")
                    .replace("%reason%", punishment.getReason() != null ? punishment.getReason() : "N/A")
                    .replace("%type%", typeName)
                    .replace("%punishment_id%", String.valueOf(punishment.getId()))
                    .replace("%date%", DATE_FORMAT.format(new Date(punishment.getDate())))
                    .replace("%end_date%", (punishment.getEnd() == -1) ? "Permanent" : DATE_FORMAT.format(new Date(punishment.getEnd())));
        }

        if (customPlaceholders != null) {
            for (Map.Entry<String, String> entry : customPlaceholders.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result = result.replace(entry.getKey(), entry.getValue());
                }
            }
        }

        if (punishment == null) {
            // Replace remaining standard placeholders with empty string if no punishment object
            String[] placeholders = {
                    "%player%", "%executor%", "%reason%", "%type%", "%punishment_id%",
                    "%duration%", "%remaining%", "%remaining_duration%", "%time_left%", "%expires_in%",
                    "%detailed_duration%", "%detailed_remaining%",
                    "%original_duration%", "%total_duration%", "%detailed_original_duration%",
                    "%date%", "%end_date%"
            };
            for (String p : placeholders) {
                if (result.contains(p)) result = result.replace(p, "");
            }
        }

        return result;
    }
    
    public static Component formatKickScreen(List<String> lines, Punishment punishment) {
        TextComponent.Builder messageBuilder = Component.text();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String replacedLine = replacePlaceholders(line, punishment);
            if (replacedLine.contains("%mid%")) replacedLine = replacedLine.replace("%mid%", "    "); 
            messageBuilder.append(parse(replacedLine));
            if (i < lines.size() - 1) messageBuilder.append(Component.newline());
        }
        return messageBuilder.build().decoration(TextDecoration.ITALIC, false);
    }
}