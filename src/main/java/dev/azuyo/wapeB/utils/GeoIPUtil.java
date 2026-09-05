package dev.azuyo.wapeB.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GeoIPUtil {

    public static class GeoInfo {
        private final String country;
        private final String countryCode;
        private final String city;
        private final String isp;

        public GeoInfo(String country, String countryCode, String city, String isp) {
            this.country = country != null ? country : "Ismeretlen";
            this.countryCode = countryCode != null ? countryCode : "??";
            this.city = city != null ? city : "Ismeretlen";
            this.isp = isp != null ? isp : "Ismeretlen";
        }

        public String getCountry() { return country; }
        public String getCountryCode() { return countryCode; }
        public String getCity() { return city; }
        public String getIsp() { return isp; }

        public String getFormatted() {
            return country + " (" + countryCode + ") | Város: " + city + " | ISP: " + isp;
        }
    }

    private static final Map<String, GeoInfo> CACHE = new ConcurrentHashMap<>();

    public static CompletableFuture<GeoInfo> getGeoInfoAsync(String ipAddress) {
        return CompletableFuture.supplyAsync(() -> getGeoInfo(ipAddress));
    }

    public static GeoInfo getGeoInfo(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || ipAddress.equals("127.0.0.1") || ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.")) {
            return new GeoInfo("Helyi hálózat (Local)", "LOCAL", "Localhost", "Internal IP");
        }

        if (CACHE.containsKey(ipAddress)) {
            return CACHE.get(ipAddress);
        }

        try {
            URL url = new URL("http://ip-api.com/json/" + ipAddress + "?fields=status,country,countryCode,city,isp");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("status") && json.get("status").getAsString().equals("success")) {
                        String country = json.has("country") ? json.get("country").getAsString() : "Ismeretlen";
                        String countryCode = json.has("countryCode") ? json.get("countryCode").getAsString() : "??";
                        String city = json.has("city") ? json.get("city").getAsString() : "Ismeretlen";
                        String isp = json.has("isp") ? json.get("isp").getAsString() : "Ismeretlen";

                        GeoInfo info = new GeoInfo(country, countryCode, city, isp);
                        CACHE.put(ipAddress, info);
                        return info;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        GeoInfo fallback = new GeoInfo("Ismeretlen", "??", "Ismeretlen", "Ismeretlen");
        CACHE.put(ipAddress, fallback);
        return fallback;
    }
}
