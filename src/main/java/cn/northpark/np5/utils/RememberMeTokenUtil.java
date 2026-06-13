package cn.northpark.np5.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 记住我令牌管理工具类
 * 提供令牌查询、删除等管理功能
 */
@Component
@Slf4j
public class RememberMeTokenUtil {

    private static final String REMEMBER_ME_KEY_PREFIX = "remember_me:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取所有记住我令牌
     * @return 令牌列表，每个元素包含 series, username, token, ttl
     */
    public List<Map<String, Object>> getAllTokens() {
        List<Map<String, Object>> tokens = new ArrayList<>();
        Set<String> keys = redisTemplate.keys(REMEMBER_ME_KEY_PREFIX + "*");
        
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                String value = redisTemplate.opsForValue().get(key);
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                
                if (value != null) {
                    String[] parts = value.split(":", 2);
                    if (parts.length == 2) {
                        Map<String, Object> tokenInfo = new HashMap<>();
                        tokenInfo.put("series", key.substring(REMEMBER_ME_KEY_PREFIX.length()));
                        tokenInfo.put("username", parts[0]);
                        tokenInfo.put("token", parts[1]);
                        tokenInfo.put("ttl", ttl);
                        tokens.add(tokenInfo);
                    }
                }
            }
        }
        
        return tokens;
    }

    /**
     * 根据用户名获取该用户的所有令牌
     * @param username 用户名（邮箱）
     * @return 该用户的令牌列表
     */
    public List<Map<String, Object>> getTokensByUsername(String username) {
        List<Map<String, Object>> userTokens = new ArrayList<>();
        List<Map<String, Object>> allTokens = getAllTokens();
        
        for (Map<String, Object> token : allTokens) {
            if (username.equals(token.get("username"))) {
                userTokens.add(token);
            }
        }
        
        return userTokens;
    }

    /**
     * 删除指定 series 的令牌
     * @param series 令牌系列号
     * @return 是否删除成功
     */
    public boolean deleteTokenBySeries(String series) {
        String key = REMEMBER_ME_KEY_PREFIX + series;
        Boolean result = redisTemplate.delete(key);
        log.info("删除记住我令牌: series={}, result={}", series, result);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 删除指定用户的所有令牌
     * @param username 用户名（邮箱）
     * @return 删除的令牌数量
     */
    public int deleteTokensByUsername(String username) {
        List<Map<String, Object>> userTokens = getTokensByUsername(username);
        int count = 0;
        
        for (Map<String, Object> token : userTokens) {
            String series = (String) token.get("series");
            if (deleteTokenBySeries(series)) {
                count++;
            }
        }
        
        log.info("删除用户所有记住我令牌: username={}, count={}", username, count);
        return count;
    }

    /**
     * 清除所有过期的令牌（Redis 会自动清除，此方法用于手动触发）
     * @return 清除的数量
     */
    public int cleanExpiredTokens() {
        int count = 0;
        Set<String> keys = redisTemplate.keys(REMEMBER_ME_KEY_PREFIX + "*");
        
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (ttl != null && ttl <= 0) {
                    if (Boolean.TRUE.equals(redisTemplate.delete(key))) {
                        count++;
                    }
                }
            }
        }
        
        log.info("清除过期记住我令牌: count={}", count);
        return count;
    }

    /**
     * 获取记住我令牌总数
     * @return 令牌总数
     */
    public long getTokenCount() {
        Set<String> keys = redisTemplate.keys(REMEMBER_ME_KEY_PREFIX + "*");
        return keys != null ? keys.size() : 0;
    }
}