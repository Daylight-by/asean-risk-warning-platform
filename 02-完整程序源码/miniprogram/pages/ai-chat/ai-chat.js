const api = require('../../utils/api');

/* ============================================================
 * 意图识别：将用户问题映射到「技能（动作）」
 * 设计原则：前端驱动的任务执行，不强依赖小模型 function calling。
 *  - 查询类技能：直接调用真实 API，渲染结构化结果卡
 *  - 写操作类技能（订阅/合规/报告）：先弹确认卡，确认后再执行
 * ============================================================ */
function detectIntent(text) {
  if (/术语|名词|什么意思|解释|定义|term|meaning|definition|thuật ngữ|định nghĩa/i.test(text)) return 'terminology';
  if (/订阅|关注.*预警|开启.*预警|订阅预警|风险提醒|风险订阅|subscribe|alert subscription|đăng ký.*cảnh báo/i.test(text)) return 'subscribe';
  if (/合规体检|合规报告|合规检查|生成合规|合规分析|合规自查|compliance|compliant|tuân thủ/i.test(text)) return 'compliance';
  if (/信用报告|征信报告|生成报告|生成信用|企业报告|风险报告|信用档案|credit report|company report|báo cáo tín dụng/i.test(text)) return 'report';
  if (/纠纷|案例|仲裁|维权|dispute|case|arbitration|tranh chấp|vụ việc/i.test(text)) return 'dispute';
  if (/口岸|通关画像|通关全链路|结算信用|口岸维度|port profile|clearance|settlement|cửa khẩu|thông quan/i.test(text)) return 'port';
  if (/图谱|关联|关系网|传导|关联企业|graph|relationship|network|đồ thị|liên kết/i.test(text)) return 'graph';
  if (/评分|打分|分数|评级|信用分|风险分|风控分|risk score|score|rating|điểm rủi ro/i.test(text)) return 'score';
  if (/企业|公司|老板|法人|股东|品牌|商标|供应商|客户|厂商|商户|company|enterprise|supplier|merchant|doanh nghiệp/i.test(text)) return 'enterprise';
  if (/预警|风险|订单|履约|失信|被执行|诉讼|处罚|异常|alert|risk|warning|cảnh báo|rủi ro/i.test(text)) return 'warning';
  if (/政策|法规|条例|规定|东盟|通关|关税|自贸|条文|policy|regulation|tariff|trade|chính sách|thuế quan/i.test(text)) return 'policy';
  return 'general';
}

// 抽取查询关键词（去疑问词，截短，去空格后用于检索）
function extractQuery(text) {
  return (text || '')
    .replace(/[?？!！。.，,吗呢吧呀如何怎么怎样什么哪哪些谁]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 20);
}

// 抽取企业名称：优先「为 X 订阅」「查一下 X 的评分」「X 公司」等结构
function extractEnterpriseName(text) {
  let m;
  m = text.match(/为\s*([^\s,，。、]+?)\s*(订阅|生成|做|进行|查询|开启|风险|预警)/);
  if (m) return m[1];
  m = text.match(/(?:查一下|查|查询|看看|分析|分析一下)\s*([^\s,，。、]+?)\s*(的|风险|风控|评分|口岸|通关|图谱|关联|报告|合规|信用)/);
  if (m) return m[1];
  m = text.match(/([一-龥A-Za-z0-9·（）()]{2,24}(?:公司|集团|企业|商户|厂|有限公司|股份公司|有限责任公司))/);
  if (m) return m[1];
  return extractQuery(text);
}

// 企业检索关键词：推荐类泛问回落到可以命中种子数据的词
function enterpriseKeyword(q) {
  if (/东兴/.test(q)) return '东兴';
  const cleaned = q.replace(/[?？!！。.，,吗呢吧呀如何怎么怎样什么哪哪些谁推荐几家优质哪些有哪些列举列出跨境企业公司商户]/g, '').trim();
  if (!cleaned || /^(跨境企业|企业|公司|商户|贸易)$/.test(cleaned)) return '贸易';
  return cleaned.slice(0, 8);
}

// 政策检索：提取国家 + 关键词
function policyKeywordAndCountry(q) {
  let country = '';
  const cm = q.match(/(越南|泰国|马来西亚|印尼|印度尼西亚|新加坡|柬埔寨|老挝|缅甸|菲律宾|文莱|东帝汶)/);
  if (cm) country = cm[1];
  let keyword = '东盟';
  if (/关税|税率|RCEP/.test(q)) keyword = '关税';
  else if (/自贸|自由贸易/.test(q)) keyword = '自贸';
  else if (/东盟/.test(q)) keyword = '东盟';
  return { keyword, country };
}

