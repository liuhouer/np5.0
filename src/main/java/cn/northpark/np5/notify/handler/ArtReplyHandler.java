package cn.northpark.np5.notify.handler;

import cn.northpark.np5.entity.NotifyRemind;
import cn.northpark.np5.notify.GeneralNotify;
import java.util.Date;

/**
 * 1类：在某文章界面评论被回复
 * 
 * @author bruce
 */
public class ArtReplyHandler extends GeneralNotify {

    @Override
    public void build(NotifyRemind param) {
        param.setRemindId(1);
        param.setSenderAction("1");
        param.setObjectType("2");
        param.setCreatedAt(new Date());
    }
}