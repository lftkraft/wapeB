package dev.azuyo.wapeB.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPUtil {

    /**
     * Checks whether an IP address matches a target IP or CIDR subnet range (e.g. 192.168.1.0/24 or 192.168.1.*).
     */
    public static boolean isIpInCidr(String ip, String cidrOrIp) {
        if (ip == null || cidrOrIp == null) return false;
        
        String normalizedCidr = normalizeCidr(cidrOrIp);
        
        // Exact IP match
        if (ip.equalsIgnoreCase(normalizedCidr)) {
            return true;
        }

        // Subnet CIDR match
        if (normalizedCidr.contains("/")) {
            try {
                String[] parts = normalizedCidr.split("/");
                String subnetIp = parts[0];
                int prefixLength = Integer.parseInt(parts[1]);

                InetAddress targetAddr = InetAddress.getByName(ip);
                InetAddress subnetAddr = InetAddress.getByName(subnetIp);

                byte[] targetBytes = targetAddr.getAddress();
                byte[] subnetBytes = subnetAddr.getAddress();

                if (targetBytes.length != subnetBytes.length) {
                    return false; // IPv4 vs IPv6 mismatch
                }

                int bytesToCheck = prefixLength / 8;
                int bitsInLastByte = prefixLength % 8;

                for (int i = 0; i < bytesToCheck; i++) {
                    if (targetBytes[i] != subnetBytes[i]) {
                        return false;
                    }
                }

                if (bitsInLastByte > 0 && bytesToCheck < targetBytes.length) {
                    int mask = (0xFF00 >> bitsInLastByte) & 0xFF;
                    if ((targetBytes[bytesToCheck] & mask) != (subnetBytes[bytesToCheck] & mask)) {
                        return false;
                    }
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    /**
     * Normalizes wildcard formats like "192.168.1.*" or "10.0.*.*" into standard CIDR format "192.168.1.0/24".
     */
    public static String normalizeCidr(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.contains("*")) {
            String[] parts = trimmed.split("\\.");
            if (parts.length == 4) {
                int wildcardCount = 0;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    if (parts[i].equals("*")) {
                        wildcardCount++;
                        sb.append("0");
                    } else {
                        sb.append(parts[i]);
                    }
                    if (i < 3) sb.append(".");
                }
                int prefix = (4 - wildcardCount) * 8;
                return sb.toString() + "/" + prefix;
            }
        }
        return trimmed;
    }

    /**
     * Checks whether string is a valid IP or CIDR notation.
     */
    public static boolean isValidIpOrCidr(String input) {
        if (input == null || input.isEmpty()) return false;
        String normalized = normalizeCidr(input);
        if (normalized.contains("/")) {
            String[] parts = normalized.split("/");
            if (parts.length != 2) return false;
            try {
                int prefix = Integer.parseInt(parts[1]);
                if (prefix < 0 || prefix > 32) return false;
                InetAddress.getByName(parts[0]);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            try {
                InetAddress.getByName(normalized);
                return true;
            } catch (UnknownHostException e) {
                return false;
            }
        }
    }
}
