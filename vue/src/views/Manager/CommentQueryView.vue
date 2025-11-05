<template>
  <div class="comment-query">
    <h2>评论查询</h2>
    <el-card>
      <div class="header">
        <el-input
            v-model="pid"
            placeholder="请输入项目 PID"
            clearable
            style="width: 400px"
        />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>

      <el-table
          v-if="tableData.length"
          :data="tableData"
          border
          stripe
          style="margin-top: 20px"
      >
        <el-table-column prop="cid" label="评论ID" width="220" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column
            prop="content"
            label="评论内容"
            min-width="260"
            :show-overflow-tooltip="true"
        />
        <el-table-column label="情感" width="100">
          <template #default="{ row }">
            <el-tag
                :type="
                row.sentimentLabel === 1
                  ? 'success'
                  : row.sentimentLabel === 0
                  ? 'info'
                  : 'danger'
              "
            >
              {{
                row.sentimentLabel === 1
                    ? "正面"
                    : row.sentimentLabel === 0
                        ? "中性"
                        : "负面"
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confidenceScore" label="置信度" width="100" />
        <el-table-column prop="likeCount" label="点赞数" width="80" />
        <el-table-column prop="replyCount" label="回复数" width="80" />
        <el-table-column prop="commentTime" label="评论时间" width="180" />
      </el-table>

      <el-empty
          v-else
          description="请输入 PID 后点击查询"
          style="margin-top: 40px"
      />

      <div style="margin-top: 15px; text-align: right;">
        <el-pagination
            background
            layout="total, prev, pager, next"
            :current-page="page"
            :page-size="pageSize"
            :total="total"
            @current-change="changePage"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import request from "@/utils/requests";

export default {
  name: "CommentQueryView",
  data() {
    return {
      pid: "",
      tableData: [],
      total: 0,
      page: 1,
      pageSize: 10,
    };
  },
  methods: {
    async fetchData() {
      if (!this.pid) {
        this.$message.warning("请输入项目PID");
        return;
      }

      try {
        const res = await request.get("/comment/with-sentiment", {
          params: {
            pid: this.pid,
            page: this.page,
            size: this.pageSize,
          },
        });

        if (res.code === "0") {
          this.tableData = res.data.list || [];
          this.total = res.data.total || 0;
        } else {
          this.$message.error(res.msg || "查询失败");
        }
      } catch (err) {
        console.error("请求错误：", err);
        this.$message.error("连接服务器失败");
      }
    },
    changePage(p) {
      this.page = p;
      this.fetchData();
    },
  },
};
</script>

<style scoped>
.comment-query {
  padding: 20px;
}
.header {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
