package edu.nju.http.utils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Searcher {
    // 外部资源目录
    public static final String BASE_DIR = System.getProperty("app.base.dir", System.getProperty("user.dir"));

    /**
     * 查找资源路径（优先外部资源）
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

    public static boolean exists(String relativePath) {
        return pathOf(relativePath) != null;
    }

    public static Path getExternalPath(String relativePath) {
        try {
            Path externalPath = Paths.get(BASE_DIR, relativePath).normalize();

            if (!isSafePath(externalPath)) {
                Log.debug("Searcher", "Unsafe external path detected: " + relativePath);
                return null;
            }

            return externalPath;
        } catch (Exception e) {
            Log.error("Searcher", "Failed to get external path: " + relativePath, e);
            return null;
        }
    }

    public static Path getInternalPath(String relativePath) {
        try {
            URL resourceUrl = Searcher.class.getClassLoader().getResource(relativePath);
            if (resourceUrl != null) {
                Path internalPath = Paths.get(resourceUrl.toURI()).normalize();

                if (!isSafePath(internalPath)) {
                    Log.debug("Searcher", "Unsafe internal path detected: " + relativePath);
                    return null;
                }

                return internalPath;
            } else {
                Log.debug("Searcher", "Internal resource not found: " + relativePath);
            }
        } catch (Exception e) {
            Log.error("Searcher", "Failed to get internal path: " + relativePath, e);
        }

        return null;
    }

    public static String getAbsolutePath(String relativePath) {
        Path path = pathOf(relativePath);
        if (path != null && path.toFile().exists()) {
            String absolutePath = path.toAbsolutePath().toString();
            Log.debug("Searcher", "Absolute path resolved: " + absolutePath);
            return absolutePath;
        }

        Log.warn("Searcher", "Failed to resolve absolute path for: " + relativePath);
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
        try {
            Path basePath = Paths.get(BASE_DIR).normalize();
            return path.startsWith(basePath);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isSafeInternal(Path path) {
        try {
            String internalBase = Searcher.class.getClassLoader()
                    .getResource("")
                    .toURI()
                    .getPath();
            Path basePath = Paths.get(internalBase).normalize();
            return path.startsWith(basePath);
        } catch (Exception e) {
            return false;
        }
    }

}
