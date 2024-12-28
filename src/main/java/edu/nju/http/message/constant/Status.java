package edu.nju.http.message.constant;

import edu.nju.http.server.Config;

import java.util.HashMap;
import java.util.Map;

public class Status {
    public static int OK = 200;
    public static int MOVED_PERMANENTLY = 301;
    public static int FOUND = 302;
    public static int NOT_MODIFIED = 304;
    public static int BAD_REQUEST = 400;
    public static int UNAUTHORIZED = 401;
    public static int NOT_FOUND = 404;
    public static int METHOD_NOT_ALLOWED = 405;
    public static int INTERNAL_SERVER_ERROR = 500;

    private static final Map<Integer, String> STATUS_PHRASES = new HashMap<>();
    private static final Map<Integer, String> DEFAULT_ERROR_PAGES = new HashMap<>();

    static {
        STATUS_PHRASES.put(Status.OK, "OK");
        STATUS_PHRASES.put(Status.MOVED_PERMANENTLY, "Moved Permanently");
        STATUS_PHRASES.put(Status.FOUND, "Found");
        STATUS_PHRASES.put(Status.NOT_MODIFIED, "Not Modified");
        STATUS_PHRASES.put(Status.BAD_REQUEST, "Bad Request");
        STATUS_PHRASES.put(Status.UNAUTHORIZED, "Unauthorized");
        STATUS_PHRASES.put(Status.NOT_FOUND, "Not Found");
        STATUS_PHRASES.put(Status.METHOD_NOT_ALLOWED, "Method Not Allowed");
        STATUS_PHRASES.put(Status.INTERNAL_SERVER_ERROR, "Internal Server Error");

        DEFAULT_ERROR_PAGES.put(Status.BAD_REQUEST, Config.STATIC_RESOURCE_DIR + "/400.html");
        DEFAULT_ERROR_PAGES.put(Status.UNAUTHORIZED, Config.STATIC_RESOURCE_DIR + "/401.html");
        DEFAULT_ERROR_PAGES.put(Status.NOT_FOUND, Config.STATIC_RESOURCE_DIR + "/404.html");
        DEFAULT_ERROR_PAGES.put(Status.METHOD_NOT_ALLOWED, Config.STATIC_RESOURCE_DIR + "/405.html");
        DEFAULT_ERROR_PAGES.put(Status.INTERNAL_SERVER_ERROR, Config.STATIC_RESOURCE_DIR + "/500.html");

    }

    public static String getStatusPhrase(int statusCode) {
        return STATUS_PHRASES.get(statusCode);
    }

    public static String getDefaultErrorPage(int statusCode) {
        return DEFAULT_ERROR_PAGES.get(statusCode);
    }

    public static void validateStatus(int statusCode) {
        if (!STATUS_PHRASES.containsKey(statusCode)) {
            throw new IllegalArgumentException("Invalid status code: " + statusCode);
        }
    }
}
