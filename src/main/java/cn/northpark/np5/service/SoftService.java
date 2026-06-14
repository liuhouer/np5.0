package cn.northpark.np5.service;

import cn.northpark.np5.entity.Soft;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface SoftService extends IService<Soft> {

    List<Map<String, Object>> querySqlMap(String sql);
}