package edu.nju.http.client;

import edu.nju.http.message.HttpRequest;
import edu.nju.http.message.HttpResponse;
import edu.nju.http.message.constant.Header;
import edu.nju.http.message.constant.Method;
import edu.nju.http.utils.Log;
import lombok.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * HttpClient
 */
public class HttpClient {
    private final Map<String, Connection> connectionMap = new HashMap<>();
    private final Cache cache = new Cache();
    private static final int MAX_REDIRECTS = 5;

    /**
     * 发送 HTTP 请求
     *
     * @param host    目标服务器地址
     * @param port    目标服务器端口
     * @param request HTTP 请求对象
     * @return HTTP 响应对象
     */
    public HttpResponse send(String host, int port, HttpRequest request) {
        // 检查缓存
        if (Method.GET.equals(request.getMethod()) && cache.contains(request)) {
            if (cache.isValid(request)) {
                Log.info("HttpClient", "Cache hit: " + request.getStartLine());
                HttpResponse cachedResponse = cache.get(request);
                // 更新 Date
                cachedResponse.setHeader(Header.Date, ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
            } else {
                Log.info("HttpClient", "Cache expired: " + request.getStartLine());
                HttpResponse cachedResponse = cache.get(request);
                request.setHeader(Header.If_None_Match, cachedResponse.getHeaderVal(Header.ETag));
                request.setHeader(Header.If_Modified_Since, cachedResponse.getHeaderVal(Header.Last_Modified));
            }
        }

        HttpResponse response = sendRequest(host, port, request);

        // 304 直接返回缓存内容
        if (response != null && response.getStatusCode() == 304) {
            Log.info("HttpClient", "Resource not modified, using cached version.");
            cache.update(request);
            return cache.get(request);
        }

        // 重定向
        if (response != null && isRedirect(response)) {
            return redirect(response, request);
        }

        // 缓存
        if (Config.ENABLE_CACHE && Method.GET.equals(request.getMethod()) && response != null && response.getStatusCode() == 200) {
            cache.put(request, response);
        }

        return response;
    }

    /**
     * 断开与特定服务器的连接
     */
    public void disconnect(String host, int port) {
        String connectionKey = host + ":" + port;
        Connection connection = connectionMap.remove(connectionKey);
        if (connection != null) {
            connection.close();
            Log.info("HttpClient", "Connection closed: " + connectionKey);
        } else {
            Log.info("HttpClient", "No connection to close: " + connectionKey);
        }
    }

    /**
     * 停止所有连接
     * 该方法将关闭所有已建立的连接，释放资源，并清空连接映射表。
     */
    public void stop() {
        Log.info("HttpClient", "Closing all connections...");

        for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
            String connectionKey = entry.getKey();
            Connection connection = entry.getValue();

            if (connection != null && connection.isConnected()) {
                connection.close();
                Log.info("HttpClient", "Connection closed: " + connectionKey);
            }
        }

        connectionMap.clear();
        Log.info("HttpClient", "All connections have been closed. Client stopped.");
    }


