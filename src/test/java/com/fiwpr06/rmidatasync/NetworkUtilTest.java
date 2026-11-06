package com.fiwpr06.rmidatasync;

import com.fiwpr06.rmidatasync.shared.util.NetworkUtil;
import org.junit.jupiter.api.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkUtilTest {
    @Test
    public void dumpNetworkInterfaces() throws Exception {
        System.out.println("=== DUMPING ALL NETWORK INTERFACES ===");
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            System.out.println("Interface: " + ni.getName() + " | Display: " + ni.getDisplayName() 
                    + " | Up: " + ni.isUp() + " | Loopback: " + ni.isLoopback() + " | Virtual: " + ni.isVirtual());
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (addr instanceof Inet4Address) {
                    System.out.println("   IPv4: " + addr.getHostAddress());
                }
            }
        }
        System.out.println("NetworkUtil.getLocalIpAddress() = " + NetworkUtil.getLocalIpAddress());
        System.out.println("======================================");
    }
}
