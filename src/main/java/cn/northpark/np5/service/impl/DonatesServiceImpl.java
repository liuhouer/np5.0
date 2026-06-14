package cn.northpark.np5.service.impl;

import cn.northpark.np5.mapper.DonatesMapper;
import cn.northpark.np5.entity.Donates;
import cn.northpark.np5.service.DonatesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class DonatesServiceImpl extends ServiceImpl<DonatesMapper, Donates> implements DonatesService {
}