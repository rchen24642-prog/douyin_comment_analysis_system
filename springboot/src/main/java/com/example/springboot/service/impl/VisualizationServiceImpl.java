package com.example.springboot.service.impl;

import com.example.springboot.dao.VisualizationDao;
import com.example.springboot.service.VisualizationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class VisualizationServiceImpl implements VisualizationService {

    @Resource
    private VisualizationDao visualizationDao;

    /** 1) 情感分布统计 */
    @Override
    public Map<String, Object> getSentimentDistribution(String pid) {
        List<Map<String, ?>> rows = visualizationDao.selectSentimentDistribution(pid);

        long pos = 0, neu = 0, neg = 0;
        for (Map<String, ?> r : rows) {
            Object labelObj = r.get("sentimentLabel");
            Object cntObj = r.get("cnt");
            if (labelObj == null || cntObj == null) continue;
            int label = ((Number) labelObj).intValue();
            long cnt = ((Number) cntObj).longValue();
            if (label == 1) pos += cnt;
            else if (label == 0) neu += cnt;
            else if (label == -1) neg += cnt;
        }
        Map<String, Object> res = new HashMap<>();
        res.put("positive", pos);
        res.put("neutral", neu);
        res.put("negative", neg);
        return res;
    }

    /** 2) 舆情热度趋势（每日评论 + 点赞） */
    @Override
    public List<Map<String, Object>> getDailyTrend(String pid, LocalDate start, LocalDate end) {
        List<Map<String, ?>> list = visualizationDao.selectDailyTrend(pid, start, end);
        // 统一构造成 Map<String,Object> 再返回
        return list.stream().map(m -> {
            Map<String, Object> r = new HashMap<>();
            r.put("date", m.get("date"));
            r.put("comments", m.get("comments"));
            r.put("likes", m.get("likes"));
            return r;
        }).collect(Collectors.toList());
    }

    /** 3) 热门关键词 TopN（中文2~6连续字 + 英数词） */
    @Override
    public List<Map<String, Object>> getTopKeywords(String pid, Integer limit) {
        List<String> texts = visualizationDao.selectRecentCommentTexts(pid, 3000);
        if (texts == null || texts.isEmpty()) return Collections.emptyList();

        Pattern zh = Pattern.compile("[\\u4e00-\\u9fa5]{2,6}");
        Pattern en = Pattern.compile("[A-Za-z0-9_]{2,}");
        Set<String> stop = new HashSet<>(Arrays.asList(
                "我们","你们","他们","这个","那个","就是","以及","还有","的话","感觉",
                "真的","非常","然后","但是","所以","而且","如果","开始","已经","可以","不会","没有"
        ));

        Map<String, Integer> freq = new HashMap<>();
        for (String t : texts) {
            if (t == null) continue;
            Matcher m1 = zh.matcher(t);
            while (m1.find()) {
                String w = m1.group();
                if (!stop.contains(w)) freq.merge(w, 1, Integer::sum);
            }
            Matcher m2 = en.matcher(t.toLowerCase());
            while (m2.find()) {
                String w = m2.group();
                if (!stop.contains(w)) freq.merge(w, 1, Integer::sum);
            }
        }

        return freq.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit == null ? 20 : limit)
                .map(e -> {
                    Map<String, Object> r = new HashMap<>();
                    r.put("word", e.getKey());
                    r.put("count", e.getValue());
                    return r;
                })
                .collect(Collectors.toList());
    }

    /** 4) 社交网络图（用户之间的互动边） */
    @Override
    public Map<String, Object> getReplyGraph(String pid, Integer limit) {
        List<Map<String, ?>> pairs = visualizationDao.selectReplyPairs(pid, limit == null ? 500 : limit);
        if (pairs == null || pairs.isEmpty()) {
            return Map.of("nodes", List.of(), "links", List.of());
        }

        Map<String, Integer> edgeCnt = new HashMap<>();
        Set<String> users = new HashSet<>();
        for (Map<String, ?> p : pairs) {
            String src = Objects.toString(p.get("childUser"), "");
            String tgt = Objects.toString(p.get("parentUser"), "");
            if (src.isEmpty() || tgt.isEmpty() || src.equals(tgt)) continue;
            String key = src + "->" + tgt;
            edgeCnt.merge(key, 1, Integer::sum);
            users.add(src);
            users.add(tgt);
        }

        Map<String, Integer> degree = new HashMap<>();
        for (Map.Entry<String, Integer> e : edgeCnt.entrySet()) {
            String[] st = e.getKey().split("->");
            degree.merge(st[0], e.getValue(), Integer::sum);
            degree.merge(st[1], e.getValue(), Integer::sum);
        }

        int maxDeg = degree.values().stream().max(Integer::compareTo).orElse(1);
        List<Map<String, Object>> nodes = users.stream().map(u -> {
            int d = degree.getOrDefault(u, 1);
            int size = 10 + (int)Math.round(40.0 * d / maxDeg);
            Map<String, Object> n = new HashMap<>();
            n.put("id", u);
            n.put("name", u);
            n.put("symbolSize", size);
            return n;
        }).collect(Collectors.toList());

        List<Map<String, Object>> links = edgeCnt.entrySet().stream().map(e -> {
            String[] st = e.getKey().split("->");
            Map<String, Object> l = new HashMap<>();
            l.put("source", st[0]);
            l.put("target", st[1]);
            l.put("value", e.getValue());
            return l;
        }).collect(Collectors.toList());

        Map<String, Object> res = new HashMap<>();
        res.put("nodes", nodes);
        res.put("links", links);
        return res;
    }
}
