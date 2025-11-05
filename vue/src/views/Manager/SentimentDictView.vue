<template>
  <div class="dict-container">
    <h2>情感词典管理</h2>
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-select
          v-model="filterType"
          placeholder="筛选情感类型"
          clearable
          style="width: 160px; margin-right: 10px;"
      >
        <el-option label="正面" value="positive"></el-option>
        <el-option label="中性" value="neutral"></el-option>
        <el-option label="负面" value="negative"></el-option>
      </el-select>

      <span class="label">词汇：</span>
      <el-input
          v-model="newWord"
          placeholder="输入新词"
          clearable
          style="width: 180px; margin-right: 10px;"
      ></el-input>

      <span class="label">情感类型：</span>
      <el-select
          v-model="newType"
          placeholder="选择情感"
          style="width: 120px; margin-right: 10px;"
      >
        <el-option label="正面" value="positive"></el-option>
        <el-option label="中性" value="neutral"></el-option>
        <el-option label="负面" value="negative"></el-option>
      </el-select>

      <span class="label">权重：</span>
      <el-input-number
          v-model="newWeight"
          :min="0"
          :max="2"
          :step="0.1"
          style="width: 120px; margin-right: 10px;"
          placeholder="权重"
      />

      <!-- ✅ 权重说明 tooltip -->
      <el-tooltip
          effect="light"
          placement="top-start"
      >
        <div slot="content" style="max-width: 250px; line-height: 1.5;">
          权重用于控制该词的情感影响强度：<br/>
          • 正/负面词：权重越大，对情感分值的加减幅度越高（约 ±0.05×weight）<br/>
          • 中性词：权重越大，越会把分值拉回中间（靠近 0.5）
        </div>
        <i class="el-icon-question" style="color:#409EFF; cursor:pointer; font-size:16px;"></i>
      </el-tooltip>

      <el-button type="primary" @click="addWord">添加词汇</el-button>
    </div>

    <!-- 词典表格 -->
    <el-table
        :data="pagedList"
        border
        stripe
        v-loading="loading"
        style="width: 100%; margin-top: 20px;"
    >
      <el-table-column prop="word" label="词汇" width="200"></el-table-column>

      <el-table-column prop="sentiment" label="情感类型" width="120">
        <template slot-scope="scope">
          <el-tag
              :type="
              scope.row.sentiment === 'positive'
                ? 'success'
                : scope.row.sentiment === 'negative'
                ? 'danger'
                : 'info'
            "
          >
            {{ scope.row.sentiment }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="weight" label="权重" width="100"></el-table-column>

      <el-table-column label="创建时间" min-width="180">
        <template slot-scope="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="100" align="center">
        <template slot-scope="scope">
          <el-button type="danger" size="mini" @click="deleteWord(scope.row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- ✅ 分页组件 -->
    <div style="margin-top: 20px; text-align: right;">
      <el-pagination
          background
          layout="prev, pager, next, jumper"
          :page-size="pageSize"
          :current-page.sync="currentPage"
          :total="filteredList.length"
      />
    </div>
  </div>
</template>

<script>
import request from "@/utils/requests";

export default {
  name: "SentimentDictView",
  data() {
    return {
      list: [],
      loading: false,
      newWord: "",
      newType: "neutral",
      newWeight: 1.0,
      filterType: "",
      pageSize: 10,
      currentPage: 1,
    };
  },
  computed: {
    // 过滤情感类型
    filteredList() {
      if (!this.filterType) return this.list;
      return this.list.filter((item) => item.sentiment === this.filterType);
    },
    // 分页数据
    pagedList() {
      const start = (this.currentPage - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.filteredList.slice(start, end);
    },
  },
  mounted() {
    this.loadList();
  },
  methods: {
    // ✅ 格式化时间函数
    formatTime(val) {
      if (!val) return "-";
      if (Array.isArray(val)) {
        const [y, m, d, h, min, s] = val;
        return `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")} `
            + `${String(h).padStart(2, "0")}:${String(min).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
      }
      if (typeof val === "string" && val.includes("T")) {
        return val.replace("T", " ").split(".")[0];
      }
      return val;
    },

    // ✅ 获取用户词典
    async loadList() {
      this.loading = true;
      try {
        const res = await request.get("/sentiment-dict/list", {
          headers: { uuid: localStorage.getItem("uuid") },
        });
        const rawList = res.data || res.data?.data || [];
        // 按创建时间降序排序
        this.list = rawList.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
      } catch (e) {
        this.$message.error("加载词典失败");
      } finally {
        this.loading = false;
      }
    },

    // ✅ 添加新词
    async addWord() {
      if (!this.newWord || !this.newType) {
        this.$message.warning("请输入完整的词汇和情感类型");
        return;
      }

      const data = {
        word: this.newWord.trim(),
        sentiment: this.newType,
        weight: this.newWeight,
        uuid: localStorage.getItem("uuid"),
      };

      try {
        const res = await request.post("/sentiment-dict/add", data);
        if (res.code === "0") {
          this.$message.success("添加成功");
          this.newWord = "";
          this.loadList();
        } else {
          this.$message.warning(res.msg || "添加失败");
        }
      } catch (e) {
        this.$message.error("添加失败");
      }
    },

    // ✅ 删除词
    async deleteWord(id) {
      try {
        await request.delete(`/sentiment-dict/${id}`);
        this.$message.success("删除成功");
        this.loadList();
      } catch (e) {
        this.$message.error("删除失败");
      }
    },
  },
};
</script>

<style scoped>
.dict-container {
  padding: 20px;
  background-color: #f9f9f9;
}
.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 15px;
}
</style>
