package cn.northpark.np5.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                    "/soft/**",
                    "/learning",
                    "/learning/**",
                    "/about",
                    "/sponsor",
                    "/sponsor/list",
                    "/notify/count",
                    "/notify/readNotify",
                    "/notifications",
                    "/notifications/page/**",
                    "/login",
                    "/signup",
                    "/api/v1/auth/forget",
                    "/api/v1/auth/**",
                    "/static/**",
                    "/css/**",
                    "/js/**",
                    "/img/**"
                ).permitAll()
                // 需要认证的路由（如评论、下载等，后台管理可以进一步配置）
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .permitAll()
            .and()
            .logout()
                .logoutSuccessUrl("/")
                .permitAll()
            .and()
            .sessionManagement()
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry());

        return http.build();
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