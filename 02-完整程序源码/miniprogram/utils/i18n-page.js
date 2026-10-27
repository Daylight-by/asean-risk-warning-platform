const i18n = require('./i18n');

module.exports = Behavior({
  data: {
    t: i18n.getMessages(),
    language: i18n.getLanguage()
  },
  pageLifetimes: {
    show() {
      this.setData({ t: i18n.getMessages(), language: i18n.getLanguage() });
    }
  }
});
