package edu.nju.http.server;

import edu.nju.http.message.HttpRequest;
import edu.nju.http.message.HttpResponse;
import edu.nju.http.message.constant.Header;
import edu.nju.http.message.constant.Status;
import edu.nju.http.utils.Log;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UserSystem - 处理用户注册、登录和会话管理
 */
public class UserSystem {
    public static final UserSystem INSTANCE = new UserSystem();

    private UserSystem() {}

    public static UserSystem getInstance() {
        return INSTANCE;
    }

    // username -> password
    private final Map<String, String> users = new ConcurrentHashMap<>();

    // sessionId -> session
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * 处理用户注册请求
     * @param request HTTP请求
     * @return HTTP响应
     */
    public HttpResponse register(HttpRequest request) {
        String query = request.getQuery();
        Map<String, String> params = parseQuery(query);
        String username = params.get("username");
        String password = params.get("password");

        Log.debug("UserSystem", "Attempting to register user: " + username);

        // 验证输入
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return ResponseBuilder.createErrorResponse(request.getVersion(), Status.BAD_REQUEST);
        }

        // 检查用户名是否已存在
        if (users.containsKey(username)) {
            return ResponseBuilder.createErrorResponse(request.getVersion(), Status.CONFLICT);
        }

        // 存储用户
        users.put(username, password);

        Log.info("UserSystem", "Register success.: " + username);

        return ResponseBuilder.createSuccessResponse(request.getVersion(), "Register success.\n username: " + username);
    }

    /**
     * 处理用户登录请求
     * @param request HTTP请求
     * @return HTTP响应
     */
    public HttpResponse login(HttpRequest request) {
        String query = request.getQuery();
        Map<String, String> params = parseQuery(query);
        String username = params.get("username");
        String password = params.get("password");

        Log.debug("UserSystem", "Attempting to login user: " + username);

        // 验证输入
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return ResponseBuilder.createErrorResponse(request.getVersion(), Status.BAD_REQUEST);
        }

        // 检查用户是否存在
        String storedPassword = users.get(username);
        if (storedPassword == null) {
            return ResponseBuilder.createErrorResponse(request.getVersion(), Status.UNAUTHORIZED);
        }

        // 验证密码
        if (!storedPassword.equals(password)) {
            return ResponseBuilder.createErrorResponse(request.getVersion(), Status.UNAUTHORIZED);
        }

        Log.info("UserSystem", "Login : " + username);

        // 生成会话ID
        String sessionId = UUID.randomUUID().toString();
        long expiryTime = System.currentTimeMillis() + Config.SESSION_EXPIRY_TIME * 1000L;
        sessions.put(sessionId, new Session(username, expiryTime));

        // 构建成功响应，设置Cookie
        HttpResponse response = ResponseBuilder.createSuccessResponse(request.getVersion(), "Login success.\n username: " + username);
        setCookie(response, sessionId, Config.SESSION_EXPIRY_TIME);
        return response;
    }

    /**
     * 处理用户登出请求
     * @param request HTTP请求
     * @return HTTP响应
     */
    public HttpResponse logout(HttpRequest request) {
        String cookieHeader = request.getHeaderVal(Header.Cookie);
        if (cookieHeader == null) {
            return ResponseBuilder.createErrorResponse(request.getVersion(), Status.BAD_REQUEST);
        }

        String[] cookies = cookieHeader.split(";");
        boolean found = false;
        for (String cookie : cookies) {
            String[] keyValue = cookie.trim().split("=", 2);
            if (keyValue.length == 2 && "sessionId".equals(keyValue[0])) {
                String sessionId = keyValue[1];
                Session session = sessions.remove(sessionId);
                if (session != null) {
                    Log.info("UserSystem", "Logout sessionId: " + sessionId);
                }
                found = true;
                break;
            }
        }

        if (!found) {
            return ResponseBuilder.createErrorResponse(request.getVersion(), Status.BAD_REQUEST);
        }

        // 设置一个已过期 Cookie，让客户端清除本地Cookie
        HttpResponse response = ResponseBuilder.createSuccessResponse(request.getVersion(), "Logout success.");
        setCookie(response, "", 0);
        return response;
    }


    /**
     * 验证会话
     * @param request HTTP请求
     * @return 用户名，如果会话有效；否则，null
     */
    public String validateSession(HttpRequest request) {
        String cookieHeader = request.getHeaderVal(Header.Cookie);
        if (cookieHeader == null) {
            return null;
        }

        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] keyValue = cookie.trim().split("=");
            if (keyValue.length == 2 && "sessionId".equals(keyValue[0])) {
                String sessionId = keyValue[1];
                Session session = sessions.get(sessionId);
                if (session != null) {
                    if (System.currentTimeMillis() <= session.getExpiryTime()) {
                        return session.getUsername();
                    } else {
                        sessions.remove(sessionId);
                        Log.info("UserSystem", "Session expired: " + sessionId);
                        return null;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 解析表单数据
     * @param query 表单数据
     * @return 参数键值对
     */
    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new ConcurrentHashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] value = pair.split("=");
            if (value.length == 2) {
                params.put(value[0], value[1]);
            }
        }
        return params;
    }

    private void setCookie(HttpResponse response, String sessionId, int expiryTime) {
        String cookie = String.format(
                "sessionId=%s; HttpOnly; Path=/; Max-Age=%d",
                sessionId,
               expiryTime
        );
        response.setHeader(Header.Set_Cookie, cookie);
    }

    /**
     * Session
     */
    @Getter
    private class Session {
        private final String username;
        private final long expiryTime;

        public Session(String username, long expiryTime) {
            this.username = username;
            this.expiryTime = expiryTime;
        }

    }


}
