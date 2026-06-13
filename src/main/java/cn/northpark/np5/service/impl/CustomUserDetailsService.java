package cn.northpark.np5.service.impl;

import cn.northpark.np5.model.User;
import cn.northpark.np5.service.UserService;
import cn.northpark.np5.utils.AuthorityBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自定义 UserDetailsService 实现
 * 用于 Spring Security 认证和"记住我"功能
 */
@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("加载用户: {}", email);
        
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("email", email.trim());
        User user = userService.getOne(query);
        
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + email);
        }
        
        if ("0".equals(user.getEmailFlag())) {
            throw new UsernameNotFoundException("邮箱未验证: " + email);
        }
        
        // 使用统一的权限构建工具
        List<GrantedAuthority> authorities = AuthorityBuilder.buildAuthorities(user);
        
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(user.getIsDel() != null && user.getIsDel() == 1)
                .build();
    }
}