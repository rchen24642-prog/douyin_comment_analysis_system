package com.example.springboot.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class HttpUtils {

    private static final String PYTHON_BASE_URL = "http://127.0.0.1:5001";

    // GET 请求
    public static JSONObject doGet(String path) {
        String url = PYTHON_BASE_URL + path;
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return JSON.parseObject(response.getBody());
    }

    // POST 请求
    public static JSONObject doPost(String path, JSONObject body) {
        String url = PYTHON_BASE_URL + path;
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return JSON.parseObject(response.getBody());
    }
}
