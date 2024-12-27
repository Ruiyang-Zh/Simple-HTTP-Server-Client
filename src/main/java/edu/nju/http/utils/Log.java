package edu.nju.http.utils;

public class Log {

    private static int level = 0;

    public static void init(int logLevel) {
        level = logLevel;
    }

    public static void info(String logger,String message) {
        logger = logger == null ? "INFO" : logger;
        if (level > 0)
            System.out.printf("[%s]: %s%n", logger, message);
    }

    public static void warn(String logger,String message) {
        logger = logger == null ? "WARN" : logger;
        if (level > 0)
            System.out.printf("[%s]: %s%n", logger, message);
    }

    public static void error(String logger,String message) {
        logger = logger == null ? "ERROR" : logger;
        if (level > 0)
            System.err.printf("[%s]: %s%n", logger, message);
    }

    public static void error(String logger ,String message, Throwable e) {
        logger = logger == null ? "ERROR" : logger;
        if (level > 0) {
            System.err.printf("[%s]: %s%n", logger, message);
            e.printStackTrace();
        }
    }

    public static void debug(String logger,String message) {
        logger = logger == null ? "DEBUG" : logger;
        if(level > 0)
            System.out.printf("[%s]: %s%n", logger, message);
    }
}
