package cn.northpark.np5.service.impl;

import cn.northpark.np5.mapper.TagsMapper;
import cn.northpark.np5.entity.Tags;
import cn.northpark.np5.service.TagsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TagsServiceImpl extends ServiceImpl<TagsMapper, Tags> implements TagsService {
}