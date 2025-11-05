package com.example.springboot.controller;
import com.alibaba.fastjson2.JSONObject;
import com.example.springboot.common.Result;
import com.example.springboot.service.GraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/graph")
public class GraphController {

    @Autowired
    private GraphService graphService;

    // POST /graph/build/{pid}
    @PostMapping("/build/{pid}")
    public Result buildGraph(@PathVariable String pid) {
        JSONObject result = graphService.buildGraph(pid);
        return Result.success(result);
    }

    // GET /graph/project/{pid}
    @GetMapping("/project/{pid}")
    public Result getProjectGraph(@PathVariable String pid) {
        JSONObject result = graphService.getGraphProject(pid);
        return Result.success(result);
    }
}
