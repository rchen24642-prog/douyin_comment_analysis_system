<template>
  <div class="login-container">
    <div style="display: flex">
      <el-form>
        <el-form-item class="form-box" style="padding-top: 40px;border-radius: 10px">
          <el-form-item>
            <el-input style="margin-bottom: 15px;" size="large" prefix-icon="el-icon-s-custom" placeholder="请输入用户名" v-model="form.username"></el-input>
          </el-form-item>
          <el-form-item>
            <el-input style="margin-bottom: 15px" size="large" show-password prefix-icon="el-icon-key" placeholder="请输入密码" v-model="form.passwordHash"></el-input>
          </el-form-item>
          <el-form-item>
            <el-input style="margin-bottom: 15px" size="large" show-password prefix-icon="el-icon-key" placeholder="请输入确认密码" v-model="form.nmm"></el-input>
          </el-form-item>
          <el-form-item>
            <el-select style="width: 100%;margin-bottom: 15px" size="large"  placeholder="请选择角色" v-model="form.role">
              <el-option value="user" label="用户"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button style="width: 100%;height: 50px;background-color: #17bb94;border-color: #17bb94;color:white" size="large" @click="register">注册</el-button>
          </el-form-item>
        </el-form-item>
        <el-form-item style="background-color: #ffffff; border-radius: 10px; display: flex; justify-content: center;align-items: center">
          <el-form-item class="regBox">
            <a href="javascript:void(0)" @click="navLogin" style="text-indent: 12px;">登录</a><span class="el-icon-right"></span>
          </el-form-item>

        </el-form-item>

      </el-form>
    </div>
  </div>
</template>


<script>

import request from "@/utils/requests";

export default {
  name: "Register",
  data() {
    return {
      form: {}
    }
  },
  // 页面加载的时候，做一些事情，在created里面
  created() {
  },
  // 定义一些页面上控件出发的事件调用的方法
  methods: {
    navLogin() {
      this.$router.push("/login")
    },
    register() {
      if (!this.form.username) {
        this.$message.error("用户名不能为空！")
        return
      }
      if (!this.form.passwordHash) {
        this.$message.error("密码不能为空！")
        return
      }
      if (this.form.passwordHash !== this.form.nmm) {
        this.$message.error("两次输入的密码不一致！")
        return
      }
      if (!this.form.role) {
        this.$message.error("角色不能为空！")
        return
      }
      const payload = {
        username: this.form.username,
        passwordHash: this.form.passwordHash,
        role: this.form.role
      };
      request.post("/user/register", this.form).then(res => {
        if (res.code === '0') {
          this.$notify({
            title: '成功',
            message: '注册成功！',
            type: 'success'
          });
          this.$router.push("/login");
        } else {
          this.$notify({
            title: '错误',
            message: res.msg || '注册失败！',
            type: 'error'
          });
        }
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  overflow: hidden;
  background-color: #808080;
  background-size: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-box {
  width: 300px;
  padding: 20px 40px;
  border-radius: 2px;
  background-color: #ffffff;
  opacity: 1;
}
/deep/.el-form-item {
  margin-bottom: 5px;
}
/deep/.el-input__inner {
  border-color: #ccc !important;
  border-radius: 2px;
  background-color: #ffffff !important;
  color: #333;
}
/deep/.el-input__icon {
  color: #666;
}

a {
  text-decoration: none;
  color: #17bb94;
  font-weight: bold;
}
.regBox {
  color: #17bb94;
  font-size: 28px;
  margin-bottom: 10px;
  margin-top: 10px;
  text-align: right;
}
</style>