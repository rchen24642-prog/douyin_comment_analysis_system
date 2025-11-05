<template>
  <div class="project-view">
    <h2>项目管理</h2>
    <el-card>
      <!-- ✅ 筛选区域 -->
      <div style="margin-bottom: 20px; display: flex; align-items: center;">
        <span style="margin-right: 10px;">运行状态：</span>
        <el-select v-model="filterStatus" placeholder="请选择运行状态" @change="fetchProjectList">
          <el-option label="全部" value=""></el-option>
          <el-option label="成功" value="success"></el-option>
          <el-option label="失败" value="fail"></el-option>
          <el-option label="运行中" value="running"></el-option>
        </el-select>
      </div>

      <!-- ✅ 项目表格 -->
      <el-table :data="projectList" border style="width: 100%">
        <el-table-column prop="pid" label="项目ID" align="center" width="220"></el-table-column>
        <el-table-column prop="projectName" label="项目名称" align="center"></el-table-column>
        <el-table-column prop="cleanType" label="清洗类型" align="center"></el-table-column>

        <el-table-column prop="status" label="运行状态" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 'success'" type="success">成功</el-tag>
            <el-tag v-else-if="scope.row.status === 'fail'" type="danger">失败</el-tag>
            <el-tag v-else type="info">运行中</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" align="center" width="200">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>

        </el-table>

      <!-- ✅ 分页组件 -->
      <div style="margin-top: 20px; text-align: right;">
        <el-pagination
            @current-change="handlePageChange"
            :current-page="pageNum"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="total">
        </el-pagination>
      </div>
    </el-card>
  </div>
</template>

<script>
import request from "@/utils/requests";

export default {
  name: "ProjectView",
  data() {
    return {
      projectList: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      filterStatus: "success", // ✅ 默认展示“成功”项目
    };
  },
  created() {
    this.fetchProjectList();
  },
  methods: {
    async fetchProjectList() {
      try {
        const res = await request.get("/project/list", {
          params: {
            pageNum: this.pageNum,
            pageSize: this.pageSize,
            status: this.filterStatus, // ✅ 英文传参
          },
        });

        if (res.code === "0") {
          this.projectList = res.data.list || [];
          this.total = res.data.total || 0;
        } else {
          this.$message.error(res.msg || "加载失败");
        }
      } catch (err) {
        this.$message.error("请求出错：" + err);
      }
    },
    handlePageChange(page) {
      this.pageNum = page;
      this.fetchProjectList(); // ✅ 翻页自动更新
    },
    formatDate(dateStr) {
      if (!dateStr) return "";
      // ✅ 去掉 T，兼容 ISO 格式
      dateStr = dateStr.replace("T", " ");
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return dateStr; // 防止解析失败
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, "0");
      const d = String(date.getDate()).padStart(2, "0");
      const h = String(date.getHours()).padStart(2, "0");
      const mi = String(date.getMinutes()).padStart(2, "0");
      const s = String(date.getSeconds()).padStart(2, "0");
      return `${y}-${m}-${d} ${h}:${mi}:${s}`;
    },


  },
};
</script>

<style scoped>
.project-view {
  padding: 20px;
}
</style>
