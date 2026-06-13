package cn.northpark.np5.service.impl;

import cn.northpark.np5.config.OAuthConfig;
import cn.northpark.np5.mapper.UserMapper;
import cn.northpark.np5.model.User;
import cn.northpark.np5.service.OAuthService;
import cn.northpark.np5.utils.PinyinUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Service
public class OAuthServiceImpl implements OAuthService {

    @Autowired
    private OAuthConfig oauthConfig;

    @Autowired
    private UserMapper userMapper;

    @Override
    public String getGoogleAuthUrl(String state) {
        try {
            if (StringUtils.isEmpty(state)) {
                state = UUID.randomUUID().toString();
            }

            StringBuilder url = new StringBuilder();
            url.append(oauthConfig.getGoogle().getAuthUrl())
               .append("?client_id=").append(oauthConfig.getGoogle().getClientId())
               .append("&redirect_uri=").append(URLEncoder.encode(oauthConfig.getGoogle().getRedirectUri(), "UTF-8"))
               .append("&scope=").append(URLEncoder.encode(oauthConfig.getGoogle().getScope(), "UTF-8"))
               .append("&response_type=code")
               .append("&state=").append(state);

            return url.toString();
        } catch (UnsupportedEncodingException e) {
            log.error("构建Google授权URL失败", e);
            throw new RuntimeException("构建授权URL失败");
        }
    }

    @Override
    public String getGithubAuthUrl(String state) {
        try {
            if (StringUtils.isEmpty(state)) {
                state = UUID.randomUUID().toString();
            }

            StringBuilder url = new StringBuilder();
            url.append(oauthConfig.getGithub().getAuthUrl())
               .append("?client_id=").append(oauthConfig.getGithub().getClientId())
               .append("&redirect_uri=").append(URLEncoder.encode(oauthConfig.getGithub().getRedirectUri(), "UTF-8"))
               .append("&scope=").append(URLEncoder.encode(oauthConfig.getGithub().getScope(), "UTF-8"))
               .append("&state=").append(state);

            return url.toString();
        } catch (UnsupportedEncodingException e) {
            log.error("构建GitHub授权URL失败", e);
            throw new RuntimeException("构建授权URL失败");
        }
    }

