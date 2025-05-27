package com.mycompany.dnsproject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void log(String level, String message) {
        String timestamp = sdf.format(new Date());
        System.out.println("[" + timestamp + "] [" + level + "] " + message);
    }
}