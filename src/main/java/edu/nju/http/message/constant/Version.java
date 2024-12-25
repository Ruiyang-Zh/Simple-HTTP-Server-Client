package edu.nju.http.message.constant;

public class Version {
    public static String HTTP_1_0 = "HTTP/1.0";
    public static String HTTP_1_1 = "HTTP/1.1";

    public static void validateVersion(String version) {
        if(!version.equals(HTTP_1_0) && !version.equals(HTTP_1_1)) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }
    }
}
