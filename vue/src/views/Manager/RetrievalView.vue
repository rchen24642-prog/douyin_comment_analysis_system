AboutView.vue<template>
  <div class="retrieval-container">
    <h2>数据检索</h2>
    <template>
      <!-- 搜索条件卡片 -->
      <el-card class="mb-4" shadow="hover">
        <div class="filter-area">
          <el-form :inline="true" label-width="80px">
            <el-form-item label="关键词">
              <el-input v-model="filters.keyword" placeholder="输入关键词"></el-input>
            </el-form-item>

            <el-form-item label="用户名">
              <el-input v-model="filters.username" placeholder="输入用户名"></el-input>
            </el-form-item>

            <el-form-item label="情感标签">
              <el-select v-model="filters.sentiment" placeholder="全部" clearable style="width: 120px">
                <el-option label="正面" :value="1"></el-option>
                <el-option label="中性" :value="0"></el-option>
                <el-option label="负面" :value="-1"></el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="时间范围">
              <el-date-picker
                  v-model="filters.dateRange"
                  type="daterange"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
              />
            </el-form-item>

            <el-form-item label="点赞数">
              <div class="like-range">
                <el-input-number v-model="filters.minLike" :min="0" placeholder="最小" />
                <span class="divider"> - </span>
                <el-input-number v-model="filters.maxLike" :min="0" placeholder="最大" />
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <!-- 搜索结果 -->
      <el-card shadow="never">
        <el-table :data="tableData" border stripe style="width: 100%">
          <el-table-column prop="cid" label="评论ID" width="180" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="content_clean" label="评论内容" min-width="300">
            <template #default="{ row }">
              <span v-html="row.content_clean"></span>
            </template>
          </el-table-column>
          <el-table-column prop="sentiment_label" label="情感" width="80">
            <template #default="{ row }">
              <el-tag
                  :type="row.sentiment_label === 1 ? 'success' : row.sentiment_label === 0 ? 'info' : 'danger'"
              >
                {{ row.sentiment_label === 1 ? '正面' : row.sentiment_label === 0 ? '中性' : '负面' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="like_count" label="点赞数" width="100" />
          <el-table-column prop="comment_time" label="评论时间" width="180" />
        </el-table>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
              background
              layout="total, prev, pager, next"
              :total="total"
              :page-size="pageSize"
              @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </template>

  </div>
</template>

<script>
import request from "@/utils/requests";
export default {
  name: "RetrievalView",
  data() {
    return {
      filters: {
        keyword: "",
        username: "",
        sentiment: null,
        dateRange: [],
        minLike: null,
        maxLike: null,
      },
      tableData: [],
      total: 0,
      page: 0,
      pageSize: 10,
    };
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    async handleSearch() {
      const [startTime, endTime] = this.filters.dateRange || [];
      try {
        const { data } = await request.get("/comment/search", {
          params: {
            keyword: this.filters.keyword,
            username: this.filters.username,
            sentiment: this.filters.sentiment,
            startTime,
            endTime,
            minLike: this.filters.minLike,
            maxLike: this.filters.maxLike,
            page: this.page,
            size: this.pageSize,
          },
        });
        console.log("🔍 返回结果：", data); // 调试输出

        if (data.total !== undefined) {
          this.tableData = data.data || [];
          this.total = data.total || 0;
        } else if (data.code === "0") {
          this.tableData = data.data.list || [];
          this.total = data.data.total || 0;
        } else {
          this.$message.error(data.msg || "搜索失败");
        }
      } catch (err) {
        console.error("请求错误：", err);
        this.$message.error("连接服务器失败");
      }
    },

    handleReset() {
      this.filters = {
        keyword: "",
        username: "",
        sentiment: null,
        dateRange: [],
        minLike: null,
        maxLike: null,
      };
      this.handleSearch();
    },
    handlePageChange(page) {
      this.page = page - 1; // Spring Boot 从0页开始
      this.handleSearch();
    },
  },
  mounted() {
    this.handleSearch();
  },
};
</script>

<style scoped>
.retrieval-container {
  padding: 20px;
}

.filter-area {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.like-range {
  display: flex;
  align-items: center;
}

.divider {
  margin: 0 8px;
  color: #999;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

::v-deep em {
  font-style: normal;
  background: #EFD658;
  padding: 0 2px;
  border-radius: 2px;
}
</style>