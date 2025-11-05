package com.example.springboot.controller;

import com.example.springboot.service.DataCleanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
@RestController
@RequestMapping("/clean")
public class DataCleanController {

    @Autowired
    private DataCleanService dataCleanService;

    /**
     * 上传文件并调用Python Flask接口执行清洗
     * @param file 上传的Excel或CSV文件
     * @param projectName 项目名称
     * @param options 前端传入的清洗选项（JSON数组字符串）
     * @return Flask返回的清洗结果JSON（含数据库preview）
     */
    @PostMapping("/upload")
    public String uploadAndClean(
            @RequestPart("file") MultipartFile file,
            @RequestPart("project_name") String projectName,
            @RequestPart(value = "options", required = false) String options,
            @RequestPart("user_uuid") String userUuid
    ) {
        if (options == null || options.isBlank()) {
            options = "[]"; // ✅ 手动设置默认值
        }
        return dataCleanService.processData(file, projectName, options, userUuid);
    }

}
