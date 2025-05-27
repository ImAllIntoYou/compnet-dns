package com.mycompany.dnsproject;

import java.io.*;
import java.text.ParseException;
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
            Logger.log("INFO", "Cache hit for A:" + domain);
            return cached.substring(4);
        }
        DnsEntry entry = mappings.get(domain);
        if (entry != null) {
            String ip = entry.ip;
            cache.put(cacheKey, "IP: " + ip);
            Logger.log("INFO", "Cache miss for A:" + domain + ", added to cache");
            return ip;
        }
        return null;
    }

    public String getDomain(String ip) {
        String cacheKey = "PTR:" + ip;
        String cached = cache.get(cacheKey);
        if (cached != null && cached.startsWith("DOMAIN: ")) {
            Logger.log("INFO", "Cache hit for PTR:" + ip);
            return cached.substring(8);
        }
        for (DnsEntry entry : mappings.values()) {
            if (entry.ip.equals(ip)) {
                cache.put(cacheKey, "DOMAIN: " + entry.domain);
                Logger.log("INFO", "Cache miss for PTR:" + ip + ", added to cache");
                return entry.domain;
            }
        }
        return null;
    }

    public String getMailServer(String domain) {
        String cacheKey = "MX:" + domain;
        String cached = cache.get(cacheKey);
        if (cached != null && cached.startsWith("MAIL: ")) {
            Logger.log("INFO", "Cache hit for MX:" + domain);
            return cached.substring(6);
        }
        String mailServer = mxRecords.get(domain);
        if (mailServer != null) {
            cache.put(cacheKey, "MAIL: " + mailServer);
            Logger.log("INFO", "Cache miss for MX:" + domain + ", added to cache");
            return mailServer;
        }
        return null;
    }

    public String assignIp(String domain) {
        if (mappings.containsKey(domain)) {
            Logger.log("ERROR", "Domain already exists: " + domain);
            return null;
        }
        if (ipPool.isEmpty()) {
            Logger.log("ERROR", "No IPs available for REGISTER: " + domain);
            return null;
        }
        String ip = ipPool.remove(0);
        long timestamp = System.currentTimeMillis();
        mappings.put(domain, new DnsEntry(domain, ip, timestamp));
        mxRecords.put(domain, "mail." + domain);
        saveMapping(domain, ip, timestamp);
        Logger.log("INFO", "Assigned IP " + ip + " to " + domain);
        return ip;
    }

    public String getAllMappings() {
        StringBuilder sb = new StringBuilder();
        for (DnsEntry entry : mappings.values()) {
            String timestampStr = Logger.sdf.format(new Date(entry.timestamp));
            sb.append(entry.domain).append(",").append(entry.ip).append(",").append(timestampStr).append(";");
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : null;
    }

    public String getCacheContents() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            sb.append(entry.getKey()).append(" -> ").append(entry.getValue()).append(";");
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "EMPTY";
    }

    private void loadMappings() {
        try (BufferedReader reader = new BufferedReader(new FileReader("dns_mappings.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String domain = parts[0];
                    String ip = parts[1];
                    long timestamp;
                    if (parts.length > 2) {
                        try {
                            // Try parsing as datetime
                            timestamp = Logger.sdf.parse(parts[2]).getTime();
                        } catch (ParseException e) {
                            // Fallback for legacy millisecond format
                            try {
                                timestamp = Long.parseLong(parts[2]);
                            } catch (NumberFormatException ex) {
                                timestamp = System.currentTimeMillis();
                            }
                        }
                    } else {
                        timestamp = System.currentTimeMillis(); // Legacy files without timestamp
                    }
                    mappings.put(domain, new DnsEntry(domain, ip, timestamp));
                    mxRecords.put(domain, "mail." + domain);
                    ipPool.remove(ip);
                    Logger.log("INFO", "Loaded mapping: " + domain + " -> " + ip + ", timestamp: " + Logger.sdf.format(new Date(timestamp)));
                }
            }
        } catch (IOException e) {
            Logger.log("ERROR", "Error loading mappings: " + e.getMessage());
        }
    }

    private void saveMapping(String domain, String ip, long timestamp) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("dns_mappings.txt", true))) {
            String timestampStr = Logger.sdf.format(new Date(timestamp));
            writer.write(domain + "," + ip + "," + timestampStr);
            writer.newLine();
            Logger.log("INFO", "Saved mapping: " + domain + "," + ip + "," + timestampStr);
        } catch (IOException e) {
            Logger.log("ERROR", "Error saving mapping: " + e.getMessage());
        }
    }

    public ConcurrentHashMap<String, DnsEntry> getMappings() {
        return mappings;
    }

    public ArrayList<String> getIpPool() {
        return ipPool;
    }
}