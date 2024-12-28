package edu.nju.http.message;

import edu.nju.http.message.constant.Status;
import edu.nju.http.message.constant.Version;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class HttpResponse extends HttpMessage{
    private int statusCode;
    private String statusMessage;

    /**
     * 使用成员参数构造 HTTP 响应
     * @param version  HTTP 协议版本
     * @param statusCode   HTTP 状态码
     * @param headers   消息头部
     * @param body      消息体
     */
    public HttpResponse(String version, int statusCode, Map<String, String> headers, byte[] body) {
        super(version, headers, body);
        setStatus(statusCode);
    }

    /**
     * 构造初始 HTTP 响应，需进一步进行自定义设置
     * @param version  HTTP 协议版本
     * @param statusCode   HTTP 状态码
     */
    public HttpResponse(String version, int statusCode) {
        super(version, new HashMap<>(), null);
        setStatus(statusCode);
    }

    /**
     * 使用原始消息构造 HTTP 响应
     * @param rawMessage 原始消息
     */
    public HttpResponse(byte[] rawMessage) {
        super(rawMessage);
    }

    public String getStartLine() {
        return version + " " + statusCode + " " + statusMessage;
    }

    public void setStartLine(String startLine) {
        String[] parts = startLine.split(" ");
        version = parts[0];
        statusCode = Integer.parseInt(parts[1]);
        statusMessage = parts[2];
    }

    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
        this.statusMessage = Status.getStatusPhrase(statusCode);
    }

}
