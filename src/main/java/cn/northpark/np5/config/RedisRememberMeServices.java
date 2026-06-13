package cn.northpark.np5.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的"记住我"服务实现
 * 将记住我令牌存储在 Redis 中,安全且可扩展
 */
@Slf4j
public class RedisRememberMeServices extends AbstractRememberMeServices {

    private static final String REMEMBER_ME_KEY_PREFIX = "remember_me:";
    private static final int DEFAULT_TOKEN_VALIDITY_SECONDS = 30 * 24 * 60 * 60; // 30天
    
    private final StringRedisTemplate redisTemplate;
    private final int tokenValiditySeconds;

    public RedisRememberMeServices(String key, 
                                   UserDetailsService userDetailsService,
                                   StringRedisTemplate redisTemplate,
                                   int tokenValiditySeconds) {
        super(key, userDetailsService);
        this.redisTemplate = redisTemplate;
        this.tokenValiditySeconds = tokenValiditySeconds;
        // 设置 cookie 名称
        setCookieName("REMEMBER_ME");
        // 设置参数名
        setParameter("remember-me");
    }

    public RedisRememberMeServices(String key, 
                                   UserDetailsService userDetailsService,
                                   StringRedisTemplate redisTemplate) {
        this(key, userDetailsService, redisTemplate, DEFAULT_TOKEN_VALIDITY_SECONDS);
    }

    @Override
    protected void onLoginSuccess(HttpServletRequest request, 
                                   HttpServletResponse response,
                                   Authentication successfulAuthentication) {
        String username = successfulAuthentication.getName();
        log.info("创建记住我令牌: {}", username);

        // 生成唯一令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        String series = UUID.randomUUID().toString().replace("-", "");
        
        // 组合 cookie 值: series:token
        String cookieValue = encodeCookie(new String[]{series, token});
        
        // 存储到 Redis: key = series, value = username:token
        String redisKey = REMEMBER_ME_KEY_PREFIX + series;
        String redisValue = username + ":" + token;
        redisTemplate.opsForValue().set(redisKey, redisValue, tokenValiditySeconds, TimeUnit.SECONDS);
        
        // 设置 Cookie
        setCookie(new String[]{series, token}, tokenValiditySeconds, request, response);
        
        log.info("记住我令牌已创建并存储: series={}", series);
    }

    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, 
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        if (cookieTokens.length != 2) {
            throw new InvalidCookieException("Cookie 令牌格式错误 (应为 2 个令牌; 实际为 " + cookieTokens.length + ")");
        }

        String series = cookieTokens[0];
        String token = cookieTokens[1];
        
        String redisKey = REMEMBER_ME_KEY_PREFIX + series;
        String redisValue = redisTemplate.opsForValue().get(redisKey);
        
        if (redisValue == null) {
            throw new RememberMeAuthenticationException("记住我令牌不存在或已过期");
        }
        
        String[] parts = redisValue.split(":", 2);
        if (parts.length != 2) {
            throw new RememberMeAuthenticationException("Redis 中的令牌格式错误");
        }
        
        String username = parts[0];
        String storedToken = parts[1];
        
        // 验证令牌
        if (!token.equals(storedToken)) {
            // 令牌不匹配,删除该记录(可能是令牌被盗用)
            redisTemplate.delete(redisKey);
            throw new RememberMeAuthenticationException("令牌不匹配,可能存在安全风险");
        }
        
        log.info("记住我自动登录成功: {}", username);
        
        // 刷新令牌(生成新的 token,保持 series 不变)
        String newToken = UUID.randomUUID().toString().replace("-", "");
        String newRedisValue = username + ":" + newToken;
        redisTemplate.opsForValue().set(redisKey, newRedisValue, tokenValiditySeconds, TimeUnit.SECONDS);
        
        // 更新 Cookie
        setCookie(new String[]{series, newToken}, tokenValiditySeconds, request, response);
        
        return getUserDetailsService().loadUserByUsername(username);
    }

    @Override
    public void logout(HttpServletRequest request, 
                       HttpServletResponse response,
                       Authentication authentication) {
        super.logout(request, response, authentication);
        
        // 从 Cookie 中获取 series
        String rememberMeCookie = extractRememberMeCookie(request);
        if (rememberMeCookie != null) {
            try {
                String[] cookieTokens = decodeCookie(rememberMeCookie);
                if (cookieTokens.length >= 1) {
                    String series = cookieTokens[0];
                    String redisKey = REMEMBER_ME_KEY_PREFIX + series;
                    redisTemplate.delete(redisKey);
                    log.info("记住我令牌已删除: series={}", series);
                }
            } catch (Exception e) {
                log.warn("删除记住我令牌时出错", e);
            }
        }
    }
}