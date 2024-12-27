package edu.nju.http.server;

import edu.nju.http.message.HttpRequest;
import edu.nju.http.message.HttpResponse;
import edu.nju.http.utils.Log;
import edu.nju.http.message.constant.Header;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HttpServer
 */
public class HttpServer {
    private final String HOST;
    private final int PORT;
    private final ExecutorService threadPool;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private boolean running = true;

    public HttpServer() {
        this(Config.HOST, Config.PORT);
    }

    public HttpServer(String host, int port) {
        if(Config.THREAD_POOL) {
            threadPool = Executors.newFixedThreadPool(Config.MAX_THREADS);
        } else {
            threadPool = null;
        }
        this.HOST = host;
        this.PORT = port;
    }

    /**
     * 启动 HTTP 服务器
     */
    public void start() {
        try {
            // 初始化 Selector 和 ServerSocketChannel
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(HOST, PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            Log.info("Server", "Server started on " + HOST + ":" + PORT);

            while (running) {
                selector.select(Config.TIMEOUT);

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    try {
                        if (key.isAcceptable()) {
                            accept(key);
                        } else if (key.isReadable()) {
                            read(key);
                        } else if (key.isWritable()) {
                            write(key);
                        }
                    } catch (Exception e) {
                        Log.error("Server", "Error handling key: " + key, e);
                        key.cancel();
                    }
                }
            }
        } catch (IOException e) {
            Log.error("Server", "Server encountered an error", e);
        } finally {
            stop();
        }
    }

    /**
     * 停止服务器
     */
    public void stop() {
        try {
            running = false;
            if (selector != null) selector.close();
            if (serverChannel != null) serverChannel.close();
            if (threadPool != null) threadPool.shutdown();
            Log.info("Server", "Server stopped");
        } catch (IOException e) {
            Log.error("Server", "Error stopping server", e);
        }
    }

    /**
     * 接受新的客户端连接
     */
    private void accept(SelectionKey key) {
        try {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            Log.info("Server", "Accepted connection from " + client.getRemoteAddress());
        } catch (IOException e) {
            Log.error("Server", "Failed to accept connection", e);
        }
    }

    /**
     * 读取客户端请求
     */
    private void read(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(Config.BUFFER_SIZE);
        StringBuilder requestBuilder = new StringBuilder();

        try {
            int bytesRead;
            while ((bytesRead = client.read(buffer)) > 0) {
                buffer.flip();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                requestBuilder.append(new String(data, Config.DEFAULT_ENCODING));
                buffer.clear();
            }

            if (bytesRead == -1) {
                client.close();
                Log.info("Server", "Connection closed by client");
                return;
            }

            String requestData = requestBuilder.toString();
            Log.debug("Server", "Request received: \n" + requestData);

            if(threadPool != null) {
                threadPool.execute(() -> processRequest(key, requestData));
            } else {
                processRequest(key, requestData);
            }
        } catch (IOException e) {
            Log.error("Server", "Error reading request", e);
            try {
                client.close();
                Log.info("Server", "Connection closed by server");
            } catch (IOException ex) {
                Log.error("Server", "Failed to close client connection", ex);
            }
        }
    }

    /**
     * 向客户端写回响应
     */
    private void write(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        HttpResponse response = (HttpResponse) key.attachment();

        try {
            ByteBuffer buffer = ByteBuffer.wrap(response.toString().getBytes());
            while (buffer.hasRemaining()) {
                client.write(buffer);
            }

            if (!Config.KEEP_ALIVE || !"keep-alive".equalsIgnoreCase(response.getHeaderVal(Header.Connection))) {
                client.close();
                Log.info("Server", "Connection closed by server");
            } else {
                key.interestOps(SelectionKey.OP_READ);
            }

            Log.info("Server", "Response sent to client");
        } catch (IOException e) {
            Log.error("Server", "Error sending response", e);
            try {
                client.close();
                Log.info("Server", "Connection closed by server");
            } catch (IOException ex) {
                Log.error("Server", "Failed to close client connection", ex);
            }
        }
    }

    private void processRequest (SelectionKey key, String requestData) {
        HttpRequest request = new HttpRequest(requestData);
        HttpResponse response = ServerHandler.handle(request);
        key.attach(response);
        key.interestOps(SelectionKey.OP_WRITE);
    }


    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        Log.init(Config.LOG_LEVEL);
        server.start();
    }
}
