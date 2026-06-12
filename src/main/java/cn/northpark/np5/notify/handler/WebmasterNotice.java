package cn.northpark.np5.notify.handler;

import cn.northpark.np5.entity.NotifyRemind;
import cn.northpark.np5.notify.GeneralNotify;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.Date;

/**
 * 5类：站长通知
 * 
 * @author bruce
 */
public class WebmasterNotice extends GeneralNotify {

    @Override
    public void build(NotifyRemind param) {
        param.setRemindId(5);
        param.setSenderId("000"); // 系统发送
        if (StringUtils.isEmpty(param.getSenderName())) {
            param.setSenderName("站内通知");
        }
        param.setSenderAction("5"); // 站内通知
        param.setObjectType("3"); // 推送
        param.setCreatedAt(new Date());
        param.setRecipientId("507723"); // 站长ID
    }
}