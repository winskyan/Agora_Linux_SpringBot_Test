package com.example.demo.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SampleLogger {
    private static boolean enableLog = true;

    public static void enableLog(boolean enable) {
        enableLog = enable;
    }

    public static void log(String msg) {
        if (enableLog) {
            System.out.println(getCurrentTime() + " " + msg);
        }
    }

    public static String getCurrentTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return LocalDateTime.now().format(dtf);
    }

}
