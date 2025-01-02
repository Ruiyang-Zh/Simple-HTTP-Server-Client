package edu.nju.http.client;

import edu.nju.http.message.HttpRequest;
import edu.nju.http.message.HttpResponse;
import edu.nju.http.message.constant.Header;
import edu.nju.http.message.constant.Method;
import edu.nju.http.utils.Log;
import lombok.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * HttpClient
 */
public class HttpClient {
    private final Map<String, Connection> connectionMap = new HashMap<>();
    private final Cache cache = new Cache();

    /**
     * 发送 HTTP 请求
     *
     * @param host    目标服务器地址
     * @param port    目标服务器端口
     * @param request HTTP 请求对象
     * @return HTTP 响应对象
     */
    public HttpResponse send(String host, int port, HttpRequest request) {
        return send(host, port, request, 0);
    }

    /**
     * 断开与特定服务器的连接
     */
    public void disconnect(String host, int port) {
        String connectionKey = host + ":" + port;
        Connection connection = connectionMap.remove(connectionKey);
        if (connection != null) {
            connection.close();
            Log.info("Client", "Connection closed: " + connectionKey);
        } else {
            Log.info("Client", "No connection to close: " + connectionKey);
        }
    }

    /**
     * 停止所有连接
     * 该方法将关闭所有已建立的连接，释放资源，并清空连接映射表。
     */
    public void stop() {
        Log.info("Client", "Closing all connections...");

        for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
            String connectionKey = entry.getKey();
            Connection connection = entry.getValue();

            if (connection != null && connection.isConnected()) {
                connection.close();
                Log.info("Client", "Connection closed: " + connectionKey);
            }
        }

