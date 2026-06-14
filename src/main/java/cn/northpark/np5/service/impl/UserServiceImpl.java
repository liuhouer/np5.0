package cn.northpark.np5.service.impl;

import cn.northpark.np5.mapper.UserMapper;
import cn.northpark.np5.entity.User;
import cn.northpark.np5.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String email, String password) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email).eq("password", password);
        return this.getOne(queryWrapper);
    }
}