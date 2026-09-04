package dev.azuyo.wapeB.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static String displayPermanent = "Permanent";
    private static String displayYear = "y";
    private static String displayWeek = "w";
    private static String displayDay = "d";
    private static String displayHour = "h";
    private static String displayMinute = "m";
    private static String displaySecond = "s";

    public static void init(String perm, String y, String w, String d, String h, String m, String s) {
        if (perm != null) displayPermanent = perm;
        if (y != null) displayYear = y;
        if (w != null) displayWeek = w;
        if (d != null) displayDay = d;
        if (h != null) displayHour = h;
        if (m != null) displayMinute = m;
        if (s != null) displaySecond = s;
    }

    public static long parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty() || timeString.equalsIgnoreCase("permanent") || timeString.equals("-1")) {
            return -1; // Represents permanent
        }

        // Always parse using standard units: s, m, h, d, w, y
        Pattern pattern = Pattern.compile("(\\d+)([smhdwy])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(timeString);

        long totalMillis = 0;

        if (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            char unit = matcher.group(2).toLowerCase().charAt(0);

            switch (unit) {
                case 's':
                    totalMillis = value * 1000L;
                    break;
                case 'm':
                    totalMillis = value * 60 * 1000L;
                    break;
                case 'h':
                    totalMillis = value * 60 * 60 * 1000L;
                    break;
                case 'd':
                    totalMillis = value * 24 * 60 * 60 * 1000L;
                    break;
                case 'w':
                    totalMillis = value * 7 * 24 * 60 * 60 * 1000L;
                    break;
                case 'y':
                    totalMillis = value * 365 * 24 * 60 * 60 * 1000L;
                    break;
                default:
                    return -1;
            }
        } else {
            return -1;
        }
        return totalMillis;
    }

    public static String formatDuration(long millis) {
        if (millis == -1) {
            return displayPermanent;
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long years = days / 365;

        if (years > 0) return years + displayYear;
        if (weeks > 0) return weeks + displayWeek;
        if (days > 0) return days + displayDay;
        if (hours > 0) return hours + displayHour;
        if (minutes > 0) return minutes + displayMinute;
        return seconds + displaySecond;
    }
}
