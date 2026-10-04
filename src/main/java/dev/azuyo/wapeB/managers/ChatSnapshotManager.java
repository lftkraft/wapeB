package dev.azuyo.wapeB.managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.ChatMessage;
import dev.azuyo.wapeB.utils.ChatSnapshot;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

public class ChatSnapshotManager implements Listener {

    private final WapeB plugin;
    private final Deque<ChatMessage> rollingBuffer = new ConcurrentLinkedDeque<>();
    private final File snapshotsDir;
    private final File pendingDir;

    private static final int MAX_BUFFER_SIZE = 300;

    public ChatSnapshotManager(WapeB plugin) {
        this.plugin = plugin;
        this.snapshotsDir = new File(plugin.getDataFolder(), "snapshots");
        this.pendingDir = new File(plugin.getDataFolder(), "pending_snapshots");

        if (!snapshotsDir.exists()) {
            snapshotsDir.mkdirs();
        }
        if (!pendingDir.exists()) {
            pendingDir.mkdirs();
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        startRetentionCleanupTask();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        String currentServer = plugin.getConfigManager().getString("server-name", "Lobby");
        ChatMessage chatMessage = new ChatMessage(
                System.currentTimeMillis(),
                player.getUniqueId(),
                player.getName(),
                event.getMessage(),
                currentServer
        );

        rollingBuffer.addLast(chatMessage);
        while (rollingBuffer.size() > MAX_BUFFER_SIZE) {
            rollingBuffer.pollFirst();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Asynchronously flush pending velocity queue when player joins
        if (isVelocityStorageEnabled() && isVelocityQueueEnabled()) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::flushPendingVelocityQueue, 40L);
        }
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().getBoolean("chat-snapshot.enabled", true);
    }

    public int getHistoryLines() {
        return plugin.getConfigManager().getInt("chat-snapshot.history-lines", 30);
    }

    public int getRetentionDays() {
        return plugin.getConfigManager().getInt("chat-snapshot.retention-days", 30);
    }

    public boolean isLocalStorageEnabled() {
        return plugin.getConfigManager().getBoolean("chat-snapshot.local-storage.enabled", true);
    }

    public boolean isVelocityStorageEnabled() {
        return plugin.getConfigManager().getBoolean("chat-snapshot.velocity-storage.enabled", false);
    }

    public boolean isVelocityQueueEnabled() {
        return plugin.getConfigManager().getBoolean("chat-snapshot.velocity-storage.queue-enabled", true);
    }

    /**
     * Captures a snapshot for a punishment from the rolling in-memory chat buffer.
     */
    public ChatSnapshot captureSnapshot(Punishment punishment) {
        return captureSnapshot(punishment, getHistoryLines());
    }

    /**
     * Captures a snapshot for a punishment with a custom line count.
     */
    public ChatSnapshot captureSnapshot(Punishment punishment, int limit) {
        if (!isEnabled() || punishment == null) return null;

        List<ChatMessage> list = new ArrayList<>(rollingBuffer);
        if (list.isEmpty()) return null;

        int lines = limit > 0 ? limit : getHistoryLines();
        int startIndex = Math.max(0, list.size() - lines);
        List<ChatMessage> captured = new ArrayList<>(list.subList(startIndex, list.size()));

        String srv = punishment.getServer() != null ? punishment.getServer() : plugin.getConfigManager().getString("server-name", "Lobby");
        return new ChatSnapshot(
                punishment.getId(),
                punishment.getPlayerUuid(),
                punishment.getPlayerName(),
                srv,
                System.currentTimeMillis(),
                captured
        );
    }

    /**
     * Captures a snapshot directly for a player UUID.
     */
    public ChatSnapshot captureSnapshotForPlayer(UUID playerUuid, int limit) {
        if (!isEnabled()) return null;

        List<ChatMessage> list = new ArrayList<>(rollingBuffer);
        if (list.isEmpty()) return null;

        int lines = limit > 0 ? limit : getHistoryLines();
        int startIndex = Math.max(0, list.size() - lines);
        List<ChatMessage> captured = new ArrayList<>(list.subList(startIndex, list.size()));

        String playerName = "";
        Player p = playerUuid != null ? Bukkit.getPlayer(playerUuid) : null;
        if (p != null) playerName = p.getName();

        String srv = plugin.getConfigManager().getString("server-name", "Lobby");
        return new ChatSnapshot(0, playerUuid, playerName, srv, System.currentTimeMillis(), captured);
    }

    /**
     * Returns recent in-memory chat messages.
     */
    public List<ChatMessage> getRecentChat(UUID playerUuid, int limit) {
        List<ChatMessage> list = new ArrayList<>(rollingBuffer);
        if (list.isEmpty()) return Collections.emptyList();

        int max = limit > 0 ? limit : getHistoryLines();
        List<ChatMessage> filtered = new ArrayList<>();

        if (playerUuid != null) {
            for (ChatMessage msg : list) {
                if (playerUuid.equals(msg.getPlayerUuid())) {
                    filtered.add(msg);
                }
            }
        } else {
            filtered.addAll(list);
        }

        int start = Math.max(0, filtered.size() - max);
        return new ArrayList<>(filtered.subList(start, filtered.size()));
    }

