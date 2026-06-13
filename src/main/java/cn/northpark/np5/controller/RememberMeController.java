package cn.northpark.np5.controller;

import cn.northpark.np5.utils.RememberMeTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记住我功能测试和管理控制器
 */
@Controller
@RequestMapping("/api/v1/remember-me")
@Slf4j
public class RememberMeController {

    @Autowired
    private RememberMeTokenUtil rememberMeTokenUtil;

    /**
     * 获取当前登录状态
     */
    @GetMapping("/status")
    @ResponseBody
    public Map<String, Object> getLoginStatus() {
        Map<String, Object> result = new HashMap<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            result.put("result", true);
            result.put("authenticated", true);
            result.put("username", auth.getName());
            result.put("authorities", auth.getAuthorities());
        } else {
            result.put("result", true);
            result.put("authenticated", false);
        }
        
        return result;
    }

    /**
     * 获取所有记住我令牌（管理员功能）
     */
    @GetMapping("/tokens")
    @ResponseBody
    public Map<String, Object> getAllTokens() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> tokens = rememberMeTokenUtil.getAllTokens();
            result.put("result", true);
            result.put("count", tokens.size());
            result.put("tokens", tokens);
        } catch (Exception e) {
            log.error("获取令牌列表失败", e);
            result.put("result", false);
            result.put("message", "获取失败");
        }
        return result;
    }

    /**
     * 根据用户名查询令牌
     */
    @GetMapping("/tokens/user/{username}")
    @ResponseBody
    public Map<String, Object> getTokensByUsername(@PathVariable String username) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> tokens = rememberMeTokenUtil.getTokensByUsername(username);
            result.put("result", true);
            result.put("username", username);
            result.put("count", tokens.size());
            result.put("tokens", tokens);
        } catch (Exception e) {
            log.error("查询用户令牌失败", e);
            result.put("result", false);
            result.put("message", "查询失败");
        }
        return result;
    }

    /**
     * 删除指定令牌
     */
    @DeleteMapping("/tokens/{series}")
    @ResponseBody
    public Map<String, Object> deleteToken(@PathVariable String series) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = rememberMeTokenUtil.deleteTokenBySeries(series);
            result.put("result", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            log.error("删除令牌失败", e);
            result.put("result", false);
            result.put("message", "删除失败");
        }
        return result;
    }

    /**
     * 删除指定用户的所有令牌
     */
    @DeleteMapping("/tokens/user/{username}")
    @ResponseBody
    public Map<String, Object> deleteUserTokens(@PathVariable String username) {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = rememberMeTokenUtil.deleteTokensByUsername(username);
            result.put("result", true);
            result.put("count", count);
            result.put("message", "删除了 " + count + " 个令牌");
        } catch (Exception e) {
            log.error("删除用户令牌失败", e);
            result.put("result", false);
            result.put("message", "删除失败");
        }
        return result;
    }

    /**
     * 获取令牌统计信息
     */
    @GetMapping("/stats")
    @ResponseBody
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            long count = rememberMeTokenUtil.getTokenCount();
            result.put("result", true);
            result.put("totalTokens", count);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            result.put("result", false);
            result.put("message", "获取失败");
        }
        return result;
    }
}