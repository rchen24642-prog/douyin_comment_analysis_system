package com.example.springboot.service;

import com.example.springboot.dao.CommentDao;
import com.example.springboot.dao.SentimentDao;
import com.example.springboot.entity.Comment;
import com.example.springboot.entity.CommentDocument;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataSyncService {

    private final CommentDao commentDao;
    private final SentimentDao sentimentDao;
    private final ElasticsearchClient client;
    private final JdbcTemplate jdbcTemplate;  // ✅ 用于查询 project.uuid

    private static final String INDEX_NAME = "comment_index";

    /**
     * ✅ 全量同步 MySQL 评论到 Elasticsearch
     */
    public String syncAllCommentsToES() {
        List<Comment> comments = commentDao.selectAll();
        if (comments == null || comments.isEmpty()) {
            return "⚠️ 数据库中暂无评论数据。";
        }
        System.out.println("✅ MySQL 读取评论数量：" + comments.size());

        // ✅ 一次性查出所有 pid→uuid 映射
        Map<String, String> pidToUuid = getProjectUuidMap();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<CommentDocument> documents = comments.stream().map(c -> {
            CommentDocument doc = new CommentDocument();
            doc.setCid(c.getCid());
            doc.setContent_clean(c.getContent());
            doc.setUsername(c.getUsername());
            doc.setLike_count(c.getLikeCount());
            Integer label = sentimentDao.findLabelByCid(c.getCid());
            doc.setSentiment_label(label != null ? label : 0);
            doc.setComment_time(c.getCommentTime() != null ? c.getCommentTime().format(formatter) : null);
            doc.setPid(c.getPid());
            doc.setUuid(pidToUuid.getOrDefault(c.getPid(), "unknown"));
            return doc;
        }).collect(Collectors.toList());

        int successCount = 0;
        for (CommentDocument doc : documents) {
            try {
                IndexRequest<CommentDocument> request = IndexRequest.of(i -> i
                        .index(INDEX_NAME)
                        .id(doc.getCid())
                        .document(doc));
                IndexResponse response = client.index(request);
                if (response.result().name().equalsIgnoreCase("Created")
                        || response.result().name().equalsIgnoreCase("Updated")) {
                    successCount++;
                }
            } catch (Exception e) {
                System.err.println("❌ 写入失败：" + doc.getCid() + " - " + e.getMessage());
            }
        }

        return "✅ 已成功导入 " + successCount + " / " + documents.size() + " 条评论到 Elasticsearch！";
    }

    /**
     * ✅ 调用 Flask 服务触发情感分析
     */
    public String analyzeSentimentByPython(String pid) {
        String flaskUrl = "http://127.0.0.1:5001/sentiment/analyze";
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, String> body = new HashMap<>();
            body.put("pid", pid);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(flaskUrl, request, String.class);
            return "✅ 已触发情感分析任务。Flask 返回：" + response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 调用 Flask 失败：" + e.getMessage();
        }
    }

    /**
     * ✅ 按项目增量同步 MySQL → Elasticsearch
     */
    public String syncProjectCommentsToES(String pid) {
        List<Comment> comments = commentDao.selectByProject(pid);
        if (comments == null || comments.isEmpty()) {
            return "⚠️ 项目 " + pid + " 暂无可同步的评论数据。";
        }

        // ✅ 获取该项目 uuid
        String uuid = getUuidByPid(pid);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int successCount = 0;

        for (Comment c : comments) {
            CommentDocument doc = new CommentDocument();
            doc.setCid(c.getCid());
            doc.setContent_clean(c.getContent());
            doc.setUsername(c.getUsername());
            doc.setLike_count(c.getLikeCount());
            Integer label = sentimentDao.findLabelByCid(c.getCid());
            doc.setSentiment_label(label != null ? label : 0);
            doc.setComment_time(c.getCommentTime() != null ? c.getCommentTime().format(formatter) : null);
            doc.setPid(c.getPid());
            doc.setUuid(uuid);

            try {
                IndexRequest<CommentDocument> request = IndexRequest.of(i -> i
                        .index(INDEX_NAME)
                        .id(doc.getCid())
                        .document(doc));
                IndexResponse response = client.index(request);
                if (response.result().name().equalsIgnoreCase("Created")
                        || response.result().name().equalsIgnoreCase("Updated")) {
                    successCount++;
                }
            } catch (Exception e) {
                System.err.println("❌ 写入失败：" + doc.getCid() + " - " + e.getMessage());
            }
        }

        return "✅ 项目 " + pid + " 已成功同步 " + successCount + " 条评论至 Elasticsearch。";
    }

    /**
     * ✅ 从 project 表查询所有 pid → uuid 映射
     */
    private Map<String, String> getProjectUuidMap() {
        String sql = "SELECT pid, uuid FROM project";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<String, String> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            map.put((String) row.get("pid"), (String) row.get("uuid"));
        }
        return map;
    }

    /**
     * ✅ 根据 pid 查询 uuid（单项目）
     */
    private String getUuidByPid(String pid) {
        try {
            String sql = "SELECT uuid FROM project WHERE pid = ?";
            return jdbcTemplate.queryForObject(sql, String.class, pid);
        } catch (Exception e) {
            System.err.println("⚠️ 查询 uuid 失败：" + e.getMessage());
            return "unknown";
        }
    }
}
