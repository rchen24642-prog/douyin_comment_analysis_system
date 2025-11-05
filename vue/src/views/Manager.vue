<template>
  <div id="app">
    <el-container style="height: 100vh; border: 1px solid #eee">
      <!-- 侧边栏 -->
      <el-aside width="200px" style="background-color: rgb(176, 196, 222)">
        <div class="m-sysName" style="height: 60px">
          <span class="n-nameText">抖音评论舆情分析系统</span>
        </div>

        <!-- 菜单栏 -->
        <el-menu :default-active="$router.path" router>
          <el-menu-item index="/manager/processing">
            <i class="el-icon-edit-outline"></i>数据清洗
          </el-menu-item>

          <el-menu-item index="/manager/network">
            <i class="el-icon-share"></i>社交网络
          </el-menu-item>

          <el-menu-item index="/manager/emotion">
            <i class="el-icon-s-data"></i>情感分析
          </el-menu-item>

          <el-menu-item index="/manager/sentiment-dict">
            <i class="el-icon-s-data"></i>情感词典管理
          </el-menu-item>

          <el-menu-item index="/manager/retrieval">
            <i class="el-icon-search"></i>数据检索
          </el-menu-item>

          <el-menu-item index="/manager/comment-query">
            <i class="el-icon-document"></i>评论查询
          </el-menu-item>

          <el-menu-item index="/manager/visual">
            <i class="el-icon-picture-outline"></i>可视化展示
          </el-menu-item>

          <el-menu-item index="/manager/project">
            <i class="el-icon-folder"></i>项目管理
          </el-menu-item>

          <el-menu-item index="/manager/userinfo">
            <i class="el-icon-user"></i>用户中心
          </el-menu-item>

        </el-menu>
      </el-aside>

      <!-- 顶部栏 -->
      <el-container>
        <el-header style="text-align: right; font-size: 22px; height: 60px">
          <div style="display: flex; justify-content: flex-end; align-items: center;">
            <img :src="user.avatarUrl || 'https://cdn-icons-png.flaticon.com/512/149/149071.png'"
                 alt="avatar"
                 style="width: 35px; height: 35px; border-radius: 50%; margin-right: 10px;" />
            <span>{{ user.username }}</span>
          </div>
        </el-header>

        <!-- 主体内容 -->
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import request from "@/utils/requests";

export default {
  data() {
    return {
      user: {}
    };
  },
  created() {
    const uuid = localStorage.getItem("uuid");
    if (uuid) {
      request.get(`/user/basic/${uuid}`).then(res => {
        if (res.code === "0") {
          this.user = res.data;
        }
      });
    }
  }
};
</script>


<style>
.el-header {
  background-color: #B0C4DE;
  color: #333;
  line-height: 60px;
}
.el-aside {
  color: #333;
}
.n-nameText{
  font-size:20px;
}
</style>