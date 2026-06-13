package cn.northpark.np5.constant;

import lombok.Getter;

/**
 * 主题类型枚举
 * 对应 bc_topic_comment 表的 topic_type 字段
 * 
 * @author bruce
 */
public enum TopicTypeEnum {

    /**
     * 1: 碎碎念
     */
    NOTE("1", "碎碎念", "NOTE_REPLY"),

    /**
     * 3: 软件
     */
    SOFT("3", "软件", "ART_REPLY"),

    /**
     * 4: 电影
     */
    MOVIE("4", "电影", "ART_REPLY"),

    /**
     * 8: 学习
     */
    LEARN("8", "学习", "ART_REPLY"),

    /**
     * 7: 打赏/捐赠
     */
    REWARD("7", "打赏", "ART_REPLY");

    @Getter
    private final String code;

    @Getter
    private final String name;

    /**
     * 对应的通知类型名称(用于消息提醒)
     */
    @Getter
    private final String notifyName;

    TopicTypeEnum(String code, String name, String notifyName) {
        this.code = code;
        this.name = name;
        this.notifyName = notifyName;
    }

    /**
     * 根据 code 匹配枚举
     */
    public static TopicTypeEnum match(String code) {
        for (TopicTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据 code 获取对应的通知类型名称
     */
    public static String getMatchNotifyName(String code) {
        TopicTypeEnum typeEnum = match(code);
        return typeEnum != null ? typeEnum.notifyName : null;
    }
}