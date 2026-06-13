package cn.northpark.np5.config;

import cn.northpark.np5.service.impl.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                // 公开的路由
                .antMatchers(
                    "/",
                    "/movies",
                    "/movies/page/**",
                    "/movies/post-*.html",
                    "/movies/tag/**",
                    "/movies/date/**",
                    "/soft",
                    "/soft/mac",
                    "/soft/mac/page/**",
                    "/soft/tag/**",
                    "/soft/month/**",
                    "/soft/*.html",
                    "/learning",
                    "/learning/page/**",
                    "/learning/tag/**",
                    "/learning/*.html",
                    "/about",
                    "/sponsor",
                    "/sponsor/list",
                    "/topicComment/list",
                    "/notify/count",
                    "/notify/readNotify",
                    "/notifications",
                    "/notifications/page/**",
                    "/login",
                    "/signup",
                    "/auth/**",
                    "/api/v1/auth/forget",
                    "/api/v1/auth/**",
                    "/api/v1/remember-me/**",
                    "/static/**",
                    "/css/**",
                    "/js/**",
                    "/img/**"
                ).permitAll()
                // 管理操作需具有 ROLE_ADMIN
                .antMatchers(
                    "/soft/handup",
                    "/soft/hideup",
                    "/soft/edit/**",
                    "/movies/handup",
                    "/movies/hideup",
                    "/movies/edit/**",
                    "/learning/handup",
                    "/learning/hideup",
                    "/learning/edit/**",
                    "/remember-me-test"// 统计记住登录的信息
                ).hasRole("ADMIN")
                // 需要认证的路由（如评论、下载等，后台管理可以进一步配置）
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .permitAll()
            .and()
            .rememberMe()
                .rememberMeServices(rememberMeServices())
                .key("northpark-remember-me-key")
            .and()
            .logout()
                .logoutSuccessUrl("/")
                .deleteCookies("REMEMBER_ME")
                .permitAll()
            .and()
            .sessionManagement()
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry());

        http
            .exceptionHandling()
                .accessDeniedPage("/error?status=403");

        return http.build();
    }

    /**
     * 配置基于 Redis 的"记住我"服务
     */
    @Bean
    public RedisRememberMeServices rememberMeServices() {
        // 30天有效期
        return new RedisRememberMeServices(
            "northpark-remember-me-key",
            customUserDetailsService,
            redisTemplate,
            30 * 24 * 60 * 60
        );
    }

    /**
     * 密码编码器 - 由于项目使用自定义加密,这里使用 NoOp
     * 实际密码验证在 CustomUserDetailsService 中通过数据库比对完成
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}