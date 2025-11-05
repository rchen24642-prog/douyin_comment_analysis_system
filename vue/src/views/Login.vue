<template>
  <div class="login-container">
    <div style="display: flex">
      <el-form :model="form" class="form-box">
        <el-form-item>
          <el-input
              v-model="form.username"
              size="large"
              prefix-icon="el-icon-user"
              placeholder="请输入用户名"
          />
        </el-form-item>
        <el-form-item>
          <el-input
              v-model="form.password"
              size="large"
              show-password
              prefix-icon="el-icon-lock"
              placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button
              type="primary"
              style="width: 100%; height: 45px; background-color: #17bb94; border-color: #17bb94"
              @click="login"
          >
            登录
          </el-button>
        </el-form-item>
        <el-form-item style="text-align: right">
          <a href="javascript:void(0)" @click="navRegister">还没有账号？去注册</a>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import md5 from "js-md5";
import request from "@/utils/requests";

export default {
  name: "Login",
  data() {
    return {
      form: {
        username: "",
        password: ""
      }
    };
  },
  methods: {
    navRegister() {
      this.$router.push("/register");
    },
    login() {
      if (!this.form.username || !this.form.password) {
        this.$message.error("用户名或密码不能为空！");
        return;
      }

      // 前端 MD5 加密
      const encrypted = md5(this.form.password);

      request
          .post("/user/login", {
            username: this.form.username,
            passwordHash: encrypted
          })
          .then(res => {
            if (res.code === 200 || res.code === "0") {
              // 保存 token 与用户信息
              localStorage.setItem("token", res.data.token);
              localStorage.setItem("role", res.data.role);
              localStorage.setItem("uuid", res.data.uuid);

              this.$notify({
                title: "成功",
                message: "登录成功！",
                type: "success"
              });

              // 跳转 ProcessingView
              this.$router.push("/manager/processing");
            } else {
              this.$notify({
                title: "错误",
                message: res.msg || "登录失败！",
                type: "error"
              });
            }
          })
          .catch(() => {
            this.$message.error("请求出错，请检查后端服务！");
          });
    }
  }
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  background-color: #808080;
  display: flex;
  justify-content: center;
  align-items: center;
}

.form-box {
  width: 300px;
  padding: 40px;
  background-color: white;
  border-radius: 10px;
}

a {
  color: #17bb94;
  font-weight: bold;
  text-decoration: none;
}
</style>
