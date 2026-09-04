package dev.azuyo.wapeB.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebhookUtil {

    private static final String AVATAR_API = "https://mc-heads.net/avatar/%uuid%/64";

    public static void sendPunishmentWebhook(Punishment punishment) {
        ConfigManager config = WapeB.getInstance().getConfigManager();
        if (!config.getBoolean("discord-webhook.enabled", false)) return;

        String webhookUrl = config.getString("discord-webhook.url", "");
        if (webhookUrl.isEmpty()) return;

        new Thread(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JsonObject json = new JsonObject();
                json.addProperty("username", config.getString("discord-webhook.username", "wapeB"));
                json.addProperty("avatar_url", config.getString("discord-webhook.avatar-url", ""));

                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();
                
                String typeKey;
                switch (punishment.getType()) {
                    case BAN:
                    case TEMPBAN:
                    case IPBAN:
                    case TEMPIPBAN:
                    case FREEZE_LOGOUT_BAN:
                        typeKey = "ban";
                        break;
                    case MUTE:
                    case TEMPMUTE:
                    case IPMUTE:
                    case TEMPIPMUTE:
                    case SENTINEL_AUTO_MUTE:
                    case SENTINEL_AI_MUTE:
                        typeKey = "mute";
                        break;
                    case KICK:
                        typeKey = "kick";
                        break;
                    case WARN:
                        typeKey = "warn";
                        break;
                    default:
                        typeKey = "default";
                }

                String title = config.getString("discord-webhook.embed." + typeKey + ".title", "New Punishment");
                String description = config.getString("discord-webhook.embed." + typeKey + ".description", "");
                
                String typeDisplayName = config.getString("punishment-types." + punishment.getType().name(), punishment.getType().name());

                // Replace placeholders in the description
                description = description
                        .replace("%type%", typeDisplayName)
                        .replace("%player%", punishment.getPlayerName() != null ? punishment.getPlayerName() : "N/A")
                        .replace("%executor%", punishment.getExecutorName() != null ? punishment.getExecutorName() : "N/A")
                        .replace("%reason%", punishment.getReason() != null ? punishment.getReason() : "N/A")
                        .replace("%duration%", TimeUtil.formatDuration(punishment.getDuration()))
                        .replace("%punishment_id%", String.valueOf(punishment.getId()));

                embed.addProperty("title", title);
                embed.addProperty("description", description);
                embed.addProperty("color", config.getInt("discord-webhook.embed.color", 16711897));
                
                // Avatar / Thumbnail
                if (config.getBoolean("discord-webhook.embed.show-avatar", true) && punishment.getPlayerUuid() != null) {
                    JsonObject thumbnail = new JsonObject();
                    thumbnail.addProperty("url", AVATAR_API.replace("%uuid%", punishment.getPlayerUuid().toString()));
                    embed.add("thumbnail", thumbnail);
                }
                
                embeds.add(embed);
                json.add("embeds", embeds);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                WapeB.getInstance().getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        }).start();
    }

    public static void sendRollbackWebhook(Punishment punishment) {
        ConfigManager config = WapeB.getInstance().getConfigManager();
        if (!config.getBoolean("discord-webhook.enabled", false)) return;

        String webhookUrl = config.getString("discord-webhook.url", "");
        if (webhookUrl.isEmpty()) return;

        new Thread(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JsonObject json = new JsonObject();
                json.addProperty("username", config.getString("discord-webhook.username", "wapeB"));
                json.addProperty("avatar_url", config.getString("discord-webhook.avatar-url", ""));

                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();
                
                String title = config.getString("discord-webhook.embed.rollback.title", "Punishment Rolled Back");
                String description = config.getString("discord-webhook.embed.rollback.description", "");
                
                String typeDisplayName = config.getString("punishment-types." + punishment.getType().name(), punishment.getType().name());

                description = description
                        .replace("%type%", typeDisplayName)
                        .replace("%player%", punishment.getPlayerName() != null ? punishment.getPlayerName() : "N/A")
                        .replace("%punishment_id%", String.valueOf(punishment.getId()));

                embed.addProperty("title", title);
                embed.addProperty("description", description);
                embed.addProperty("color", 65280); // Green

                if (config.getBoolean("discord-webhook.embed.show-avatar", true) && punishment.getPlayerUuid() != null) {
                    JsonObject thumbnail = new JsonObject();
                    thumbnail.addProperty("url", AVATAR_API.replace("%uuid%", punishment.getPlayerUuid().toString()));
                    embed.add("thumbnail", thumbnail);
                }
                
                embeds.add(embed);
                json.add("embeds", embeds);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                WapeB.getInstance().getLogger().warning("Failed to send Discord rollback webhook: " + e.getMessage());
            }
        }).start();
    }
}
