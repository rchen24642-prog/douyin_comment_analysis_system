package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.common.ResultCode;
import com.example.springboot.entity.Sentiment;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.SentimentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import com.example.springboot.service.DataSyncService;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/sentiment")
public class SentimentController {

    @Resource
    SentimentService sentimentService;

    @Resource
    private DataSyncService dataSyncService;

    // 获取所有情感分析记录
    @GetMapping("/alldata")
    public Result getAllSentiments() {
        List<Sentiment> list = sentimentService.getAll();
        if (list.isEmpty()) {
            throw new CustomException(ResultCode.DATA_LESS);
        }
        return Result.success(list);
    }

    // 根据情感分析ID查询
    @GetMapping("/{sid}")
    public Result getSentimentById(@PathVariable Integer sid) {
        Sentiment sentiment = sentimentService.getBySid(sid);
        if (sentiment == null) {
            throw new CustomException(ResultCode.DATA_NOT_FOUND);
        }
        return Result.success(sentiment);
    }

    // 根据项目ID查询该项目的情感分析记录
    @GetMapping("/byproject/{pid}")
    public Result getSentimentsByProject(@PathVariable String pid) {
        List<Sentiment> list = sentimentService.getByProjectId(pid);
        if (list.isEmpty()) {
            throw new CustomException(ResultCode.DATA_LESS);
        }
        return Result.success(list);
    }

    // 根据评论ID查询情感分析记录
    @GetMapping("/bycomment/{cid}")
    public Result getSentimentByComment(@PathVariable String cid) {
        Sentiment sentiment = sentimentService.getByCommentId(cid);
        if (sentiment == null) {
            throw new CustomException(ResultCode.DATA_NOT_FOUND);
        }
        return Result.success(sentiment);
    }

    /**
     * 触发情感分析
     * 前端调用：POST /sentiment/analyze/{pid}
     */
    @PostMapping("/analyze/{pid}")
    public Result analyzeSentiment(@PathVariable String pid) {
        String message = dataSyncService.analyzeSentimentByPython(pid);
        return Result.success(message);
    }

    // ✅ 新增：兼容 JSON 体传 pid 的调用：POST /sentiment/analyze
    @PostMapping("/analyze")
    public Result analyzeSentimentJson(@RequestBody Map<String, Object> body) {
                Object pidObj = body.get("pid");
                if (pidObj == null) {
                        return Result.error("pid is required");
                    }
                String pid = String.valueOf(pidObj);
                String message = dataSyncService.analyzeSentimentByPython(pid);
                return Result.success(message);
            }

    @GetMapping("/table/{pid}")
    public Result getTableData(@PathVariable String pid) {
        List<Map<String, Object>> list = sentimentService.getWithContentByProjectId(pid);
        if (list == null || list.isEmpty()) {
            throw new CustomException(ResultCode.DATA_LESS);
        }
        return Result.success(list);
    }
}
