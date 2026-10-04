package dev.azuyo.wapeB.utils;

import com.google.gson.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatSnapshot {

    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final int punishmentId;
    private final UUID targetUuid;
    private final String targetName;
    private final String server;
    private final long createdAt;
    private final List<ChatMessage> messages;

    public ChatSnapshot(int punishmentId, UUID targetUuid, String targetName, String server, long createdAt, List<ChatMessage> messages) {
        this.punishmentId = punishmentId;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.server = (server != null && !server.trim().isEmpty()) ? server : "global";
        this.createdAt = createdAt;
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }

    public int getPunishmentId() {
        return punishmentId;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getServer() {
        return server;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getFormattedCreatedAt() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date(createdAt));
        }
    }

    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public List<String> getChatLogLines() {
        List<String> lines = new ArrayList<>();
        for (ChatMessage msg : messages) {
            lines.add(msg.toLogLine());
        }
        return lines;
    }

    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addProperty("punishment_id", punishmentId);
        obj.addProperty("target_name", targetName != null ? targetName : "");
        obj.addProperty("target_uuid", targetUuid != null ? targetUuid.toString() : "");
        obj.addProperty("server", server != null ? server : "global");
        obj.addProperty("created_date", getFormattedCreatedAt());
        obj.addProperty("created_at", createdAt);
        obj.addProperty("message_count", messages.size());

        JsonArray logLines = new JsonArray();
        for (ChatMessage msg : messages) {
            logLines.add(msg.toLogLine());
        }
        obj.add("chat_log", logLines);

        JsonArray msgArr = new JsonArray();
        for (ChatMessage msg : messages) {
            msgArr.add(msg.toJson());
        }
        obj.add("messages", msgArr);
        return obj;
    }

    public String toJson() {
        return toJsonObject().toString();
    }

    public String toJsonPretty() {
        return GSON_PRETTY.toJson(toJsonObject());
    }

    public static ChatSnapshot fromJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return null;
        try {
            JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
            return fromJsonObject(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public static ChatSnapshot fromJsonObject(JsonObject obj) {
        if (obj == null) return null;
        int pId = obj.has("punishment_id") ? obj.get("punishment_id").getAsInt() : 0;
        UUID uuid = null;
        if (obj.has("target_uuid") && !obj.get("target_uuid").getAsString().isEmpty()) {
            try {
                uuid = UUID.fromString(obj.get("target_uuid").getAsString());
            } catch (IllegalArgumentException ignored) {}
        }
        String name = obj.has("target_name") ? obj.get("target_name").getAsString() : "";
        String srv = obj.has("server") ? obj.get("server").getAsString() : "global";
        long created = obj.has("created_at") ? obj.get("created_at").getAsLong() : System.currentTimeMillis();

        List<ChatMessage> msgs = new ArrayList<>();
        if (obj.has("messages") && obj.get("messages").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("messages")) {
                if (el.isJsonObject()) {
                    ChatMessage cm = ChatMessage.fromJson(el.getAsJsonObject());
                    if (cm != null) msgs.add(cm);
                }
            }
        }
        return new ChatSnapshot(pId, uuid, name, srv, created, msgs);
    }
}
