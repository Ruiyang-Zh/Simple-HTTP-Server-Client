package edu.nju.http.server;

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
 * 外部资源配置直接加载，内部资源配置按 "server" 条目加载。
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
    public static final int MAX_THREADS;
    public static final int MAX_CONNECTIONS;

    // ================== 缓存配置 ==================
    public static final boolean ENABLE_CACHE;
    public static final String CACHE_CONTROL;

    // ================== 默认资源配置 ==================
    public static final String DEFAULT_PAGE;
    public static final String DEFAULT_ENCODING;
    public static final String DEFAULT_CONTENT_TYPE;

    // ================== 资源路径 ==================
    public static final String STATIC_RESOURCE_DIR;
    public static final String LOG_DIR;
    public static final String DATA_DIR;

    // ================== 重定向规则 ==================
    public static final Map<String, RedirectRule> REDIRECT_RULES = new HashMap<>();

    // ================== 日志级别 ==================
    public static final int LOG_LEVEL = 2; // 0: none, 1: info, 2: debug

    // ================== 配置文件路径 ==================
    private static final String CONFIG_FILE = "config/config.json";
    private static final String REDIRECT_CONFIG_FILE = "config/redirect.json";

    static {
        JSONObject configJson = null;
        boolean isExternal = false;

        try {
            // 优先加载外部配置
            Path externalPath = Searcher.getExternalPath(CONFIG_FILE);
            if (externalPath != null && externalPath.toFile().exists()) {
                Log.debug("Config", "Loading external configuration file: " + externalPath);
                FileInputStream fis = new FileInputStream(externalPath.toFile());
                configJson = new JSONObject(new JSONTokener(fis));
                fis.close();
                isExternal = true;
            } else {
                // 加载内部 resources 配置
                Log.debug("Config", "External configuration file not found. Loading internal configuration.");
                Path internalPath = Searcher.getInternalPath(CONFIG_FILE);
                if(internalPath == null) {
                    Log.debug("Config", "Internal configuration file not found.");
                } else {
                    Log.debug("Config", "Loading internal configuration file: " + internalPath);
                    FileInputStream fis = new FileInputStream(internalPath.toFile());
                    configJson = new JSONObject(new JSONTokener(fis));
                    fis.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file.", e);
        }

        JSONObject serverConfig = null;
        if(configJson == null){
            Log.warn("Config", "Configuration file not found. Using default settings.");
            serverConfig = new JSONObject();
        } else {
            serverConfig = isExternal ? configJson : configJson.optJSONObject("server");
        }

        SERVER_NAME = serverConfig.optString("server_name", "SimpleHttpServer");
        SERVER_VERSION = serverConfig.optString("server_version", "1.0");
        SERVER_SIGNATURE = SERVER_NAME + "/" + SERVER_VERSION;

        HOST = serverConfig.optString("host", "localhost");
        PORT = serverConfig.optInt("port", 8080);
        KEEP_ALIVE = serverConfig.optBoolean("keep_alive", true);
        TIMEOUT = serverConfig.optInt("timeout", 5000);
        MAX_THREADS = serverConfig.optInt("max_threads", 200);
        MAX_CONNECTIONS = serverConfig.optInt("max_connections", 1000);

        ENABLE_CACHE = serverConfig.optBoolean("enable_cache", false);
        CACHE_CONTROL = serverConfig.optString("cache_control", "public,max-age=3600");

        DEFAULT_PAGE = serverConfig.optString("default_page", "index.html");
        DEFAULT_ENCODING = serverConfig.optString("default_encoding", "UTF-8");
        DEFAULT_CONTENT_TYPE = serverConfig.optString("default_content_type", "text/html");

        STATIC_RESOURCE_DIR = serverConfig.optString("static_resource_dir", "static/");
        LOG_DIR = serverConfig.optString("log_dir", "log");
        DATA_DIR = serverConfig.optString("data_dir", "data");

        loadRedirectRules();

    }

    private static void loadRedirectRules() {
        try {
            Path redirectConfigPath = Searcher.pathOf(REDIRECT_CONFIG_FILE);
            if (redirectConfigPath != null && redirectConfigPath.toFile().exists()) {
                Log.debug("Config", "Loading redirect rules from: " + REDIRECT_CONFIG_FILE);
                FileInputStream fis = new FileInputStream(redirectConfigPath.toFile());
                JSONObject configJson = new JSONObject(new JSONTokener(fis));
                fis.close();

                JSONArray redirects = configJson.optJSONArray("redirects");
                if (redirects != null) {
                    for (int i = 0; i < redirects.length(); i++) {
                        JSONObject rule = redirects.getJSONObject(i);
                        String path = rule.optString("path");
                        String target = rule.optString("target");
                        int status = rule.optInt("status", 302);

                        if (!path.isEmpty() && !target.isEmpty()) {
                            REDIRECT_RULES.put(path, new RedirectRule(target, status));
                            Log.debug("Config","Loaded redirect rule: " + path + " -> " + target + " (" + status + ")");
                        }
                    }
                }
            } else {
                Log.debug("Config","Redirect configuration file not found.");
            }
        } catch (IOException e) {
            Log.error("Config","Failed to load redirect.json.");
        }
    }

    public static class RedirectRule {
        public final String target;
        public final int statusCode;

        public RedirectRule(String target, int statusCode) {
            this.target = target;
            this.statusCode = statusCode;
        }
    }

}


