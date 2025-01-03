package edu.nju.http.message;

import edu.nju.http.message.constant.*;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
     * 构造初始 HTTP 消息，需进一步进行自定义设置
     */
    public HttpMessage() {
        this.version = Version.HTTP_1_1;
        this.headers = new HashMap<>();
        this.body = null;
    }

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
    public HttpMessage(byte[] rawMessage) {
        this.headers = new HashMap<>();
        parseRawMessage(rawMessage);
    }

    /**
     * 创建 HTTP 消息的副本
     * @param message 源 HTTP 消息
     */
    public HttpMessage(HttpMessage message) {
        this.version = message.version;
        this.headers = new HashMap<>(message.headers);
        this.body = new byte[message.body.length];
        System.arraycopy(message.body, 0, this.body, 0, message.body.length);
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

    private void setBody(byte[] body) {
        this.body = body;
        if(body == null)
            return;
        setHeader(Header.Content_Length, String.valueOf(body.length));
    }

    public void setBody(byte[] body, String type) {
        setHeader(Header.Content_Type, type == null ? MIME.DEFAULT_TYPE : type);
        setBody(body);
    }

    public void setBody(String plainText) {
        setBody(plainText.getBytes(), MIME.TEXT_PLAIN);
    }

    public void setBody(Path absolutePath) throws IOException {
        if (absolutePath == null || !Files.exists(absolutePath)) {
            throw new FileNotFoundException();
        }
        try (FileInputStream fis = new FileInputStream(absolutePath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] fileBytes = baos.toByteArray();
            setBody(fileBytes, MIME.getMimeType(MIME.getFileExtension(absolutePath.toString())));
        }
    }

    public String getBodyAsString() {
        return body == null ? "" : new String(body);
    }

    // =============Utils=================

    @Override
    public String toString() {
        String bodyType = getHeaderVal(Header.Content_Type);
        if(MIME.isTextType(bodyType == null ? "" : bodyType.split(";")[0])){
            return getStartLine() + "\r\n" + getFormattedHeaders() + "\r\n" + getBodyAsString();
        }
        return getStartLine() + "\r\n" + getFormattedHeaders() + "\r\n" + (body == null ? "" : getHeaderVal(Header.Content_Type) + ": "+ body.length + " bytes");
    }

    public byte[] toBytes() {
        byte[] headerBytes = (getStartLine() + "\r\n" + getFormattedHeaders() + "\r\n").getBytes();
        if(body == null){
            return headerBytes;
        }
        byte[] message = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, message, 0, headerBytes.length);
        System.arraycopy(body, 0, message, headerBytes.length, body.length);
        return message;
    }

    private void parseRawMessage(byte[] rawMessage) {
        if (rawMessage == null || rawMessage.length == 0) {
            throw new IllegalArgumentException("Raw HTTP message cannot be null or empty");
        }

        int headerEndIndex = findHeaderEnd(rawMessage);

        byte[] headerBytes = Arrays.copyOfRange(rawMessage, 0, headerEndIndex);
        String headers = new String(headerBytes, StandardCharsets.UTF_8);
        String[] lines = headers.split("\r\n");
        setStartLine(lines[0]);
        int i = 1;
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

        byte[] bodyBytes = Arrays.copyOfRange(rawMessage, headerEndIndex + 4, rawMessage.length);

        // 不完全的构造，消息体还未设置
        if(bodyBytes.length == 0 && getHeaderVal(Header.Content_Length) != null)
            return;

        setBody(bodyBytes);
    }

    public static int findHeaderEnd(byte[] rawMessage) {
        for (int i = 0; i < rawMessage.length - 3; i++) {
            if (rawMessage[i] == 0x0D && rawMessage[i + 1] == 0x0A && // \r\n
                    rawMessage[i + 2] == 0x0D && rawMessage[i + 3] == 0x0A) { // \r\n
                return i;
            }
        }
        return -1;
    }


}
