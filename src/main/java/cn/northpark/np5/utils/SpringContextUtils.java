package cn.northpark.np5.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring手动获取bean的工具类
 * 
 * @author Bruce
 */
@Component
public class SpringContextUtils implements ApplicationContextAware {

    private static volatile ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        if (SpringContextUtils.applicationContext == null) {
            SpringContextUtils.applicationContext = appContext;
        }
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }
}