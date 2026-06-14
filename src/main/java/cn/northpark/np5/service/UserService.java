package cn.northpark.np5.service;

import cn.northpark.np5.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    User login(String email, String password);
}