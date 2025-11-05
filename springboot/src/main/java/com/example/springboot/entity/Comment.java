package com.example.springboot.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;


@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;  // 主键 id，自增

    @Column(name = "cid", length = 50, nullable = false)
    private String cid;  // 评论编号（对应清洗前后的评论）

    @Column(name = "content", length = 255)
    private String content = "";  // 评论内容，默认空字符串

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "comment_time")
    private LocalDateTime commentTime;  // 评论时间（统一格式）

    @Column(name = "username", length = 50)
    private String username = "未知用户";  // 用户名，默认“未知用户”

    @Column(name = "like_count")
    private Integer likeCount = 0;  // 点赞数，默认0

    @Column(name = "reply_count")
    private Integer replyCount = 0;  // 回复数，默认0

    @Column(name = "is_abnormal", nullable = false)
    private Boolean isAbnormal = false;  // 是否异常（0：否；1：是）

    @Column(name = "pid", length = 50, nullable = false)
    private String pid;  // 项目编号，外键（关联 project.pid）

    @Column(name = "clean_status", length = 20, nullable = false)
    private String cleanStatus = "initial";  // 清洗状态（initial/success/fail）

    @Column(name = "comment_type", nullable = false)
    private Integer commentType = 0;  // 评论类型（0：一级；1：二级）

    @Column(name = "parent_cid", length = 50)
    private String parentCid;  // 父评论编号（二级评论时使用）

    // ====================== getter & setter ======================


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCommentTime() {
        return commentTime;
    }

    public void setCommentTime(LocalDateTime commentTime) {
        this.commentTime = commentTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public Boolean getAbnormal() {
        return isAbnormal;
    }

    public void setAbnormal(Boolean abnormal) {
        isAbnormal = abnormal;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getCleanStatus() {
        return cleanStatus;
    }

    public void setCleanStatus(String cleanStatus) {
        this.cleanStatus = cleanStatus;
    }

    public Integer getCommentType() {
        return commentType;
    }

    public void setCommentType(Integer commentType) {
        this.commentType = commentType;
    }

    public String getParentCid() {
        return parentCid;
    }

    public void setParentCid(String parentCid) {
        this.parentCid = parentCid;
    }
}