        connectionMap.clear();
        Log.info("Client", "All connections have been closed. Client stopped.");
    }


    /**
     * HTTP 请求发送逻辑
     *
     * @param host          目标服务器地址
     * @param port          目标服务器端口
     * @param request       HTTP 请求对象
     * @param redirectCount 重定向次数
     * @return HTTP 响应对象
     */
    private HttpResponse send(String host, int port, HttpRequest request, int redirectCount) {
        // 检查缓存
        if (Method.GET.equals(request.getMethod()) && cache.contains(request)) {
            if (cache.isValid(request)) {
                Log.info("Client", "Cache hit: " + request.getStartLine());
                HttpResponse cachedResponse = cache.get(request);
                // 更新 Date
                cachedResponse.setHeader(Header.Date, ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
                return cachedResponse;
            } else {
                Log.info("Client", "Cache expired: " + request.getStartLine());
                HttpResponse cachedResponse = cache.get(request);
                request.setHeader(Header.If_None_Match, cachedResponse.getHeaderVal(Header.ETag));
                request.setHeader(Header.If_Modified_Since, cachedResponse.getHeaderVal(Header.Last_Modified));
            }
        }

        HttpResponse response = sendRequest(host, port, request);

        // 304 直接返回缓存内容
        if (response != null && response.getStatusCode() == 304) {
            Log.info("Client", "Resource not modified, using cached version.");
            cache.update(request);
            return cache.get(request);
        }

        // 重定向
        if (response != null && isRedirect(response)) {
            if(redirectCount >= Config.MAX_REDIRECTS) {
                Log.warn("Client", "Too many redirects.");
                return null;
            }
            return redirect(response, request, redirectCount);
        }

        // 缓存
        if (Config.ENABLE_CACHE
                && Method.GET.equals(request.getMethod())
                && response != null
                && response.getStatusCode() == 200
                && response.getHeaderVal(Header.Cache_Control) != null
                && !response.getHeaderVal(Header.Cache_Control).contains("no-store")) {
            cache.put(request, response);
        }

        return response;
    }

    /**
     * 处理重定向逻辑
     *
     * @param response 原始响应
     * @param request  原始请求
     * @param redirectCount 重定向次数
     * @return HTTP 响应对象
     */
    private HttpResponse redirect(HttpResponse response, HttpRequest request, int redirectCount) {

        String location = response.getHeaderVal(Header.Location);
        if (location == null) {
            Log.warn("Client", "Redirect location not specified.");
            return null;
        }

        Log.info("Client", "Redirecting to: " + location);

        if (location.contains("://")) {
            if(!location.startsWith("http")) {
                Log.error("Client", "Unsupported protocol: " + location);
                return null;
            }
        }

        request.setTarget(location);

        String[] hostPort = request.getHeaderVal(Header.Host).split(":");
        String host = hostPort[0];
        int port = hostPort.length == 2 ? Integer.parseInt(hostPort[1]) : 80;

        return send(host, port, request, redirectCount + 1);
    }

    private boolean isRedirect(HttpResponse response) {
        int statusCode = response.getStatusCode();
        return statusCode == 301 || statusCode == 302 || statusCode == 303;
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
            long timeout = System.currentTimeMillis() + Config.CONNECTION_TIMEOUT;

            try {
                while (true) {
                    if (System.currentTimeMillis() > timeout) {
                        Log.error("Client", "Response timeout while waiting for data.");
                        disconnect(host, port);
                        return null;
                    }
                    int bytesRead = connection.channel.read(responseBuffer);
                    if (bytesRead == -1) {
                        Log.warn("Client", "Connection closed by server.");
                        break;
                    } else if (bytesRead == 0) {
                        Thread.sleep(10);
                        continue;
                    }

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
                Log.error("Client", "Request/Response failed");
                disconnect(host, port);
            } catch (InterruptedException e) {
                Log.error("Client", "Thread interrupted during sleep", e);
                Thread.currentThread().interrupt();
                disconnect(host, port);
                return null;
            }

            if(response == null) {
                Log.error("Client", "Unexpected error occurred while parsing response.");
                return null;
            }

            Log.info("Client", "Response received: " + response.getStartLine());

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
            Log.error("Client", "Request/Response failed");
            disconnect(host, port);
            return null;
        }
    }

    /**
     * 连接管理
     */
    private static class Connection {
        private static final Pattern IPV4_PATTERN = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");

        private final String host;
        private final String resolvedHost;
        private final int port;
        private SocketChannel channel;
        @Getter @Setter
        private String cookie;

        public Connection(String host, int port) {
            this.host = host;
            this.port = port;
            this.resolvedHost = resolveHost(host);
        }

        public void connect() {
            try {
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(new InetSocketAddress(resolvedHost, port));
                long timeout = System.currentTimeMillis() + Config.CONNECTION_TIMEOUT;

                while (!channel.finishConnect()) {
                    if (System.currentTimeMillis() > timeout) {
                        Log.error("Connection", "Connection timeout while connecting to " + host + ":" + port);
                        close();
                        return;
                    }
                    Thread.sleep(50);
                }
                Log.info("Connection", "Connected to " + host + ":" + port);
            } catch (IOException e) {
                Log.error("Connection", "Failed to connect to " + host + ":" + port);
            } catch (InterruptedException e) {
                Log.error("Connection", "Thread interrupted while connecting to " + host + ":" + port, e);
                Thread.currentThread().interrupt();
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

        private String resolveHost(String host) {
            if (isIPAddress(host)) {
                Log.debug("Connection", "Using provided IP address directly: " + host);
                return host;
            }
            try {
                InetAddress address = InetAddress.getByName(host);
                Log.debug("Connection", "Resolved host " + host + " to IP " + address.getHostAddress());
                return address.getHostAddress();
            } catch (IOException e) {
                Log.error("Connection", "Failed to resolve host: " + host);
                throw new IllegalArgumentException("Invalid host: " + host);
            }
        }

        private boolean isIPAddress(String host) {
            return IPV4_PATTERN.matcher(host).matches();
        }

    }

}
