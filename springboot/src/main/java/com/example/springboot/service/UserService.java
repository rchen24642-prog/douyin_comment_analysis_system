package com.example.springboot.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import com.example.springboot.common.ResultCode;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import com.example.springboot.dao.UserDao;
import org.springframework.util.DigestUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;


@Service
public class UserService {

    @Resource
    UserDao userDao;

    public List<User> getAll() {
       return userDao.selectAll();
    }

    public User UserRegister(User user) {

        String userName = user.getUsername();//用户输入的用户名

        User dbUser = userDao.findByUsername(userName);// 根据用户输入，查询数据库结果

        if (ObjectUtil.isNotEmpty(dbUser)){ //用户已存在
            throw new CustomException(ResultCode.USER_EXIST_ERROR);

        }

        // ✅ 为新用户生成 uuid
        user.setUuid(UUID.randomUUID().toString());

        // ✅ 加密密码
        user.setPasswordHash(DigestUtils.md5DigestAsHex(user.getPasswordHash().getBytes()));

        //用户不存在，则注册
        userDao.insertSelective(user);

        return user;
    }

    public User findByUsername(String username) {
        return userDao.findByUsername(username);
    }

    public User findByUuid(String uuid) {
        Example example = new Example(User.class);
        example.createCriteria().andEqualTo("uuid", uuid);
        return userDao.selectOneByExample(example);
    }

    public void updateUserAvatar(User user) {
        userDao.updateByPrimaryKeySelective(user);
    }




}


