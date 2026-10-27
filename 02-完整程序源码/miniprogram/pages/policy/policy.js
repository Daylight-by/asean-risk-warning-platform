const api = require('../../utils/api');
const i18n = require('../../utils/i18n');
const translation = require('../../utils/translation-demo');

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    showSearch: true,
    keyword: '',
    selectedCountry: '',
    countries: ['泰国', '越南', '马来西亚', '印度尼西亚', '菲律宾', '新加坡', '缅甸', '柬埔寨', '老挝', '文莱'],
    countryOptions: [],
    policyList: [],
    page: 1,
    pageSize: 10,
    loading: false,
    hasMore: true
    ,showOriginal: false
  },

  onLoad() {
    this.refreshCountryOptions();
    this.loadPolicies();
  },

  onShow() {
    this.refreshCountryOptions();
    this.setData({ policyList: this.data.policyList.map(item => ({ ...translation.decorate(item, ['title', 'summary']), countryDisplay: i18n.displayCountry(item.country) })) });
  },

  refreshCountryOptions() {
    this.setData({ countryOptions: this.data.countries.map(value => ({ value, label: i18n.displayCountry(value) })) });
  },

  onToggleOriginal() { this.setData({ showOriginal: !this.data.showOriginal }); },

  onUnload() {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
  },

  onPullDownRefresh() {
    this.setData({ page: 1, policyList: [], hasMore: true });
    this.loadPolicies().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 });
      this.loadPolicies();
    }
  },

  // 输入事件（防抖）
  onInput(e) {
    this.setData({ keyword: e.detail.value });
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
    this.debounceTimer = setTimeout(() => {
      this.setData({ page: 1, policyList: [], hasMore: true });
      this.loadPolicies();
    }, 500);
  },

  onSearch() {
    this.setData({ page: 1, policyList: [], hasMore: true });
    this.loadPolicies();
  },

  // 清除关键词
  onClearKeyword() {
    this.setData({ keyword: '' });
    this.setData({ page: 1, policyList: [], hasMore: true });
    this.loadPolicies();
  },

  onFilterCountry(e) {
    const country = e.currentTarget.dataset.country;
    this.setData({
      selectedCountry: country,
      page: 1,
      policyList: [],
      hasMore: true
    });
    this.loadPolicies();
  },

  async loadPolicies() {
    if (this.data.loading) return;
    this.setData({ loading: true });

    try {
      const res = await api.searchPolicy(
        this.data.keyword,
        this.data.selectedCountry,
        this.data.page,
        this.data.pageSize
      );
      let list = res.data?.list || res.data || [];

      // 映射后端字段名到前端期望的字段名
      list = list.map(item => ({
        id: item.id,
        country: item.target_country || item.targetCountry || item.country || '',
        countryDisplay: i18n.displayCountry(item.target_country || item.targetCountry || item.country || ''),
        title: item.policy_title || item.policyTitle || item.title || '',
        summary: item.policy_content || item.policyContent || item.summary || '',
        publishDate: item.publish_date || item.publishDate || '',
        source: item.data_source || item.dataSource || item.source || '官方发布',
        tags: item.tags || []
      }));

      list = list.map(item => translation.decorate(item, ['title', 'summary']));
      const locale = i18n.getLanguage();
      if (locale !== 'zh' && list.length) {
        try {
          const translationRes = await api.getMultilingualContent('policy', locale, list.map(item => item.id));
          const dictionary = (translationRes && translationRes.data) || {};
          list = list.map(item => {
            const fields = dictionary[String(item.id)] || {};
            return {
              ...item,
              ...(fields.title ? { displayTitle: fields.title } : {}),
              ...(fields.summary ? { displaySummary: fields.summary } : {}),
              ...(fields.translationStatus ? { translationStatus: fields.translationStatus } : {}),
              ...(fields.verificationStatus ? { verificationStatus: fields.verificationStatus } : {})
            };
          });
        } catch (_) { /* 演示词典兜底，不影响政策查询 */ }
      }
      this.setData({
        policyList: this.data.page === 1
          ? list
          : [...this.data.policyList, ...list],
        hasMore: list.length >= this.data.pageSize
      });
    } catch (err) {
      // API不可用，使用模拟数据
      if (this.data.page === 1) {
        const mockData = this.getMockData();
        // 过滤
        let filtered = mockData;
        if (this.data.selectedCountry) {
          filtered = filtered.filter(item => item.country === this.data.selectedCountry);
        }
        if (this.data.keyword) {
          const kw = this.data.keyword.toLowerCase();
          filtered = filtered.filter(item =>
            item.title.toLowerCase().includes(kw) ||
            item.summary.toLowerCase().includes(kw)
          );
        }
        this.setData({ policyList: filtered.map(item => translation.decorate(item, ['title', 'summary'])), hasMore: false });
      } else {
        this.setData({ hasMore: false });
      }
    } finally {
      this.setData({ loading: false });
    }
  },

  getMockData() {
    return [
      {
        id: 'p001',
        country: '泰国',
        title: '泰国2025年跨境电商税收新规',
        summary: '泰国税务局发布跨境电商税收征收管理办法，对跨境B2C交易征收增值税，年交易额超过180万泰铢需进行税务登记。',
        publishDate: '2025-06-15',
        source: '泰国税务局',
        tags: ['跨境电商', '税收政策', '增值税']
      },
      {
        id: 'p002',
        country: '越南',
        title: '越南-欧盟自贸协定原产地规则更新',
        summary: '越南工贸部发布第15/2025号通知，更新EVFTA原产地规则实施细则，涉及纺织品、电子产品等类目。',
        publishDate: '2025-05-28',
        source: '越南工贸部',
        tags: ['自贸协定', '原产地规则', '关税优惠']
      },
      {
        id: 'p003',
        country: '马来西亚',
        title: '马来西亚2025年数字经济发展规划',
        summary: '马来西亚数字经济局推出数字贸易便利化措施，简化中小企业跨境电商备案流程。',
        publishDate: '2025-04-20',
        source: 'MDEC',
        tags: ['数字经济', '跨境电商', '中小企业']
      },
      {
        id: 'p004',
        country: '印度尼西亚',
        title: '印尼食品安全进口新标准发布',
        summary: '印尼食品药品监管局发布食品进口安全标准修订版，新增检测项目及合规要求。',
        publishDate: '2025-06-01',
        source: 'BPOM',
        tags: ['食品安全', '进口标准', '合规要求']
      },
      {
        id: 'p005',
        country: '新加坡',
        title: '新加坡海关电子化通关全面升级',
        summary: '新加坡海关推出TradeNet 2.0系统，全面实现进出口通关电子化，缩短清关时间。',
        publishDate: '2025-05-15',
        source: '新加坡海关',
        tags: ['通关便利化', '电子化', '贸易数字化']
      },
      {
        id: 'p006',
        country: '菲律宾',
        title: '菲律宾跨境电商平台运营规范出台',
        summary: '菲律宾贸工部发布电商平台运营管理规范，要求平台对跨境商家进行资质审核。',
        publishDate: '2025-03-10',
        source: 'DTI',
        tags: ['电商规范', '平台管理', '资质审核']
      },
      {
        id: 'p007',
        country: '泰国',
        title: '泰国投资促进委员会2025年投资优惠政策',
        summary: 'BOI更新外商投资鼓励目录，对数字产业、物流仓储、农产品加工等领域提供税收优惠。',
        publishDate: '2025-02-18',
        source: 'BOI',
        tags: ['投资优惠', '外商投资', '税收减免']
      },
      {
        id: 'p008',
        country: '越南',
        title: '越南2025年最低工资标准调整通知',
        summary: '越南劳动部发布2025年区域最低工资标准，四类地区工资上调6%-8%。',
        publishDate: '2025-01-05',
        source: '越南劳动部',
        tags: ['劳动法规', '最低工资', '合规']
      }
    ].map(item => ({ ...item, countryDisplay: i18n.displayCountry(item.country) }));
  },

  onViewPolicy(e) {
    const { id } = e.currentTarget.dataset;
    const item = this.data.policyList.find(p => p.id === id);
    if (item) {
      wx.showModal({
        title: this.data.showOriginal ? item.original.title : item.displayTitle,
        content: (this.data.showOriginal ? item.original.summary : item.displaySummary) + '\n\n' + (this.data.language === 'vi' ? 'Nguồn: ' : this.data.language === 'en' ? 'Source: ' : '来源：') + item.source + '\n' + (this.data.language === 'vi' ? 'Ngày: ' : this.data.language === 'en' ? 'Published: ' : '发布时间：') + item.publishDate,
        showCancel: false,
        confirmText: this.data.language === 'vi' ? 'Đã hiểu' : this.data.language === 'en' ? 'Got it' : '知道了'
      });
    }
  },

  onShareAppMessage() {
    return {
      title: '东盟风控预警平台 · 东盟政策库',
      path: '/pages/policy/policy'
    };
  }
});
