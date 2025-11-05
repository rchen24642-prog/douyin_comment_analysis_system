const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,

  devServer: {
    port: 8080, // 前端运行端口
    proxy: {
      '/api': {
        target: 'http://localhost:9090', // Spring Boot 后端地址
        changeOrigin: true,
        pathRewrite: { '^/api': '' } // 去掉前缀 /api
      }
    }
  }
})
