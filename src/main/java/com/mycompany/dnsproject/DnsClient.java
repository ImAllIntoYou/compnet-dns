package com.mycompany.dnsproject;

import java.io.*;
import java.net.*;
import java.util.UUID;

public class DnsClient {
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 53;

            while (true) {
                // Test A query
                sendQuery(socket, serverAddress, serverPort, "A example.com");
                sendQuery(socket, serverAddress, serverPort, "A uia.org");

                // Test REGISTER query for existing domains
                sendQuery(socket, serverAddress, serverPort, "REGISTER example.com");
                sendQuery(socket, serverAddress, serverPort, "REGISTER crypto.com");

                // Test REGISTER for new computers joining
                String newClientDomain1 = "client-" + UUID.randomUUID().toString().substring(0, 8) + ".local";
                String newClientDomain2 = "client-" + UUID.randomUUID().toString().substring(0, 8) + ".local";
                sendQuery(socket, serverAddress, serverPort, "REGISTER " + newClientDomain1);
                sendQuery(socket, serverAddress, serverPort, "REGISTER " + newClientDomain2);

                // Test PTR query
                sendQuery(socket, serverAddress, serverPort, "PTR 192.168.1.10");

                // Test ZONE query
                sendQuery(socket, serverAddress, serverPort, "ZONE request");

                // Test MX query
                sendQuery(socket, serverAddress, serverPort, "MX client1.local");

                // Test CACHE query
                sendQuery(socket, serverAddress, serverPort, "CACHE request");

                Thread.sleep(60000);
            }
        } catch (Exception e) {
            Logger.log("ERROR", "Client error: " + e.getMessage());
        }
    }

    private static void sendQuery(DatagramSocket socket, InetAddress address, int port, String query) throws IOException {
        byte[] queryBytes = query.getBytes();
        DatagramPacket packet = new DatagramPacket(queryBytes, queryBytes.length, address, port);
        socket.send(packet);

        byte[] buffer = new byte[512];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        socket.receive(response);

        String responseStr = new String(response.getData(), 0, response.getLength());
        Logger.log("INFO", "Query: " + query + " -> Response: " + responseStr);
    }
}