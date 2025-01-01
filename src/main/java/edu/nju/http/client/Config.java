package edu.nju.http.client;

import edu.nju.http.utils.Log;
import edu.nju.http.utils.Searcher;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Config - 客户端配置类
 */
public class Config {
    // ================== 客户端基础信息 ==================
    public static final String CLIENT_NAME;
    public static final String CLIENT_VERSION;
    public static final String USER_AGENT;

    // ================== 网络配置 ==================
    public static final boolean KEEP_ALIVE;
    public static final int CONNECTION_TIMEOUT;
    public static final int BUFFER_SIZE;

    // ================== 缓存配置 ==================
    public static final boolean ENABLE_CACHE;
    public static final int CACHE_MAX_AGE;
    public static final String CACHE_CONTROL;

    // ================== 日志配置 ==================
    public static final int LOG_LEVEL;
    public static final String LOG_DIR;

    // ================== 文件路径 ==================
    private static final String CONFIG_FILE = "config/config.json";
    public static final String DATA_DIR;

    // ================== 其他设置 ==================
    public static final int MAX_DISPLAY_SIZE;

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

        JSONObject clientConfig;
        if(configJson == null){
            Log.debug("Config", "Configuration file not found. Using default settings.");
            clientConfig = new JSONObject();
        } else {
            JSONObject tmp = configJson.optJSONObject("client");
            clientConfig = tmp == null ? configJson : tmp;
        }

        CLIENT_NAME = clientConfig.optString("client_name", "SimpleHttpClient");
        CLIENT_VERSION = clientConfig.optString("client_version", "1.0");
        USER_AGENT = CLIENT_NAME + "/" + CLIENT_VERSION;

        KEEP_ALIVE = clientConfig.optBoolean("keep_alive", true);
        CONNECTION_TIMEOUT = clientConfig.optInt("connection_timeout", 5000);
        BUFFER_SIZE = clientConfig.optInt("buffer_size", 2048);

        ENABLE_CACHE = clientConfig.optBoolean("enable_cache", true);
        CACHE_MAX_AGE = clientConfig.optInt("cache_max_age", 3600);
        CACHE_CONTROL = clientConfig.optString("cache_control", "public, max-age=3600");

        LOG_LEVEL = clientConfig.optInt("log_level", 1);
        LOG_DIR = clientConfig.optString("log_dir", "logs");

        DATA_DIR = clientConfig.optString("data_dir", "data");

        MAX_DISPLAY_SIZE = clientConfig.optInt("max_display_size", 1024);
    }
}
