import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)
const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '数据大屏' }
  },
  {
    path: '/enterprise',
    name: 'EnterpriseManage',
    component: () => import('@/views/EnterpriseManage.vue'),
    meta: { title: '企业风控管理' }
  },
  {
    path: '/warning',
    name: 'WarningManage',
    component: () => import('@/views/WarningManage.vue'),
    meta: { title: '预警管理' }
  },
  {
    path: '/policy',
    name: 'PolicyManage',
    component: () => import('@/views/PolicyManage.vue'),
    meta: { title: '政策库管理' }
  },
  {
    path: '/system',
    name: 'SystemConfig',
    component: () => import('@/views/SystemConfig.vue'),
    meta: { title: '系统设置' }
  },
  {
    path: '/relation-graph',
    name: 'RelationGraph',
    component: () => import('@/views/RelationGraph.vue'),
    meta: { title: '跨境关联图谱' }
  },
  {
    path: '/fraud-alert',
    name: 'FraudAlert',
    component: () => import('@/views/FraudAlert.vue'),
    meta: { title: '欺诈预警' }
  },
  {
    path: '/settlement-credit',
    name: 'SettlementCredit',
    component: () => import('@/views/SettlementCredit.vue'),
    meta: { title: '跨境结算信用' }
  },
  {
    path: '/compliance-check',
    name: 'ComplianceCheck',
    component: () => import('@/views/ComplianceCheck.vue'),
    meta: { title: '合规体检' }
  },
  {
    path: '/supervision',
    name: 'SupervisionDashboard',
    component: () => import('@/views/SupervisionDashboard.vue'),
    meta: { title: '监管大屏' }
  },
  {
    path: '/terminology',
    name: 'TradeTerminology',
    component: () => import('@/views/TradeTerminology.vue'),
    meta: { title: '商事术语库' }
  },
  {
    path: '/dispute-cases',
    name: 'DisputeCases',
    component: () => import('@/views/DisputeCases.vue'),
    meta: { title: '纠纷案例库' }
  },
  {
    path: '/country-risk',
    name: 'CountryRisk',
    component: () => import('@/views/CountryRisk.vue'),
    meta: { title: '国别风控' }
  },
  {
    path: '/visual-query',
    name: 'VisualQuery',
    component: () => import('@/views/VisualQuery.vue'),
    meta: { title: '可视化查询' }
  },
  {
    path: '/compliance-report/:creditCode/:targetCountry?',
    name: 'ComplianceReport',
    component: () => import('@/views/ComplianceReport.vue'),
    meta: { title: '合规报告' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  }
]

const router = new Router({
  mode: 'hash',
  routes
})

export default router
