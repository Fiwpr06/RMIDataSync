package com.fiwpr06.rmidatasync.shared.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

/**
 * Tien ich mang va cau hinh RMI Hostname cho he thong phan tan may that va may ao
 */
public final class NetworkUtil {
    private static final Logger LOGGER = Logger.getLogger(NetworkUtil.class.getName());

    private NetworkUtil() {}

    /**
     * Lay dia chi IPv4 kha dung tot nhat cua may tinh tren mang LAN vat ly (Wi-Fi, Ethernet).
     * Tu dong bo qua cac giao dien ao (WSL, Hyper-V, VirtualBox, VMware, Docker, Loopback, VPN).
     */
    public static String getLocalIpAddress() {
        List<String> validIps = getAllLocalIpv4Addresses();
        if (validIps.isEmpty()) {
            return "127.0.0.1";
        }
        return validIps.get(0);
    }

    /**
     * Tra ve danh sach tat ca cac dia chi IPv4 kha dung tren he thong,
     * sap xep uu tien tu cao xuong thap (LAN vat ly > cac mang khac > Loopback).
     */
    public static List<String> getAllLocalIpv4Addresses() {
        List<ScoredIp> scoredList = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;

                String name = iface.getName().toLowerCase();
                String display = iface.getDisplayName().toLowerCase();

                // Loc bo cac card ao / may ao / bo dem bo qua
                if (isVirtualOrIgnored(name) || isVirtualOrIgnored(display)) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String ip = addr.getHostAddress();
                        int score = calculateIpScore(ip, name, display);
                        scoredList.add(new ScoredIp(ip, score));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("[NET_UTIL] Loi khi quet card mang: " + e.getMessage());
        }

        // Sap xep theo diem uu tien giam dan
        scoredList.sort((a, b) -> Integer.compare(b.score, a.score));

        List<String> results = new ArrayList<>();
        for (ScoredIp sip : scoredList) {
            if (!results.contains(sip.ip)) {
                results.add(sip.ip);
            }
        }

        if (results.isEmpty()) {
            results.add("127.0.0.1");
        }

        return results;
    }

    private static boolean isVirtualOrIgnored(String str) {
        return str.contains("wsl")
                || str.contains("virtual")
                || str.contains("vmnet")
                || str.contains("vmware")
                || str.contains("virtualbox")
                || str.contains("vethernet")
                || str.contains("hyper-v")
                || str.contains("docker")
                || str.contains("pseudo")
                || str.contains("teredo")
                || str.contains("isatap")
                || str.contains("npcap")
                || str.contains("tailscale")
                || str.contains("zerotier")
                || str.contains("bluetooth")
                || str.contains("vpn");
    }

    private static int calculateIpScore(String ip, String name, String display) {
        int score = 0;

        // Uu tien adapter vat ly Wi-Fi hoac Ethernet
        boolean isPhysical = display.contains("wireless") || display.contains("wi-fi") || display.contains("802.11")
                || display.contains("ethernet") || display.contains("pci-e") || display.contains("realtek")
                || display.contains("intel") || display.contains("broadcom") || display.contains("killer");
        if (isPhysical) {
            score += 50;
        }

        // Uu tien dải IP mang cuc bo pho bien
        if (ip.startsWith("192.168.")) {
            score += 40;
        } else if (ip.startsWith("10.")) {
            score += 35;
        } else if (ip.startsWith("172.")) {
            // Private class B range 172.16 - 172.31
            String[] parts = ip.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        score += 30;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        return score;
    }

    private static class ScoredIp {
        final String ip;
        final int score;

        ScoredIp(String ip, int score) {
            this.ip = ip;
            this.score = score;
        }
    }

    /**
     * Thiet lap hostname cho Java RMI stub de dam bao client va peer co the ket noi qua LAN
     */
    public static void setupRmiHostname(String ip) {
        String targetIp;
        if (ip != null && !ip.trim().isEmpty() && !ip.equals("0.0.0.0")) {
            targetIp = ip.trim();
        } else {
            targetIp = getLocalIpAddress();
        }
        System.setProperty("java.rmi.server.hostname", targetIp);
        LOGGER.info("[RMI] Thiet lap java.rmi.server.hostname = " + targetIp);
    }
}
