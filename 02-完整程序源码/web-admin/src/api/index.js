import axios from 'axios'
import { Message } from 'element-ui'

const BASE_URL = '/api'

const request = axios.create({
  baseURL: BASE_URL,
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = sessionStorage.getItem('token')
  if (token) config.headers['Authorization'] = `Bearer ${token}`
  return config
}, error => Promise.reject(error))

request.interceptors.response.use(response => {
  const res = response.data
  // 后端统一返回 code=0 表示成功（与 API.md 约定一致）；兼容历史 200
  if (res.code === 0 || res.code === 200) return res
  Message.error(res.message || '请求失败')
  return Promise.reject(new Error(res.message || '请求失败'))
}, error => {
  if (error.response && error.response.status === 401) {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('username')
    window.location.hash = '#/login'
    Message.error('登录已过期，请重新登录')
  } else {
    Message.error(error.response?.data?.message || '网络异常')
  }
  return Promise.reject(error)
})

// ==================== 认证 ====================
export function login(data) { return request.post('/login', data) }

// ==================== Dashboard ====================
export function getDashboardOverview() { return request.get('/dashboard/overview') }
export function getRiskDistribution() { return request.get('/dashboard/risk-distribution') }
export function getPortActivity() { return request.get('/dashboard/port-activity') }
export function getMonthlyTrend() { return request.get('/dashboard/monthly-trend') }
export function getIndustryRanking() { return request.get('/dashboard/industry-ranking') }
export function getPortDistribution() { return request.get('/dashboard/port-distribution') }

// ==================== 企业管理 ====================
export function searchEnterprise(keyword) { return request.get('/enterprise/search', { params: { keyword } }) }
export function getEnterpriseScore(creditCode) { return request.get('/enterprise/score', { params: { creditCode } }) }
export function getEnterpriseList(params) { return request.get('/enterprise/list', { params }) }
export function getEnterpriseDetail(id) { return request.get(`/enterprise/${id}`) }

// ==================== 预警管理 ====================
export function getWarningList(params) { return request.get('/warning/list', { params }) }

// ==================== 政策库 ====================
export function searchPolicy(params) { return request.get('/policy/search', { params }) }
export function getPolicyList(params) { return request.get('/policy/search', { params }) }
export function createPolicy(data) { return request.post('/policy', data) }
export function updatePolicy(id, data) { return request.put(`/policy/${id}`, data) }
export function deletePolicy(id) { return request.delete(`/policy/${id}`) }

// ==================== 跨境关联图谱 ====================
export function getRelationGraph(params) { return request.get(`/relations/graph/${params.creditCode}`) }

// ==================== 欺诈预警 ====================
export function getFraudAlertList(params) { return request.get('/fraud-alerts/list', { params }) }
export function updateFraudAlertStatus(id, status) { return request.put(`/fraud-alerts/${id}/status`, { status }) }
export function processFraudAlert(payload) { return request.post('/fraud-alerts/process', payload) }
export function getFraudAlertStats() { return request.get('/fraud-alerts/stats') }

// ==================== 跨境结算信用 ====================
export function getSettlementList(params) { return request.get('/settlements/list', { params }) }
export function getSettlementTrend() { return request.get('/settlements/trend') }

// ==================== 合规体检 ====================
export function runComplianceCheck(data) { return request.post('/compliance/check', data) }

// ==================== 政企协同监管大屏 ====================
export function getSupervisionData() { return request.get('/supervision/overview') }

// ==================== 可视化查询（轻量低代码） ====================
export function getQuerySources() { return request.get('/query/sources') }
export function executeQuery(data) { return request.post('/query/execute', data) }

// ==================== 我的看板（后端持久化） ====================
// 身份通过 Authorization: Bearer token 传递，由拦截器自动注入，无需额外 header
export function getDashboards() { return request.get('/query/dashboards') }
export function saveDashboard(data) { return request.post('/query/dashboards', data) }
export function deleteDashboard(id) { return request.delete(`/query/dashboards/${id}`) }

export default request
