const api = require('../../utils/api');
const i18n = require('../../utils/i18n');
const translation = require('../../utils/translation-demo');

function buildFunctions(language) {
  const labels = {
    zh: [['风控评分', '企业信用画像'], ['信用报告', '多维风险报告'], ['合规体检', '一键合规扫描'], ['数据看板', '监管态势总览'], ['口岸画像', '跨境通关画像'], ['跨境图谱', '关联风险穿透'], ['多语种档案', ''], ['监管后台', ''], ['纠纷案例', ''], ['术语库', '']],
    en: [['Risk score', 'Company credit profile'], ['Credit report', 'Multi-factor risk report'], ['Compliance', 'One-tap compliance check'], ['Dashboard', 'Regulatory overview'], ['Port profile', ''], ['Risk graph', ''], ['Multilingual archive', ''], ['Regulation', ''], ['Dispute cases', ''], ['Glossary', '']],
    vi: [['Điểm rủi ro', 'Hồ sơ tín dụng doanh nghiệp'], ['Báo cáo tín dụng', 'Báo cáo rủi ro đa chiều'], ['Kiểm tra tuân thủ', 'Quét tuân thủ một chạm'], ['Bảng điều khiển', 'Tổng quan quản lý'], ['Hồ sơ cửa khẩu', ''], ['Đồ thị liên kết', ''], ['Hồ sơ đa ngôn ngữ', ''], ['Quản lý', ''], ['Tranh chấp', ''], ['Thuật ngữ', '']]
  }[language] || [];
  const core = [['score', '#EAF1FF', '/pages/search/search'], ['report', '#FFF4E0', '/pages/report/report'], ['compliance', '#E6F7F2', '/pages/compliance/compliance'], ['dashboard', '#E9F7EF', '/pages/dashboard/dashboard']];
  const more = [['port', '#E6F4FF', '/pages/port/port'], ['graph', '#EFEAFF', '/pages/graph/graph'], ['archive', '#E6F7F2', '/pages/archive/archive'], ['regulate', '#FFEEE8', '/pages/regulate/regulate'], ['dispute', '#F3EBFA', '/pages/dispute/dispute'], ['terminology', '#EAF1FF', '/pages/terminology/terminology']];
  return { coreFuncs: core.map((item, index) => ({ key: item[0], bg: item[1], path: item[2], text: labels[index][0], desc: labels[index][1] })), moreFuncs: more.map((item, index) => ({ key: item[0], bg: item[1], path: item[2], text: labels[index + 4][0], desc: labels[index + 4][1] })) };
}

