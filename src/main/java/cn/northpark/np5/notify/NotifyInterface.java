package cn.northpark.np5.notify;

import cn.northpark.np5.entity.NotifyRemind;

/**
 * 消息提醒通知接口
 * 
 * @author bruce
 */
public interface NotifyInterface {

    /**
     * 执行通知保存及相关前置构建
     * 
     * @param param 通知参数
     */
    void execute(NotifyRemind param);

    /**
     * 异步通知执行接口，利用 Spring 异步机制
     * 
     * @param param 通知参数
     */
    void startSync(NotifyRemind param);
}