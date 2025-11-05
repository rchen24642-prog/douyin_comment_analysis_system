package com.example.springboot.service;

import com.alibaba.fastjson2.JSONObject;
import com.example.springboot.utils.HttpUtils;
import org.springframework.stereotype.Service;

@Service
public class GraphService {

    // 调用 Flask 构建图接口
    public JSONObject buildGraph(String pid) {
        JSONObject body = new JSONObject();
        body.put("pid", pid);
        return HttpUtils.doPost("/graph/build", body);
    }

    // 调用 Flask 导出图接口
    public JSONObject getGraphProject(String pid) {
        return HttpUtils.doGet("/graph/project?pid=" + pid);
    }
}
