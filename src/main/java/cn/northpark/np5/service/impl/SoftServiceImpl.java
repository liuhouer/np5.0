package cn.northpark.np5.service.impl;

import cn.northpark.np5.mapper.SoftMapper;
import cn.northpark.np5.entity.Soft;
import cn.northpark.np5.service.SoftService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SoftServiceImpl extends ServiceImpl<SoftMapper, Soft> implements SoftService {

    @Override
    public List<Map<String, Object>> querySqlMap(String sql) {
        return baseMapper.querySqlMap(sql);
    }
}