package com.example.springboot.dao;

import com.example.springboot.entity.Comment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Repository
public interface CommentDao extends Mapper<Comment> {

    // ✅ 获取最近清洗完成的数据（50条）
    @Select("""
    SELECT c.* FROM comment c
    JOIN project p ON c.pid = p.pid
    WHERE c.clean_status = 'cleaned'
      AND p.uuid = #{uuid}
    ORDER BY c.comment_time DESC
    LIMIT #{limit}
""")
    List<Comment> selectRecentCleaned(@Param("uuid") String uuid, @Param("limit") int limit);

    // ✅ 检查同一用户、同一评论内容、同一状态是否已存在（防止重复插入）
    @Select("""
        SELECT COUNT(*) > 0 
        FROM comment 
        WHERE username = #{username}
          AND content = #{content}
          AND clean_status = #{cleanStatus}
    """)
    Boolean existsByUserContentStatus(@Param("username") String username,
                                      @Param("content") String content,
                                      @Param("cleanStatus") String cleanStatus);

    @Select("SELECT COUNT(*) > 0 FROM comment WHERE cid = #{cid}")
    Boolean existsByCid(@Param("cid") String cid);

    // 按项目 pid 获取评论（用于增量同步 ES）
    @Select("SELECT * FROM comment WHERE pid = #{pid} ORDER BY comment_time DESC")
    List<Comment> selectByProject(@Param("pid") String pid);

    // ✅ 联查 comment + sentiment（按 pid & uuid）
    @Select("""
SELECT 
  c.cid, c.pid, c.username, c.content,
  DATE_FORMAT(c.comment_time, '%Y-%m-%d %H:%i:%s') AS commentTime,
  c.like_count AS likeCount, c.reply_count AS replyCount,
  c.comment_type AS commentType, c.parent_cid AS parentCid,
  s.sentiment_label AS sentimentLabel, s.confidence_score AS confidenceScore
FROM comment c
JOIN project p ON c.pid = p.pid
LEFT JOIN sentiment s ON s.cid = c.cid
WHERE c.pid = #{pid} AND p.uuid = #{uuid}
ORDER BY c.comment_time DESC
LIMIT #{limit} OFFSET #{offset}
""")
    List<com.example.springboot.dto.CommentWithSentiment> selectWithSentimentByPid(
            @Param("pid") String pid,
            @Param("uuid") String uuid,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
SELECT COUNT(1)
FROM comment c
JOIN project p ON c.pid = p.pid
WHERE c.pid = #{pid} AND p.uuid = #{uuid}
""")
    int countByPidAndUuid(@Param("pid") String pid, @Param("uuid") String uuid);


}
