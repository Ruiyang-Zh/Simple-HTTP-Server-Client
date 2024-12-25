package edu.nju.http.message;

import edu.nju.http.message.constant.*;
import edu.nju.http.utils.Searcher;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class HttpMessage {
    @Getter
    protected String version; // HTTP 协议版本
    protected Map<String, String> headers; // HTTP 头部字段
    @Getter
    protected byte[] body; // HTTP 消息体

    /**
     *使用成员参数构造 HTTP 消息
     * @param version  HTTP 协议版本
     * @param headers   消息头部
     * @param body      消息体
     */
    public HttpMessage(String version, Map<String, String> headers, byte[] body) {
        this.version = version;
        this.headers = (headers != null) ? headers : new HashMap<>();
        setBody(body);
    }


    /**
     *使用原始消息构造 HTTP 消息
     * @param rawMessage 原始消息
     */
    public HttpMessage(String rawMessage) {
        this.headers = new HashMap<>();
        parseRawMessage(rawMessage);
    }

    // =============StartLine=================

    public abstract String getStartLine();

    public abstract void setStartLine(String startLine);

    // =============Header=================

    public String getHeaderVal(String key) {
        return headers.get(key);
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    public void removeHeader(String key) {
        headers.remove(key);
    }

    public Set<String> getHeaderKeys() {
        return headers.keySet();
    }

    public String getFormattedHeaders() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        return builder.toString();
    }

    // ==============Body================

    public void setBody(byte[] body) {
        this.body = body;
        if(body == null)
            return;
        setHeader(Header.Content_Length, String.valueOf(body.length));
    }

    public void setBody(byte[] body, String type) {
        setHeader(Header.Content_Type, MIME.getMimeType(type));
        setBody(body);
    }

    public void setBody(String resource){
        setBody(Searcher.pathOf(resource));
    }

    public void setBody(Path absolutePath) {
        if (!Files.exists(absolutePath)) {
            throw new IllegalArgumentException("File not found: " + absolutePath);
        }
        try {
            setBody(Files.readAllBytes(absolutePath), MIME.getMimeType(MIME.getFileExtension(absolutePath.toString())));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + absolutePath, e);
        }
    }

    // =============Utils=================

    @Override
    public String toString() {
        return getStartLine() + "\r\n" + getFormattedHeaders() + "\r\n" + new String(body);
    }

    private void parseRawMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            throw new IllegalArgumentException("Raw HTTP message cannot be null or empty");
        }
        int i = 0;
        String[] lines = rawMessage.split("\r\n");
        setStartLine(lines[i++]);
        while (i < lines.length && !lines[i].isEmpty()) {
            String line = lines[i];
            int colonIndex = line.indexOf(":");
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                setHeader(key, value);
            } else {
                throw new IllegalArgumentException("Invalid header format: " + line);
            }
            i++;
        }

        i++;
        StringBuilder bodyBuilder = new StringBuilder();
        while (i < lines.length) {
            bodyBuilder.append(lines[i]);
            bodyBuilder.append("\r\n");
            i++;
        }
        String body = bodyBuilder.toString().trim();
        setBody(body.getBytes());
    }

}
