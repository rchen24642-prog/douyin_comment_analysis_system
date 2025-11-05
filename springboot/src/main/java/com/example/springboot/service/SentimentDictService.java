package com.example.springboot.service;

import com.example.springboot.dao.SentimentDictDao;
import com.example.springboot.entity.SentimentDict;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.List;

@Service
public class SentimentDictService {

    @Resource
    private SentimentDictDao sentimentDictDao;

    public List<SentimentDict> getUserDict(String uuid) {
        return sentimentDictDao.findByUser(uuid);
    }

    public void addWord(SentimentDict dict) {
        sentimentDictDao.insertWord(dict);
    }

    public void deleteWord(Integer id) {
        sentimentDictDao.deleteById(id);
    }
}