    /**
     * 实际发送 HTTP 请求
     */
    private HttpResponse sendRequest(String host, int port, HttpRequest request) {
        String connectionKey = host + ":" + port;
        Connection connection = connectionMap.computeIfAbsent(connectionKey, k -> {
            Connection conn = new Connection(host, port);
            conn.connect();
            return conn;
        });

        // 添加 Cookie
        if (connection.getCookie() != null) {
            request.setHeader(Header.Cookie, connection.getCookie());
        }

        try {
            // 发送请求
            ByteBuffer requestBuffer = ByteBuffer.wrap(request.toBytes());
            while (requestBuffer.hasRemaining()) {
                connection.channel.write(requestBuffer);
            }

            // 读取响应
            HttpResponse response = null;
            ByteArrayOutputStream responseData = new ByteArrayOutputStream();
            ByteBuffer responseBuffer = ByteBuffer.allocate(Config.BUFFER_SIZE);

            boolean headerParsed = false;
            int contentLength = -1;
            int headerEndIndex = -1;

            try {
                while (true) {
                    int bytesRead = connection.channel.read(responseBuffer);
                    if (bytesRead == -1) break;

                    responseBuffer.flip();
                    byte[] data = new byte[responseBuffer.remaining()];
                    responseBuffer.get(data);
                    responseData.write(data);
                    responseBuffer.clear();

                    // 响应头，解析以获取结束位置
                    if (!headerParsed) {
                        headerEndIndex = HttpResponse.findHeaderEnd(responseData.toByteArray());
                        if (headerEndIndex != -1) {
                            headerParsed = true;

                            byte[] headerBytes = Arrays.copyOfRange(responseData.toByteArray(), 0, headerEndIndex + 4);
                            response = new HttpResponse(headerBytes);

                            String contentLengthVal = response.getHeaderVal(Header.Content_Length);
                            if (contentLengthVal != null) {
                                contentLength = Integer.parseInt(contentLengthVal.trim());
                            }

                            byte[] remainingBody = Arrays.copyOfRange(responseData.toByteArray(), headerEndIndex + 4, responseData.size());
                            responseData.reset();
                            responseData.write(remainingBody);
                        }
                    }

                    // 响应体
                    if (headerParsed) {
                        if (contentLength >= 0) {
                            if (responseData.size() >= contentLength) {
                                byte[] body = Arrays.copyOfRange(responseData.toByteArray(), 0, contentLength);
                                response.setBody(body, response.getHeaderVal(Header.Content_Type));
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.error("HttpClient", "Request/Response failed", e);
                disconnect(host, port);
            }

            if(response == null) {
                Log.error("HttpClient", "Unexpected error occurred while parsing response.");
                return null;
            }

            Log.info("HttpClient", "Response received: " + response.getStartLine());

            // 处理 Cookie
            String setCookie = response.getHeaderVal(Header.Set_Cookie);
            if (setCookie != null) {
                connection.setCookie(setCookie);
            }

            // 长连接
            String connectionHeader = response.getHeaderVal(Header.Connection);
            if (!"keep-alive".equalsIgnoreCase(connectionHeader)) {
                disconnect(host, port);
            }

            return response;

        } catch (IOException e) {
            Log.error("HttpClient", "Request/Response failed", e);
            disconnect(host, port);
            return null;
        }
    }

    /**
     * 处理重定向逻辑
     *
     * @param response 原始响应
     * @param request  原始请求
     * @return HTTP 响应对象
     */
    private HttpResponse redirect(HttpResponse response, HttpRequest request) {

        String location = response.getHeaderVal(Header.Location);
        if (location == null) {
            Log.warn("HttpClient", "Redirect location not specified.");
            return null;
        }

        Log.info("HttpClient", "Redirecting to: " + location);

        String host, target;
        int port;

        try {
            if (location.startsWith("http://") || location.startsWith("https://")) {
                URI uri = new URI(location);
                host = uri.getHost();
                port = uri.getPort() != -1 ? uri.getPort() : 80;
                target = uri.getPath() != null ? uri.getPath() : "/";
                if (uri.getQuery() != null) {
                    target += "?" + uri.getQuery();
                }
            } else {
                String[] parts = request.getHeaderVal(Header.Host).split(":");
                host = parts[0];
                port = parts.length > 1 ? Integer.parseInt(parts[1]) : 80;
                target = location.startsWith("/") ? location : "/" + location;
            }
        } catch (URISyntaxException e) {
            Log.error("HttpClient", "Invalid redirect URI: " + location, e);
            return null;
        }

        request.setHeader(Header.Host, host + ":" + port);
        request.setTarget(target);

        return send(host, port, request);
    }

    private boolean isRedirect(HttpResponse response) {
        int statusCode = response.getStatusCode();
        return statusCode == 301 || statusCode == 302 || statusCode == 303;
    }

    /**
     * 连接管理
     */
    private class Connection {
        private final String host;
        private final int port;
        private SocketChannel channel;
        @Getter @Setter
        private String cookie;

        public Connection(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public void connect() {
            try {
                channel = SocketChannel.open();
                channel.configureBlocking(true);
                channel.connect(new InetSocketAddress(host, port));
                Log.info("Connection", "Connected to " + host + ":" + port);
            } catch (IOException e) {
                Log.error("Connection", "Failed to connect to " + host + ":" + port, e);
            }
        }

        public void close() {
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                    Log.info("Connection", "Connection closed: " + host + ":" + port);
                }
            } catch (IOException e) {
                Log.error("Connection", "Error closing connection", e);
            }
        }

        public boolean isConnected() {
            return channel != null && channel.isOpen();
        }

    }

}
