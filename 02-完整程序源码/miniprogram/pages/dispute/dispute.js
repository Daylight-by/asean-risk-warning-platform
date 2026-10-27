const api = require('../../utils/api')
const translation = require('../../utils/translation-demo')
Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    caseList: [],
    activeType: '',
    loading: false,
    page: 1,
    pageSize: 20,
    hasMore: true,
    showMatchModal: false,
    showDetailModal: false,
    matchForm: { tradeCategory: '', countries: '' },
    detailCase: {},
    showOriginal: false
  },
  onLoad() { this.loadCases() },
  onPullDownRefresh() {
    this.setData({ page: 1, hasMore: true })
    this.loadCases().then(() => wx.stopPullDownRefresh()).catch(() => wx.stopPullDownRefresh())
  },
  onTypeChange(e) {
    this.setData({ activeType: e.currentTarget.dataset.type, page: 1, hasMore: true })
    this.loadCases()
  },
  async loadCases() {
    if (this.data.loading) return
    this.setData({ loading: true })
    try {
      const res = await api.get('/dispute/list', {
        type: this.data.activeType,
        keyword: '',
        page: this.data.page,
        pageSize: this.data.pageSize
      })
      const list = res.data?.list || []
      this.setData({
        caseList: (this.data.page === 1 ? list : [...this.data.caseList, ...list]).map(item => translation.decorate(item, ['title', 'summary', 'judgment', 'riskAdvice'])),
        hasMore: list.length >= this.data.pageSize,
        loading: false
      })
    } catch (e) {
      const mockCases = [
        { id: 1, caseNo: 'GX-2025-001', title: '越南买方拒付农产品货款案', caseType: '合同纠纷', tradeCategory: '农产品', countries: '中国-越南', summary: '中方出口商向越南买方出口一批热带水果，货到后买方以质量不符为由拒付货款，涉及金额120万元。', judgment: '法院判决买方支付80%货款，中方承担20%质量瑕疵责任。', riskAdvice: '签订合同时明确质量标准和检验方式，建议采用信用证结算。', court: '南宁中院', publishDate: '2025-06-15' },
        { id: 2, caseNo: 'GX-2025-002', title: '泰国转口贸易原产地证伪造案', caseType: '合同纠纷', tradeCategory: '电子产品', countries: '中国-泰国-美国', summary: '中方企业通过泰国转口向美国出口电子产品，使用伪造的泰国原产地证享受优惠关税。', judgment: '中方企业被处以货值3倍罚款，列入海关失信名单。', riskAdvice: '转口贸易务必确保原产地证真实合法，避免使用虚假中转文件。', court: '南宁海关', publishDate: '2025-05-20' },
        { id: 3, caseNo: 'GX-2025-003', title: '马来西亚棕榈油质量纠纷案', caseType: '产品质量', tradeCategory: '农产品', countries: '中国-马来西亚', summary: '中方进口马来西亚棕榈油，到货后发现酸价超标，不符合合同约定标准。', judgment: '卖方退还50%货款并承担检验费用。', riskAdvice: '进口农产品应在合同中明确质量指标和第三方检验机构。', court: '防城港法院', publishDate: '2025-04-10' },
        { id: 4, caseNo: 'GX-2025-004', title: '印尼镍矿出口许可违规案', caseType: '合同纠纷', tradeCategory: '矿产品', countries: '中国-印尼', summary: '中方企业在印尼投资镍矿项目，未取得当地出口许可证即擅自出口。', judgment: '货物被没收，企业被列入印尼贸易黑名单。', riskAdvice: '投资印尼矿产资源前务必了解外资持股限制和出口许可要求。', court: '最高法', publishDate: '2025-03-25' },
        { id: 5, caseNo: 'GX-2024-007', title: '越南商标侵权纠纷案', caseType: '知识产权', tradeCategory: '日用品', countries: '中国-越南', summary: '中方企业在越南注册的商标被当地企业仿冒使用，涉及日化产品。', judgment: '越南方停止侵权并赔偿30万元。', riskAdvice: '出口东盟前应提前在目标国注册商标，做好知识产权保护。', court: '越南河内法院', publishDate: '2024-12-15' },
        { id: 6, caseNo: 'GX-2024-009', title: '跨境支付汇率损失纠纷案', caseType: '支付纠纷', tradeCategory: '农产品', countries: '中国-越南', summary: '中越贸易采用美元结算，汇率大幅波动导致一方遭受重大汇兑损失。', judgment: '法院认定汇率风险由双方共同承担，各担50%。', riskAdvice: '跨境贸易建议在合同中约定汇率波动分担机制或使用人民币结算。', court: '南宁中院', publishDate: '2024-10-05' }
      ]
      this.setData({ caseList: (this.data.page === 1 ? mockCases : [...this.data.caseList, ...mockCases]).map(item => translation.decorate(item, ['title', 'summary', 'judgment', 'riskAdvice'])), hasMore: false, loading: false })
    }
  },
  showMatchDialog() { this.setData({ showMatchModal: true }) },
  onToggleOriginal() { this.setData({ showOriginal: !this.data.showOriginal }) },
  hideMatchDialog() { this.setData({ showMatchModal: false }) },
  onMatchInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`matchForm.${field}`]: e.detail.value })
  },
  async doMatch() {
    wx.showLoading({ title: this.data.t.pages.dispute.matching })
    try {
      const res = await api.post('/dispute/match', this.data.matchForm)
      const list = res.data || []
      this.setData({ caseList: list, showMatchModal: false })
      wx.hideLoading()
      if (list.length === 0) wx.showToast({ title: this.data.t.pages.dispute.noMatch, icon: 'none' })
    } catch (e) {
      wx.hideLoading()
      wx.showToast({ title: this.data.t.pages.dispute.matchFailed, icon: 'none' })
    }
  },
  showDetail(e) {
    const index = e.currentTarget.dataset.index
    this.setData({ detailCase: this.data.caseList[index], showDetailModal: true })
  },
  hideDetail() { this.setData({ showDetailModal: false }) },
  onReachBottom() {
    if (this.data.hasMore) {
      this.setData({ page: this.data.page + 1 })
      this.loadCases()
    }
  }
})
