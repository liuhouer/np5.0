package cn.northpark.np5.notify.handler;

import cn.northpark.np5.entity.NotifyRemind;
import cn.northpark.np5.notify.GeneralNotify;
import java.util.Date;

/**
 * 6类：站内反馈通知
 * 
 * @author bruce
 */
public class FeedNotice extends GeneralNotify {

    @Override
    public void build(NotifyRemind param) {
        param.setRemindId(6);
        param.setSenderId("000"); // 系统发送
        param.setSenderName("站内通知");
        param.setSenderAction("5"); // 站内通知
        param.setObjectType("2"); // 文章类型
        param.setCreatedAt(new Date());
    }
}