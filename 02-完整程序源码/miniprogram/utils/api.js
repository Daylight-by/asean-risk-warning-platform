const DEFAULT_BASE_URL = 'http://127.0.0.1:8080/api';

function getBaseURL() {
  const app = getApp();
  return (app && app.globalData && app.globalData.baseURL) || DEFAULT_BASE_URL;
}

// ===== 性能层：请求去重 + 可选 TTL 缓存 =====
//
// 1) 同一时刻、同一 URL+method+data 的多个请求，自动合并为一次网络请求（_pending）
// 2) 显式传入 opts.cacheTTL（毫秒）才会启用本地缓存；到期前再次调用直接返回缓存
//    - 仅缓存 statusCode===200 且业务 code===0/200（或无 code 字段）的成功响应
//    - 401 / 500 / 网络失败永远不缓存
// 3) 调用 request() 时传 opts.cacheTTL=60000 等即可启用；不传则只享受去重
//
// 外部可通过 clearCache() 主动清空（例如下拉刷新语义、清登录信息等场景）

const _pending = new Map();   // key -> Promise（去重）
const _cache = new Map();     // key -> { data, expire }

function _keyOf(url, method, data) {
  return (method || 'GET').toUpperCase() + ' ' + url + ' ' + JSON.stringify(data || {});
}

function clearCache(prefix) {
  if (!prefix) { _cache.clear(); return; }
  for (const k of Array.from(_cache.keys())) {
    if (k.indexOf(prefix) !== -1) _cache.delete(k);
  }
}

/**
 * 通用请求封装：内置去重 + 可选缓存
 */
function request(url, method = 'GET', data = {}, opts = {}) {
  const silent = opts.silent;
  const cacheTTL = opts.cacheTTL || 0;
  const key = _keyOf(url, method, data);
  const now = Date.now();

  // 1) TTL 缓存命中（仅命中显式启用 cacheTTL 的请求）
  if (cacheTTL > 0) {
    const hit = _cache.get(key);
    if (hit && hit.expire > now) {
      return Promise.resolve(hit.data);
    }
  }

  // 2) 正在飞行的同 key 请求：复用其 Promise
  if (_pending.has(key)) {
    return _pending.get(key);
  }

  // 3) 真实发起请求
  const p = new Promise((resolve, reject) => {
    wx.request({
      url: getBaseURL() + url,
      method: method,
      data: data,
      timeout: opts.timeout || 10000,
      header: {
        'Content-Type': 'application/json',
        'Authorization': wx.getStorageSync('token') || ''
      },
      success(res) {
        if (res.statusCode === 200) {
          // 仅缓存业务成功响应（code===0/200 或无 code 字段），错误响应不缓存
          const body = res.data;
          const businessOk = !body || body.code === undefined || body.code === 0 || body.code === 200;
          if (cacheTTL > 0 && businessOk) {
            _cache.set(key, { data: body, expire: now + cacheTTL });
          }
          resolve(body);
        } else if (res.statusCode === 401) {
          wx.removeStorageSync('token');
          const app = getApp();
          if (app && app.globalData) app.globalData.token = null;
          if (!silent) wx.showToast({ title: '登录已过期，请重新进入', icon: 'none' });
          reject(res);
        } else {
          if (!silent) wx.showToast({ title: res.data?.message || '请求失败', icon: 'none' });
          reject(res);
        }
      },
      fail(err) {
        if (!silent) wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
        reject(err);
      },
      complete() {
        // 无论成功失败都释放 in-flight 槽位
        _pending.delete(key);
      }
    });
  });

  _pending.set(key, p);
  return p;
}

function get(url, data = {}, opts = {}) {
  return request(url, 'GET', data, opts);
}

function post(url, data = {}, opts = {}) {
  return request(url, 'POST', data, opts);
}

/**
 * 搜索企业（模糊搜索，支持联想）
 * 后端: GET /api/enterprise/search?keyword=xxx
 */
function searchEnterprise(keyword) {
  return request('/enterprise/search', 'GET', { keyword });
}

/**
 * 获取企业风控评分
 * 后端: GET /api/enterprise/score?enterpriseId=xxx
 */
function getRiskScore(enterpriseId) {
  return request('/enterprise/score', 'GET', { enterpriseId });
}

/**
 * 生成信用报告
 * 后端: POST /api/enterprise/report
 */
function generateReport(enterpriseId) {
  return request('/enterprise/report', 'POST', { enterpriseId });
}

function getOpenId() {
  return wx.getStorageSync('openid') || 'default_user';
}

/**
 * 订阅预警
 * 后端: POST /api/warning/subscribe
 */
function subscribeWarning(enterpriseId, options = {}) {
  return request('/warning/subscribe', 'POST', { enterpriseId, openId: getOpenId(), ...options });
}

