package edu.nju.http.message;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

import edu.nju.http.message.constant.*;

@Getter
public class HttpRequest extends HttpMessage {
    private String method;
    @Setter
    private String uri;

    /**
     * 使用成员参数构造 HTTP 请求
     * @param version  HTTP 协议版本
     * @param method   HTTP 请求方法
     * @param uri      请求 URI
     * @param headers   消息头部
     * @param body      消息体
     */
    public HttpRequest(String version, String method, String uri, Map<String, String> headers, byte[] body) {
        super(version, headers, body);
        this.method = method;
        this.uri = uri;
    }

    /**
     * 构造初始 HTTP 请求，需进一步进行自定义设置
     * @param version  HTTP 协议版本
     * @param method   HTTP 请求方法
     */
    public HttpRequest(String version, String method){
        super(version, new HashMap<>(), null);
        this.method = method;
    }

    /**
     * 使用原始消息构造 HTTP 请求
     * @param rawMessage 原始消息
     */
    public HttpRequest(byte[] rawMessage) {
        super(rawMessage);
    }

    public String getStartLine() {
        return method + " " + uri + " " + version;
    }

    public void setStartLine(String startLine) {
        String[] parts = startLine.split(" ");
        method = parts[0];
        uri = parts[1];
        version = parts[2];
    }

    public String getQuery() {
        if(method.equals(Method.GET)){
            int index = uri.indexOf('?');
            if (index == -1) {
                return "";
            }
            return uri.substring(index + 1);
        }else if(method.equals(Method.POST)){
            return getBodyAsString();
        }
        return "";
    }

    public void setTarget(String path) {
        uri = path + (uri.contains("?") ? uri.substring(uri.indexOf("?")) : "");
    }

    public String getTarget() {
        int index = uri.indexOf('?');
        if (index == -1) {
            return uri;
        }
        return uri.substring(0, index);
    }

}
