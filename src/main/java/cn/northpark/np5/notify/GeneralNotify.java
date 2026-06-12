package cn.northpark.np5.notify;

import cn.northpark.np5.entity.NotifyRemind;
import cn.northpark.np5.service.NotifyRemindService;
import cn.northpark.np5.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

/**
 * 消息提醒引擎抽象基类
 * 
 * @author bruce
 */
@Slf4j
public abstract class GeneralNotify implements NotifyInterface {

    private NotifyRemindService getNotifyRemindService() {
        return SpringContextUtils.getBean(NotifyRemindService.class);
    }

    /**
     * 构建特定消息类型的专属字段
     * 
     * @param param 通知参数
     */
    public abstract void build(NotifyRemind param);

    @Override
    public final void execute(NotifyRemind param) {
        build(param);
        getNotifyRemindService().save(param);
    }

    @Override
    public void startSync(NotifyRemind param) {
        try {
            Executor executor = (Executor) SpringContextUtils.getBean("notifyAsyncExecutor");
            executor.execute(() -> {
                try {
                    execute(param);
                } catch (Exception e) {
                    log.error("northpark异步发送通知处理异常", e);
                }
            });
        } catch (Exception e) {
            log.error("northpark异步发送通知提交异常，转为同步发送", e);
            try {
                execute(param);
            } catch (Exception ex) {
                log.error("northpark同步发送通知异常", ex);
            }
        }
    }
}