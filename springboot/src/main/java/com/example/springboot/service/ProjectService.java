package com.example.springboot.service;

import com.example.springboot.dao.ProjectDao;
import com.example.springboot.entity.Project;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ProjectService {

    @Resource
    private ProjectDao projectDao;

    //根据用户uuid获取所有项目
    public List<Project> getAllByUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return List.of();
        }
        return projectDao.selectByUuid(uuid);
    }

    // 根据状态和用户uid获取项目
    public List<Project> getByStatusAndUid(String status, String uuid) {
        if (status == null || status.isEmpty()) {
            return projectDao.selectByUuid(uuid);
        }

        switch (status) {
            case "成功" -> status = "success";
            case "失败" -> status = "fail";
            case "运行中" -> status = "running";
        }

        return projectDao.selectByStatusAndUid(status, uuid);
    }

    // 获取所有项目
    public List<Project> getAll() {
        return projectDao.selectAll();
    }

    // 根据项目ID查询单个项目
    public Project getByPid(String pid) {
        return projectDao.selectByPrimaryKey(pid);
    }

    // 新增项目
    public int addProject(Project project) {
        return projectDao.insert(project);
    }

    // 更新项目信息
    public int updateProject(Project project) {
        return projectDao.updateByPrimaryKeySelective(project);
    }


    public List<Project> getByStatus(String status) {
        if (status == null || status.isEmpty()) {
            return projectDao.selectAll();
        }

        // ✅ 映射前端中文状态到数据库英文值
        switch (status) {
            case "成功" -> status = "success";
            case "失败" -> status = "fail";
            case "运行中" -> status = "running";
        }

        return projectDao.selectByStatus(status);
    }

}
