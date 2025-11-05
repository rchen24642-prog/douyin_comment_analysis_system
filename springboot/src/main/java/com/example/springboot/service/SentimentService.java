package com.example.springboot.service;

import com.example.springboot.dao.SentimentDao;
import com.example.springboot.entity.Sentiment;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SentimentService {

    @Resource
    private SentimentDao sentimentDao;

    // 获取所有情感分析记录
    public List<Sentiment> getAll() {
        return sentimentDao.selectAll();
    }

    // 根据情感分析ID查询
    public Sentiment getBySid(Integer sid) {
        return sentimentDao.selectByPrimaryKey(sid);
    }

    // 根据项目ID查询情感分析记录
    public List<Sentiment> getByProjectId(String pid) {
        Sentiment query = new Sentiment();
        query.setPid(pid);
        return sentimentDao.select(query);
    }

    // 根据评论ID查询情感分析记录
    public Sentiment getByCommentId(String cid) {
        Sentiment query = new Sentiment();
        query.setCid(cid);
        List<Sentiment> list = sentimentDao.select(query);
        return list.isEmpty() ? null : list.get(0);
    }

    // 新增情感分析记录
    public int addSentiment(Sentiment sentiment) {
        return sentimentDao.insert(sentiment);
    }

    // 更新情感分析记录
    public int updateSentiment(Sentiment sentiment) {
        return sentimentDao.updateByPrimaryKeySelective(sentiment);
    }

    public List<Map<String, Object>> getWithContentByProjectId(String pid) {
        return sentimentDao.selectWithContentByPid(pid);
    }
}