package dev.azuyo.wapeB.importers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class VanillaImporter implements PunishmentImporter {

    private final WapeB plugin;
    private static final SimpleDateFormat[] DATE_FORMATS = new SimpleDateFormat[]{
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    };

    public VanillaImporter(WapeB plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "vanilla";
    }

    @Override
    public String getDescription() {
        return "Imports banned players and IPs from Minecraft native banned-players.json and banned-ips.json files.";
    }

    @Override
    public CompletableFuture<ImportResult> executeImport(Map<String, Object> options) {
        CompletableFuture<ImportResult> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long startTime = System.currentTimeMillis();
            ImportResult result = new ImportResult(getName(), true);

            File playerBans = new File("banned-players.json");
            File ipBans = new File("banned-ips.json");

            if (options != null) {
                if (options.containsKey("playerBansFile")) {
                    playerBans = new File((String) options.get("playerBansFile"));
                }
                if (options.containsKey("ipBansFile")) {
                    ipBans = new File((String) options.get("ipBansFile"));
                }
            }

            importPlayerBans(playerBans, result);
            importIpBans(ipBans, result);

            result.setDurationMillis(System.currentTimeMillis() - startTime);
            result.addDetail("Vanilla ban import finished: " + result.getImportedCount() + " imported, " + result.getFailedCount() + " failed.");
            future.complete(result);
        });

        return future;
    }

    private void importPlayerBans(File file, ImportResult result) {
        if (!file.exists()) {
            result.addDetail("File " + file.getName() + " not found, skipping player bans.");
            return;
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(reader);
            if (!el.isJsonArray()) return;

            JsonArray array = el.getAsJsonArray();
            for (JsonElement item : array) {
                try {
                    JsonObject obj = item.getAsJsonObject();
                    String uuidStr = obj.has("uuid") ? obj.get("uuid").getAsString() : null;
                    String name = obj.has("name") ? obj.get("name").getAsString() : "Unknown";
                    String reason = obj.has("reason") ? obj.get("reason").getAsString() : "Banned by an operator.";
                    String source = obj.has("source") ? obj.get("source").getAsString() : "Server";
                    String createdStr = obj.has("created") ? obj.get("created").getAsString() : null;
                    String expiresStr = obj.has("expires") ? obj.get("expires").getAsString() : null;

                    long time = parseDate(createdStr);
                    long expires = parseDate(expiresStr);

                    long duration = -1;
                    if (expires > 0 && expires > time) {
                        duration = expires - time;
                    }

                    boolean active = (expires <= 0) || (expires > System.currentTimeMillis());
                    Punishment.PunishmentType type = (duration > 0) ? Punishment.PunishmentType.TEMPBAN : Punishment.PunishmentType.BAN;

                    UUID uuid = null;
                    if (uuidStr != null) {
                        try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException ignored) {}
                    }
                    if (uuid == null) {
                        uuid = UUID.nameUUIDFromBytes(name.getBytes());
                    }

                    String srv = plugin.getConfigManager().getString("server-name", "Lobby");

                    Punishment p = new Punishment(
                            -1,
                            uuid,
                            name,
                            "N/A",
                            type,
                            reason,
                            source,
                            time,
                            duration,
                            active,
                            "global",
                            srv,
                            null
                    );

                    plugin.getDataManager().savePunishment(p);
                    result.incrementImported();
                } catch (Exception ex) {
                    result.incrementFailed();
                    result.addError("Error importing player ban: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            result.addError("Failed to parse " + file.getName() + ": " + e.getMessage());
        }
    }

    private void importIpBans(File file, ImportResult result) {
        if (!file.exists()) {
            result.addDetail("File " + file.getName() + " not found, skipping IP bans.");
            return;
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(reader);
            if (!el.isJsonArray()) return;

            JsonArray array = el.getAsJsonArray();
            for (JsonElement item : array) {
                try {
                    JsonObject obj = item.getAsJsonObject();
                    String ip = obj.has("ip") ? obj.get("ip").getAsString() : null;
                    if (ip == null || ip.isEmpty()) continue;

                    String reason = obj.has("reason") ? obj.get("reason").getAsString() : "Banned by an operator.";
                    String source = obj.has("source") ? obj.get("source").getAsString() : "Server";
                    String createdStr = obj.has("created") ? obj.get("created").getAsString() : null;
                    String expiresStr = obj.has("expires") ? obj.get("expires").getAsString() : null;

                    long time = parseDate(createdStr);
                    long expires = parseDate(expiresStr);

                    long duration = -1;
                    if (expires > 0 && expires > time) {
                        duration = expires - time;
                    }

                    boolean active = (expires <= 0) || (expires > System.currentTimeMillis());
                    Punishment.PunishmentType type = (duration > 0) ? Punishment.PunishmentType.TEMPIPMUTE : Punishment.PunishmentType.IPBAN;
                    UUID uuid = UUID.nameUUIDFromBytes(ip.getBytes());
                    String srv = plugin.getConfigManager().getString("server-name", "Lobby");

                    Punishment p = new Punishment(
                            -1,
                            uuid,
                            ip,
                            ip,
                            type,
                            reason,
                            source,
                            time,
                            duration,
                            active,
                            "global",
                            srv,
                            null
                    );

                    plugin.getDataManager().savePunishment(p);
                    result.incrementImported();
                } catch (Exception ex) {
                    result.incrementFailed();
                    result.addError("Error importing IP ban: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            result.addError("Failed to parse " + file.getName() + ": " + e.getMessage());
        }
    }

    private long parseDate(String str) {
        if (str == null || str.trim().isEmpty() || str.equalsIgnoreCase("forever")) return -1;
        for (SimpleDateFormat sdf : DATE_FORMATS) {
            try {
                Date d = sdf.parse(str);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        return System.currentTimeMillis();
    }
}
