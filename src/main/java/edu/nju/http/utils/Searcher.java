package edu.nju.http.utils;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Searcher {
    // 基础目录
    public static final String BASE_DIR = System.getProperty("app.base.dir", System.getProperty("user.dir"));

    // 外部可访问资源目录
    public static final String EXTERNAL_DIR = getExternalPath("resources").toString();

    // 内部可访问资源目录
    public static final String INTERNAL_DIR = getInternalPath("static").toString();


    /**
     * 查找路径
     */
    public static Path pathOf(String relativePath) {
        Log.debug("Searcher", "Attempting to find resource: " + relativePath);

        Path externalPath = getExternalPath(relativePath);
        if (externalPath != null && Files.exists(externalPath)) {
            Log.debug("Searcher", "External resource found: " + externalPath);
            return externalPath;
        } else {
            Log.debug("Searcher", "External resource not found: " + relativePath);
        }

        Path internalPath = getInternalPath(relativePath);
        if (internalPath != null && Files.exists(internalPath)) {
            Log.debug("Searcher", "Internal resource found: " + internalPath);
            return internalPath;
        } else {
            Log.debug("Searcher", "Internal resource not found: " + relativePath);
        }

        return null;
    }

    /**
     * 对外部或内部找到的资源做安全检查，不安全则抛异常
     */
    public static Path getResource(String relativePath) throws IllegalAccessException {
        Path externalPath = getExternalPath("resources/" + relativePath);
        if (externalPath != null && Files.exists(externalPath)) {
            if (isSafeExternal(externalPath)) {
                Log.debug("Searcher", "External resource found: " + externalPath);
                return externalPath;
            } else {
                throw new IllegalAccessException("Unsafe path detected: " + externalPath);
            }
        }
        Log.debug("Searcher", "External resource not found: " + relativePath);
        Path internalPath = getInternalPath(relativePath);
        if (internalPath != null && Files.exists(internalPath)) {
            if (isSafeInternal(internalPath)) {
                return internalPath;
            } else {
                throw new IllegalAccessException("Unsafe path detected: " + internalPath);
            }
        }
        Log.debug("Searcher", "Internal resource not found: " + relativePath);
        return null;
    }

    public static boolean exists(String relativePath) {
        return pathOf(relativePath) != null;
    }

    public static Path getExternalPath(String relativePath) {
        try {
            return Paths.get(BASE_DIR, relativePath).normalize();
        } catch (Exception e) {
            Log.error("Searcher", "Failed to get external path: " + relativePath, e);
            return null;
        }
    }

    public static Path getInternalPath(String relativePath) {
        try {
            URL resourceUrl = Searcher.class.getClassLoader().getResource(relativePath);
            if (resourceUrl != null) {
                return Paths.get(resourceUrl.toURI()).normalize();
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.error("Searcher", "Failed to get internal path: " + relativePath, e);
        }
        return null;
    }

    /**
     * 安全路径检查
     * @param path 需要检查的路径
     * @return 如果路径是安全的，则返回 true，否则返回 false
     */
    private static boolean isSafePath(Path path) {
        boolean isExternalSafe = isSafeExternal(path);
        boolean isInternalSafe = isSafeInternal(path);

        if (isExternalSafe || isInternalSafe) {
            Log.debug("Searcher", "Safe path verified: " + path);
            return true;
        }

        Log.warn("Searcher", "Unsafe path detected: " + path);
        return false;
    }

    private static boolean isSafeExternal(Path path) {
       return path.startsWith(Paths.get(EXTERNAL_DIR).normalize());
    }

    private static boolean isSafeInternal(Path path) {
        return path.startsWith(Paths.get(INTERNAL_DIR).normalize());
    }

}
