package dev.azuyo.wapeB.utils;

import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ChatMessage {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final long timestamp;
    private final UUID playerUuid;
    private final String playerName;
    private final String message;
    private final String server;

    public ChatMessage(long timestamp, UUID playerUuid, String playerName, String message, String server) {
        this.timestamp = timestamp;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.message = message;
        this.server = (server != null && !server.trim().isEmpty()) ? server : "global";
    }

    public long getTimestamp() {
        return timestamp;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }

    public String getServer() {
        return server;
    }

    public String getFormattedDate() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date(timestamp));
        }
    }

    public String getFormattedTime() {
        synchronized (TIME_FORMAT) {
            return TIME_FORMAT.format(new Date(timestamp));
        }
    }

    public String toLogLine() {
        return "[" + getFormattedTime() + "] [" + getServer() + "] " + (playerName != null ? playerName : "Unknown") + ": " + (message != null ? message : "");
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("time", getFormattedTime());
        obj.addProperty("date", getFormattedDate());
        obj.addProperty("timestamp", timestamp);
        obj.addProperty("player", playerName != null ? playerName : "");
        obj.addProperty("uuid", playerUuid != null ? playerUuid.toString() : "");
        obj.addProperty("server", server != null ? server : "global");
        obj.addProperty("message", message != null ? message : "");
        obj.addProperty("playerName", playerName != null ? playerName : "");
        obj.addProperty("playerUuid", playerUuid != null ? playerUuid.toString() : "");
        return obj;
    }

    public static ChatMessage fromJson(JsonObject obj) {
        if (obj == null) return null;
        long timestamp = obj.has("timestamp") ? obj.get("timestamp").getAsLong() : System.currentTimeMillis();
        UUID uuid = null;
        String uuidStr = obj.has("uuid") ? obj.get("uuid").getAsString() : (obj.has("playerUuid") ? obj.get("playerUuid").getAsString() : "");
        if (uuidStr != null && !uuidStr.isEmpty()) {
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {}
        }
        String name = obj.has("player") ? obj.get("player").getAsString() : (obj.has("playerName") ? obj.get("playerName").getAsString() : "");
        String msg = obj.has("message") ? obj.get("message").getAsString() : "";
        String srv = obj.has("server") ? obj.get("server").getAsString() : "global";
        return new ChatMessage(timestamp, uuid, name, msg, srv);
    }
}