    @Override
    public User handleGoogleCallback(String code, String state) {
        try {
            // 1. 用授权码换取access_token
            String accessToken = getGoogleAccessToken(code);
            if (StringUtils.isEmpty(accessToken)) {
                throw new RuntimeException("获取Google访问令牌失败");
            }

            // 2. 用access_token获取用户信息
            JSONObject userInfo = getGoogleUserInfo(accessToken);
            if (userInfo == null) {
                throw new RuntimeException("获取Google用户信息失败");
            }

            String googleId = userInfo.getStr("id");
            String email = userInfo.getStr("email");

            // 3. 检查用户是否已存在（通过Google ID）
            User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("google_id", googleId));
            if (existingUser != null) {
                // 更新最后登录时间和用户信息
                updateUserFromGoogle(existingUser, userInfo);
                return existingUser;
            }

            // 4. 检查邮箱是否已被其他账号使用
            if (StringUtils.isNotEmpty(email)) {
                User emailUser = userMapper.selectOne(new QueryWrapper<User>().eq("email", email));
                if (emailUser != null) {
                    // 邮箱已存在，将Google信息合并到已有账户
                    log.info("邮箱{}已存在，将Google信息合并到已有账户ID:{}", email, emailUser.getId());
                    mergeGoogleInfoToExistingUser(emailUser, userInfo);
                    return emailUser;
                }
            }

            // 5. 创建新用户
            User newUser = createUserFromGoogle(userInfo);
            userMapper.insert(newUser);

            return newUser;

        } catch (Exception e) {
            log.error("处理Google回调失败", e);
            throw new RuntimeException("Google登录失败：" + e.getMessage());
        }
    }

    @Override
    public User handleGithubCallback(String code, String state) {
        try {
            // 1. 用授权码换取access_token
            String accessToken = getGithubAccessToken(code);
            if (StringUtils.isEmpty(accessToken)) {
                throw new RuntimeException("获取GitHub访问令牌失败");
            }

            // 2. 用access_token获取用户信息
            JSONObject userInfo = getGithubUserInfo(accessToken);
            if (userInfo == null) {
                throw new RuntimeException("获取GitHub用户信息失败");
            }

            // 3. 获取用户邮箱
            String email = getGithubUserEmail(accessToken);
            if (StringUtils.isNotEmpty(email)) {
                userInfo.set("email", email);
            }

            String githubId = userInfo.getStr("id");

            // 4. 检查用户是否已存在（通过GitHub ID）
            User existingUser = findUserByGithubId(githubId);
            if (existingUser != null) {
                // 更新最后登录时间和用户信息
                updateUserFromGithub(existingUser, userInfo);
                return existingUser;
            }

            // 5. 检查邮箱是否已被其他账号使用
            if (StringUtils.isNotEmpty(email)) {
                User emailUser = userMapper.selectOne(new QueryWrapper<User>().eq("email", email));
                if (emailUser != null) {
                    // 邮箱已存在，将GitHub信息合并到已有账户
                    log.info("邮箱{}已存在，将GitHub信息合并到已有账户ID:{}", email, emailUser.getId());
                    mergeGithubInfoToExistingUser(emailUser, userInfo);
                    return emailUser;
                }
            }

            // 6. 创建新用户
            User newUser = createUserFromGithub(userInfo);
            userMapper.insert(newUser);

            return newUser;

        } catch (Exception e) {
            log.error("处理GitHub回调失败", e);
            throw new RuntimeException("GitHub登录失败：" + e.getMessage());
        }
    }

    private String getGoogleAccessToken(String code) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("client_id", oauthConfig.getGoogle().getClientId());
            params.put("client_secret", oauthConfig.getGoogle().getClientSecret());
            params.put("code", code);
            params.put("grant_type", "authorization_code");
            params.put("redirect_uri", oauthConfig.getGoogle().getRedirectUri());

            String body = HttpUtil.post(oauthConfig.getGoogle().getTokenUrl(), params);
            JSONObject json = JSONUtil.parseObj(body);
            return json.getStr("access_token");
        } catch (Exception e) {
            log.error("获取Google访问令牌失败", e);
            return null;
        }
    }

    private JSONObject getGoogleUserInfo(String accessToken) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + accessToken);

            String body = HttpUtil.createGet(oauthConfig.getGoogle().getUserInfoUrl())
                    .addHeaders(headers)
                    .execute()
                    .body();
            return JSONUtil.parseObj(body);
        } catch (Exception e) {
            log.error("获取Google用户信息失败", e);
            return null;
        }
    }

    private String getGithubAccessToken(String code) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("client_id", oauthConfig.getGithub().getClientId());
            params.put("client_secret", oauthConfig.getGithub().getClientSecret());
            params.put("code", code);

            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");

            String body = HttpUtil.createPost(oauthConfig.getGithub().getTokenUrl())
                    .form(params)
                    .addHeaders(headers)
                    .execute()
                    .body();
            JSONObject json = JSONUtil.parseObj(body);
            return json.getStr("access_token");
        } catch (Exception e) {
            log.error("获取GitHub访问令牌失败", e);
            return null;
        }
    }

    private JSONObject getGithubUserInfo(String accessToken) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + accessToken);
            headers.put("User-Agent", "NorthPark-App");

            String body = HttpUtil.createGet(oauthConfig.getGithub().getUserInfoUrl())
                    .addHeaders(headers)
                    .execute()
                    .body();
            return JSONUtil.parseObj(body);
        } catch (Exception e) {
            log.error("获取GitHub用户信息失败", e);
            return null;
        }
    }

    private String getGithubUserEmail(String accessToken) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + accessToken);
            headers.put("User-Agent", "NorthPark-App");

            String body = HttpUtil.createGet(oauthConfig.getGithub().getEmailUrl())
                    .addHeaders(headers)
                    .execute()
                    .body();
            JSONArray emails = JSONUtil.parseArray(body);
            for (int i = 0; i < emails.size(); i++) {
                JSONObject emailObj = emails.getJSONObject(i);
                if (emailObj.getBool("primary", false)) {
                    return emailObj.getStr("email");
                }
            }
            return null;
        } catch (Exception e) {
            log.error("获取GitHub用户邮箱失败", e);
            return null;
        }
    }

    @Override
    public User findUserByGithubId(String githubId) {
        try {
            return userMapper.selectOne(new QueryWrapper<User>().eq("github_id", githubId));
        } catch (Exception e) {
            log.error("根据GitHub ID查找用户失败", e);
            return null;
        }
    }

    private User createUserFromGoogle(JSONObject userInfo) {
        User user = new User();
        user.setGoogleId(userInfo.getStr("id"));
        user.setGoogleInfo(userInfo.toString());
        user.setEmail(userInfo.getStr("email"));
        user.setUsername(userInfo.getStr("name"));
        user.setRealName(userInfo.getStr("name"));
        user.setAvatarUrl(userInfo.getStr("picture"));
        user.setLoginType("google");
        user.setEmailFlag("1"); // Google邮箱默认已验证
        user.setDateJoined(nowTime());
        user.setLastLogin(nowTime());
        user.setIsDel(0);

        //默认字符头像
        String abc = PinyinUtil.paraseStringToPinyin(user.getUsername());
        if (StringUtils.isNotEmpty(abc)) {
            String head_span = abc.substring(0, 1).toUpperCase();
            String head_span_class = "text-" + head_span.toLowerCase();
            user.setHeadSpan(head_span);
            user.setHeadSpanClass(head_span_class);
        }

        // 生成唯一的tail_slug
        String tailSlug = generateTailSlug(userInfo.getStr("name"));
        user.setTailSlug(tailSlug);

        return user;
    }

    private User createUserFromGithub(JSONObject userInfo) {
        User user = new User();
        user.setGithubId(userInfo.getStr("id"));
        user.setGithubInfo(userInfo.toString());
        user.setEmail(userInfo.getStr("email"));
        user.setUsername(userInfo.getStr("login"));
        user.setRealName(userInfo.getStr("name"));
        user.setAvatarUrl(userInfo.getStr("avatar_url"));
        user.setLocation(userInfo.getStr("location"));
        user.setCompany(userInfo.getStr("company"));
        user.setBio(userInfo.getStr("bio"));
        user.setBlogSite(userInfo.getStr("blog"));
        user.setLoginType("github");
        user.setEmailFlag(StringUtils.isNotEmpty(userInfo.getStr("email")) ? "1" : "0");
        user.setDateJoined(nowTime());
        user.setLastLogin(nowTime());
        user.setIsDel(0);
        
        //默认字符头像
        String abc = PinyinUtil.paraseStringToPinyin(user.getUsername());
        if (StringUtils.isNotEmpty(abc)) {
            String head_span = abc.substring(0, 1).toUpperCase();
            String head_span_class = "text-" + head_span.toLowerCase();
            user.setHeadSpan(head_span);
            user.setHeadSpanClass(head_span_class);
        }

        // 生成唯一的tail_slug
        String tailSlug = generateTailSlug(userInfo.getStr("login"));
        user.setTailSlug(tailSlug);

        return user;
    }

    private void updateUserFromGoogle(User user, JSONObject userInfo) {
        user.setGoogleInfo(userInfo.toString());
        user.setLastLogin(nowTime());

        // 更新可能变化的信息
        if (StringUtils.isNotEmpty(userInfo.getStr("name"))) {
            user.setRealName(userInfo.getStr("name"));
        }
        if (StringUtils.isNotEmpty(userInfo.getStr("picture"))) {
            user.setAvatarUrl(userInfo.getStr("picture"));
        }

        userMapper.updateById(user);
    }

    private void updateUserFromGithub(User user, JSONObject userInfo) {
        user.setGithubInfo(userInfo.toString());
        user.setLastLogin(nowTime());

        // 更新可能变化的信息
        if (StringUtils.isNotEmpty(userInfo.getStr("name"))) {
            user.setRealName(userInfo.getStr("name"));
        }
        if (StringUtils.isNotEmpty(userInfo.getStr("avatar_url"))) {
            user.setAvatarUrl(userInfo.getStr("avatar_url"));
        }
        if (StringUtils.isNotEmpty(userInfo.getStr("location"))) {
            user.setLocation(userInfo.getStr("location"));
        }
        if (StringUtils.isNotEmpty(userInfo.getStr("company"))) {
            user.setCompany(userInfo.getStr("company"));
        }
        if (StringUtils.isNotEmpty(userInfo.getStr("bio"))) {
            user.setBio(userInfo.getStr("bio"));
        }
        if (StringUtils.isNotEmpty(userInfo.getStr("blog"))) {
            user.setBlogSite(userInfo.getStr("blog"));
        }

        userMapper.updateById(user);
    }

    /**
     * 将Google信息合并到已有用户账户
     */
    private void mergeGoogleInfoToExistingUser(User existingUser, JSONObject userInfo) {
        // 设置Google相关信息
        existingUser.setGoogleId(userInfo.getStr("id"));
        existingUser.setGoogleInfo(userInfo.toString());
        existingUser.setLastLogin(nowTime());

        // 如果现有用户没有头像，使用Google头像
        if (StringUtils.isEmpty(existingUser.getAvatarUrl()) && StringUtils.isNotEmpty(userInfo.getStr("picture"))) {
            existingUser.setAvatarUrl(userInfo.getStr("picture"));
        }

        // 如果现有用户没有真实姓名，使用Google姓名
        if (StringUtils.isEmpty(existingUser.getRealName()) && StringUtils.isNotEmpty(userInfo.getStr("name"))) {
            existingUser.setRealName(userInfo.getStr("name"));
        }

        // 确保邮箱验证标志为已验证（Google邮箱默认已验证）
        if (StringUtils.isEmpty(existingUser.getEmailFlag()) || "0".equals(existingUser.getEmailFlag())) {
            existingUser.setEmailFlag("1");
        }

        userMapper.updateById(existingUser);
        log.info("成功将Google信息合并到用户ID:{}，邮箱:{}", existingUser.getId(), existingUser.getEmail());
    }

    /**
     * 将GitHub信息合并到已有用户账户
     */
    private void mergeGithubInfoToExistingUser(User existingUser, JSONObject userInfo) {
        // 设置GitHub相关信息
        existingUser.setGithubId(userInfo.getStr("id"));
        existingUser.setGithubInfo(userInfo.toString());
        existingUser.setLastLogin(nowTime());

        // 如果现有用户没有头像，使用GitHub头像
        if (StringUtils.isEmpty(existingUser.getAvatarUrl()) && StringUtils.isNotEmpty(userInfo.getStr("avatar_url"))) {
            existingUser.setAvatarUrl(userInfo.getStr("avatar_url"));
        }

        // 如果现有用户没有真实姓名，使用GitHub姓名
        if (StringUtils.isEmpty(existingUser.getRealName()) && StringUtils.isNotEmpty(userInfo.getStr("name"))) {
            existingUser.setRealName(userInfo.getStr("name"));
        }

        // 补充其他GitHub特有信息
        if (StringUtils.isEmpty(existingUser.getLocation()) && StringUtils.isNotEmpty(userInfo.getStr("location"))) {
            existingUser.setLocation(userInfo.getStr("location"));
        }
        if (StringUtils.isEmpty(existingUser.getCompany()) && StringUtils.isNotEmpty(userInfo.getStr("company"))) {
            existingUser.setCompany(userInfo.getStr("company"));
        }
        if (StringUtils.isEmpty(existingUser.getBio()) && StringUtils.isNotEmpty(userInfo.getStr("bio"))) {
            existingUser.setBio(userInfo.getStr("bio"));
        }
        if (StringUtils.isEmpty(existingUser.getBlogSite()) && StringUtils.isNotEmpty(userInfo.getStr("blog"))) {
            existingUser.setBlogSite(userInfo.getStr("blog"));
        }

        // 如果有邮箱且邮箱验证标志为空或未验证，设置为已验证
        if (StringUtils.isNotEmpty(userInfo.getStr("email")) &&
            (StringUtils.isEmpty(existingUser.getEmailFlag()) || "0".equals(existingUser.getEmailFlag()))) {
            existingUser.setEmailFlag("1");
        }

        userMapper.updateById(existingUser);
        log.info("成功将GitHub信息合并到用户ID:{}，邮箱:{}", existingUser.getId(), existingUser.getEmail());
    }

    @Override
    public String generateTailSlug(String baseName) {
        if (StringUtils.isEmpty(baseName)) {
            baseName = "user";
        }

        // 清理用户名，只保留字母数字和下划线
        String cleanName = baseName.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (cleanName.length() > 20) {
            cleanName = cleanName.substring(0, 20);
        }

        String tailSlug = cleanName;
        int counter = 1;

        // 检查是否已存在，如果存在则添加数字后缀
        while (isSlugExists(tailSlug)) {
            tailSlug = cleanName + counter;
            counter++;
        }

        return tailSlug;
    }

    private boolean isSlugExists(String tailSlug) {
        try {
            Long count = userMapper.selectCount(new QueryWrapper<User>().eq("tail_slug", tailSlug));
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查tail_slug是否存在失败", e);
            return true; // 出错时返回true，避免重复
        }
    }

    private String nowTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}