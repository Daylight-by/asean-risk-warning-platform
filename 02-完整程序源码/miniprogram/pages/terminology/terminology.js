const api = require('../../utils/api')
Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    termList: [],
    activeCategory: '',
    keyword: '',
    loading: false,
    page: 1,
    pageSize: 20,
    hasMore: true
  },
  onLoad() { this.loadTerms() },
  onPullDownRefresh() {
    this.setData({ page: 1, hasMore: true })
    this.loadTerms().then(() => wx.stopPullDownRefresh()).catch(() => wx.stopPullDownRefresh())
  },
  onCategoryChange(e) {
    this.setData({ activeCategory: e.currentTarget.dataset.cat, page: 1, hasMore: true })
    this.loadTerms()
  },
  onInput(e) { this.setData({ keyword: e.detail.value }) },
  onSearch() { this.setData({ page: 1, hasMore: true }); this.loadTerms() },
  async loadTerms() {
    if (this.data.loading) return
    this.setData({ loading: true })
    try {
      const res = await api.get('/terminology/list', {
        category: this.data.activeCategory,
        keyword: this.data.keyword,
        page: this.data.page,
        pageSize: this.data.pageSize
      })
      const list = res.data?.list || []
      this.setData({
        termList: this.data.page === 1 ? list : [...this.data.termList, ...list],
        hasMore: list.length >= this.data.pageSize,
        loading: false
      })
    } catch (e) {
      // mock数据
      const mockTerms = [
        { id: 1, termCn: '报关单', termEn: 'Customs Declaration', termVn: 'Tờ khai hải quan', termTh: 'ใบขนสินค้า', category: '报关', hsCode: '', description: '进出口货物向海关申报的法定单据', source: '海关总署' },
        { id: 2, termCn: '原产地证', termEn: 'Certificate of Origin', termVn: 'Giấy chứng nhận xuất xứ', termTh: 'หนังสือรับรองถิ่นกำเนิด', category: '报关', hsCode: '', description: '证明货物原产国的官方文件', source: '广西商务厅' },
        { id: 3, termCn: '出口退税', termEn: 'Export Tax Rebate', termVn: 'Hoàn thuế xuất khẩu', termTh: 'คืนภาษีส่งออก', category: '税务', hsCode: '', description: '对出口货物退还已征收的国内税款', source: '税务局' },
        { id: 4, termCn: '跨境人民币结算', termEn: 'Cross-border RMB Settlement', category: '支付', description: '使用人民币进行跨境贸易结算', source: '人民银行' },
        { id: 5, termCn: '信用证', termEn: 'Letter of Credit', termVn: 'Thư tín dụng', category: '支付', description: '银行开立的有条件付款承诺', source: '人民银行' },
        { id: 6, termCn: '提单', termEn: 'Bill of Lading', termVn: 'Vận đơn', category: '物流', description: '承运人签发的货物收据和运输合同证明', source: '交通运输部' },
        { id: 7, termCn: '自由贸易协定', termEn: 'Free Trade Agreement', termVn: 'Hiệp định thương mại tự do', category: '法律', description: '两国或多国之间降低贸易壁垒的协议', source: '商务部' },
        { id: 8, termCn: '反倾销税', termEn: 'Anti-dumping Duty', category: '税务', description: '对倾销进口商品征收的附加税', source: '商务部' }
      ]
      this.setData({ termList: this.data.page === 1 ? mockTerms : [...this.data.termList, ...mockTerms], hasMore: false, loading: false })
    }
  },
  onReachBottom() {
    if (this.data.hasMore) {
      this.setData({ page: this.data.page + 1 })
      this.loadTerms()
    }
  }
})
