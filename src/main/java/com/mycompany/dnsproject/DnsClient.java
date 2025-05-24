/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.dnsproject;

/**
 *
 * @author Admin
 */
import java.net.*;
import java.io.*;

public class DnsClient {
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 8053;

            // Test A query
            sendQuery(socket, serverAddress, serverPort, "A example.com");

            // Test REGISTER query
            sendQuery(socket, serverAddress, serverPort, "REGISTER nhaccuatui.com");

            // Test PTR query
            sendQuery(socket, serverAddress, serverPort, "PTR 192.168.1.10");

            // Test ZONE query
            sendQuery(socket, serverAddress, serverPort, "ZONE request");
            
            // Test MX
            sendQuery(socket, serverAddress, serverPort, "MX wannacry.local");

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
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
        System.out.println("Query: " + query + " -> Response: " + responseStr);
    }
}
