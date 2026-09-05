package dev.azuyo.wapeB.utils;

public class IpHistoryRecord {
    private final String ipAddress;
    private final long timestamp;

    public IpHistoryRecord(String ipAddress, long timestamp) {
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    public String getIpAddress() { return ipAddress; }
    public long getTimestamp() { return timestamp; }
}
