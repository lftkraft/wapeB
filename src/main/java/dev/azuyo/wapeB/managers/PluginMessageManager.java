package dev.azuyo.wapeB.managers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PluginMessageManager implements PluginMessageListener {

    public static final String CHANNEL = "wapeb:main";
    private static final Set<Integer> PROCESSED_IDS = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, Long> PROCESSED_KEYS = new ConcurrentHashMap<>();

    private final WapeB plugin;

    public PluginMessageManager(WapeB plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public static boolean isProcessed(int id, String key) {
        if (id > 0 && PROCESSED_IDS.contains(id)) {
            return true;
        }
        if (key != null) {
            Long time = PROCESSED_KEYS.get(key);
            if (time != null && System.currentTimeMillis() - time < 10000) {
                return true;
            }
        }
        return false;
    }

    public static void markProcessed(int id, String key) {
        if (id > 0) {
            PROCESSED_IDS.add(id);
            if (PROCESSED_IDS.size() > 5000) {
                PROCESSED_IDS.clear();
            }
        }
        if (key != null) {
            PROCESSED_KEYS.put(key, System.currentTimeMillis());
            if (PROCESSED_KEYS.size() > 5000) {
                PROCESSED_KEYS.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue() > 10000);
            }
        }
    }

    public void sendPunishmentBroadcast(String action, Punishment punishment, boolean silent, String broadcastMessage) {
        if (punishment == null) return;

        String uniqueKey = (punishment.getPlayerName() != null ? punishment.getPlayerName() : "") + ":" + action + ":" + punishment.getReason() + ":" + punishment.getExecutorName();
        markProcessed(punishment.getId(), uniqueKey);

        String formattedBroadcast = MessageUtil.replacePlaceholders(broadcastMessage, punishment);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PUNISH_BROADCAST");
        out.writeInt(punishment.getId());
        out.writeUTF(action != null ? action : punishment.getType().name());
        out.writeUTF(punishment.getPlayerUuid() != null ? punishment.getPlayerUuid().toString() : "");
        out.writeUTF(punishment.getPlayerName() != null ? punishment.getPlayerName() : "");
        out.writeUTF(punishment.getIpAddress() != null ? punishment.getIpAddress() : "");
        out.writeUTF(punishment.getReason() != null ? punishment.getReason() : "");
        out.writeUTF(punishment.getExecutorName() != null ? punishment.getExecutorName() : "");
        out.writeUTF(punishment.getActiveServer() != null ? punishment.getActiveServer() : "global");
        out.writeLong(punishment.getDuration());
        out.writeBoolean(silent);
        out.writeUTF(punishment.getServer() != null ? punishment.getServer() : plugin.getConfigManager().getString("server-name", "Lobby"));
        out.writeUTF(formattedBroadcast != null ? formattedBroadcast : "");

        byte[] bytes = out.toByteArray();

        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            Player p = Bukkit.getOnlinePlayers().iterator().next();
            p.sendPluginMessage(plugin, CHANNEL, bytes);
        } else {
            try {
                plugin.getServer().sendPluginMessage(plugin, CHANNEL, bytes);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equalsIgnoreCase(channel)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if ("PUNISH_BROADCAST".equalsIgnoreCase(subChannel)) {
            try {
                int punishmentId = in.readInt();
                String action = in.readUTF();
                String targetUuidStr = in.readUTF();
                String targetName = in.readUTF();
                String targetIp = in.readUTF();
                String reason = in.readUTF();
                String executor = in.readUTF();
                String serverScope = in.readUTF();
                long duration = in.readLong();
                boolean silent = in.readBoolean();
                String sourceServerName = in.readUTF();
                String broadcastMessage = in.readUTF();

                String currentServer = plugin.getConfigManager().getString("server-name", "Lobby");

                // If sent from this server, ignore to avoid duplicate broadcast
                if (sourceServerName != null && sourceServerName.equalsIgnoreCase(currentServer)) {
                    return;
                }

                String uniqueKey = targetName + ":" + action + ":" + reason + ":" + executor;
                if (isProcessed(punishmentId, uniqueKey)) {
                    return;
                }
                markProcessed(punishmentId, uniqueKey);

                UUID targetUuid = null;
                if (!targetUuidStr.isEmpty()) {
                    try {
                        targetUuid = UUID.fromString(targetUuidStr);
                    } catch (IllegalArgumentException ignored) {}
                }

                Punishment.PunishmentType pType;
                try {
                    pType = Punishment.PunishmentType.valueOf(action.toUpperCase());
                } catch (Exception e) {
                    pType = Punishment.PunishmentType.BAN;
                }

                Punishment dummy = new Punishment(punishmentId, targetUuid, targetName, targetIp, pType, reason, executor, serverScope, sourceServerName, System.currentTimeMillis(), duration);

                boolean appliesToThisServer = dummy.isAppliesTo(currentServer);
                boolean showRemoteBroadcast = plugin.getConfigManager().getBoolean("broadcast.show-remote-punishments", true);

                // 1. Kick online player if ban/kick applies to this server
                if (appliesToThisServer && ("BAN".equalsIgnoreCase(action) || "TEMPBAN".equalsIgnoreCase(action) || "IPBAN".equalsIgnoreCase(action) || "TEMPIPBAN".equalsIgnoreCase(action) || "KICK".equalsIgnoreCase(action))) {
                    Player online = targetUuid != null ? Bukkit.getPlayer(targetUuid) : Bukkit.getPlayer(targetName);
                    if (online != null && online.isOnline()) {
                        String kickKey = "KICK".equalsIgnoreCase(action) ? "messages.kick.kick-screen" : "messages.ban.kick-screen";
                        online.kick(MessageUtil.formatKickScreen(plugin.getConfigManager().getStringList(kickKey), dummy));
                    }
                }

                // 2. Broadcast to players and console on this server
                if (appliesToThisServer || showRemoteBroadcast) {
                    if (broadcastMessage != null && !broadcastMessage.isEmpty()) {
                        Component comp = MessageUtil.createComponent(broadcastMessage, dummy);
                        if (silent) {
                            String silentPrefix = plugin.getConfigManager().getString("messages." + action.toLowerCase() + ".silent.prefix", "&7(Silent) ");
                            Bukkit.broadcast(MessageUtil.createComponent(silentPrefix + broadcastMessage, dummy), "wapeb.notify");
                        } else {
                            Bukkit.broadcast(comp);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to process incoming punishment broadcast: " + e.getMessage());
            }
        }
    }
}
