const api = require('../../utils/api');
const i18n = require('../../utils/i18n');

Page({
  data: {
    keyword: '',
    loading: false,
    archive: null,
    countries: [],
    activeCountry: '',
    activeItem: {}
  },

  onLoad() { this.refreshLanguage(); },

  onShow() { this.refreshLanguage(); },

  refreshLanguage() {
    const language = i18n.getLanguage();
    const messages = i18n.getMessages();
    this.setData({
      i18n: { ...messages, archive: { ...messages.archive, alignedText: i18n.format(messages.archive.aligned, { count: this.data.countries.length }) } },
      languageShort: i18n.languages.find(item => item.key === language).short
    });
  },

  onChooseLanguage() {
    i18n.chooseLanguage(() => this.refreshLanguage());
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  async onSearch() {
    const kw = this.data.keyword.trim();
    if (!kw) {
      wx.showToast({ title: i18n.getMessages().archive.required, icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    try {
      const res = await api.getCrossBorderArchive('', kw);
      if (res.code === 500 || res.code === 404) {
        this.loadMock(kw);
      } else {
        const a = res.data || res;
        const items = a.items || [];
        const first = items.length ? items[0].country : '';
        this.setData({
          archive: a,
          countries: items.map(i => i.country),
          activeCountry: first,
          activeItem: items.find(i => i.country === first) || {}
        });
        this.refreshLanguage();
      }
    } catch (err) {
      this.loadMock(kw);
    } finally {
      this.setData({ loading: false });
    }
  },

  onSwitchCountry(e) {
    const country = e.currentTarget.dataset.country;
    const item = (this.data.archive.items || []).find(i => i.country === country) || {};
    this.setData({ activeCountry: country, activeItem: item });
  },

  loadMock(name) {
    const archive = {
      enterpriseName: name,
      items: [
        { country: '越南', localName: 'CÔNG TY TNHH TM ĐÔNG HƯNG', legalPerson: 'NGUYEN VAN A', businessScope: 'Xuất nhập khẩu nông sản', hsCode: 'HS 0901.21', hsDesc: '咖啡豆进出口（对齐国内：货物进出口）', customsRecord: '清关12次，无违规', translateNote: '越语译名已对齐：东兴贸易有限公司' },
        { country: '泰国', localName: 'BORAN IMPORT CO.,LTD', legalPerson: 'SOMCHAI', businessScope: 'Import agricultural products', hsCode: 'HS 0801.39', hsDesc: '坚果进出口（对齐国内：干果类）', customsRecord: '清关8次，1次查验', translateNote: '英/泰双语对齐完成' },
        { country: '马来西亚', localName: 'TIMUR TRADE SDN BHD', legalPerson: 'TAN AH LIM', businessScope: 'Palm oil trading', hsCode: 'HS 1511.90', hsDesc: '棕榈油贸易（需大宗商品资质）', customsRecord: '清关5次，合规', translateNote: '马来语对齐完成' },
        { country: '印度尼西亚', localName: 'PT NUSANTARA NIAGA', legalPerson: 'BUDI SANTOSO', businessScope: 'Perdagangan hasil perikanan', hsCode: 'HS 0306.17', hsDesc: '冷冻虾产品（对齐国内：水产品）', customsRecord: '演示档案：待核验', translateNote: '印尼语别名映射演示' },
        { country: '柬埔寨', localName: 'ក្រុមហ៊ុន មេគង្គ ពាណិជ្ជកម្ម', legalPerson: 'SOK DARA', businessScope: 'Agricultural commodity trade', hsCode: 'HS 1006.30', hsDesc: '大米贸易（对齐国内：粮食类）', customsRecord: '演示档案：待核验', translateNote: '高棉语别名映射演示' }
      ]
    };
    this.setData({
      archive,
      countries: archive.items.map(i => i.country),
      activeCountry: archive.items[0].country,
      activeItem: archive.items[0]
    });
    this.refreshLanguage();
  },

  onShareAppMessage() {
    return { title: '东盟风控预警平台 · 多语种跨境档案', path: '/pages/archive/archive' };
  }
});
