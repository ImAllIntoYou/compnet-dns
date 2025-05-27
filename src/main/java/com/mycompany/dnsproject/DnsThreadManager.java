package com.mycompany.dnsproject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DnsThreadManager {
    private DatagramSocket socket;
    private DnsDatabase db;
    private static final long TTL = 300000; // 5 minutes

    public DnsThreadManager(DatagramSocket socket, DnsDatabase db) {
        this.socket = socket;
        this.db = db;
        startExpiryThread();
    }

    public void run() {
        try {
            while (true) {
                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                Logger.log("INFO", "New client packet from: " + packet.getSocketAddress());

                ClientHandler handler = new ClientHandler(socket, packet, db);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            Logger.log("ERROR", "Thread manager error: " + e.getMessage());
        }
    }

    private void startExpiryThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Check every minute
                    long now = System.currentTimeMillis();
                    List<String> expired = new ArrayList<>();
                    for (Map.Entry<String, DnsDatabase.DnsEntry> entry : db.getMappings().entrySet()) {
                        if (now - entry.getValue().timestamp > TTL) {
                            expired.add(entry.getKey());
                        }
                    }
                    for (String domain : expired) {
                        DnsDatabase.DnsEntry entry = db.getMappings().remove(domain);
                        db.getIpPool().add(entry.ip);
                        Logger.log("INFO", "Expired mapping: " + domain + " -> " + entry.ip);
                    }
                    if (!expired.isEmpty()) {
                        rewriteFile();
                    }
                } catch (InterruptedException e) {
                    Logger.log("ERROR", "Expiry thread interrupted: " + e.getMessage());
                }
            }
        }).start();
    }

    private void rewriteFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("dns_mappings.txt"))) {
            for (DnsDatabase.DnsEntry entry : db.getMappings().values()) {
                String timestampStr = Logger.sdf.format(new Date(entry.timestamp));
                writer.write(entry.domain + "," + entry.ip + "," + timestampStr);
                writer.newLine();
            }
            Logger.log("INFO", "Rewrote dns_mappings.txt after expiry");
        } catch (IOException e) {
            Logger.log("ERROR", "Error rewriting mappings: " + e.getMessage());
        }
    }
}