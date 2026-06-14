package cn.northpark.np5.service.impl;

import cn.northpark.np5.mapper.KnowledgeMapper;
import cn.northpark.np5.entity.Knowledge;
import cn.northpark.np5.service.KnowledgeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge> implements KnowledgeService {

    @Override
    public List<Map<String, Object>> querySqlMap(String sql) {
        return baseMapper.querySqlMap(sql);
    }
}