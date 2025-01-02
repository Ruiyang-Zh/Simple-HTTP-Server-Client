package edu.nju.http.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;

public class Searcher {
    // 基础目录
    public static final String BASE_DIR = System.getProperty("app.base.dir", System.getProperty("user.dir"));

    // 外部可访问资源目录
    public static final String RESOURCES_DIR = Paths.get(BASE_DIR, "resources").toString();

    static {
        extract("config", Paths.get(BASE_DIR));
        extract("static", Paths.get(RESOURCES_DIR));
    }

    /**
     * 获取可访问资源路径
     *
     * @param relativePath 相对路径
     * @return 资源路径
     * @throws IllegalAccessException 如果路径不安全
     * @throws FileNotFoundException 如果资源未找到
     */
    public static Path getResource(String relativePath) throws IllegalAccessException, FileNotFoundException {
        Log.debug("Searcher", "Attempting to get resource: " + relativePath);

        Path resourcePath = Paths.get(RESOURCES_DIR, relativePath).normalize();

        if (Files.exists(resourcePath)) {
            if (resourcePath.startsWith(RESOURCES_DIR)) {
                Log.debug("Searcher", "Resource found: " + resourcePath);
                return resourcePath;
            } else {
                throw new IllegalAccessException();
            }
        }

        Log.warn("Searcher", "Resource not found: " + relativePath);
        throw new FileNotFoundException();
    }

    /**
     * 获取文件路径
     *
     * @param relativePath 相对路径
     * @return 路径或 null
     */
    public static Path pathOf(String relativePath) {
        Log.debug("Searcher", "Finding file: " + relativePath);

        Path filePath = Paths.get(BASE_DIR, relativePath).normalize();
        if (Files.exists(filePath)) {
            Log.debug("Searcher", "File found: " + filePath);
            return filePath;
        }

        Log.warn("Searcher", "File not found: " + relativePath);
        return null;
    }

    /**
     * 检查文件是否存在
     *
     * @param relativePath 相对路径
     * @return 是否存在
     */
    public static boolean exists(String relativePath) {
        return pathOf(relativePath) != null;
    }


    /**
     * 将内部资源提取到外部目录下, 已存在的文件/目录不覆盖, 仅补足缺失内容。
     * @param src    内部资源文件或目录
     * @param dest   目标目录
     */
    private static void extract(String src, Path dest) {
        try {
            if (!Files.exists(dest)) {
                Files.createDirectories(dest);
            }

            URL resourceUrl = Searcher.class.getClassLoader().getResource(src);
            if (resourceUrl == null) {
                Log.warn("ResourceExtractor", "Resource not found: " + src);
                return;
            }

            String protocol = resourceUrl.getProtocol();
            if ("jar".equals(protocol)) {
                copyFromJar(resourceUrl, src, dest);
            } else {
                copyFromFileSystem(resourceUrl, dest);
            }
        } catch (IOException e) {
            Log.warn("ResourceExtractor", "Failed to extract resource: " + src);
        }
    }

    /**
     * 从 JAR 中复制资源到外部目录
     */
    private static void copyFromJar(URL resourceUrl, String srcInJar, Path destDir){
        try {
            Path jarPath = Paths.get(
                    Searcher.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            try (FileSystem jarFs = FileSystems.newFileSystem(jarPath, (ClassLoader) null)) {
                Path srcRoot = jarFs.getPath(srcInJar);

                if (!Files.exists(srcRoot)) {
                    Log.debug("ResourceExtractor", "Source path does not exist in JAR: " + srcInJar);
                }

                copyRecursively(srcRoot, destDir);
            }
        } catch (URISyntaxException e) {
            Log.debug("ResourceExtractor", "Failed to resolve jar path: " + resourceUrl);
        } catch (IOException e) {
            Log.debug("ResourceExtractor", "Failed to read JAR file: " + resourceUrl);
        }
    }

    /**
     * 从文件系统中复制资源
     */
    private static void copyFromFileSystem(URL resourceUrl, Path destDir) {
        try {
            Path srcRoot = Paths.get(resourceUrl.toURI());
            if (!Files.exists(srcRoot)) {
                Log.debug("ResourceExtractor", "Source path does not exist: " + srcRoot);
            }
            copyRecursively(srcRoot, destDir);
        } catch (URISyntaxException e) {
            Log.debug("ResourceExtractor", "Failed to parse resource URI: " + resourceUrl);
        }
    }

    /**
     * 如果是文件，则目标下无同名文件时执行复制；如果是目录，则继续往下处理子目录与文件
     *
     * @param src  源文件或目录
     * @param dest 目标目录
     */
    private static void copyRecursively(Path src, Path dest) {
        try {
            if (Files.isRegularFile(src)) {
                Path targetFile = dest.resolve(src.getFileName().toString());
                if (!Files.exists(targetFile)) {
                    Files.copy(src, targetFile);
                    Log.debug("ResourceExtractor", "Copied file: " + src + " -> " + targetFile);
                } else {
                    Log.debug("ResourceExtractor", "File exists, skip copy: " + targetFile);
                }
            } else if (Files.isDirectory(src)) {
                Path targetDir = dest.resolve(src.getFileName().toString());
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                    Log.debug("ResourceExtractor", "Created directory: " + targetDir);
                }
                try (DirectoryStream<Path> children = Files.newDirectoryStream(src)) {
                    for (Path subSrc : children) {
                        copyRecursively(subSrc, targetDir);
                    }
                }
            }
        } catch (IOException e){
            Log.debug("ResourceExtractor", "Failed to copy resource: " + src);
        }
    }

}
