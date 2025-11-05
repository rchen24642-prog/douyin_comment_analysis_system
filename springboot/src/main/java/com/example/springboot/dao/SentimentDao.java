package com.example.springboot.dao;

import com.example.springboot.entity.Sentiment;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Repository
public interface SentimentDao extends Mapper<Sentiment> {
    @Select("""
        SELECT DISTINCT
            s.sid,
            s.cid,
            s.pid,
            c.content,
            s.sentiment_label AS sentimentLabel,
            s.confidence_score AS confidenceScore,
            s.analysis_time AS analysisTime
        FROM sentiment s
        LEFT JOIN comment c ON s.cid = c.cid AND c.pid = s.pid
        WHERE s.pid = #{pid}
        ORDER BY s.analysis_time DESC
    """)
    List<Map<String, Object>> selectWithContentByPid(@Param("pid") String pid);

    @Select("SELECT sentiment_label FROM sentiment WHERE cid = #{cid} LIMIT 1")
    Integer findLabelByCid(@Param("cid") String cid);

}
