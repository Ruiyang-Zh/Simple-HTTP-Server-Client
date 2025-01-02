package edu.nju.http.server;

import edu.nju.http.message.HttpRequest;
import edu.nju.http.message.HttpResponse;
import edu.nju.http.message.constant.*;
import edu.nju.http.utils.Log;
import edu.nju.http.utils.Searcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

/**
 * ServerHandler - 处理 HTTP 请求
 */
public class ServerHandler {

    public static HttpResponse handle(HttpRequest request) {
        HttpResponse response;
        Log.debug("ServerHandler", "Handling request: " + request.getMethod() + " " + request.getTarget());

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
                    response = ResponseBuilder.createErrorResponse(request.getVersion(), Status.METHOD_NOT_ALLOWED);
            }
        } catch (IllegalArgumentException e) {
            Log.warn("Server", "Bad request: " + e.getMessage());
            response = ResponseBuilder.createErrorResponse(request.getVersion(), Status.BAD_REQUEST);
        } catch (Exception e) {
            Log.error("Server", "Internal server error", e);
            response = ResponseBuilder.createErrorResponse(request.getVersion(), Status.INTERNAL_SERVER_ERROR);
        }

        if (response == null) {
            Log.warn("Server", "Response was null, returning 500 Internal Server Error");
            response = ResponseBuilder.createErrorResponse(request.getVersion(), Status.INTERNAL_SERVER_ERROR);
        }

        Log.info("Server", "Response status: " + response.getStatusCode());
        return response;
    }

    /**
     * 处理 GET 请求
     */
    private static HttpResponse handleGet(HttpRequest request) {
        String target = request.getTarget();
        Log.debug("ServerHandler", "Handling GET request for target: " + target);

        // 检查重定向规则
        if (Config.REDIRECT_RULES.containsKey(target)) {
            Config.RedirectRule rule = Config.REDIRECT_RULES.get(target);
            Log.info("Server", "Redirecting to: " + rule.target + " with status: " + rule.statusCode);
            return ResponseBuilder.createRedirectResponse(request.getVersion(), rule.statusCode, rule.target);
        }

        // USER_DIR 下的资源需要验证登陆
        if (target.startsWith("/" + Config.USER_DIR)){
            String username = UserSystem.getInstance().validateSession(request);
            if (username == null) {
                return ResponseBuilder.createErrorResponse(request.getVersion(), Status.UNAUTHORIZED);
            }
        }

        target = target.equals("/") ? Config.DEFAULT_PAGE : target;
        target = Paths.get(Config.STATIC_RESOURCE_DIR, target).toString();
        // 查找资源路径
        Path filePath;
        try {
            filePath = Searcher.getResource(target);
        } catch (IllegalAccessException e) {
            Log.warn("Server", "Access denied: " + target);
            return ResponseBuilder.createErrorResponse(request.getVersion(), Status.FORBIDDEN);
        } catch (FileNotFoundException e) {
            Log.warn("Server", "Resource not found: " + target);
            return ResponseBuilder.createErrorResponse(request.getVersion(), Status.NOT_FOUND);
        }

        try {
            // 客户端缓存校验
            if (checkClientCache(request, filePath)) {
                Log.info("Server", "Client cache valid, returning 304 Not Modified");
                return ResponseBuilder.createNotModifiedResponse(request.getVersion());
            }

            HttpResponse response = ResponseBuilder.createSuccessResponse(request.getVersion(), filePath);

            Log.info("Server", "Serving file: " + filePath + " with type: " + response.getHeaderVal(Header.Content_Type));

            return response;
        } catch (IOException e) {
            Log.error("Server", "Failed to read file: " + filePath, e);
            return ResponseBuilder.createErrorResponse(request.getVersion(), Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 处理 POST 请求
     */
    private static HttpResponse handlePost(HttpRequest request) {
        String target = request.getTarget();
        switch (target) {
            case "/login":
                return UserSystem.getInstance().login(request);
            case "/register":
                return UserSystem.getInstance().register(request);
            case "/logout":
                return UserSystem.getInstance().logout(request);
            default:
                Log.warn("ServerHandler", "Unsupported POST target: " + target);
                return ResponseBuilder.createErrorResponse(request.getVersion(), Status.NOT_FOUND);
        }
    }


    /**
     * 检查客户端缓存头部
     */
    private static boolean checkClientCache(HttpRequest request, Path filePath) {
        if(request.getHeaderVal(Header.Cache_Control) != null && request.getHeaderVal(Header.Cache_Control).contains("no-cache")) {
            Log.debug("ServerHandler", "client cache-control: no-cache");
            return false;
        }

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
