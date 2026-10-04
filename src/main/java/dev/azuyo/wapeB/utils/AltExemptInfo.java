package dev.azuyo.wapeB.utils;

import java.util.UUID;

public class AltExemptInfo {

    private final UUID playerUuid;
    private final String playerName;
    private final boolean exempt;
    private final String exemptBy;
    private final long exemptDate;

    public AltExemptInfo(UUID playerUuid, String playerName, boolean exempt, String exemptBy, long exemptDate) {
        this.playerUuid = playerUuid;
        this.playerName = playerName != null ? playerName : "Unknown";
        this.exempt = exempt;
        this.exemptBy = exemptBy != null ? exemptBy : "Console";
        this.exemptDate = exemptDate;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public UUID getUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getName() {
        return playerName;
    }

    public boolean isExempt() {
        return exempt;
    }

    public String getExemptBy() {
        return exemptBy;
    }

    public long getExemptDate() {
        return exemptDate;
    }
}
