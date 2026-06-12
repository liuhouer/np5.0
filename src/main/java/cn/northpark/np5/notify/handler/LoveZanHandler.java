package cn.northpark.np5.notify.handler;

import cn.northpark.np5.entity.NotifyRemind;
import cn.northpark.np5.notify.GeneralNotify;
import java.util.Date;

/**
 * 2类：最爱图册被点赞通知
 * 
 * @author bruce
 */
public class LoveZanHandler extends GeneralNotify {

    @Override
    public void build(NotifyRemind param) {
        param.setRemindId(2);
        param.setSenderAction("2");
        param.setObjectType("2");
        param.setCreatedAt(new Date());
    }
}