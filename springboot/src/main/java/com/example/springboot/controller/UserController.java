package com.example.springboot.controller;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.SecureUtil;
import com.example.springboot.common.Result;
import com.example.springboot.common.ResultCode;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    UserService userService;

    @GetMapping("/alldata")
    public Result getData() {
        List<User> all = userService.getAll();
        if (all.size() < 1) {
            throw new CustomException(ResultCode.DATA_LESS);
        }

        return Result.success(userService.getAll());

    }

    /*注册接口*/
    @PostMapping("/register")
    public Result userRegister(@RequestBody User user){

        userService.UserRegister(user);
        user.setPasswordHash(SecureUtil.md5(user.getPasswordHash())); // 使用 Hutool 或 Java 自带
        return Result.success("注册成功");

    }

    /*登录接口*/
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        User dbUser = userService.findByUsername(user.getUsername());
        if (dbUser == null || !dbUser.getPasswordHash().equals(user.getPasswordHash())) {
            throw new CustomException(ResultCode.USER_ACCOUNT_ERROR);
        }

        // ✅ 构建返回数据，只包含前端需要的字段
        Map<String, Object> data = new HashMap<>();
        data.put("username", dbUser.getUsername());
        data.put("role", dbUser.getRole());
        data.put("uuid", dbUser.getUuid());   // ✅ 关键字段：uuid
        return Result.success(data);
    }


    /*获取当前用户信息*/
    @GetMapping("/info/{uuid}")
    public Result getUserInfo(@PathVariable String uuid) {
        User user = userService.findByUuid(uuid);
        if (user == null) {
            throw new CustomException(ResultCode.DATA_NOT_FOUND);
        }
        // 只返回必要信息（防止密码泄露）
        Map<String, Object> info = new HashMap<>();
        info.put("uuid", user.getUuid());
        info.put("username", user.getUsername());
        info.put("role", user.getRole());
        info.put("avatarUrl", user.getAvatarUrl());
        info.put("createdAt", user.getCreatedAt());
        info.put("lastLogin", user.getLastLogin());
        return Result.success(info);
    }

    /*上传头像*/
    @PostMapping("/upload-avatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file, @RequestParam("uuid") String uuid) {
        try {
            if (file.isEmpty()) {
                return Result.error("上传文件为空");
            }

            // 保存路径
            String uploadDir = System.getProperty("user.dir") + "/uploads/avatar/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            // 生成唯一文件名
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + fileName;
            file.transferTo(new File(filePath));

            // 保存访问路径（前端可访问）
            String avatarUrl = "http://localhost:9090/uploads/avatar/" + fileName;

            // 更新数据库记录
            User user = userService.findByUuid(uuid);
            if (user == null) {
                return Result.error("用户不存在");
            }
            user.setAvatarUrl(avatarUrl);
            userService.updateUserAvatar(user);

            return Result.success(avatarUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("头像上传失败：" + e.getMessage());
        }
    }

    /**
     * 获取基础信息（顶部栏）
     */
    @GetMapping("/basic/{uuid}")
    public Result getBasicUserInfo(@PathVariable String uuid) {
        User user = userService.findByUuid(uuid);
        if (user == null) {
            return Result.error("用户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("avatarUrl", user.getAvatarUrl());
        return Result.success(data);
    }

    /**
     * 修改用户名
     */
    @PostMapping("/update-username")
    public Result updateUsername(@RequestParam String uuid, @RequestParam String username) {
        User user = userService.findByUuid(uuid);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setUsername(username);
        userService.updateUserAvatar(user);  // 这里方法名虽然叫updateUserAvatar，但其实是updateByPrimaryKeySelective，可以复用
        return Result.success("用户名修改成功");
    }




}

