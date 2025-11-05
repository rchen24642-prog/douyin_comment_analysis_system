package com.example.springboot.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface VisualizationService {

    Map<String, Object> getSentimentDistribution(String pid);

    List<Map<String, Object>> getDailyTrend(String pid, LocalDate start, LocalDate end);

    List<Map<String, Object>> getTopKeywords(String pid, Integer limit);

    Map<String, Object> getReplyGraph(String pid, Integer limit);
}
