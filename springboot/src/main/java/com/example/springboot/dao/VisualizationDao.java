package com.example.springboot.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface VisualizationDao {

    /** 情感分布统计 */
    List<Map<String, ?>> selectSentimentDistribution(@Param("pid") String pid);

    /** 舆情热度趋势（评论数 + 点赞数） */
    List<Map<String, ?>> selectDailyTrend(@Param("pid") String pid,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);

    /** 评论文本（用于关键词统计） */
    List<String> selectRecentCommentTexts(@Param("pid") String pid,
                                          @Param("limit") Integer limit);

    /** 子评论与父评论用户名对（用于社交网络图） */
    List<Map<String, ?>> selectReplyPairs(@Param("pid") String pid,
                                          @Param("limit") Integer limit);
}
