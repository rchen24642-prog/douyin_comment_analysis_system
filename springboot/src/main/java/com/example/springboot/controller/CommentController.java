package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.common.ResultCode;
import com.example.springboot.entity.Comment;
import com.example.springboot.service.CommentService;
import com.example.springboot.service.CommentSearchService;
import com.example.springboot.service.DataSyncService;
import com.example.springboot.dao.CommentDao;
import com.example.springboot.exception.CustomException;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    CommentService commentService;

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private CommentSearchService commentSearchService;

    @Autowired
    private DataSyncService dataSyncService;

    // 获取所有评论
    @GetMapping("/alldata")
    public Result getAllComments() {
        List<Comment> comments = commentService.getAll();
        if (comments.isEmpty()) throw new CustomException(ResultCode.DATA_LESS);
        return Result.success(comments);
    }

    // 按ID获取评论
    @GetMapping("/id/{id}")
    public Result getById(@PathVariable Integer id) {
        Comment comment = commentDao.selectByPrimaryKey(id);
        return Result.success(comment);
    }

    // 按项目获取评论
    @GetMapping("/byproject/{pid}")
    public Result getCommentsByProject(@PathVariable String pid) {
        List<Comment> comments = commentService.getByProjectId(pid);
        if (comments.isEmpty()) throw new CustomException(ResultCode.DATA_LESS);
        return Result.success(comments);
    }

    // 预览50条评论
    @GetMapping("/preview")
    public Result getPreview(@RequestParam String uuid) {
        List<Comment> list = commentDao.selectRecentCleaned(uuid,50);
        return Result.success(list);
    }

    // ✅ 同步 MySQL 数据到 Elasticsearch
    @PostMapping("/sync")
    public Result syncCommentsToEs() {
        try {
            String msg = dataSyncService.syncAllCommentsToES();
            return Result.success(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("同步失败：" + e.getMessage());
        }
    }

    // ✅ 多条件搜索接口（供前端检索页调用）
    @GetMapping("/search")
    public Result searchComments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer sentiment,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer minLike,
            @RequestParam(required = false) Integer maxLike,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("uuid") String uuid

    ) {
        try {

            Map<String, Object> result = commentSearchService.search(
                    keyword, username, sentiment, startTime, endTime, minLike, maxLike, page, size,uuid
            );
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SEARCH_ERROR.msg);
        }
    }

    // ✅ 评论 + 情感 联查接口
    @GetMapping("/with-sentiment")
    public Result getCommentsWithSentiment(
            @RequestParam String pid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("uuid") String uuid
    ) {
        if (pid == null || pid.isBlank()) {
            return Result.error("pid 不能为空");
        }
        Map<String, Object> data = commentService.queryWithSentimentByPid(pid, uuid, page, size);
        return Result.success(data);
    }

}