/**
 * 获取预警消息列表
 * 后端: GET /api/warning/list?page=1&pageSize=10
 */
function getWarningList(page = 1, pageSize = 10) {
  return request('/warning/list', 'GET', { page, pageSize });
}

/**
 * 搜索政策
 * 后端: GET /api/policy/search?keyword=&country=&page=1&pageSize=10
 */
function searchPolicy(keyword = '', country = '', page = 1, pageSize = 10) {
  return request('/policy/search', 'GET', { keyword, country, page, pageSize });
}

/**
 * 获取订阅状态
 * 后端: GET /api/warning/status?enterpriseId=xxx
 */
function getSubscribeStatus(enterpriseId) {
  return request('/warning/status', 'GET', { enterpriseId, openId: getOpenId() });
}

/**
 * 取消订阅预警
 * 后端: POST /api/warning/unsubscribe
 */
function unsubscribeWarning(enterpriseId) {
  return request('/warning/unsubscribe', 'POST', { enterpriseId, openId: getOpenId() });
}

function getSubscribedList() {
  return request('/warning/subscribed-list', 'GET', { openId: getOpenId() });
}

/**
 * 获取数据概览
 * 后端: GET /api/dashboard/overview
 * 启用 60s 缓存：首页与 dashboard 都调用此接口，统计信息允许近实时
 */
function getDashboardOverview() {
  return request('/dashboard/overview', 'GET', {}, { cacheTTL: 60000 });
}

/**
 * 获取政策数量
 * 后端: GET /api/policy/search?keyword=&country=&page=1&pageSize=1
 */
function getPolicyCount() {
  return request('/policy/search', 'GET', { keyword: '', country: '', page: 1, pageSize: 1 });
}

/**
 * 口岸通关全链路画像 + 跨境人民币结算信用分
 * 后端: GET /api/enterprise/port-profile?enterpriseId=xxx
 * 创新点：一(二) 边境口岸维度 / 跨境结算子模块
 */
function getPortProfile(enterpriseId) {
  return request('/enterprise/port-profile', 'GET', { enterpriseId });
}

/**
 * 跨境关联风险传导图谱（中-东盟多层关联网络）
 * 后端: GET /api/enterprise/relation-graph?enterpriseId=xxx
 * 创新点：二(1) 跨境关联风险传导动态图谱
 */
function getRelationGraph(enterpriseId) {
  return request('/enterprise/relation-graph', 'GET', { enterpriseId });
}

/**
 * 多语种跨境企业档案（东盟多语种异构数据自动对齐引擎）
 * 后端: GET /api/enterprise/cross-border-archive?enterpriseId=xxx&keyword=xxx
 * 创新点：一(一) 多语种跨境数据融合 / 术语标准化词库
 */
function getCrossBorderArchive(enterpriseId, keyword) {
  return request('/enterprise/cross-border-archive', 'GET', { enterpriseId, keyword });
}

function getMultilingualContent(type, locale, ids) {
  return request('/i18n/content', 'GET', { type, locale, ids: Array.isArray(ids) ? ids.join(',') : ids }, { silent: true });
}

/**
 * 中小微边贸企业合规体检（生成《东盟跨境贸易合规报告》）
 * 后端: POST /api/enterprise/compliance-check
 * 创新点：三(1) 轻量化合规辅助工具
 */
function complianceCheck(payload) {
  return request('/enterprise/compliance-check', 'POST', payload);
}

/**
 * 政企协同监管 - 区域跨境贸易风险热力图
 * 后端: GET /api/regulate/heatmap
 * 创新点：三(2) 政企双向协同监管模块
 */
function getRegulateHeatmap() {
  return request('/regulate/heatmap', 'GET');
}

/**
 * 政企协同监管 - 高风险企业集群自动筛查
 * 后端: GET /api/regulate/clusters?region=&level=
 * 创新点：三(2) 政企双向协同监管模块
 */
function getRegulateClusters(region, level) {
  // 不传 undefined：undefined 会被 wx.request 序列化成字符串 "undefined"，后端会 500
  const data = {};
  if (region != null) data.region = region;
  if (level != null) data.level = level;
  return request('/regulate/clusters', 'GET', data);
}

/**
 * 跨境贸易纠纷案例智能匹配
 * 后端: POST /api/dispute/match
 * 创新点：四(2) 跨境贸易纠纷案例智能匹配库
 */
function matchDisputeCases(payload) {
  return request('/dispute/match', 'POST', payload);
}

/**
 * 分国别差异化跨境风控评分（一国一策权重模型）
 * 后端: GET /api/enterprise/score?enterpriseId=xxx&country=越南
 * 创新点：二(2) 分国别差异化跨境风控评分模型
 */
function getCountryRiskScore(enterpriseId, country) {
  return request('/enterprise/score', 'GET', { enterpriseId, country });
}

