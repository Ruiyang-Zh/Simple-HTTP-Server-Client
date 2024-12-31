package edu.nju.http.server;

import edu.nju.http.message.HttpResponse;
import edu.nju.http.message.constant.Header;
import edu.nju.http.message.constant.Status;
import edu.nju.http.utils.Log;
import edu.nju.http.utils.Searcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ResponseBuilder {
    /**
     * 创建 200 OK 成功响应: 文本内容
     */
    protected static HttpResponse createSuccessResponse(String version, String plainText) {
        HttpResponse response = new HttpResponse(version, Status.OK);
        response.setBody(plainText);
        setCommonHeaders(response);
        return response;
    }

    /**
     * 创建 200 OK 成功响应: 文件内容
     */
    protected static HttpResponse createSuccessResponse (String version, Path filePath) throws IOException {
        HttpResponse response = new HttpResponse(version, Status.OK);
        response.setBody(filePath);
        setResourceHeaders(response, filePath);
        setCommonHeaders(response);
        return response;
    }

    /**
     * 创建 304 Not Modified 响应
     */
    protected static HttpResponse createNotModifiedResponse(String version) {
        HttpResponse response = new HttpResponse(version, Status.NOT_MODIFIED);
        setCommonHeaders(response);
        return response;
    }

    /**
     * 创建重定向响应
     */
    protected static HttpResponse createRedirectResponse(String version, int statusCode, String location) {
        HttpResponse response = new HttpResponse(version, statusCode);
        response.setHeader(Header.Location, location);
        response.setBody("Redirecting to " + location);
        setCommonHeaders(response);
        return response;
    }

    /**
     * 创建错误响应
     */
    protected static HttpResponse createErrorResponse(String version, int statusCode) {
        HttpResponse response = new HttpResponse(version, statusCode);
        try {
            response.setBody(Searcher.getResource(Status.getDefaultErrorPage(statusCode).toString()));
        } catch (IOException | IllegalAccessException e) {
            Log.error("ServerHandler", "Failed to return error response, use the plain text instead", e);
            response.setBody(statusCode + ": " +  Status.getStatusPhrase(statusCode));
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
            response.setHeader(Header.Last_Modified, lastModifiedTime.toString());
            Log.debug("ServerHandler", "Set resource headers: ETag=" + eTag);
        } catch (IOException e) {
            Log.error("ServerHandler", "Failed to set resource headers", e);
        }
    }
}