/* ============================================================
 * 技能注册表 ACTIONS
 *  - needsConfirm: 写操作需二次确认
 *  - describe(ctx): LLM 不可用时的兜底叙述
 *  - run(q, ctx, page, ent?): 执行真实 API，返回卡片对象
 * ============================================================ */
const ACTIONS = {
  enterprise: {
    needsConfirm: false,
    describe: () => '为您查询到以下相关跨境企业，点击可查看其风控评分：',
    async run(q, ctx) {
      const list = (ctx.enterprises || []).slice(0, 5).map(e => ({
        id: e.creditCode || e.id,
        name: e.enterpriseName || e.name || '跨境企业',
        riskScore: e.riskScore,
        navUrl: '/pages/score/score?code=' + encodeURIComponent(e.creditCode || e.id || '')
      }));
      return { kind: 'enterprise', items: list };
    }
  },
  warning: {
    needsConfirm: false,
    describe: () => '近期平台监测到的风险动态如下，点击可前往风险预警页：',
    async run(q, ctx, page) {
      const list = (ctx.warnings || []).slice(0, 5).map(w => ({
        id: w.id,
        name: w.enterpriseName || '某企业',
        levelKey: page._riskLevelKey(w.warningLevel),
        level: page._riskLevelText(w.warningLevel),
        content: w.content || w.warningContent || '',
        navUrl: '/pages/warning/warning'
      }));
      return { kind: 'warning', items: list };
    }
  },
  policy: {
    needsConfirm: false,
    describe: () => '为您整理的近期相关政策，点击可前往政策资讯页：',
    async run(q, ctx) {
      const list = (ctx.policies || []).slice(0, 5).map(p => ({
        id: p.id,
        country: p.country || p.targetCountry || '东盟',
        title: p.title || p.policyTitle || '政策',
        navUrl: '/pages/policy/policy'
      }));
      return { kind: 'policy', items: list };
    }
  },
  score: {
    needsConfirm: false,
    describe: (ctx, ent) => '已为您生成企业风控评分，详见下方卡片：',
    async run(q, ctx, page) {
      const ent = await page._resolveEnterprise(q);
      if (!ent) return { kind: 'error', message: '未找到匹配的企业，请告诉我具体企业名称，例如「查一下 东兴市亚宏进出口贸易有限公司 的风控评分」' };
      const res = await api.getRiskScore(ent.creditCode).catch(() => null);
      const raw = (res && (res.data || res)) || {};
      const d = { ...raw, riskLevelKey: page._riskLevelKey(raw.riskLevel), riskLevelLabel: page._riskLevelText(raw.riskLevel) };
      return {
        kind: 'score',
        name: ent.enterpriseName,
        creditCode: ent.creditCode,
        data: d,
        navUrl: '/pages/score/score?code=' + encodeURIComponent(ent.creditCode)
      };
    }
  },
  port: {
    needsConfirm: false,
    describe: () => '已为企业生成口岸通关画像，详见下方卡片：',
    async run(q, ctx, page) {
      const ent = await page._resolveEnterprise(q);
      if (!ent) return { kind: 'error', message: '未找到匹配的企业，请告诉我具体企业名称，例如「查一下 东兴市亚宏进出口贸易有限公司 的口岸画像」' };
      const res = await api.getPortProfile(ent.creditCode).catch(() => null);
      const d = (res && (res.data || res)) || {};
      return {
        kind: 'port',
        name: ent.enterpriseName,
        data: d,
        navUrl: '/pages/port/port?code=' + encodeURIComponent(ent.creditCode)
      };
    }
  },
  graph: {
    needsConfirm: false,
    describe: () => '已为企业生成跨境关联风险传导图谱，点击卡片查看完整图谱：',
    async run(q, ctx, page) {
      const ent = await page._resolveEnterprise(q);
      if (!ent) return { kind: 'error', message: '未找到匹配的企业，请告诉我具体企业名称，例如「查一下 东兴市亚宏进出口贸易有限公司 的关联图谱」' };
      const res = await api.getRelationGraph(ent.creditCode).catch(() => null);
      const d = (res && (res.data || res)) || {};
      return {
        kind: 'graph',
        name: ent.enterpriseName,
        data: d,
        navUrl: '/pages/graph/graph?code=' + encodeURIComponent(ent.creditCode) + '&name=' + encodeURIComponent(ent.enterpriseName)
      };
    }
  },
  subscribe: {
    needsConfirm: true,
    actionNameKey: 'subscribeAction',
    async run(q, ctx, page, ent) {
      const res = await api.subscribeWarning(ent.creditCode).catch(() => null);
      const msg = (res && (res.data || res)) || '已提交';
      return { kind: 'subscribe', name: ent.enterpriseName, result: msg };
    }
  },
  compliance: {
    needsConfirm: true,
    actionNameKey: 'complianceAction',
    async run(q, ctx, page, ent) {
      const res = await api.complianceCheck({ enterpriseName: ent.enterpriseName, country: '越南', category: '农产品' }).catch(() => null);
      const d = (res && (res.data || res)) || {};
      return {
        kind: 'compliance',
        name: ent.enterpriseName,
        data: d,
        navUrl: '/pages/compliance/compliance'
      };
    }
  },
  report: {
    needsConfirm: true,
    actionNameKey: 'reportAction',
    async run(q, ctx, page, ent) {
      await api.generateReport(ent.creditCode).catch(() => null);
      return {
        kind: 'report',
        name: ent.enterpriseName,
        creditCode: ent.creditCode,
        navUrl: '/pages/report/report?code=' + encodeURIComponent(ent.creditCode)
      };
    }
  }
};

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    messages: [],
    inputValue: '',
    canSend: false,
    sending: false,
    scrollTop: 0,
    suggestions: []
  },

  _uid: 0,
  _sending: false,
  _streamTimer: null,
  _scrollSeq: 0,
  _pendingConfirm: {},
  _msgAction: {},

  onLoad() {
    this._pendingConfirm = {};
    this._msgAction = {};
    const history = wx.getStorageSync('aiChatHistory') || [];
    if (history.length) {
      this._uid = Math.max(0, ...history.map(m => m.id || 0));
      this.setData({ messages: history });
      this._scrollToBottom();
    }
    this._refreshLocalizedData();
  },

  onShow() {
    this._refreshLocalizedData();
  },

  onUnload() {
    this._persist();
    if (this._streamTimer) clearInterval(this._streamTimer);
  },

  onInput(e) {
    const v = e.detail.value || '';
    this.setData({ inputValue: v, canSend: v.trim().length > 0 });
  },

  onUseSuggestion(e) {
    const text = e.currentTarget.dataset.text;
    this.setData({ inputValue: text, canSend: true });
    this.onSend();
  },

  async onSend() {
    const q = (this.data.inputValue || '').trim();
    if (!q || this._sending) return;
    this._sending = true;

    const uid = ++this._uid;
    const userMsg = { id: uid, role: 'user', content: q };
    const aiId = ++this._uid;
    const aiMsg = { id: aiId, role: 'assistant', content: '', typing: true, error: '' };

    this.setData({
      messages: this.data.messages.concat([userMsg, aiMsg]),
      inputValue: '',
      canSend: false,
      sending: true
    });
    this._scrollToBottom();

    const intent = detectIntent(q);
    const action = ACTIONS[intent] || null;
    const ctx = await this._buildContext(q);

    try {
      // 写操作：先解析企业，弹确认卡
      if (action && action.needsConfirm) {
        const ent = await this._resolveEnterprise(q);
        this._setTyping(aiId, false);
        if (!ent) {
          this._setCard(aiId, {
            kind: 'error',
            message: this.data.t.pages.ai.notFound
          });
          return;
        }
        this._msgAction[aiId] = intent;
        this._pendingConfirm[aiId] = { intent, ent };
        const actionName = this.data.t.pages.ai[action.actionNameKey] || action.actionNameKey;
        this._setContent(aiId, this.data.language === 'zh' ? '我将为您执行「' + actionName + '」，请确认：' : actionName);
        this._setConfirm(aiId, {
          actionName,
          name: ent.enterpriseName,
          creditCode: ent.creditCode
        });
        return;
      }

      // 读操作 / 任务执行：执行动作 + 叙述并发
      if (action) {
        const cardPromise = Promise.resolve(action.run(q, ctx, this)).catch(err => {
          console.warn('[ai-chat] action 执行失败:', err);
          return { kind: 'error', message: (err && err.message) || this.data.t.pages.ai.executionFailed };
        });
        const narrativePromise = this._streamNarrative(aiId, q, ctx, action);
        const card = await cardPromise;
        this._setCard(aiId, card);
        await narrativePromise;
        return;
      }

      // 纯对话
      await this._streamNarrative(aiId, q, ctx, null);
    } catch (err) {
      this._setTyping(aiId, false);
      this._setMsgError(aiId, this.data.t.pages.ai.serviceUnavailable);
    } finally {
      this._sending = false;
      this.setData({ sending: false });
      this._scrollToBottom();
      this._persist();
    }
  },

  // 确认执行写操作
  async onConfirmAction(e) {
    const id = e.currentTarget.dataset.id;
    const pending = this._pendingConfirm[id];
    if (!pending) return;
    delete this._pendingConfirm[id];
    const intent = pending.intent;
    const action = ACTIONS[intent];
    if (!action) return;

    const idx = this.data.messages.findIndex(m => m.id === id);
    const q = idx > 0 ? (this.data.messages[idx - 1].content || '') : '';
    this._setConfirm(id, null);
    this._setLoading(id, true);
    try {
      const card = await action.run(q, {}, this, pending.ent);
      this._setCard(id, card);
    } catch (err) {
      console.warn('[ai-chat] 确认执行失败:', err);
      this._setCard(id, { kind: 'error', message: (err && err.message) || this.data.t.pages.ai.executionFailed });
    } finally {
      this._setLoading(id, false);
      this._scrollToBottom();
      this._persist();
    }
  },

  onCancelConfirm(e) {
    const id = e.currentTarget.dataset.id;
    if (this._pendingConfirm[id]) delete this._pendingConfirm[id];
    this._setConfirm(id, null);
    this._setContent(id, this.data.t.pages.ai.cancelled);
  },

  // 卡片/错误重试：复用整条会话重发
  onRetry(e) {
    const id = e.currentTarget.dataset.id;
    const idx = this.data.messages.findIndex(m => m.id === id);
    if (idx <= 0) return;
    const q = this.data.messages[idx - 1].content;
    this.setData({ messages: this.data.messages.slice(0, idx) });
    this.setData({ inputValue: q });
    this.onSend();
  },

  // 卡片点击跳转
  onTapNavigate(e) {
    const url = e.currentTarget.dataset.url;
    if (!url) return;
    wx.navigateTo({ url });
  },

  onClearHistory() {
    wx.showModal({
      title: this.data.t.pages.ai.clearTitle,
      content: this.data.t.pages.ai.clearContent,
      success: (r) => {
        if (r.confirm) {
          wx.removeStorageSync('aiChatHistory');
          this.setData({ messages: [], inputValue: '', canSend: false });
        }
      }
    });
  },

  /* ---------- 内部方法 ---------- */

  // 按意图拉取真实业务数据，作为 AI 上下文（仅企业/预警/政策走检索）
  async _buildContext(q) {
    const intent = detectIntent(q);
    const context = { intent: intent, query: extractQuery(q), language: this.data.language };
    try {
      if (intent === 'enterprise') {
        const res = await api.searchEnterprise(enterpriseKeyword(q)).catch(() => null);
        context.enterprises = this._asList(res).slice(0, 5);
      } else if (intent === 'warning') {
        const res = await api.getWarningList(1, 5).catch(() => null);
        context.warnings = this._asList(res).slice(0, 5);
      } else if (intent === 'policy') {
        const { keyword, country } = policyKeywordAndCountry(q);
        const res = await api.searchPolicy(keyword, country, 1, 5).catch(() => null);
        context.policies = this._asList(res).slice(0, 5);
      }
    } catch (_) { /* 拉取失败不影响对话 */ }
    return context;
  },

  // 解析企业：用关键词检索，取首个匹配
  async _resolveEnterprise(q) {
    const name = extractEnterpriseName(q);
    if (!name || name.length < 2) return null;
    const res = await api.searchEnterprise(name).catch(() => null);
    const list = this._asList(res);
    return (list && list[0]) || null;
  },

  _asList(res) {
    if (!res) return [];
    if (Array.isArray(res)) return res;
    if (res.data) {
      if (Array.isArray(res.data)) return res.data;
      if (res.data.list) return res.data.list;
    }
    if (res.list) return res.list;
    return [];
  },

  _refreshLocalizedData() {
    const ai = (this.data.t && this.data.t.pages && this.data.t.pages.ai) || require('../../utils/i18n').getMessages().pages.ai;
    this.setData({ suggestions: ai.suggestions || [] });
  },

  _riskLevelKey(level) {
    const value = String(level || '').toLowerCase();
    if (value === 'high' || level === '高风险') return 'high';
    if (value === 'medium' || level === '中风险') return 'medium';
    return 'low';
  },

  _riskLevelText(level) {
    const key = this._riskLevelKey(level);
    return this.data.t.common[key];
  },

  // 用真实业务数据合成回复文本（无后端/无模型时的兜底，覆盖多意图）
  _buildLocalReply(q, context) {
    if (this.data.language !== 'zh') return this.data.t.pages.ai.fallbackReply;
    const intent = context.intent;
    if (intent === 'enterprise') {
      const list = context.enterprises || [];
      if (!list.length) return '暂时没有匹配的企业数据，您可以换个关键词，或在「风控评分」中输入具体企业名称查询它的风险评分与信用报告。';
      const names = list.map(e => e.enterpriseName || e.name || '某跨境企业').join('、');
      return '根据平台融合的口岸与工商数据，为您找到以下相关跨境企业：' + names +
        '。您可在「风控评分」中查看它们的详细风险评分、跨境结算信用分与信用报告；告诉我具体企业名称，我帮您进一步分析。';
    }
    if (intent === 'warning') {
      const list = context.warnings || [];
      if (!list.length) return '当前暂无新的风险预警。建议您在「风险预警」中订阅重点企业，平台会在风险变化时第一时间提醒您。';
      const items = list.map(w =>
        '• ' + (w.enterpriseName || '某企业') + '（' + (w.warningLevel || '风险') + '）'
      ).join('\n');
      return '近期平台监测到的风险动态：\n' + items +
        '\n您可在「风险预警」查看详情并订阅提醒；需要某条预警的应对建议，告诉我即可。';
    }
    if (intent === 'policy') {
      const list = context.policies || [];
      if (!list.length) return '暂无匹配的政策更新。您可以在「东盟政策」中按国家浏览全部政策与法规。';
      const items = list.map(p =>
        '• [' + (p.country || '东盟') + '] ' + (p.title || p.policyTitle || '')
      ).join('\n');
      return '为您整理的近期相关政策：\n' + items +
        '\n需要某条政策的详细解读或合规要点，告诉我即可。';
    }
    if (intent === 'score') return '平台提供分国别差异化的跨境风控评分模型与跨境结算信用分。在「风控评分」输入企业即可查看；把企业名称发我，我帮您调出评分要点。';
    if (intent === 'port') return '口岸维度可看企业通关全链路画像与跨境人民币结算信用分。打开「口岸画像」输入企业即可查看；需要我解读某项口岸指标，告诉我即可。';
    if (intent === 'graph') return '「跨境图谱」展示中—东盟多层关联风险传导网络，能看出一家企业的关联企业与风险传导路径。打开对应模块输入企业即可生成；想了解某类风险如何传导，告诉我。';
    if (intent === 'terminology') return '跨境贸易有不少专业术语（如 FOB、CIF、原产地证、离岸价等）。您可在「术语库」中按关键词查询标准释义；把想了解的术语发给我，我也能为您解释。';
    if (intent === 'compliance') return '中小跨境商户常见合规点包括：报关单证、原产地规则、跨境结算与税务。您可使用「合规体检」生成《东盟跨境贸易合规报告》；告诉我您的行业或目标国家，我帮您梳理重点。';
    if (intent === 'dispute') return '跨境贸易纠纷常见于货款拖欠、质量争议、物流损缺等。平台「纠纷案例」可按关键词智能匹配类似判例与处置建议；描述您的纠纷情况，我帮您检索参考案例。';
    if (intent === 'subscribe') return '我可以帮您订阅重点企业的风险预警，平台会在风险变化时第一时间提醒您。告诉我企业名称，例如「为 东兴市亚宏进出口贸易有限公司 订阅风险预警」。';
    if (intent === 'report' || intent === 'compliance') return '我可以帮您生成企业信用报告或合规体检报告。告诉我企业名称即可。';
    if (q) return '您提到的「' + q + '」我已记录。我是东盟风控 AI 助手，可以帮您查询跨境企业风险、风险预警、东盟贸易政策与合规，也能直接执行订阅预警、生成报告等任务。试试问我「近期有哪些风险预警」吧～';
    return '我是东盟跨境中小商户智能风控预警平台的 AI 助手，可以帮您查询跨境企业风险、风险预警、东盟贸易政策与合规，也能直接执行订阅预警、生成报告等任务。';
  },

  // 流式叙述：优先真实流式接口，失败/空则用本地兜底文本
  async _streamNarrative(aiId, q, ctx, action) {
    let got = false;
    try {
      await api.aiChatStream(
        { messages: this._toPayloadMessages(), context: ctx, question: q },
        {
          onChunk: (delta) => {
            if (delta) {
              got = true;
              this._setTyping(aiId, false);
              this._appendStream(aiId, delta);
            }
          },
          onDone: () => {},
          onError: (err) => { console.warn('[ai-chat] stream 出错:', err); }
        }
      );
    } catch (err) {
      console.warn('[ai-chat] stream 异常:', err);
    }
    if (!got) {
      const fallback = this.data.language !== 'zh'
        ? this.data.t.pages.ai.fallbackReply
        : action && action.describe ? action.describe(ctx) : this._buildLocalReply(q, ctx);
      if (fallback) {
        this._setTyping(aiId, false);
        await this._streamText(aiId, fallback);
      }
    }
  },

  // 增量追加到指定消息（路径更新，降低 setData 负载）
  _appendStream(id, delta) {
    if (!delta) return;
    const messages = this.data.messages;
    const idx = messages.findIndex(m => m.id === id);
    if (idx < 0) return;
    const cur = messages[idx].content || '';
    this.setData({
      ['messages[' + idx + '].content']: cur + delta,
      scrollTop: ++this._scrollSeq * 100000
    });
  },

  _setTyping(id, val) {
    const idx = this.data.messages.findIndex(m => m.id === id);
    if (idx < 0) return;
    this.setData({ ['messages[' + idx + '].typing']: val });
  },

  _setContent(id, text) {
    const idx = this.data.messages.findIndex(m => m.id === id);
    if (idx < 0) return;
    this.setData({ ['messages[' + idx + '].content']: text });
  },

  _setCard(id, card) {
    const idx = this.data.messages.findIndex(m => m.id === id);
    if (idx < 0) return;
    this.setData({
      ['messages[' + idx + '].card']: card,
      ['messages[' + idx + '].typing']: false,
      ['messages[' + idx + '].loading']: false
    });
  },

  _setConfirm(id, confirm) {
    const idx = this.data.messages.findIndex(m => m.id === id);
    if (idx < 0) return;
    this.setData({ ['messages[' + idx + '].confirm']: confirm });
  },

  _setLoading(id, val) {
    const idx = this.data.messages.findIndex(m => m.id === id);
    if (idx < 0) return;
    this.setData({ ['messages[' + idx + '].loading']: val });
  },

  _setMsgError(id, msg) {
    const idx = this.data.messages.findIndex(m => m.id === id);
    if (idx < 0) return;
    this.setData({
      ['messages[' + idx + '].error']: msg,
      ['messages[' + idx + '].typing']: false
    });
  },

  // 本地模拟逐字流式（兜底用）
  _streamText(id, text) {
    return new Promise((resolve) => {
      let i = 0;
      const step = 2;
      if (this._streamTimer) clearInterval(this._streamTimer);
      this._streamTimer = setInterval(() => {
        if (i >= text.length) {
          clearInterval(this._streamTimer);
          this._streamTimer = null;
          resolve();
          return;
        }
        const delta = text.slice(i, i + step);
        i += step;
        this._appendStream(id, delta);
      }, 24);
    });
  },

  _toPayloadMessages() {
    return this.data.messages
      .filter(m => m.role && m.content)
      .map(m => ({ role: m.role, content: m.content }));
  },

  _scrollToBottom() {
    if (!this.data.messages.length) return;
    // 递增的极大值会被 scroll-view 自动收敛到底部，且每次变化都能触发回滚
    this.setData({ scrollTop: ++this._scrollSeq * 100000 });
  },

  // 持久化：最多保留最近 50 条有效消息
  _persist() {
    const clean = this.data.messages
      .filter(m => m.content && !m.error)
      .map(m => ({ id: m.id, role: m.role, content: m.content }));
    wx.setStorageSync('aiChatHistory', clean.slice(-50));
  }
});
