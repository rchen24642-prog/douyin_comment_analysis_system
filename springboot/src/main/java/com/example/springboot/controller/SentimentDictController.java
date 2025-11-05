package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.SentimentDict;
import com.example.springboot.service.SentimentDictService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/sentiment-dict")
public class SentimentDictController {

    @Resource
    private SentimentDictService service;

    @GetMapping("/list")
    public Result list(@RequestHeader("uuid") String uuid) {
        List<SentimentDict> list = service.getUserDict(uuid);
        return Result.success(list);
    }

    @PostMapping("/add")
    public Result add(@RequestBody SentimentDict dict) {
        service.addWord(dict);
        return Result.success("添加成功");
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        service.deleteWord(id);
        return Result.success("删除成功");
    }
}
