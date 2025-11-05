package com.example.springboot.service;

import com.example.springboot.entity.CommentDocument;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommentSearchService {

    private final ElasticsearchClient client;
    private static final String INDEX_NAME = "comment_index";

    public Map<String, Object> search(String keyword,
                                      String username,
                                      Integer sentiment,
                                      String startTime,
                                      String endTime,
                                      Integer minLike,
                                      Integer maxLike,
                                      int page,
                                      int size,
                                      String uuid) {

        if (size <= 0) size = 100;
        if (page < 0) page = 0;

        try {
            // 1) 组装 must 子句（使用 Map 构造原生 ES DSL，完全避开 Builder API）
            List<Map<String, Object>> must = new ArrayList<>();

            if (StringUtils.hasText(uuid)) {
                must.add(Map.of("term", Map.of("uuid",uuid)));
            }
            if (StringUtils.hasText(keyword)) {
                must.add(Map.of("match", Map.of("content_clean", keyword)));
            }
            if (StringUtils.hasText(username)) {
                must.add(Map.of("wildcard", Map.of("username", "*" + username + "*")));
            }
            if (sentiment != null) {
                must.add(Map.of("term", Map.of("sentiment_label", sentiment)));
            }
            // ✅ 点赞范围过滤（只有当用户明确输入了最小或最大点赞数时才生效）
            if ((minLike != null && minLike > 0) || (maxLike != null && maxLike > 0)) {
                Map<String, Object> range = new HashMap<>();
                if (minLike != null && minLike > 0) {
                    range.put("gte", minLike);
                }
                if (maxLike != null && maxLike > 0) {
                    range.put("lte", maxLike);
                }

                // ✅ 只有当 range 里真的有键时才加入 must
                if (!range.isEmpty()) {
                    must.add(Map.of("range", Map.of("like_count", range)));
                }
            }

            if (StringUtils.hasText(startTime) || StringUtils.hasText(endTime)) {
                Map<String, Object> range = new HashMap<>();
                if (StringUtils.hasText(startTime)) range.put("gte", startTime);
                if (StringUtils.hasText(endTime)) range.put("lte", endTime);
                must.add(Map.of("range", Map.of("comment_time", range)));
            }


            // ✅ 如果没有任何搜索条件，则添加 match_all，避免空查询
            if (must.isEmpty()) {
                must.add(Map.of("match_all", Map.of()));
            }

// ✅ 用 Map 直接组装查询体
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", page * size);
            body.put("size", size);
            body.put("query", Map.of("bool", Map.of("must", must)));

// ✅ 用 toJson(body) 把 Map 转成标准 JSON，再交给 ES 客户端执行
            System.out.println("🔍 最终查询DSL：" + toJson(body));

            SearchResponse<CommentDocument> response = client.search(s -> s
                            .index(INDEX_NAME)
                            .withJson(new StringReader(toJson(body))),
                    CommentDocument.class);


            // 3) 结果解析
            List<Map<String, Object>> data = new ArrayList<>();
            for (Hit<CommentDocument> hit : response.hits().hits()) {
                CommentDocument doc = hit.source();
                if (doc == null) continue;

                Map<String, Object> map = new HashMap<>();
                map.put("cid", doc.getCid());
                map.put("pid", doc.getPid());
                map.put("uuid", doc.getUuid());
                map.put("username", doc.getUsername());
                map.put("like_count", doc.getLike_count());
                map.put("sentiment_label", doc.getSentiment_label());
                map.put("comment_time", doc.getComment_time());

                String text = doc.getContent_clean();
                if (StringUtils.hasText(keyword) && StringUtils.hasText(text)) {
                    String pattern = Pattern.quote(keyword);
                    text = text.replaceAll(pattern, "<em style='color:red'>$0</em>");
                }
                map.put("content_clean", text);

                data.add(map);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", response.hits().total() != null ? response.hits().total().value() : 0);
            result.put("data", data);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ ES 查询失败：" + e.getMessage());
        }
    }

    // ====== 极简 Map -> JSON 序列化（只覆盖本方法里用到的结构，够用即可）======
    private static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Number || obj instanceof Boolean) return String.valueOf(obj);
        if (obj instanceof String) return "\"" + escape((String) obj) + "\"";
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) obj).entrySet()) {
                if (!first) sb.append(",");
                sb.append(toJson(String.valueOf(e.getKey()))).append(":").append(toJson(e.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Iterable) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Object o : (Iterable<?>) obj) {
                if (!first) sb.append(",");
                sb.append(toJson(o));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        // 兜底：转字符串
        return "\"" + escape(String.valueOf(obj)) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
