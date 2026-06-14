package cn.northpark.np5.utils;

import cn.northpark.np5.mapper.EnvCfgMapper;
import cn.northpark.np5.entity.EnvCfg;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class EnvCfgUtil implements CommandLineRunner {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    @Autowired
    private EnvCfgMapper envCfgMapper;

    @Override
    public void run(String... args) {
        log.info("开始加载 bc_env_cfg 配置缓存...");
        try {
            QueryWrapper<EnvCfg> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("c_status", "1");
            List<EnvCfg> list = envCfgMapper.selectList(queryWrapper);
            for (EnvCfg cfg : list) {
                if (cfg.getVcCfgName() != null && cfg.getVcCfgValue() != null) {
                    CACHE.put(cfg.getVcCfgName(), cfg.getVcCfgValue());
                }
            }
            log.info("加载 bc_env_cfg 配置成功，共加载 {} 条有效配置。", CACHE.size());
        } catch (Exception e) {
            log.error("加载 bc_env_cfg 配置异常", e);
        }
    }

    public static String getValByCfgName(String vcCfgName) {
        return CACHE.getOrDefault(vcCfgName, "");
    }
}