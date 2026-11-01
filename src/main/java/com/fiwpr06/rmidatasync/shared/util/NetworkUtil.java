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
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * Thiet lap hostname cho Java RMI stub de cac may ao (VMware/VirtualBox) va may that khac co the goi nguoc lai
     */
    public static void setupRmiHostname(String ip) {
        String targetIp = (ip != null && !ip.isEmpty() && !ip.equals("127.0.0.1") && !ip.equals("0.0.0.0")) ? ip : getLocalIpAddress();
        System.setProperty("java.rmi.server.hostname", targetIp);
        LOGGER.info("[RMI] Set java.rmi.server.hostname = " + targetIp);
    }
}
