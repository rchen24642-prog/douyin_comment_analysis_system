<template>
  <div class="user-info-container">
    <el-card class="info-card">
      <h2>用户中心</h2>
      <el-form :model="user" label-width="100px" style="max-width:auto; margin: auto;">
        <el-form-item label="头像">
          <el-upload
              class="avatar-uploader"
              action="http://localhost:9090/user/upload-avatar"
              :data="{ uuid: user.uuid }"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
          >
            <img v-if="user.avatarUrl" :src="user.avatarUrl" class="avatar" />
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
        </el-form-item>

        <el-form-item label="用户ID">
          <el-input v-model="user.uuid" disabled></el-input>
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="user.username"></el-input>
        </el-form-item>

        <el-form-item label="角色">
          <el-input v-model="user.role" disabled></el-input>
        </el-form-item>

        <el-form-item label="创建时间">
          <el-input v-model="user.createdAt" disabled></el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="updateUsername">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import request from "@/utils/requests";

export default {
  name: "UserInfoView",
  data() {
    return {
      user: {}
    };
  },
  created() {
    const uuid = localStorage.getItem("uuid");
    if (!uuid) {
      this.$message.error("用户未登录！");
      this.$router.push("/login");
      return;
    }

    request.get(`/user/info/${uuid}`).then(res => {
      if (res.code === "0") {
        this.user = res.data;
      } else {
        this.$message.error(res.msg || "获取用户信息失败！");
      }
    });
  },
  methods: {
    handleAvatarSuccess(res) {
      if (res.code === "0") {
        this.user.avatarUrl = res.data;
        this.$message.success("头像上传成功！");
      } else {
        this.$message.error(res.msg || "上传失败！");
      }
    },
    beforeAvatarUpload(file) {
      const isImage = file.type === "image/jpeg" || file.type === "image/png";
      const isLt2M = file.size / 1024 / 1024 < 2;
      if (!isImage) this.$message.error("只能上传 JPG/PNG 图片！");
      if (!isLt2M) this.$message.error("图片大小不能超过 2MB！");
      return isImage && isLt2M;
    },
    updateUsername() {
      request
          .post("/user/update-username", null, {
            params: {
              uuid: this.user.uuid,
              username: this.user.username
            }
          })
          .then(res => {
            if (res.code === "0") {
              this.$message.success("用户名修改成功！");
              localStorage.setItem("username", this.user.username);
            } else {
              this.$message.error(res.msg || "修改失败！");
            }
          });
    }
  }
};
</script>

<style scoped>
.user-info-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}
.info-card {
  width: 650px;
  padding: 20px;
}
.avatar-uploader {
  width: 80px;
  height: 80px;
  border: 2px dashed #17bb94;
  border-radius: 50%;
  overflow: hidden;
}
.avatar-uploader-icon {
  font-size: 20px;
  color: #17bb94;
  line-height: 80px;
  text-align: center;
}
.avatar {
  width: 100%;
  height: 100%;
  border-radius:50%;
}
</style>
