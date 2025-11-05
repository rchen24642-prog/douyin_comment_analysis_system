package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.service.CommentIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/es")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EsReindexController {

    private final CommentIndexService commentIndexService;

    /**
     * ✅ 手动重建指定项目的 ES 索引
     */
    @PostMapping("/reindex/{pid}")
    public Result reindex(@PathVariable String pid) {
        try {
            commentIndexService.indexCommentsByPid(pid);
            return Result.success("✅ 索引已重建: " + pid);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("索引重建失败：" + e.getMessage());
        }
    }
}
