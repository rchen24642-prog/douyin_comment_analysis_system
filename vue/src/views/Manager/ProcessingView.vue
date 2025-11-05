<template>
  <div class="processing-container">
    <h2>数据清洗</h2>
    <el-card class="upload-card">

      <!-- 文件上传 -->
      <el-upload
          class="upload-demo"
          drag
          action=""
          :auto-upload="false"
          :on-change="handleFileChange"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">拖拽文件到此处，或点击上传</div>
        <div class="el-upload__tip">支持 Excel (.xlsx) / CSV 文件</div>
      </el-upload>

      <!-- 项目名称输入 -->
      <el-input
          v-model="projectName"
          placeholder="请输入项目名称"
          style="margin-top: 15px;"
      ></el-input>

      <!-- 多选清洗类型 -->
      <el-select
          v-model="selectedOptions"
          multiple
          placeholder="请选择清洗操作"
          style="width: 100%; margin-top: 15px;"
      >
        <el-option
            v-for="opt in cleanOptions"
            :key="opt"
            :label="opt"
            :value="opt"
        ></el-option>
      </el-select>

      <!-- 操作按钮 -->
      <div style="margin-top: 20px;">
        <el-button
            type="primary"
            :loading="loading"
            @click="uploadAndClean"
        >开始清洗</el-button>
        <el-button
            v-if="downloadUrl"
            type="success"
            @click="downloadCleanedFile"
        >下载清洗结果</el-button>
      </div>

      <!-- 状态显示 -->
      <div v-if="statusMsg" style="margin-top: 15px;">
        <el-alert
            :title="statusMsg"
            :type="statusType"
            show-icon
            :closable="false"
        />
      </div>
    </el-card>

    <!-- 清洗结果预览 -->
    <el-card class="result-card">
      <h3>✅ 最新清洗数据（数据库中最近50条）</h3>
      <el-table
          :data="previewData"
          border
          stripe
          v-loading="previewLoading"
          style="width: 100%"
          empty-text="暂无清洗数据"
      >
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column prop="content" label="评论内容" />
        <el-table-column prop="commentTime" label="时间" width="180" />
        <el-table-column prop="likeCount" label="点赞数" width="100" />
        <el-table-column prop="replyCount" label="回复数" width="100" />
        <el-table-column prop="commentType" label="类型" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import request from "@/utils/requests"; // 使用统一封装的 axios
import axios from "axios";

export default {
  name: "ProcessingView",
  data() {
    return {
      file: null,
      projectName: "",
      selectedOptions: [],
      cleanOptions: [
        "删除缺失行",
        "填充默认值",
        "删除重复行",
        "箱型图检测",
        "3σ检测",
        "日期标准化",
        "网络用语替换"
      ],
      previewData: [],
      previewLoading: false,
      downloadUrl: "",
      loading: false,
      statusMsg: "",
      statusType: "info"
    };
  },
  created() {
    // 页面加载时自动获取数据库最新50条清洗结果
    this.loadPreview();
  },
  methods: {
    handleFileChange(file) {
      this.file = file.raw;
    },

    // ========== 上传并清洗 ==========
    async uploadAndClean() {
      if (!this.file || !this.projectName) {
        this.$message.warning("请上传文件并输入项目名称！");
        return;
      }

      this.loading = true;
      this.statusMsg = "正在清洗中，请稍候...";
      this.statusType = "info";

      try {
        const formData = new FormData();
        formData.append("file", this.file);
        formData.append("project_name", this.projectName);
        formData.append("options", JSON.stringify(this.selectedOptions));
        formData.append('user_uuid', localStorage.getItem('uuid'));

        const res = await axios.post("http://localhost:9090/clean/upload", formData,{
          timeout: 60000
        });

        this.loading = false;
        const data = res.data && typeof res.data === "object" ? res.data : JSON.parse(res.data || "{}");
        console.log("清洗返回：", data);

        if (data.status === "success") {
          this.statusMsg = data.message || "清洗完成";
          this.statusType = "success";
          this.downloadUrl = "http://127.0.0.1:5001/" + data.output_path;

          // ✅ 刷新数据库中的最新清洗数据
          this.loadPreview();
        } else {
          this.statusMsg = "清洗失败：" + (data.message || "未知错误");
          this.statusType = "error";
        }
      } catch (err) {
        console.error(err);
        this.loading = false;
        this.statusMsg = "清洗请求失败，请检查后端服务。";
        this.statusType = "error";
      }
    },

    // ========== 读取数据库中最近50条清洗结果 ==========
    loadPreview() {
      this.previewLoading = true;
      const uuid = localStorage.getItem("uuid");  // ✅ 取登录用户的 uuid
      request
          .get("/comment/preview", { params: { uuid } })  // ✅ 加上 uuid 参数
          .then(res => {
            if (res.code === "0") {
              this.previewData = res.data;
            } else {
              this.$notify.error(res.msg || "获取清洗数据失败");
            }
            this.previewLoading = false;
          })
          .catch(() => {
            this.$message.error("请求失败，请检查后端服务");
            this.previewLoading = false;
          });
    },

    downloadCleanedFile() {
      window.open(this.downloadUrl, "_blank");
    }
  }
};
</script>

<style scoped>
.processing-container {
  width: 100%;          /* ✅ 占满父容器 */
  max-width: none;      /* ✅ 移除固定宽度限制 */
  padding: 20px 40px;   /* ✅ 可适当调整左右留白 */
  box-sizing: border-box;
}
.upload-card,
.result-card {
  margin-bottom: 30px;
}
.el-upload {
  width: 100%;
}
</style>
