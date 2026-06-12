package cn.northpark.np5.notify.handler;

import cn.northpark.np5.entity.NotifyRemind;
import cn.northpark.np5.notify.GeneralNotify;
import java.util.Date;

/**
 * 3类：树洞界面的留言被回复
 * 
 * @author bruce
 */
public class NoteReplyHandler extends GeneralNotify {

    @Override
    public void build(NotifyRemind param) {
        param.setRemindId(3);
        param.setSenderAction("1");
        param.setObjectType("1");
        param.setCreatedAt(new Date());
    }
}