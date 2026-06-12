package cn.northpark.np5.notify;

import cn.northpark.np5.notify.handler.*;
import lombok.Getter;

/**
 * 通知类型枚举类
 * 
 * @author bruce
 */
public enum NotifyEnum {

    /**
     * 1类：在某文章界面评论被回复
     */
    ART_REPLY("ART_REPLY", new ArtReplyHandler()),

    /**
     * 2类：最爱图册被点赞通知
     */
    LOVE_ZAN("LOVE_ZAN", new LoveZanHandler()),

    /**
     * 3类：树洞界面的留言被回复
     */
    NOTE_REPLY("NOTE_REPLY", new NoteReplyHandler()),

    /**
     * 4类：xx关注了yy
     */
    FOLLOW("FOLLOW", new FollowHandler()),

    /**
     * 5类：站长通知
     */
    WEBMASTER("WEBMASTER", new WebmasterNotice()),

    /**
     * 6类：站内通知
     */
    FEED("FEED", new FeedNotice());

    @Getter
    private final String name;

    @Getter
    private final GeneralNotify notifyInstance;

    NotifyEnum(String name, GeneralNotify notifyInstance) {
        this.name = name;
        this.notifyInstance = notifyInstance;
    }

    public static NotifyEnum match(String name) {
        for (NotifyEnum value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return null;
    }
}