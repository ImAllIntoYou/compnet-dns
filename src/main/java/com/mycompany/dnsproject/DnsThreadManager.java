/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.dnsproject;

/**
 *
 * @author Admin
 */
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
                System.out.println("New client packet from: " + packet.getAddress());

                ClientHandler handler = new ClientHandler(socket, packet, db);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Thread manager error: " + e.getMessage());
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
                        System.out.println("Expired mapping: " + domain + " -> " + entry.ip);
                    }
                    if (!expired.isEmpty()) {
                        rewriteFile();
                    }
                } catch (InterruptedException e) {
                    System.err.println("Expiry thread interrupted: " + e.getMessage());
                }
            }
        }).start();
    }

    private void rewriteFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("dns_mappings.txt"))) {
            for (DnsDatabase.DnsEntry entry : db.getMappings().values()) {
                writer.write(entry.domain + "," + entry.ip);
                writer.newLine();
            }
            System.out.println("Rewrote dns_mappings.txt after expiry");
        } catch (IOException e) {
            System.err.println("Error rewriting mappings: " + e.getMessage());
        }
    }
}
