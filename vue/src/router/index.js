import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/manager',
    name: 'manager',
    component: () => import('../views/Manager.vue'),
    children: [
      { path: 'processing', component: () => import('../views/Manager/ProcessingView.vue') },
      { path: 'network', component: () => import('../views/Manager/NetworkView.vue') },
      { path: 'emotion', component: () => import('../views/Manager/SentimentView.vue') },
      { path: 'retrieval', component: () => import('../views/Manager/RetrievalView.vue') },
      { path: 'visual', component: () => import('../views/Manager/VisualView.vue') },
      { path: 'project', component: () => import('../views/Manager/ProjectView.vue') },
      { path: 'overview', component: () => import('../views/Manager/OverView.vue') },
      { path: 'userinfo', component: () => import('../views/Manager/UserInfoView.vue') },
      { path: 'comment-query', component: () => import('../views/Manager/CommentQueryView.vue') },
      { path: 'sentiment-dict', component: () => import('../views/Manager/SentimentDictView.vue') }, // ✅ 新增
    ]
  },
  { path: '*', redirect: '/login' } // 404 兜底
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// ✅ 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const whiteList = ['/login', '/register']

  // 白名单直接放行
  if (whiteList.includes(to.path)) {
    return next()
  }

  // 未登录 → 跳转登录页
  if (!token) {
    return next('/login')
  }

  // 已登录 → 放行
  next()
})

export default router
