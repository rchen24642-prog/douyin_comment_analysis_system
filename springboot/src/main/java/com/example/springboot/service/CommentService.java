package com.example.springboot.service;

import com.example.springboot.dao.CommentDao;
import com.example.springboot.entity.Comment;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class CommentService {

    @Resource
    private CommentDao commentDao;

    // 获取所有评论
    public List<Comment> getAll() {
        return commentDao.selectAll();
    }

    // 根据评论ID查询单条评论
    public Comment getById(Integer id) {
        return commentDao.selectByPrimaryKey(id);
    }

    // 根据项目ID查询评论列表
    public List<Comment> getByProjectId(String pid) {
        Comment query = new Comment();
        query.setPid(pid);
        return commentDao.select(query);
    }

    // 新增评论
    public int addComment(Comment comment) {
        return commentDao.insert(comment);
    }

    // 更新评论信息
    public int updateComment(Comment comment) {
        return commentDao.updateByPrimaryKeySelective(comment);
    }
    public Map<String, Object> queryWithSentimentByPid(String pid, String uuid, int page, int size) {
        int offset = (page - 1) * size;
        List<com.example.springboot.dto.CommentWithSentiment> list =
                commentDao.selectWithSentimentByPid(pid, uuid, size, offset);
        int total = commentDao.countByPidAndUuid(pid, uuid);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", list);
        return result;
    }

}