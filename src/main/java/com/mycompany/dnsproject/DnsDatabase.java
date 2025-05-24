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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DnsDatabase {
    private ConcurrentHashMap<String, DnsEntry> mappings;
    private ConcurrentHashMap<String, String> mxRecords;
    private ConcurrentHashMap<String, String> cache;
    private ArrayList<String> ipPool;

    public static class DnsEntry {
        String domain;
        String ip;
        long timestamp;

        DnsEntry(String domain, String ip, long timestamp) {
            this.domain = domain;
            this.ip = ip;
            this.timestamp = timestamp;
        }
    }

    public DnsDatabase() {
        mappings = new ConcurrentHashMap<>();
        mxRecords = new ConcurrentHashMap<>();
        cache = new ConcurrentHashMap<>();
        ipPool = new ArrayList<>();
        for (int i = 2; i <= 254; i++) {
            ipPool.add("192.168.1." + i);
        }
        loadMappings();
    }

    public String getIp(String domain) {
        String cacheKey = "A:" + domain;
        String cached = cache.get(cacheKey);
        if (cached != null && cached.startsWith("IP: ")) {
            System.out.println("Cache hit for A:" + domain);
            return cached.substring(4);
        }
        DnsEntry entry = mappings.get(domain);
        if (entry != null) {
            String ip = entry.ip;
            cache.put(cacheKey, "IP: " + ip);
            System.out.println("Cache miss for A:" + domain + ", added to cache");
            return ip;
        }
        return null;
    }

    public String getDomain(String ip) {
        String cacheKey = "PTR:" + ip;
        String cached = cache.get(cacheKey);
        if (cached != null && cached.startsWith("DOMAIN: ")) {
            System.out.println("Cache hit for PTR:" + ip);
            return cached.substring(8);
        }
        for (DnsEntry entry : mappings.values()) {
            if (entry.ip.equals(ip)) {
                cache.put(cacheKey, "DOMAIN: " + entry.domain);
                System.out.println("Cache miss for PTR:" + ip + ", added to cache");
                return entry.domain;
            }
        }
        return null;
    }

    public String getMailServer(String domain) {
        String cacheKey = "MX:" + domain;
        String cached = cache.get(cacheKey);
        if (cached != null && cached.startsWith("MAIL: ")) {
            System.out.println("Cache hit for MX:" + domain);
            return cached.substring(6);
        }
        String mailServer = mxRecords.get(domain);
        if (mailServer != null) {
            cache.put(cacheKey, "MAIL: " + mailServer);
            System.out.println("Cache miss for MX:" + domain + ", added to cache");
            return mailServer;
        }
        return null;
    }

    public String assignIp(String domain) {
        if (mappings.containsKey(domain)) {
            System.out.println("Domain already exists: " + domain);
            return null;
        }
        if (ipPool.isEmpty()) {
            System.out.println("No IPs available for REGISTER: " + domain);
            return null;
        }
        String ip = ipPool.remove(0);
        mappings.put(domain, new DnsEntry(domain, ip, System.currentTimeMillis()));
        mxRecords.put(domain, "mail." + domain); // Default MX record
        saveMapping(domain, ip);
        System.out.println("Assigned IP " + ip + " to " + domain);
        return ip;
    }

    public String getAllMappings() {
        StringBuilder sb = new StringBuilder();
        for (DnsEntry entry : mappings.values()) {
            sb.append(entry.domain).append(",").append(entry.ip).append(";");
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : null;
    }

    private void loadMappings() {
        try (BufferedReader reader = new BufferedReader(new FileReader("dns_mappings.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    mappings.put(parts[0], new DnsEntry(parts[0], parts[1], System.currentTimeMillis()));
                    mxRecords.put(parts[0], "mail." + parts[0]);
                    ipPool.remove(parts[1]);
                    System.out.println("Loaded mapping: " + parts[0] + " -> " + parts[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading mappings: " + e.getMessage());
        }
    }

    private void saveMapping(String domain, String ip) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("dns_mappings.txt", true))) {
            writer.write(domain + "," + ip);
            writer.newLine();
            System.out.println("Saved mapping: " + domain + "," + ip);
        } catch (IOException e) {
            System.err.println("Error saving mapping: " + e.getMessage());
        }
    }

    public ConcurrentHashMap<String, DnsEntry> getMappings() {
        return mappings;
    }

    public ArrayList<String> getIpPool() {
        return ipPool;
    }
}
