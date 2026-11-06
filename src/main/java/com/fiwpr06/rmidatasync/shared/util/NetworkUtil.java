package com.fiwpr06.rmidatasync.shared.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 * Tien ich mang va cau hinh RMI Hostname cho he thong phan tan may that va may ao
 */
public final class NetworkUtil {
    private static final Logger LOGGER = Logger.getLogger(NetworkUtil.class.getName());

    private NetworkUtil() {}

    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;

                String name = iface.getName().toLowerCase();
                String display = iface.getDisplayName().toLowerCase();
                if (name.contains("wsl") || name.contains("virtual") || name.contains("vmnet") || name.contains("vethernet")
                        || display.contains("wsl") || display.contains("virtual") || display.contains("hyper-v") || display.contains("pseudo")) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            return "127.0.0.1";
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * Thiet lap hostname cho Java RMI stub de dam bao client va peer co the ket noi
     */
    public static void setupRmiHostname(String ip) {
        String targetIp;
        if (ip != null && !ip.trim().isEmpty() && !ip.equals("0.0.0.0")) {
            targetIp = ip.trim();
        } else {
            targetIp = "127.0.0.1";
        }
        System.setProperty("java.rmi.server.hostname", targetIp);
        LOGGER.info("[RMI] Set java.rmi.server.hostname = " + targetIp);
    }
}
