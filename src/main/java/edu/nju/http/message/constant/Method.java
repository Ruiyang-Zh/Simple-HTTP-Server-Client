package edu.nju.http.message.constant;

public class Method {
    public static final String GET = "GET";
    public static final String POST = "POST";

    public static void validateMethod(String method) {
        if (!method.equals(Method.GET) && !method.equals(Method.POST)) {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }
}
