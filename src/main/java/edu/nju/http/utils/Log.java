package edu.nju.http.utils;

import edu.nju.http.server.Config;

public class Log {

    public static void info(String logger,String message) {
        logger = logger == null ? "INFO" : logger;
        if (Config.LOG_LEVEL > 0)
            System.out.printf("[%s]: %s%n", logger, message);
    }

    public static void warn(String logger,String message) {
        logger = logger == null ? "WARN" : logger;
        if (Config.LOG_LEVEL > 0)
            System.out.printf("[%s]: %s%n", logger, message);
    }

    public static void error(String logger,String message) {
        logger = logger == null ? "ERROR" : logger;
        if (Config.LOG_LEVEL > 0)
            System.err.printf("[%s]: %s%n", logger, message);
    }

    public static void error(String logger ,String message, Throwable e) {
        logger = logger == null ? "ERROR" : logger;
        if (Config.LOG_LEVEL > 0) {
            System.err.printf("[%s]: %s%n", logger, message);
            e.printStackTrace();
        }
    }

    public static void debug(String logger,String message) {
        logger = logger == null ? "DEBUG" : logger;
        if(Config.LOG_LEVEL > 0)
            System.out.printf("[%s]: %s%n", logger, message);
    }
}
