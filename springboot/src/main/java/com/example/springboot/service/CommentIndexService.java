package com.example.springboot.service;

import com.example.springboot.entity.CommentDocument;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentIndexService {

    private final JdbcTemplate jdbcTemplate;
    private final ElasticsearchClient client;

    /**
     * ✅ 根据项目 pid，把 MySQL 的 comment 数据同步到 Elasticsearch
     */
    public void indexCommentsByPid(String pid) {
        String sql = """
           SELECT c.cid,
                  c.content AS content_clean,
                  c.username,
                  c.like_count,
                  s.sentiment_label,
                  c.comment_time,
                  c.pid,
                  p.uuid
           FROM comment c
                LEFT JOIN sentiment s ON c.cid = s.cid
                JOIN project p ON c.pid = p.pid
           WHERE c.pid = ?
           """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, pid);
        System.out.println("🔍 SQL 返回行数：" + rows.size());

        for (Map<String, Object> row : rows) {
            CommentDocument doc = new CommentDocument();
            doc.setCid((String) row.get("cid"));
            doc.setContent_clean((String) row.get("content_clean"));
            doc.setUsername((String) row.get("username"));
            doc.setLike_count((Integer) row.getOrDefault("like_count", 0));
            doc.setSentiment_label((Integer) row.getOrDefault("sentiment_label", 0));

            Object rawTime = row.get("comment_time");
            if (rawTime != null) {
                String timeStr = rawTime.toString().trim();
                if (timeStr.contains(".")) {
                    timeStr = timeStr.substring(0, timeStr.indexOf("."));
                }
                doc.setComment_time(timeStr);
            }

            doc.setPid((String) row.get("pid"));
            doc.setUuid((String) row.get("uuid"));

            try {
                // ✅ 直接使用 Elasticsearch 官方客户端写入
                IndexRequest<CommentDocument> request = IndexRequest.of(i -> i
                        .index("comment_index")
                        .id(doc.getCid())
                        .document(doc)
                );
                IndexResponse response = client.index(request);

                if (response.result().name().equalsIgnoreCase("Created") ||
                        response.result().name().equalsIgnoreCase("Updated")) {
                    System.out.println("✅ 已写入 ES：" + doc.getCid() + " - " + doc.getContent_clean());
                } else {
                    System.err.println("⚠️ 未知结果：" + response.result());
                }

            } catch (Exception e) {
                System.err.println("❌ 写入失败：" + doc.getCid() + " - " + e.getMessage());
            }
        }

        System.out.println("✅ 已为项目 " + pid + " 建立索引：" + rows.size() + " 条记录");
    }
}
