package edu.nju.http.server;

import edu.nju.http.message.HttpRequest;
import edu.nju.http.message.HttpResponse;
import edu.nju.http.message.constant.*;
import edu.nju.http.message.MIME;
import edu.nju.http.utils.Log;
import edu.nju.http.utils.Searcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ServerHandler - 处理 HTTP 请求
 */
public class ServerHandler {

    public static HttpResponse handle(HttpRequest request) {
        HttpResponse response;
        Log.debug("ServerHandler", "Handling request: " + request.getMethod() + " " + request.getPath());

        try {
            Version.validateVersion(request.getVersion());
            switch (request.getMethod()) {
                case Method.GET:
                    response = handleGet(request);
                    break;
                case Method.POST:
                    response = handlePost(request);
                    break;
                default:
                    Log.warn("Server", "Unsupported method: " + request.getMethod());
                    response = createErrorResponse(request.getVersion(), Status.METHOD_NOT_ALLOWED);
            }
        } catch (IllegalArgumentException e) {
            Log.warn("Server", "Bad request: " + e.getMessage());
            response = createErrorResponse(request.getVersion(), Status.BAD_REQUEST);
        } catch (Exception e) {
            Log.error("Server", "Internal server error", e);
            response = createErrorResponse(request.getVersion(), Status.INTERNAL_SERVER_ERROR);
        }

        if (response == null) {
            Log.warn("Server", "Response was null, returning 500 Internal Server Error");
            response = createErrorResponse(request.getVersion(), Status.INTERNAL_SERVER_ERROR);
        }

        Log.info("Server", "Response status: " + response.getStatusCode());
        return response;
    }

    /**
     * 处理 GET 请求
     */
    private static HttpResponse handleGet(HttpRequest request) {
        String path = request.getPath();
        Log.debug("ServerHandler", "Handling GET request for path: " + path);

        // 检查重定向规则
        if (Config.REDIRECT_RULES.containsKey(path)) {
            Config.RedirectRule rule = Config.REDIRECT_RULES.get(path);
            Log.info("Server", "Redirecting to: " + rule.target + " with status: " + rule.statusCode);
            return createRedirectResponse(request.getVersion(), rule.statusCode, rule.target);
        }

        path = path.equals("/") ? Config.DEFAULT_PAGE : path;

        // 查找资源路径
        Path filePath = Searcher.pathOf(Config.STATIC_RESOURCE_DIR + path);
        if (filePath == null || !Files.exists(filePath)) {
            Log.info("Server: ", "File not found: " + path);
            return createErrorResponse(request.getVersion(), Status.NOT_FOUND);
        }

        try {
            // 客户端缓存校验
            if (checkClientCache(request, filePath)) {
                Log.info("ServerHandler", "Client cache valid, returning 304 Not Modified");
                return createNotModifiedResponse(request.getVersion());
            }

            HttpResponse response = createSuccessResponse(request.getVersion(), filePath);

            Log.info("ServerHandler", "Serving file: " + filePath + " with type: " + response.getHeaderVal(Header.Content_Type));

            return response;
        } catch (IOException e) {
            Log.error("ServerHandler", "Failed to read file: " + filePath, e);
            return createErrorResponse(request.getVersion(), Status.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 处理 POST 请求
     */
    private static HttpResponse handlePost(HttpRequest request) {
        String body = new String(request.getBody());
        System.out.println("Received POST Data: " + body);
        return createSuccessResponse(request.getVersion(), "Received: " + body);
    }

    /**
     * 创建 200 OK 成功响应: 文本内容
     */
    private static HttpResponse createSuccessResponse(String version, String body) {
        HttpResponse response = new HttpResponse(version, Status.OK);
        response.setBody(body.getBytes(), MIME.TEXT_PLAIN);
        setCommonHeaders(response);
        return response;
    }

    /**
     * 创建 200 OK 成功响应: 文件内容
     */
    private static HttpResponse createSuccessResponse (String version, Path filePath) throws IOException {
        HttpResponse response = new HttpResponse(version, Status.OK);
        response.setBody(filePath);
        setResourceHeaders(response, filePath);
        setCommonHeaders(response);
        return response;
    }

    /**
     * 创建 304 Not Modified 响应
     */
    private static HttpResponse createNotModifiedResponse(String version) {
        HttpResponse response = new HttpResponse(version, Status.NOT_MODIFIED);
        setCommonHeaders(response);
        return response;
    }

    /**
     * 创建重定向响应
     */
    private static HttpResponse createRedirectResponse(String version, int statusCode, String location) {
        HttpResponse response = new HttpResponse(version, statusCode);
        response.setHeader(Header.Location, location);
        response.setBody(("Redirecting to " + location).getBytes(), MIME.TEXT_PLAIN);
        setCommonHeaders(response);
        return response;
    }

    /**
     * 创建错误响应
     */
    private static HttpResponse createErrorResponse(String version, int statusCode) {
        HttpResponse response = new HttpResponse(version, statusCode);
        try {
            response.setBody(Status.getDefaultErrorPage(statusCode));
        } catch (IOException e) {
            Log.error("ServerHandler", "Failed to return error response", e);
            throw new RuntimeException(e);
        }
        setCommonHeaders(response);
        return response;
    }

    private static void setCommonHeaders(HttpResponse response) {
        response.setHeader(Header.Server, Config.SERVER_SIGNATURE);
        response.setHeader(Header.Connection, Config.KEEP_ALIVE ? "keep-alive" : "close");
        String date = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        response.setHeader(Header.Date, date);
        if (response.getHeaderVal(Header.Cache_Control) == null) {
            response.setHeader(Header.Cache_Control, Config.CACHE_CONTROL);
        }
    }

    /**
     * 设置资源相关头部信息
     */
    private static void setResourceHeaders(HttpResponse response, Path filePath) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(filePath);
            String eTag = String.valueOf(lastModifiedTime.toMillis());

            response.setHeader(Header.ETag, eTag);
            response.setHeader(Header.Last_Modified, ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
            Log.debug("ServerHandler", "Set resource headers: ETag=" + eTag);
        } catch (IOException e) {
            Log.error("ServerHandler", "Failed to set resource headers", e);
        }
    }

    /**
     * 检查客户端缓存头部
     */
    private static boolean checkClientCache(HttpRequest request, Path filePath) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(filePath);
            String ifModifiedSince = request.getHeaderVal(Header.If_Modified_Since);
            String ifNoneMatch = request.getHeaderVal(Header.If_None_Match);
            String eTag = String.valueOf(lastModifiedTime.toMillis());

            if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
                Log.debug("ServerHandler", "ETag matches, cache valid");
                return true;
            }

            if (ifModifiedSince != null) {
                long ifModifiedSinceTime = Long.parseLong(ifModifiedSince);
                if (lastModifiedTime.toMillis() <= ifModifiedSinceTime) {
                    Log.debug("ServerHandler", "If-Modified-Since matches, cache valid");
                    return true;
                }
            }
        } catch (IOException e) {
            Log.error("ServerHandler", "Failed to check client cache headers", e);
        }

        return false;
    }

}
