package com.mycompany.dnsproject;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private DatagramSocket socket;
    private DatagramPacket packet;
    private DnsDatabase db;

    public ClientHandler(DatagramSocket socket, DatagramPacket packet, DnsDatabase db) {
        this.socket = socket;
        this.packet = packet;
        this.db = db;
    }

    public void run() {
        try {
            String request = new String(packet.getData(), 0, packet.getLength()).trim();
            if (request.isEmpty()) return;

            Logger.log("INFO", "Received request from " + packet.getSocketAddress() + ": " + request);

            String response;
            if (request.startsWith("A ")) {
                String domain = request.substring(2).trim();
                String ip = db.getIp(domain);
                response = ip != null ? "IP: " + ip : "NOT FOUND";
                Logger.log("INFO", "A query for " + domain + ": " + response);

            } else if (request.startsWith("REGISTER ")) {
                String domain = request.substring(9).trim();
                String ip = db.assignIp(domain);
                response = ip != null ? "IP: " + ip : "ERROR: No IPs available or domain exists";
                Logger.log("INFO", "REGISTER for " + domain + ": " + response);

            } else if (request.startsWith("PTR ")) {
                String ip = request.substring(4).trim();
                String domain = db.getDomain(ip);
                response = domain != null ? "DOMAIN: " + domain : "NOT FOUND";
                Logger.log("INFO", "PTR query for " + ip + ": " + response);

            } else if (request.startsWith("ZONE ")) {
                String mappings = db.getAllMappings();
                response = mappings != null ? "ZONE: " + mappings : "ERROR: No mappings";
                Logger.log("INFO", "ZONE request: " + response);

            } else if (request.startsWith("MX ")) {
                String domain = request.substring(3).trim();
                String mailServer = db.getMailServer(domain);
                response = mailServer != null ? "MAIL: " + mailServer : "NOT FOUND";
                Logger.log("INFO", "MX query for " + domain + ": " + response);

            } else if (request.startsWith("CACHE ")) {
                String cacheContents = db.getCacheContents();
                response = "CACHE: " + cacheContents;
                Logger.log("INFO", "CACHE request: " + response);

            } else {
                response = "INVALID REQUEST";
                Logger.log("ERROR", "Invalid request: " + request);
            }

            byte[] responseBytes = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length,
                    packet.getAddress(), packet.getPort());
            socket.send(responsePacket);

        } catch (IOException e) {
            Logger.log("ERROR", "Error handling client: " + e.getMessage());
        }
    }
}