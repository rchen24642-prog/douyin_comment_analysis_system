package com.example.springboot.controller;

import com.example.springboot.service.VisualizationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/visual")
public class VisualizationController {

    @Resource
    private VisualizationService visualizationService;

    /**
     * 情感分布（按项目可选）
     * GET /visual/sentiment?pid=xxx
     * 返回: { positive: 532, neutral: 214, negative: 125 }
     */
    @GetMapping("/sentiment")
    public Map<String, Object> sentiment(@RequestParam(required = false) String pid) {
        return visualizationService.getSentimentDistribution(pid);
    }

    /**
     * 舆情热度趋势（每日评论数 + 点赞数）
     * GET /visual/trend?pid=xxx&start=2025-10-01&end=2025-10-31
     * 返回: [{date:"2025-10-01", comments:123, likes:520}, ...]
     */
    @GetMapping("/trend")
    public List<Map<String, Object>> trend(@RequestParam(required = false) String pid,
                                           @RequestParam(required = false) String start,
                                           @RequestParam(required = false) String end) {
        LocalDate s = (start == null || start.isEmpty()) ? null : LocalDate.parse(start);
        LocalDate e = (end == null || end.isEmpty()) ? null : LocalDate.parse(end);
        return visualizationService.getDailyTrend(pid, s, e);
    }

    /**
     * 关键词 TopN（默认Top20，简单分词）
     * GET /visual/keywords?pid=xxx&limit=20
     * 返回: [{word:"疫情", count:80}, ...]
     */
    @GetMapping("/keywords")
    public List<Map<String, Object>> keywords(@RequestParam(required = false) String pid,
                                              @RequestParam(defaultValue = "20") Integer limit) {
        return visualizationService.getTopKeywords(pid, limit);
    }

    /**
     * 社交网络图（通过父子评论构边）
     * GET /visual/graph?pid=xxx&limit=500
     * 返回: { nodes:[{id:"用户A",name:"用户A",symbolSize:30},...], links:[{source:"用户A",target:"用户B",value:3},...] }
     */
    @GetMapping("/graph")
    public Map<String, Object> graph(@RequestParam(required = false) String pid,
                                     @RequestParam(defaultValue = "500") Integer limit) {
        return visualizationService.getReplyGraph(pid, limit);
    }
}
