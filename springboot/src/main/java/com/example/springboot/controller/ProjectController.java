package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.common.ResultCode;
import com.example.springboot.entity.Project;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.ProjectService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/project")
public class ProjectController {

    @Resource
    ProjectService projectService;

    // 获取所有项目
    @GetMapping("/alldata")
    public Result getAllProjects(@RequestHeader("uuid") String uuid){
        List<Project> projects = projectService.getAllByUuid(uuid);
        if (projects.isEmpty()) {
            throw new CustomException(ResultCode.DATA_LESS);
        }
        return Result.success(projects);
    }

    // 根据项目ID查询单个项目
    @GetMapping("/{pid}")
    public Result getProjectById(@PathVariable String pid, @RequestHeader("uuid") String uuid) {
        Project project = projectService.getByPid(pid);
        if (project == null) {
            throw new CustomException(ResultCode.DATA_NOT_FOUND);
        }
        // ⛔ 核心：只允许项目归属人为当前uuid
        if (project.getUuid() == null || !uuid.equals(project.getUuid())) {
            throw new CustomException(ResultCode.NO_AUTH); // 403/无权限
        }
        return Result.success(project);
    }


    // 分页+筛选接口
    @GetMapping("/list")
    public Result listProjects(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestHeader("uuid") String uuid
    ) {
        PageHelper.startPage(pageNum, pageSize);
        List<Project> projects = projectService.getByStatusAndUid(status, uuid);
        PageInfo<Project> pageInfo = new PageInfo<>(projects);
        return Result.success(pageInfo);
    }

}