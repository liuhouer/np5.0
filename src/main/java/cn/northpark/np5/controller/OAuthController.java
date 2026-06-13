package cn.northpark.np5.controller;

import cn.northpark.np5.config.RedisRememberMeServices;
import cn.northpark.np5.model.User;
import cn.northpark.np5.model.Result;
import cn.northpark.np5.service.OAuthService;
import cn.northpark.np5.utils.AuthorityBuilder;
import cn.northpark.np5.utils.ResultGenerator;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/auth")
public class OAuthController {

    @Autowired
    private OAuthService oauthService;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private RedisRememberMeServices rememberMeServices;

    /**
     * 获取Google授权URL
     */
    @RequestMapping("/google/url")
    @ResponseBody
    public Result<String> getGoogleAuthUrl(@RequestParam(required = false) String redirectURI) {
        try {
            String state = StringUtils.isNotEmpty(redirectURI) ? redirectURI : "/";
            String authUrl = oauthService.getGoogleAuthUrl(state);
            return ResultGenerator.genSuccessResult(authUrl);
        } catch (Exception e) {
            log.error("获取Google授权URL失败", e);
            return ResultGenerator.genErrorResult(500, "获取Google授权URL失败");
        }
    }

    /**
     * 获取GitHub授权URL
     */
    @RequestMapping("/github/url")
    @ResponseBody
    public Result<String> getGithubAuthUrl(@RequestParam(required = false) String redirectURI) {
        try {
            String state = StringUtils.isNotEmpty(redirectURI) ? redirectURI : "/";
            String authUrl = oauthService.getGithubAuthUrl(state);
            return ResultGenerator.genSuccessResult(authUrl);
        } catch (Exception e) {
            log.error("获取GitHub授权URL失败", e);
            return ResultGenerator.genErrorResult(500, "获取GitHub授权URL失败");
        }
    }

    /**
     * Google授权回调
     */
    @RequestMapping("/google/callback")
    public String googleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response,
            ModelMap map) {

        try {
            if (StringUtils.isNotEmpty(error)) {
                log.error("Google授权失败: {}", error);
                map.addAttribute("error", "Google授权失败");
                return "login";
            }

            // 处理Google回调
            User user = oauthService.handleGoogleCallback(code, state);
            if (user == null) {
                map.addAttribute("error", "Google登录失败");
                return "login";
            }

            // 注册登录状态
            registerSpringSecuritySession(user, request, response);

            log.info("Google登录成功: {}", user.getEmail());

            // 发送异步站长通知消息
            try {
                cn.northpark.np5.entity.NotifyRemind nr = new cn.northpark.np5.entity.NotifyRemind();
                nr.setMessage(user.toString() + "---" + LocalDateTime.now().toString() + "---通过Google登录了---");
                nr.setStatus("0");
                cn.northpark.np5.notify.NotifyEnum.WEBMASTER.getNotifyInstance().startSync(nr);
            } catch (Exception ig) {
                log.error("login-notice-has-ignored-------:", ig);
            }

            // 重定向到指定页面或首页
            String redirectUrl = StringUtils.isNotEmpty(state) && !state.equals("null") ? state : "/";
            return "redirect:" + redirectUrl;

        } catch (Exception e) {
            log.error("Google登录回调处理失败", e);
            map.addAttribute("error", "Google登录失败，请稍后重试");
            return "login";
        }
    }

    /**
     * GitHub授权回调
     */
    @RequestMapping("/github/callback")
    public String githubCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response,
            ModelMap map) {

        try {
            if (StringUtils.isNotEmpty(error)) {
                log.error("GitHub授权失败: {}", error);
                map.addAttribute("error", "GitHub授权失败");
                return "login";
            }

            // 处理GitHub回调
            User user = oauthService.handleGithubCallback(code, state);
            if (user == null) {
                map.addAttribute("error", "GitHub登录失败");
                return "login";
            }

            // 注册登录状态
            registerSpringSecuritySession(user, request, response);

            log.info("GitHub登录成功: {}", user.getEmail());

            // 发送异步站长通知消息
            try {
                cn.northpark.np5.entity.NotifyRemind nr = new cn.northpark.np5.entity.NotifyRemind();
                nr.setMessage(user.toString() + "---" + LocalDateTime.now().toString() + "---通过Github登录了---");
                nr.setStatus("0");
                cn.northpark.np5.notify.NotifyEnum.WEBMASTER.getNotifyInstance().startSync(nr);
            } catch (Exception ig) {
                log.error("login-notice-has-ignored-------:", ig);
            }

            // 重定向到指定页面或首页
            String redirectUrl = StringUtils.isNotEmpty(state) && !state.equals("null") ? state : "/";
            return "redirect:" + redirectUrl;

        } catch (Exception e) {
            log.error("GitHub登录回调处理失败", e);
            map.addAttribute("error", "GitHub登录失败，请稍后重试");
            return "login";
        }
    }

    /**
     * 直接跳转到Google授权页面
     */
    @RequestMapping("/google")
    public void redirectToGoogle(
            @RequestParam(required = false) String redirectURI,
            HttpServletResponse response) throws IOException {

        try {
            String state = StringUtils.isNotEmpty(redirectURI) ? redirectURI : "/";
            String authUrl = oauthService.getGoogleAuthUrl(state);
            response.sendRedirect(authUrl);
        } catch (Exception e) {
            log.error("跳转到Google授权页面失败", e);
            response.sendRedirect("/login?error=oauth_error");
        }
    }

    /**
     * 直接跳转到GitHub授权页面
     */
    @RequestMapping("/github")
    public void redirectToGithub(
            @RequestParam(required = false) String redirectURI,
            HttpServletResponse response) throws IOException {

        try {
            String state = StringUtils.isNotEmpty(redirectURI) ? redirectURI : "/";
            String authUrl = oauthService.getGithubAuthUrl(state);
            response.sendRedirect(authUrl);
        } catch (Exception e) {
            log.error("跳转到GitHub授权页面失败", e);
            response.sendRedirect("/login?error=oauth_error");
        }
    }

    /**
     * 在 Spring Security 中手动构建并注册 User Session 状态
     */
    private void registerSpringSecuritySession(User user, HttpServletRequest request, HttpServletResponse response) {
        List<GrantedAuthority> authorities = AuthorityBuilder.buildAuthorities(user);
        
        org.springframework.security.core.userdetails.User springUser = 
            new org.springframework.security.core.userdetails.User(user.getEmail(), "", authorities);
        UsernamePasswordAuthenticationToken authenticationToken = 
            new UsernamePasswordAuthenticationToken(springUser, null, authorities);
        
        HttpSession session = request.getSession(true);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        sessionRegistry.registerNewSession(session.getId(), springUser);

        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        session.setAttribute("user", user);

        // 默认让第三方登录的用户支持记住登录功能
        rememberMeServices.loginSuccess(request, response, authenticationToken);
    }
}