package cn.northpark.np5.utils;

import cn.northpark.np5.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限构建工具类
 * 统一管理用户权限的构建逻辑，避免代码重复
 */
public class AuthorityBuilder {

    /**
     * 管理员用户ID列表
     */
    private static final Integer[] ADMIN_USER_IDS = {507723, 508200};

    /**
     * 根据用户信息构建权限列表
     * 
     * @param user 用户对象
     * @return 权限列表
     */
    public static List<GrantedAuthority> buildAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 所有用户都有 ROLE_USER 权限
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // 判断是否是管理员
        if (isAdmin(user)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        
        return authorities;
    }

    /**
     * 判断用户是否是管理员
     * 
     * @param user 用户对象
     * @return 是否是管理员
     */
    public static boolean isAdmin(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        
        for (Integer adminId : ADMIN_USER_IDS) {
            if (adminId.equals(user.getId())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 根据用户ID判断是否是管理员
     * 
     * @param userId 用户ID
     * @return 是否是管理员
     */
    public static boolean isAdmin(Integer userId) {
        if (userId == null) {
            return false;
        }
        
        for (Integer adminId : ADMIN_USER_IDS) {
            if (adminId.equals(userId)) {
                return true;
            }
        }
        
        return false;
    }
}