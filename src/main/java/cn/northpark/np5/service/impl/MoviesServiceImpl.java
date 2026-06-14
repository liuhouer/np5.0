package cn.northpark.np5.service.impl;

import cn.northpark.np5.mapper.MoviesMapper;
import cn.northpark.np5.entity.Movies;
import cn.northpark.np5.service.MoviesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class MoviesServiceImpl extends ServiceImpl<MoviesMapper, Movies> implements MoviesService {
}