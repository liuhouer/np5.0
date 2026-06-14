package cn.northpark.np5.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bc_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;

    private String email;

    private String emailFlag;

    private String tailSlug;

    private String password;

    private String headSpanClass;

    private String headSpan;

    private String headPath;

    private String meta;

    private String blogSite;

    private String dateJoined;

    private String lastLogin;

    private String qqOpenid;

    private String qqInfo;

    private String googleId;

    private String googleInfo;

    private String githubId;

    private String githubInfo;

    private String loginType;

    private String avatarUrl;

    private String realName;

    private String location;

    private String company;

    private String bio;

    private Integer isDel;
}