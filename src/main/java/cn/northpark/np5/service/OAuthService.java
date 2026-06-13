package cn.northpark.np5.service;

import cn.northpark.np5.model.User;

public interface OAuthService {

    /**
     * 获取Google授权URL
     */
    String getGoogleAuthUrl(String state);

    /**
     * 获取GitHub授权URL
     */
    String getGithubAuthUrl(String state);

    /**
     * Google授权回调处理
     */
    User handleGoogleCallback(String code, String state);

    /**
     * GitHub授权回调处理
     */
    User handleGithubCallback(String code, String state);

    /**
     * 根据GitHub ID查找用户
     */
    User findUserByGithubId(String githubId);

    /**
     * 生成唯一的尾部Slug
     */
    String generateTailSlug(String baseName);
}