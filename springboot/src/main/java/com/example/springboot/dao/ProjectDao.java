package com.example.springboot.dao;

import com.example.springboot.entity.Project;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Repository
public interface ProjectDao extends Mapper<Project> {
    // ✅ 只更新状态与结束时间
    @Update("UPDATE project SET status = #{status}, end_time = #{endTime} " +
            "WHERE pid = #{pid}")
    void updateStatus(Project project);

    List<Project> selectByStatus(@Param("status") String status);

    // ✅ 新增：根据uid查所有项目
    @Select("SELECT * FROM project " +
            "WHERE uuid = #{uuid} " +
            "ORDER BY create_time DESC")
    List<Project> selectByUuid(@Param("uuid") String uuid);

    // ✅ 新增：根据uid和状态查项目
    @Select({
            "<script>",
            "SELECT * FROM project WHERE uuid = #{uuid}",
            "<if test='status != null and status != \"\"'>",
            "AND status = #{status}",
            "</if>",
            "ORDER BY create_time DESC",
            "</script>"
    })
    List<Project> selectByStatusAndUid(@Param("status") String status, @Param("uuid") String uuid);
}
