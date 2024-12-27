package edu.nju.http.message;

import java.util.HashMap;
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
    private static final String DEFAULT_TYPE = "application/octet-stream";

    private static final Map<String, String> mimeTypes = new HashMap<>();
    private static final Set<String> textTypes = Set.of("text/html", "text/plain", "text/css", "text/javascript");
    private static final Set<String> imageTypes = Set.of("image/png", "image/jpeg", "image/gif");

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

    public static String getFileExtension(String path) {
        int lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == path.length() - 1) {
            return ""; // 没有扩展名
        }
        return path.substring(lastDotIndex + 1).toLowerCase();
    }

    public static String getMimeType(String extension) {
        return mimeTypes.getOrDefault(extension.toLowerCase(), DEFAULT_TYPE);
    }

    public static boolean isTextType(String mimeType) {
        return textTypes.contains(mimeType);
    }

    public static boolean isImageType(String mimeType) {
        return imageTypes.contains(mimeType);
    }

    public static boolean isBinaryType(String mimeType) {
        return !isTextType(mimeType) && !isImageType(mimeType);
    }

}