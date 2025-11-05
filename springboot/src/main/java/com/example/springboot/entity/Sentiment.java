package com.example.springboot.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;


@Table(name = "sentiment")
public class Sentiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sid;  // 主键 ID，自增

    @Column(name = "cid", length = 50, nullable = false)
    private String cid;  // 评论编号（关联 comment.cid）

    @Column(name = "pid", length = 50, nullable = false)
    private String pid;  // 项目编号（关联 project.pid）

    @Column(name = "sentiment_label")
    private Integer sentimentLabel;  // 情感标签：1=正面，0=中性，-1=负面

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore ;  // 置信度（0-1之间）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "analysis_time", nullable = false)
    private LocalDateTime analysisTime;

    // ====================== getter & setter ======================

    public Integer getSid() {
        return sid;
    }

    public void setSid(Integer sid) {
        this.sid = sid;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Integer getSentimentLabel() {
        return sentimentLabel;
    }

    public void setSentimentLabel(Integer sentimentLabel) {
        this.sentimentLabel = sentimentLabel;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public LocalDateTime getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(LocalDateTime analysisTime) {
        this.analysisTime = analysisTime;
    }
}

