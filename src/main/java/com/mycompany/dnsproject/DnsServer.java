package com.mycompany.dnsproject;


import java.net.*;

public class DnsServer {
    public static final int PORT = 53;

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            System.out.println("DNS Server started on port " + PORT);

            DnsDatabase database = new DnsDatabase();
            DnsThreadManager threadManager = new DnsThreadManager(serverSocket, database);
            System.out.println("Initial mappings: " + database.getAllMappings());

            threadManager.run(); // Delegate to thread manager

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}