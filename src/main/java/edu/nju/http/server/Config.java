package edu.nju.http.server;

import edu.nju.http.message.constant.Status;
import edu.nju.http.utils.Log;
import edu.nju.http.utils.Searcher;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Config - 服务器配置类
 */
public class Config {
    // ================== 服务器基础信息 ==================
    public static final String SERVER_NAME;
    public static final String SERVER_VERSION;
    public static final String SERVER_SIGNATURE;

    // ================== 网络配置 ==================
    public static final String HOST;
    public static final int PORT;
    public static final boolean KEEP_ALIVE;
    public static final int TIMEOUT;
    public static final boolean THREAD_POOL;
    public static final int MAX_THREADS;
    public static final int MAX_CONNECTIONS;
    public static final int BUFFER_SIZE;

    // ================== 会话配置 ==================
    public static final int SESSION_EXPIRY_TIME;

    // ================== 缓存配置 ==================
    public static final boolean ENABLE_CACHE;
    public static final String CACHE_CONTROL;

    // ================== 默认资源配置 ==================
    public static final String DEFAULT_PAGE;
    public static final String DEFAULT_ENCODING;

    // ================== 资源路径 ==================
    public static final String STATIC_RESOURCE_DIR;
    public static final String USER_DIR;

    // ================== 配置文件路径 ==================
    private static final String CONFIG_FILE = "config/config.json";

    // ================== 数据存储路径 ==================
    public static final String DATA_DIR;

    // ================== 重定向规则 ==================
    public static class RedirectRule {
        public final String target;
        public final int statusCode;

        public RedirectRule(String target, int statusCode) {
            this.target = target;
            this.statusCode = statusCode;
        }
    }

    public static final Map<String, RedirectRule> REDIRECT_RULES = new HashMap<>();

    // ================== 日志设置 ==================
    public static final String LOG_DIR;
    public static final int LOG_LEVEL = 2; // 0: none, 1: info, 2: debug

    static {
        JSONObject configJson = null;
        try {
            Path configPath = Searcher.pathOf(CONFIG_FILE);
            if (configPath != null && configPath.toFile().exists()) {
                Log.debug("Config", "Loading configuration file: " + configPath);
                FileInputStream fis = new FileInputStream(configPath.toFile());
                configJson = new JSONObject(new JSONTokener(fis));
                fis.close();
            }
        } catch (IOException e) {
            Log.error("Config", "Failed to load configuration file: " + CONFIG_FILE, e);
        }

        JSONObject serverConfig;
        if(configJson == null){
            Log.debug("Config", "Configuration file not found. Using default settings.");
            serverConfig = new JSONObject();
        } else {
            JSONObject tmp = configJson.optJSONObject("server");
            serverConfig = tmp == null ? configJson : tmp;
        }

        SERVER_NAME = serverConfig.optString("server_name", "SimpleHttpServer");
        SERVER_VERSION = serverConfig.optString("server_version", "1.0");
        SERVER_SIGNATURE = SERVER_NAME + "/" + SERVER_VERSION;

        HOST = serverConfig.optString("host", "localhost");
        PORT = serverConfig.optInt("port", 8080);
        KEEP_ALIVE = serverConfig.optBoolean("keep_alive", true);
        TIMEOUT = serverConfig.optInt("timeout", 5000);
        THREAD_POOL = serverConfig.optBoolean("thread_pool", false);
        int cores = Runtime.getRuntime().availableProcessors();
        int maxThreads = serverConfig.optInt("max_threads", 4);
        if(maxThreads <= 0) maxThreads = 4;
        MAX_THREADS = Math.min(maxThreads, cores * 2);
        MAX_CONNECTIONS = serverConfig.optInt("max_connections", 1000);
        BUFFER_SIZE = serverConfig.optInt("buffer_size", 2048);

        SESSION_EXPIRY_TIME = serverConfig.optInt("session_expiry_time", 3600);

        ENABLE_CACHE = serverConfig.optBoolean("enable_cache", false);
        CACHE_CONTROL = serverConfig.optString("cache_control", "public,max-age=3600");

        DEFAULT_PAGE = serverConfig.optString("default_page", "index.html");
        DEFAULT_ENCODING = serverConfig.optString("default_encoding", "UTF-8");

        STATIC_RESOURCE_DIR = serverConfig.optString("static_resource_dir", "static");
        USER_DIR = serverConfig.optString("user_path", "user");
        DATA_DIR = serverConfig.optString("data_dir", "data");
        LOG_DIR = DATA_DIR + "/log";

        JSONArray redirects = serverConfig.optJSONArray("redirects");
        if (redirects != null) {
            for (int i = 0; i < redirects.length(); i++) {
                JSONObject rule = redirects.getJSONObject(i);
                String path = rule.optString("path");
                String target = rule.optString("target");
                int status = rule.optInt("status", Status.FOUND);

                if (!path.isEmpty() && !target.isEmpty()) {
                    REDIRECT_RULES.put(path, new RedirectRule(target, status));
                    Log.debug("Config","Loaded redirect rule: " + path + " -> " + target + " (" + status + ")");
                }
            }
        }
    }

}


