package cn.northpark.np5.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthConfig {

    private Google google = new Google();
    private Github github = new Github();

    @Data
    public static class Google {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String scope = "openid email profile";
        private String authUrl = "https://accounts.google.com/o/oauth2/v2/auth";
        private String tokenUrl = "https://oauth2.googleapis.com/token";
        private String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
    }

    @Data
    public static class Github {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String scope = "user:email";
        private String authUrl = "https://github.com/login/oauth/authorize";
        private String tokenUrl = "https://github.com/login/oauth/access_token";
        private String userInfoUrl = "https://api.github.com/user";
        private String emailUrl = "https://api.github.com/user/emails";
    }
}