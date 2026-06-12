package cn.northpark.np5.notify.handler;

import cn.northpark.np5.entity.NotifyRemind;
import cn.northpark.np5.notify.GeneralNotify;
import java.util.Date;

/**
 * 4类：xx关注了yy
 * 
 * @author bruce
 */
public class FollowHandler extends GeneralNotify {

    @Override
    public void build(NotifyRemind param) {
        param.setRemindId(4);
        param.setSenderAction("3");
        param.setObjectType("1");
        param.setCreatedAt(new Date());
    }
}