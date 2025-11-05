package com.example.springboot.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentDocument {

    private String cid;
    private String content_clean;
    private String username;
    private Integer like_count;
    private Integer sentiment_label;
    private String comment_time;
    private String pid;
    private String uuid;
}
