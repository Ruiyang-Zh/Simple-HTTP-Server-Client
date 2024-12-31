package edu.nju.http.message;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MIME {
    public static final String TEXT_HTML = "text/html";
    public static final String TEXT_CSS = "text/css";
    public static final String TEXT_JAVASCRIPT = "text/javascript";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String IMAGE_GIF = "image/gif";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_PDF = "application/pdf";
    public static final String APPLICATION_ZIP = "application/zip";
    public static final String DEFAULT_TYPE = "application/octet-stream";

    private static final Map<String, String> mimeTypes = new HashMap<>();
    private static final Map<String, String> extensions = new HashMap<>();
    private static final Set<String> textTypes = new HashSet<>();

    static {
        // 文本类型
        mimeTypes.put("html", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "text/javascript");
        mimeTypes.put("txt", "text/plain");

        // 图片类型
        mimeTypes.put("png", "image/png");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("gif", "image/gif");

        // 应用类型
        mimeTypes.put("json", "application/json");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("zip", "application/zip");

        // 默认类型
        mimeTypes.put("default", DEFAULT_TYPE);
    }

    static {
        extensions.put(TEXT_HTML, "html");
        extensions.put(TEXT_CSS, "css");
        extensions.put(TEXT_JAVASCRIPT, "js");
        extensions.put(TEXT_PLAIN, "txt");
        extensions.put(IMAGE_PNG, "png");
        extensions.put(IMAGE_JPEG, "jpg");
        extensions.put(IMAGE_GIF, "gif");
        extensions.put(APPLICATION_JSON, "json");
        extensions.put(APPLICATION_PDF, "pdf");
        extensions.put(APPLICATION_ZIP, "zip");
    }

    static {
        textTypes.add(TEXT_HTML);
        textTypes.add(TEXT_CSS);
        textTypes.add(TEXT_JAVASCRIPT);
        textTypes.add(TEXT_PLAIN);
    }

    public static String getFileExtension(String path) {
        if(path == null) {
            return "";
        }
        int lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == path.length() - 1) {
            return ""; // 没有扩展名
        }
        return path.substring(lastDotIndex + 1).toLowerCase();
    }

    public static String getMimeType(String extension) {
        return mimeTypes.getOrDefault(extension.toLowerCase(), DEFAULT_TYPE);
    }

    public static String toExtension(String mimeType) {
        return extensions.getOrDefault(mimeType, "bin");
    }

    public static boolean supportType(String type) {
        return mimeTypes.containsValue(type);
    }

    public static boolean supportFileExtension(String extension) {
        return mimeTypes.containsKey(extension);
    }

    public static boolean isTextType(String mimeType) {
        return textTypes.contains(mimeType);
    }

    public static boolean isBinaryType(String mimeType) {
        return !isTextType(mimeType);
    }

}
