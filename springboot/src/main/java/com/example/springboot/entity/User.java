package com.example.springboot.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.GeneratedValue;
import java.time.LocalDateTime;
import javax.persistence.*;


@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name= "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer uid;              // 用户id，自增主键

    @Column(name = "username", nullable = false, unique = true)
    private String username;          // 登录用户名（唯一）

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;      // 加密后的密码

    @Column(name = "role", nullable = false)
    private String role;              // 用户角色（admin/user）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;  // 账户创建时间

    @Column(name = "last_login")
    private LocalDateTime lastLogin;  // 最后登录时间

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;// 用户前端id

    @Column(name = "avatar_url")
    private String avatarUrl;  // 用户头像链接

    // ====================== getter & setter ======================

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}


