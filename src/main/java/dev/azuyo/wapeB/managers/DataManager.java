package dev.azuyo.wapeB.managers;

import dev.azuyo.wapeB.utils.Punishment;

import java.util.List;
import java.util.UUID;

public interface DataManager {

    void savePunishment(Punishment punishment);

    Punishment getPunishment(int id);
    
    List<Punishment> getWarnings(UUID playerUuid);
    
    List<Punishment> getHistory(UUID playerUuid);

    List<Punishment> getAllActiveBans();

    List<Punishment> getStaffHistory(String executorName);

    List<String> getAltNamesByIp(String ipAddress, UUID excludeUuid);

    Punishment getActivePunishment(UUID playerUuid, String ipAddress, List<Punishment.PunishmentType> types);

    void removePunishment(int id);

    int getNextId();

    void deactivateAllBans();

    List<Punishment> getAllPunishments();

    // Web Password Methods
    void setWebPassword(UUID uuid, String hashedPassword);
    String getWebPassword(UUID uuid);
}