package cn.northpark.np5.service;

import cn.northpark.np5.entity.Knowledge;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface KnowledgeService extends IService<Knowledge> {

    List<Map<String, Object>> querySqlMap(String sql);
}