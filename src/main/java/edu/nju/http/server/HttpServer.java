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

/**
 * HttpServer - 基于 NIO 的 HTTP 服务器主类
 */
public class HttpServer {
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private boolean running = true;

    /**
     * 启动 HTTP 服务器
     */
    public void start() {
        try {
            // 初始化 Selector 和 ServerSocketChannel
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(Config.HOST, Config.PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            Log.info("Server", "Server started on " + Config.HOST + ":" + Config.PORT);

            while (running) {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
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
        ByteBuffer buffer = ByteBuffer.allocate(2048);

        try {
            int bytesRead = client.read(buffer);
            if (bytesRead == -1) {
                client.close();
                Log.info("Server", "Connection closed by client");
                return;
            }

            buffer.flip();
            String requestData = new String(buffer.array(), 0, buffer.limit());
            Log.debug("Server", "Request received: \n" + requestData);

            HttpRequest request = new HttpRequest(requestData);
            HttpResponse response = ServerHandler.handle(request);

            key.attach(response);
            key.interestOps(SelectionKey.OP_WRITE);
        } catch (IOException e) {
            Log.error("Server", "Error reading request", e);
            try {
                client.close();
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
            } else {
                key.interestOps(SelectionKey.OP_READ);
            }

            Log.info("Server", "Response sent to client");
        } catch (IOException e) {
            Log.error("Server", "Error sending response", e);
            try {
                client.close();
            } catch (IOException ex) {
                Log.error("Server", "Failed to close client connection", ex);
            }
        }
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        server.start();
    }
}