/**
 * 跨境贸易欺诈前置预警（时序预测，事前预警）
 * 后端: GET /api/enterprise/predictive-warning?enterpriseId=xxx
 * 创新点：二(3) 跨境贸易欺诈前置预警
 */
function getPredictiveWarning(enterpriseId) {
  return request('/enterprise/predictive-warning', 'GET', { enterpriseId });
}

/**
 * ============================================================
 * AI 风控助手模块
 * ============================================================
 * 后端约定：
 *   - 普通（非流式）: POST /api/ai/chat      { messages, context, question } -> { code, data:{ reply } }
 *   - 流式          : POST /api/ai/chat/stream (enableChunked)
 *        · 可用 SSE 格式逐块下发：data: {"choices":[{"delta":{"content":"x"}}]}\n\n
 *        · 也可直接 chunked 下发纯文本
 * 注意：BASE_URL 当前为本地地址，生产需改为已备案 HTTPS 域名并加入 request 合法域名。
 */

// 基础库是否支持分块流式接收（2.19.0+）
const SUPPORTS_CHUNK = typeof wx !== 'undefined' && wx.canIUse && wx.canIUse('request.enableChunked');

// ArrayBuffer -> UTF-8 字符串（wx 环境无全局 TextDecoder，用 escape/decodeURIComponent 兜底）
function decodeChunk(buffer) {
  if (!buffer) return '';
  const bytes = new Uint8Array(buffer);
  let binary = '';
  const step = 8192;
  for (let i = 0; i < bytes.length; i += step) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + step));
  }
  try {
    return decodeURIComponent(escape(binary));
  } catch (_) {
    return binary;
  }
}

/**
 * 普通请求（非流式）
 */
function aiChat(payload, opts = {}) {
  return request('/ai/chat', 'POST', payload, opts);
}

/**
 * 流式对话。handlers: { onChunk(delta, acc), onDone(acc), onError(err) }
 * 不支持分块时自动降级为 aiChat() 一次性返回。
 */
function aiChatStream(payload, handlers = {}) {
  const { onChunk, onDone, onError } = handlers;
  return new Promise((resolve, reject) => {
    if (!SUPPORTS_CHUNK) {
      aiChat(payload, { silent: true })
        .then(res => {
          const text = (res && (res.data && (res.data.reply || res.data.content) || res.reply || res.content)) || '';
          onChunk && onChunk(text, text);
          onDone && onDone(text);
          resolve(text);
        })
        .catch(err => { onError && onError(err); reject(err); });
      return;
    }

    let acc = '';
    const timeout = payload.timeout || 30000;
    const timer = setTimeout(() => {
      if (task && task.abort) task.abort();
      const err = { code: 'TIMEOUT', message: 'AI 响应超时，请重试' };
      onError && onError(err);
      reject(err);
    }, timeout);

    const task = wx.request({
      url: getBaseURL() + '/ai/chat/stream',
      method: 'POST',
      data: payload,
      enableChunked: true,
      timeout: timeout,
      header: {
        'Content-Type': 'application/json',
        'Authorization': wx.getStorageSync('token') || ''
      },
      success(res) {
        clearTimeout(timer);
        if (res.statusCode === 200) {
          onDone && onDone(acc);
          resolve(acc);
        } else {
          const err = { code: res.statusCode, message: (res.data && res.data.message) || 'AI 服务异常' };
          onError && onError(err);
          reject(err);
        }
      },
      fail(err) {
        clearTimeout(timer);
        onError && onError(err);
        reject(err);
      }
    });

    // 逐块接收：兼容 SSE（data: JSON）与纯文本
    task.onChunkReceived(res => {
      const raw = decodeChunk(res.data);
      acc += raw;
      let delta = raw;
      const m = raw.match(/data:\s*([^\n]+)/);
      if (m) {
        try {
          const json = JSON.parse(m[1].trim());
          delta = json.choices && json.choices[0] && json.choices[0].delta && json.choices[0].delta.content
            || json.content || json.reply || '';
        } catch (_) { /* 非 JSON，按纯文本原样追加 */ }
      }
      onChunk && onChunk(delta, acc);
    });
  });
}

module.exports = {
  getBaseURL,
  request,
  get,
  post,
  clearCache,
  searchEnterprise,
  getRiskScore,
  generateReport,
  subscribeWarning,
  getWarningList,
  searchPolicy,
  getSubscribeStatus,
  unsubscribeWarning,
  getSubscribedList,
  getDashboardOverview,
  getPolicyCount,
  getPortProfile,
  getRelationGraph,
  getCrossBorderArchive,
  getMultilingualContent,
  complianceCheck,
  getRegulateHeatmap,
  getRegulateClusters,
  matchDisputeCases,
  getCountryRiskScore,
  getPredictiveWarning,
  aiChat,
  aiChatStream
};
