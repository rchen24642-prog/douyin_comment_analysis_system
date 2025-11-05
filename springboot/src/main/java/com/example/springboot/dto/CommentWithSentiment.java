package com.example.springboot.dto;

import lombok.Data;

@Data
public class CommentWithSentiment {
    private String cid;
    private String pid;
    private String username;
    private String content;
    private String commentTime;
    private Integer likeCount;
    private Integer replyCount;
    private Integer commentType;
    private String parentCid;

    // 来自 sentiment 表的字段
    private Integer sentimentLabel;   // -1：负面，0：中性，1：正面
    private Double confidenceScore;    // 可选：情感置信度（如果表中有）
}