    /**
     * Saves a snapshot according to local and velocity configuration.
     */
    public void saveSnapshotAsync(ChatSnapshot snapshot) {
        if (snapshot == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // 1. Local Storage
            if (isLocalStorageEnabled() && snapshot.getPunishmentId() > 0) {
                saveToLocalStorage(snapshot);
            }

            // 2. Velocity Storage
            if (isVelocityStorageEnabled() && snapshot.getPunishmentId() > 0) {
                sendOrQueueVelocitySnapshot(snapshot);
            }
        });
    }

    private void saveToLocalStorage(ChatSnapshot snapshot) {
        try {
            File file = new File(snapshotsDir, snapshot.getPunishmentId() + ".json");
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(snapshot.toJsonPretty());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save local chat snapshot #" + snapshot.getPunishmentId() + ": " + e.getMessage());
        }
    }

    private void sendOrQueueVelocitySnapshot(ChatSnapshot snapshot) {
        boolean hasOnlinePlayers = !Bukkit.getOnlinePlayers().isEmpty();

        if (hasOnlinePlayers) {
            sendVelocityPluginMessage(snapshot);
        } else {
            if (isVelocityQueueEnabled()) {
                saveToPendingQueue(snapshot);
            }
            // If queue-enabled is false and 0 players online, snapshot is discarded for Velocity.
        }
    }

    private void sendVelocityPluginMessage(ChatSnapshot snapshot) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("CHAT_SNAPSHOT");
            out.writeInt(snapshot.getPunishmentId());
            out.writeUTF(snapshot.getServer() != null ? snapshot.getServer() : plugin.getConfigManager().getString("server-name", "Lobby"));
            out.writeUTF(snapshot.toJsonPretty());

            byte[] bytes = out.toByteArray();

            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                Player carrier = Bukkit.getOnlinePlayers().iterator().next();
                carrier.sendPluginMessage(plugin, PluginMessageManager.CHANNEL, bytes);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send chat snapshot #" + snapshot.getPunishmentId() + " to Velocity: " + e.getMessage());
        }
    }

    private void saveToPendingQueue(ChatSnapshot snapshot) {
        try {
            File file = new File(pendingDir, snapshot.getPunishmentId() + ".json");
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(snapshot.toJsonPretty());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save pending chat snapshot #" + snapshot.getPunishmentId() + ": " + e.getMessage());
        }
    }

    /**
     * Flushes all pending snapshots to Velocity.
     */
    public synchronized void flushPendingVelocityQueue() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;

        File[] pendingFiles = pendingDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (pendingFiles == null || pendingFiles.length == 0) return;

        for (File file : pendingFiles) {
            try {
                ChatSnapshot snapshot;
                try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[1024];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, read);
                    }
                    snapshot = ChatSnapshot.fromJson(sb.toString());
                }

                if (snapshot != null) {
                    sendVelocityPluginMessage(snapshot);
                }
                file.delete();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to flush pending snapshot " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public ChatSnapshot getChatSnapshot(int punishmentId) {
        if (punishmentId <= 0) return null;
        File file = new File(snapshotsDir, punishmentId + ".json");
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            return ChatSnapshot.fromJson(sb.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read chat snapshot #" + punishmentId + ": " + e.getMessage());
            return null;
        }
    }

    public boolean hasChatSnapshot(int punishmentId) {
        if (punishmentId <= 0) return false;
        File file = new File(snapshotsDir, punishmentId + ".json");
        return file.exists();
    }

    public boolean deleteChatSnapshot(int punishmentId) {
        if (punishmentId <= 0) return false;
        File file = new File(snapshotsDir, punishmentId + ".json");
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    public long getRetentionMillis() {
        String retentionStr = plugin.getConfigManager().getString("chat-snapshot.retention", "");
        if (!retentionStr.isEmpty()) {
            return dev.azuyo.wapeB.utils.TimeUtil.parseTime(retentionStr);
        }
        int retentionDays = plugin.getConfigManager().getInt("chat-snapshot.retention-days", 30);
        return retentionDays > 0 ? TimeUnit.DAYS.toMillis(retentionDays) : -1;
    }

    private void startRetentionCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long maxAgeMillis = getRetentionMillis();
            if (maxAgeMillis <= 0) return;

            long now = System.currentTimeMillis();

            File[] files = snapshotsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    if (now - f.lastModified() > maxAgeMillis) {
                        f.delete();
                    }
                }
            }
        }, 18000L, 18000L); // Every 15 minutes asynchronously
    }
}