Page({
  data: {
    showSplash: true,
    keyword: '',
    // 核心功能：4 个，与底部 Tab 不冲突，一屏直达高频任务
    coreFuncs: [
      { key: 'score', text: '风控评分', desc: '企业信用画像', bg: '#EAF1FF', path: '/pages/search/search' },
      { key: 'report', text: '信用报告', desc: '多维风险报告', bg: '#FFF4E0', path: '/pages/report/report' },
      { key: 'compliance', text: '合规体检', desc: '一键合规扫描', bg: '#E6F7F2', path: '/pages/compliance/compliance' },
      { key: 'dashboard', text: '数据看板', desc: '监管态势总览', bg: '#E9F7EF', path: '/pages/dashboard/dashboard' }
    ],
    // 更多工具：横滑收纳（次要功能·专业工具）
    moreFuncs: [
      { key: 'port', text: '口岸画像', bg: '#E6F4FF', path: '/pages/port/port' },
      { key: 'graph', text: '跨境图谱', bg: '#EFEAFF', path: '/pages/graph/graph' },
      { key: 'archive', text: '多语种档案', bg: '#E6F7F2', path: '/pages/archive/archive' },
      { key: 'regulate', text: '监管后台', bg: '#FFEEE8', path: '/pages/regulate/regulate' },
      { key: 'dispute', text: '纠纷案例', bg: '#F3EBFA', path: '/pages/dispute/dispute' },
      { key: 'terminology', text: '术语库', bg: '#EAF1FF', path: '/pages/terminology/terminology' }
    ],
    stats: {
      enterpriseTotal: '1286',
      warningTotal: '0',
      policyTotal: '8',
      countryCount: '10'
    },
    warnings: [],
    policies: [],
    showOriginalFeed: false
  },

  onLoad() {
    this.refreshLanguage();
    this._splashStart = Date.now();
    this.loadFeed();
  },

  onShow() {
    this.refreshLanguage();
    this.refreshFeedTranslations();
  },

  refreshLanguage() {
    const language = i18n.getLanguage();
    const funcs = buildFunctions(language);
    this.setData({ language, languageShort: i18n.languages.find(item => item.key === language).short, i18n: i18n.getMessages(), ...funcs });
  },

  onChooseLanguage() {
    i18n.chooseLanguage(() => {
      this.refreshLanguage();
      this.refreshFeedTranslations();
    });
  },

  onToggleFeedOriginal() { this.setData({ showOriginalFeed: !this.data.showOriginalFeed }); },

  refreshFeedTranslations() {
    this.setData({
      warnings: this.data.warnings.map(item => ({ ...translation.decorate(item, ['content']), levelText: i18n.riskLabel(item.level) })),
      policies: this.data.policies.map(item => ({ ...translation.decorate(item, ['title', 'summary']), countryDisplay: i18n.displayCountry(item.country) }))
    });
  },

  // 加载首页信息流（保留原有数据交互）
  async loadFeed() {
    const safeRequest = promise => promise.catch(() => null);
    try {
      const [overviewRes, warningRes, policyRes] = await Promise.all([
        safeRequest(api.getDashboardOverview()),
        safeRequest(api.getWarningList(1, 6)),
        safeRequest(api.searchPolicy('', '', 1, 6))
      ]);
      const overview = (overviewRes && (overviewRes.data || overviewRes)) || {};
      const warningData = (warningRes && (warningRes.data || warningRes)) || {};
      const policyData = (policyRes && (policyRes.data || policyRes)) || {};
      const wList = Array.isArray(warningData) ? warningData : (warningData.list || []);
      const pList = Array.isArray(policyData) ? policyData : (policyData.list || []);

      const warnings = wList.slice(0, 6).map(it => {
        const lvl = it.warningLevel === '高风险' || it.warningLevel === 'high' ? 'high'
          : it.warningLevel === '中风险' || it.warningLevel === 'medium' ? 'medium' : 'low';
        return translation.decorate({
          id: it.id,
          enterpriseName: it.enterpriseName || '未知企业',
          displayEnterpriseName: translation.enterpriseAlias(it.enterpriseName || '未知企业'),
          level: lvl,
          levelText: i18n.riskLabel(lvl),
          content: it.content || it.warningContent || it.title || ''
        }, ['content']);
      });

      const policies = pList.slice(0, 6).map(it => translation.decorate({
        id: it.id,
        country: it.country || it.target_country || '',
        title: it.title || it.policy_title || '',
        summary: it.summary || it.policy_content || ''
      }, ['title', 'summary']));

      const locale = i18n.getLanguage();
      if (locale !== 'zh') {
        try {
          const [warningTranslations, policyTranslations, enterpriseTranslations] = await Promise.all([
            api.getMultilingualContent('warning', locale, warnings.map(item => item.id)),
            api.getMultilingualContent('policy', locale, policies.map(item => item.id)),
            api.getMultilingualContent('enterprise', locale, warnings.map(item => item.enterpriseName))
          ]);
          const wDict = (warningTranslations && warningTranslations.data) || {};
          const pDict = (policyTranslations && policyTranslations.data) || {};
          const eDict = (enterpriseTranslations && enterpriseTranslations.data) || {};
          warnings.forEach(item => {
            if (wDict[String(item.id)]?.content) item.displayContent = wDict[String(item.id)].content;
            if (eDict[item.enterpriseName]?.alias) item.displayEnterpriseName = eDict[item.enterpriseName].alias;
          });
          policies.forEach(item => {
            if (pDict[String(item.id)]?.title) item.displayTitle = pDict[String(item.id)].title;
            if (pDict[String(item.id)]?.summary) item.displaySummary = pDict[String(item.id)].summary;
          });
        } catch (_) { /* 已有本地映射兜底 */ }
      }

      this.setData({
        stats: {
          enterpriseTotal: overview.enterpriseTotal || overview.total || '1286',
          warningTotal: overview.warningTotal || wList.length || '0',
          policyTotal: overview.policyTotal || pList.length || '8',
          countryCount: Math.max(Number(overview.countryCount) || 0, 10)
        },
        warnings,
        policies
      });
    } catch (err) {
      // 保留默认展示
    } finally {
      this.dismissSplash();
    }
  },

  onPullDownRefresh() {
    api.clearCache();
    this.loadFeed().then(() => wx.stopPullDownRefresh());
  },

  onSkip() {
    this.setData({ showSplash: false });
  },

  onUnload() {
    if (this._splashTimer) clearTimeout(this._splashTimer);
  },

  // 闪屏在数据就绪后关闭（最少展示 800ms，避免一闪而过）
  dismissSplash() {
    if (!this.data.showSplash) return;
    const wait = Math.max(0, 800 - (Date.now() - (this._splashStart || 0)));
    if (this._splashTimer) clearTimeout(this._splashTimer);
    this._splashTimer = setTimeout(() => { this.setData({ showSplash: false }); }, wait);
  },

  onFuncTap(e) {
    const path = e.currentTarget.dataset.path;
    if (!path) return;
    // warning/policy 是 tabBar 页，必须用 switchTab；其余非 tab 页用 navigateTo
    if (path === '/pages/warning/warning' || path === '/pages/policy/policy') {
      wx.switchTab({ url: path });
    } else {
      wx.navigateTo({ url: path });
    }
  },

  onGoSearch() {
    wx.navigateTo({ url: '/pages/search/search' });
  },

  onGoWarning() {
    wx.setStorageSync('lastViewWarningTime', Date.now());
    wx.switchTab({ url: '/pages/warning/warning' });
  },

  onGoPolicy() {
    wx.switchTab({ url: '/pages/policy/policy' });
  },

  onGoReport() {
    wx.navigateTo({ url: '/pages/report/report' });
  },

  onGoDashboard() {
    wx.navigateTo({ url: '/pages/dashboard/dashboard' });
  },

  // 打开 AI 风控助手
  onOpenAI() {
    wx.navigateTo({ url: '/pages/ai-chat/ai-chat' });
  }
});
