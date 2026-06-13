package cn.northpark.np5.controller;

import cn.northpark.np5.config.RedisRememberMeServices;
import cn.northpark.np5.model.User;
import cn.northpark.np5.service.UserService;
import cn.northpark.np5.utils.AuthorityBuilder;
import cn.northpark.np5.utils.EmailUtils;
import cn.northpark.np5.utils.encrypt.NorthParkCryptUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private RedisRememberMeServices rememberMeServices;

    /**
     * 跳转至登录页面
     */
    @GetMapping("/login")
    public String loginPage(Model model, @RequestParam(value = "redirectURI", required = false) String redirectURI) {
        if (StringUtils.isNotBlank(redirectURI) && !redirectURI.equals("/login")) {
            model.addAttribute("redirectURI", redirectURI);
        } else {
            model.addAttribute("redirectURI", "/");
        }
        return "login";
    }

    /**
     * 获取当前活跃用户数
     */
    @GetMapping("/api/v1/auth/active-sessions")
    @ResponseBody
    public Map<String, Object> getActiveSessionsCount() {
        Map<String, Object> result = new HashMap<>();
        // 获取所有 SessionPrincipal
        List<Object> principals = sessionRegistry.getAllPrincipals();
        int activeUsers = 0;
        for (Object principal : principals) {
            activeUsers += sessionRegistry.getAllSessions(principal, false).size();
        }

        result.put("result", true);
        result.put("activeUsers", activeUsers);
        return result;
    }

    /**
     * 登录逻辑接口
     */
    @PostMapping("/api/v1/auth/login")
    @ResponseBody
    public Map<String, Object> login(HttpServletRequest request, HttpServletResponse response,
                                     @RequestParam("email") String email,
                                     @RequestParam("password") String password,
                                     @RequestParam(value = "remember-me", required = false) String rememberMe) {
        Map<String, Object> result = new HashMap<>();
        if (StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
            result.put("result", false);
            result.put("message", "参数错误");
            return result;
        }

        try {
            String encryptedPwd = NorthParkCryptUtils.northparkEncrypt(password);
            User user = userService.login(email.trim(), encryptedPwd);

            if (user != null) {
                if ("0".equals(user.getEmailFlag())) {
                    result.put("result", false);
                    result.put("message", "邮箱未通过验证，请联系站长。");
                    return result;
                }

                // 使用统一的权限构建工具
                List<GrantedAuthority> authorities = AuthorityBuilder.buildAuthorities(user);
                
                // 将 Principal 设置为当前 User 实体，以便 SessionRegistry 识别活跃 Principal
                org.springframework.security.core.userdetails.User springUser = 
                    new org.springframework.security.core.userdetails.User(user.getEmail(), "", authorities);
                UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(springUser, null, authorities);
                
                // 显式在 Session 开启前为当前 Session 注册登录事件，解决 sessionRegistry 追踪滞后问题
                HttpSession session = request.getSession(true);
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                sessionRegistry.registerNewSession(session.getId(), springUser);

                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                session.setAttribute("user", user);

                // 处理"记住我"功能
                if ("on".equals(rememberMe) || "true".equals(rememberMe)) {
                    log.info("用户选择了记住我功能: {}", email);
                    rememberMeServices.loginSuccess(request, response, authenticationToken);
                }

                // 更新最近登录信息
                user.setLastLogin(LocalDate.now().toString());
                userService.updateById(user);

                // 发送站长消息通知通知登录
                try {
                    cn.northpark.np5.entity.NotifyRemind nr = new cn.northpark.np5.entity.NotifyRemind();
                    nr.setMessage(user.toString() + "---" + java.time.LocalDateTime.now().toString() + "---登录了---");
                    nr.setStatus("0");
                    cn.northpark.np5.notify.NotifyEnum.WEBMASTER.getNotifyInstance().startSync(nr);
                } catch (Exception ex) {
                    log.error("login notice error", ex);
                }

                result.put("result", true);
                result.put("message", "登录成功");
                result.put("data", "/");
            } else {
                result.put("result", false);
                result.put("message", "账号或密码错误");
            }
        } catch (Exception e) {
            log.error("登录异常", e);
            result.put("result", false);
            result.put("message", "系统故障，请稍后重试");
        }
        return result;
    }

    /**
     * 跳转至注册页面
     */
    @GetMapping("/signup")
    public String signupPage(Model model, @RequestParam(value = "redirectURI", required = false) String redirectURI) {
        if (StringUtils.isNotBlank(redirectURI) && !redirectURI.equals("/login")) {
            model.addAttribute("redirectURI", redirectURI);
        } else {
            model.addAttribute("redirectURI", "/");
        }
        return "signup";
    }

    /**
     * 发送注册验证码邮件
     */
    @PostMapping("/api/v1/auth/register-email")
    @ResponseBody
    public Map<String, Object> sendRegisterEmail(@RequestParam("email") String email) {
        Map<String, Object> result = new HashMap<>();
        if (StringUtils.isBlank(email)) {
            result.put("result", false);
            result.put("message", "邮箱参数缺失");
            return result;
        }

        // 检查邮箱是否已注册
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("email", email.trim());
        if (userService.count(query) > 0) {
            result.put("result", false);
            result.put("message", "该邮箱已被注册");
            return result;
        }

        // 生成6位验证码并存入 Redis，有效期 5 分钟
        String code = String.format("%06d", new Random().nextInt(1000000));
        redisTemplate.opsForValue().set("email_verify:" + email.trim(), code, 5, TimeUnit.MINUTES);

        // 发送邮件
        try {
            EmailUtils.getInstance().sendEmail(email.trim(), "NorthPark 注册验证码", "您的注册验证码是：<b>" + code + "</b>，有效期为 5 分钟。");
            result.put("result", true);
            result.put("message", "验证邮件已发送");
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            result.put("result", false);
            result.put("message", "邮件发送失败，请重试");
        }
        return result;
    }

    /**
     * 校验邮箱验证码
     */
    @PostMapping("/api/v1/auth/verify-email-code")
    @ResponseBody
    public Map<String, Object> verifyEmailCode(@RequestParam("email") String email, @RequestParam("code") String code) {
        Map<String, Object> result = new HashMap<>();
        String redisKey = "email_verify:" + email.trim();
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isBlank(storedCode)) {
            result.put("result", false);
            result.put("message", "验证码已过期");
            return result;
        }

        if (storedCode.equals(code.trim())) {
            // 校验成功，在 Redis 存入已校验标记，10分钟有效
            redisTemplate.opsForValue().set("email_verified:" + email.trim(), "1", 10, TimeUnit.MINUTES);
            redisTemplate.delete(redisKey);
            result.put("result", true);
            result.put("message", "验证成功");
        } else {
            result.put("result", false);
            result.put("message", "验证码错误");
        }
        return result;
    }

    /**
     * 注册提交接口
     */
    @PostMapping("/api/v1/auth/signup")
    @ResponseBody
    public Map<String, Object> signup(HttpSession session, HttpServletRequest request,
                                      @RequestParam("email") String email,
                                      @RequestParam("password") String password) {
        Map<String, Object> result = new HashMap<>();
        if (StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
            result.put("result", false);
            result.put("message", "参数错误");
            return result;
        }

        // 校验是否已通过邮箱验证
        String verifiedKey = "email_verified:" + email.trim();
        if (Boolean.FALSE.equals(redisTemplate.hasKey(verifiedKey))) {
            result.put("result", false);
            result.put("message", "请先通过邮箱验证");
            return result;
        }

        // 检查是否重复注册
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("email", email.trim());
        if (userService.count(query) > 0) {
            result.put("result", false);
            result.put("message", "该邮箱已存在");
            return result;
        }

        try {
            User user = new User();
            user.setEmail(email.trim());
            user.setPassword(NorthParkCryptUtils.northparkEncrypt(password));
            user.setDateJoined(LocalDate.now().toString());
            user.setEmailFlag("1");
            user.setIsDel(0);

            // 获取默认用户名
            String username = email.contains("@") ? email.split("@")[0] : email;
            user.setUsername(username);
            user.setTailSlug(UUID.randomUUID().toString().substring(0, 8));

            userService.save(user);

            // 使用统一的权限构建工具
            List<GrantedAuthority> authorities = AuthorityBuilder.buildAuthorities(user);
            
            // 将 Principal 设置为当前 User 实体，以便 SessionRegistry 识别活跃 Principal
            org.springframework.security.core.userdetails.User springUser = 
                new org.springframework.security.core.userdetails.User(user.getEmail(), "", authorities);
            UsernamePasswordAuthenticationToken authenticationToken = 
                new UsernamePasswordAuthenticationToken(springUser, null, authorities);
            
            // 显式在 Session 开启前为当前 Session 注册登录事件，解决 sessionRegistry 追踪滞后问题
            HttpSession httpSession = request.getSession(true);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            sessionRegistry.registerNewSession(httpSession.getId(), springUser);

            httpSession.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            httpSession.setAttribute("user", user);

            redisTemplate.delete(verifiedKey);

            // 发送异步站长通知消息通知注册
            try {
                cn.northpark.np5.entity.NotifyRemind nr = new cn.northpark.np5.entity.NotifyRemind();
                nr.setMessage(user.toString() + "---" + java.time.LocalDateTime.now().toString() + "---注册了---");
                nr.setStatus("0");
                cn.northpark.np5.notify.NotifyEnum.WEBMASTER.getNotifyInstance().startSync(nr);
            } catch (Exception ex) {
                log.error("signup notice error", ex);
            }

            result.put("result", true);
            result.put("message", "注册成功");
            result.put("data", "注册成功");
        } catch (Exception e) {
            log.error("注册失败", e);
            result.put("result", false);
            result.put("message", "注册异常，请稍后重试");
        }
        return result;
    }

    /**
     * 跳转至找回密码页面
     */
    @GetMapping("/api/v1/auth/forget")
    public String forgetPage() {
        return "forget";
    }

    /**
     * 发送忘记密码验证码
     */
    @PostMapping("/api/v1/auth/forget-code")
    @ResponseBody
    public Map<String, Object> sendForgetCode(@RequestParam("email") String email) {
        Map<String, Object> result = new HashMap<>();
        if (StringUtils.isBlank(email)) {
            result.put("result", false);
            result.put("message", "请输入邮箱");
            return result;
        }

        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("email", email.trim());
        if (userService.count(query) == 0) {
            result.put("result", false);
            result.put("message", "该邮箱未注册");
            return result;
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        redisTemplate.opsForValue().set("forget_code:" + email.trim(), code, 5, TimeUnit.MINUTES);

        try {
            EmailUtils.getInstance().sendEmail(email.trim(), "NorthPark 重置密码", "您正在进行重置密码操作，验证码是：<b>" + code + "</b>，5分钟内有效。");
            result.put("result", true);
            result.put("data", "ok");
            result.put("message", "验证码已发送");
        } catch (Exception e) {
            log.error("重置验证码发送失败", e);
            result.put("result", false);
            result.put("message", "发送失败，请重试");
        }
        return result;
    }

    /**
     * 校验忘记密码验证码
     */
    @PostMapping("/api/v1/auth/verify-forget-code")
    @ResponseBody
    public Map<String, Object> verifyForgetCode(@RequestParam("email") String email, @RequestParam("code") String code) {
        Map<String, Object> result = new HashMap<>();
        String redisKey = "forget_code:" + email.trim();
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isBlank(storedCode)) {
            result.put("result", false);
            result.put("message", "验证码已失效");
            return result;
        }

        if (storedCode.equals(code.trim())) {
            redisTemplate.opsForValue().set("forget_verified:" + email.trim(), "1", 10, TimeUnit.MINUTES);
            redisTemplate.delete(redisKey);
            result.put("result", true);
            result.put("data", "ok");
            result.put("message", "验证成功");
        } else {
            result.put("result", false);
            result.put("message", "验证码错误");
        }
        return result;
    }

    /**
     * 重新设置密码
     */
    @PostMapping("/api/v1/auth/reset-password")
    @ResponseBody
    public Map<String, Object> resetPassword(@RequestParam("email") String email, @RequestParam("newPassword") String newPassword) {
        Map<String, Object> result = new HashMap<>();
        if (StringUtils.isBlank(email) || StringUtils.isBlank(newPassword)) {
            result.put("result", false);
            result.put("message", "参数缺失");
            return result;
        }

        String verifiedKey = "forget_verified:" + email.trim();
        if (Boolean.FALSE.equals(redisTemplate.hasKey(verifiedKey))) {
            result.put("result", false);
            result.put("message", "校验凭证已失效，请重新验证邮箱");
            return result;
        }

        try {
            QueryWrapper<User> query = new QueryWrapper<>();
            query.eq("email", email.trim());
            User user = userService.getOne(query);
            if (user != null) {
                user.setPassword(NorthParkCryptUtils.northparkEncrypt(newPassword));
                userService.updateById(user);
                redisTemplate.delete(verifiedKey);
                result.put("result", true);
                result.put("data", "ok");
                result.put("message", "密码重置成功");
            } else {
                result.put("result", false);
                result.put("message", "该邮箱未注册");
            }
        } catch (Exception e) {
            log.error("密码重置失败", e);
            result.put("result", false);
            result.put("message", "重置异常，请重试");
        }
        return result;
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionRegistry.removeSessionInformation(session.getId());
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    /**
     * 查询邮箱是否存在接口
     */
    @PostMapping("/api/v1/auth/email-flag")
    @ResponseBody
    public Map<String, Object> emailFlag(@RequestParam("email") String email) {
        Map<String, Object> result = new HashMap<>();
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("email", email.trim());
        if (userService.count(query) > 0) {
            result.put("result", true);
            result.put("data", "exist");
        } else {
            result.put("result", true);
            result.put("data", "not_exist");
        }
        return result;
    }
}