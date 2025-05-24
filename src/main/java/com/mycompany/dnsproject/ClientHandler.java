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
            // Extract query from packet
            String request = new String(packet.getData(), 0, packet.getLength()).trim();
            if (request.isEmpty()) return;

            System.out.println("Received request: " + request);

            String response;
            if (request.startsWith("A ")) {
                String domain = request.substring(2).trim();
                String ip = db.getIp(domain);
                response = ip != null ? "IP: " + ip : "NOT FOUND";
                System.out.println("A query for " + domain + ": " + response);

            } else if (request.startsWith("REGISTER ")) {
                String domain = request.substring(9).trim();
                String ip = db.assignIp(domain);
                response = ip != null ? "IP: " + ip : "ERROR: No IPs available or domain exists";
                System.out.println("REGISTER for " + domain + ": " + response);

            } else if (request.startsWith("PTR ")) {
                String ip = request.substring(4).trim();
                String domain = db.getDomain(ip);
                response = domain != null ? "DOMAIN: " + domain : "NOT FOUND";
                System.out.println("PTR query for " + ip + ": " + response);

            } else if (request.startsWith("ZONE ")) {
                String mappings = db.getAllMappings();
                response = mappings != null ? "ZONE: " + mappings : "ERROR: No mappings";
                System.out.println("ZONE request: " + response);
            
            } else if (request.startsWith("MX ")) {
                String domain = request.substring(3).trim();
                String mailServer = db.getMailServer(domain);
                response = mailServer != null ? "MAIL: " + mailServer : "NOT FOUND";
                System.out.println("MX query for " + domain + ": " + response);

            } else {
                response = "INVALID REQUEST";
                System.out.println("Invalid request: " + request);
            }

            // Send response
            byte[] responseBytes = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length,
                    packet.getAddress(), packet.getPort());
            socket.send(responsePacket);

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }
}