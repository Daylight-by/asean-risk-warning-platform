import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import echarts from '@/utils/echarts'
import App from './App.vue'
import router from './router'
import request from './api'
// 全局视觉增强层（置于最后，确保覆盖既有样式；仅改外观，不改任何功能）
import './styles/theme-enhance.css'

Vue.use(ElementUI)

Vue.prototype.$echarts = echarts
Vue.prototype.$http = request

Vue.config.productionTip = false

// 路由守卫 - 登录验证
router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')
  if (to.path === '/login') {
    if (token) {
      next('/dashboard')
    } else {
      next()
    }
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
