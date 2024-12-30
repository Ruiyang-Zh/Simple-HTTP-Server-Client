package edu.nju.http.client;

import edu.nju.http.message.HttpRequest;
import edu.nju.http.message.HttpResponse;
import edu.nju.http.message.constant.Header;

import java.util.HashMap;
import java.util.Map;

public class Cache {
    private final Map<String, CachedEntry> cache = new HashMap<>();

    /**
     * 生成缓存 Key
     */
    private String generateCacheKey(HttpRequest request) {
        String method = request.getMethod();
        String host = request.getHeaderVal(Header.Host);
        String uri = request.getTarget();
        return method + "|" + host + "|" + uri;
    }

    /**
     * 获取缓存的响应
     */
    public HttpResponse get(HttpRequest request) {
        String key = generateCacheKey(request);
        CachedEntry entry = cache.get(key);
        return (entry != null) ? entry.getResponse() : null;
    }

    /**
     * 存储缓存
     */
    public void put(HttpRequest request, HttpResponse response) {
        String key = generateCacheKey(request);
        cache.put(key, new CachedEntry(response));
    }

    /**
     * 检查缓存是否存在
     */
    public boolean contains(HttpRequest request) {
        return cache.containsKey(generateCacheKey(request));
    }

    /**
     * 检查缓存是否有效
     */
    public boolean isValid(HttpRequest request) {
        String key = generateCacheKey(request);
        CachedEntry entry = cache.get(key);
        if (entry == null) return false;
        return !entry.isExpired();
    }

    /**
     * 更新缓存有效期
     */
    public void update(HttpRequest request) {
        String key = generateCacheKey(request);
        CachedEntry entry = cache.get(key);
        if (entry != null) {
            entry.update();
        }
    }

    /**
     * 缓存条目
     */
    private static class CachedEntry {
        private final HttpResponse response;
        private long expiryTime;

        public CachedEntry(HttpResponse response) {
            this.response = response;
            this.expiryTime = calculateNewExpiryTime();
        }

        public HttpResponse getResponse() {
            return new HttpResponse(response);
        }

        public void update() {
            expiryTime = calculateNewExpiryTime();
        }

        public boolean isExpired() {
            return expiryTime > 0 && System.currentTimeMillis() > expiryTime;
        }

        private long calculateNewExpiryTime() {
            String cacheControl = response.getHeaderVal(Header.Cache_Control);
            if (cacheControl != null && cacheControl.contains("max-age")) {
                try {
                    int maxAge = Integer.parseInt(cacheControl.split("=")[1]);
                    return System.currentTimeMillis() + maxAge * 1000L;
                } catch (NumberFormatException e) {
                    return Config.CACHE_MAX_AGE;
                }
            } else {
                return Config.CACHE_MAX_AGE;
            }

        }
    }
}
