<template>
  <div id="app">
    <a href="#main-content" class="skip-link">跳到主要内容</a>

    <!-- 已登录：顶部导航 + 内容区 -->
    <div v-if="$route.path !== '/login'" class="app-shell">
      <!-- 顶部导航栏：桌面常驻横向菜单，移动端折叠为汉堡抽屉 -->
      <header class="app-nav" v-show="!isFullscreen" :class="{ 'nav-scrolled': scrolled }">
        <div class="nav-inner">
          <!-- 移动端汉堡按钮 -->
          <button
            class="hamburger"
            type="button"
            :aria-label="mobileOpen ? '收起导航菜单' : '展开导航菜单'"
            :aria-expanded="mobileOpen"
            @click="toggleNav"
          >
            <span class="hamburger-box" :class="{ open: mobileOpen }"><i></i><i></i><i></i></span>
          </button>

          <!-- 品牌区 -->
          <router-link to="/dashboard" class="brand" aria-label="东盟风控平台 首页">
            <span class="brand-logo" aria-hidden="true"><i class="el-icon-s-data"></i></span>
            <span class="brand-name">东盟风控平台</span>
          </router-link>

          <!-- 桌面端横向菜单 -->
          <nav class="nav-menu" aria-label="主导航菜单">
            <ul class="nav-list">
              <li
                v-for="(grp, gi) in navGroups"
                :key="'nav-' + gi"
                class="nav-item"
                :class="{ 'has-dropdown': isDropdown(grp), 'is-open': openGroups[gi] }"
                @mouseenter="onEnter(gi)"
                @mouseleave="onLeave(gi)"
                @focusout="onFocusOut(gi, $event)"
              >
                <!-- 单条目组：直接链接 -->
                <router-link
                  v-if="!isDropdown(grp)"
                  :to="grp.items[0].index"
                  class="nav-link"
                  :class="{ active: isActive(grp.items[0].index) }"
                  :aria-current="isActive(grp.items[0].index) ? 'page' : null"
                >
                  <i :class="grp.items[0].icon" class="nav-ico" aria-hidden="true"></i>
                  <span>{{ grp.label }}</span>
                </router-link>

                <!-- 多条目组：下拉父级 -->
                <button
                  v-else
                  type="button"
                  class="nav-link nav-parent"
                  :class="{ active: isGroupActive(grp) }"
                  :aria-haspopup="true"
                  :aria-expanded="!!openGroups[gi]"
                  :aria-label="grp.label + ' 子菜单'"
                  @click="toggleGroup(gi)"
                  @focus="onEnter(gi)"
                >
                  <i :class="grp.items[0].icon" class="nav-ico" aria-hidden="true"></i>
                  <span>{{ grp.label }}</span>
                  <i class="el-icon-arrow-down nav-caret" aria-hidden="true"></i>
                </button>

                <!-- 下拉面板：始终在 DOM 中，靠 .is-open 切换显隐，纯 CSS 过渡无闪烁 -->
                <ul
                  v-if="isDropdown(grp)"
                  class="dropdown"
                  :aria-label="grp.label + ' 子菜单'"
                >
                  <li v-for="it in grp.items" :key="it.index">
                    <router-link
                      :to="it.index"
                      class="dropdown-link"
                      :class="{ active: isActive(it.index) }"
                      :aria-current="isActive(it.index) ? 'page' : null"
                      @click="closeAll"
                    >
                      <i :class="it.icon" class="nav-ico" aria-hidden="true"></i>
                      <span>{{ it.label }}</span>
                    </router-link>
                  </li>
                </ul>
              </li>
            </ul>
          </nav>

          <!-- 用户信息 -->
          <div class="nav-user">
            <span class="admin-name">{{ username }}</span>
            <button type="button" class="logout-btn" @click="handleLogout">退出</button>
          </div>
        </div>
      </header>

      <!-- 移动端抽屉 -->
      <div class="mobile-drawer" :class="{ open: mobileOpen }" :aria-hidden="!mobileOpen">
        <div class="drawer-head">
          <span class="brand-logo" aria-hidden="true"><i class="el-icon-s-data"></i></span>
          <span class="brand-name">东盟风控平台</span>
        </div>
        <nav class="drawer-nav" aria-label="移动端导航菜单">
          <div v-for="(grp, gi) in navGroups" :key="'dr-' + gi" class="drawer-group">
            <div class="drawer-group-label">{{ grp.label }}</div>
            <router-link
              v-for="it in grp.items"
              :key="it.index"
              :to="it.index"
              class="drawer-link"
              :class="{ active: isActive(it.index) }"
              :aria-current="isActive(it.index) ? 'page' : null"
              @click="closeMobile"
            >
              <i :class="it.icon" class="nav-ico" aria-hidden="true"></i>
              <span>{{ it.label }}</span>
            </router-link>
          </div>
        </nav>
      </div>

      <!-- 抽屉遮罩：点击关闭 -->
      <div
        class="nav-backdrop"
        :class="{ show: mobileOpen }"
        @click="closeMobile"
        aria-hidden="true"
      ></div>

      <!-- 主内容 -->
      <main
        id="main-content"
        tabindex="-1"
        class="app-main"
        :class="{ 'app-main-fullscreen': isFullscreen }"
      >
        <router-view />
      </main>
    </div>

    <!-- 登录页不显示导航 -->
    <router-view v-else />
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      isFullscreen: false,
      mobileOpen: false,
      isMobile: false,
      scrolled: false,
      openGroups: {}, // 下拉展开状态（gi -> bool）
      leaveTimers: {}, // 延迟关闭定时器（gi -> timerId）
      username: '',
      // 导航分组：顺序符合使用习惯。多条目组自动变为下拉。
      navGroups: [
        {
          label: '数据监控',
          items: [
            { index: '/dashboard', icon: 'el-icon-s-data', label: '数据大屏' },
            { index: '/supervision', icon: 'el-icon-monitor', label: '监管大屏' },
            { index: '/enterprise', icon: 'el-icon-s-grid', label: '企业风控' },
            { index: '/warning', icon: 'el-icon-bell', label: '预警管理' }
          ]
        },
        {
          label: '知识中心',
          items: [
            { index: '/policy', icon: 'el-icon-document', label: '政策库' }
          ]
        },
        {
          label: '创新模块',
          submenu: true,
          items: [
            { index: '/relation-graph', icon: 'el-icon-share', label: '跨境关联图谱' },
            { index: '/fraud-alert', icon: 'el-icon-warning-outline', label: '欺诈预警' },
            { index: '/settlement-credit', icon: 'el-icon-coin', label: '跨境结算信用' },
            { index: '/compliance-check', icon: 'el-icon-s-check', label: '合规体检' },
            { index: '/terminology', icon: 'el-icon-notebook-2', label: '商事术语库' },
            { index: '/dispute-cases', icon: 'el-icon-document-checked', label: '纠纷案例库' },
            { index: '/country-risk', icon: 'el-icon-map-location', label: '国别风控' },
            { index: '/visual-query', icon: 'el-icon-data-analysis', label: '可视化查询' }
          ]
        },
        {
          label: '系统',
          items: [
            { index: '/system', icon: 'el-icon-setting', label: '系统设置' }
          ]
        }
      ]
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    }
  },
  watch: {
    '$route.path': {
      handler() {
        // 切出页面时退出全屏，确保顶部导航和退出按钮始终可用
        this.isFullscreen = false
        // 路由切换：收起移动端抽屉并关闭所有下拉
        this.mobileOpen = false
        this.closeAll()
      },
      immediate: true
    }
  },
  created() {
    this.username = sessionStorage.getItem('username') || '管理员'
    this.handleResize()
    window.addEventListener('resize', this.handleResize)
    window.addEventListener('scroll', this.onScroll, { passive: true })
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    window.removeEventListener('scroll', this.onScroll)
  },
  methods: {
    // 多条目组（或显式 submenu）显示为下拉
    isDropdown(grp) {
      return !!grp.submenu || (grp.items && grp.items.length > 1)
    },
    isActive(index) {
      return this.activeMenu === index
    },
    isGroupActive(grp) {
      return grp.items.some((it) => this.isActive(it.index))
    },
    // 下拉展开/收起（用 $set 保证新增 key 的响应性）
    onEnter(gi) {
      // 鼠标重新进入时，取消待执行的关闭定时器
      if (this.leaveTimers[gi]) {
        clearTimeout(this.leaveTimers[gi])
        delete this.leaveTimers[gi]
      }
      this.$set(this.openGroups, gi, true)
    },
    onLeave(gi) {
      // 延迟 150ms 关闭，给鼠标留出从触发器滑入下拉面板的时间
      this.$set(this.leaveTimers, gi, setTimeout(() => {
        this.$set(this.openGroups, gi, false)
        delete this.leaveTimers[gi]
      }, 150))
    },
    toggleGroup(gi) {
      this.$set(this.openGroups, gi, !this.openGroups[gi])
    },
    onFocusOut(gi, e) {
      // 焦点完全离开该组时才收起（键盘可达）
      if (!e.currentTarget.contains(e.relatedTarget)) {
        this.$set(this.openGroups, gi, false)
      }
    },
    closeAll() {
      Object.values(this.leaveTimers).forEach(clearTimeout)
      this.leaveTimers = {}
      this.openGroups = {}
    },
    // 移动端抽屉
    toggleNav() {
      this.mobileOpen = !this.mobileOpen
    },
    closeMobile() {
      this.mobileOpen = false
    },
    handleResize() {
      const mobile = window.innerWidth <= 1024
      this.isMobile = mobile
      if (!mobile) this.mobileOpen = false
    },
    onScroll() {
      this.scrolled = window.scrollY > 8
    },
    // 大屏页面通过此方法切换是否隐藏顶部导航（默认不隐藏，防止登录后无法退出）
    toggleFullscreen() {
      this.isFullscreen = !this.isFullscreen
    },
    handleLogout() {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('username')
      this.$router.push('/login')
    }
  }
}
</script>

<style>
/* App.vue 外壳样式 · 顶部固定导航栏重构（仅外观/交互，路由与 index 不变）
   设计令牌见 src/styles/theme-enhance.css 的 :root */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: var(--font-body);
}

#app {
  background: var(--bg-base);
}

.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 跳转链接（无障碍）：键盘聚焦时显现 */
.skip-link {
  position: absolute;
  left: -9999px;
  top: 10px;
  z-index: 2000;
  padding: 8px 16px;
  background: var(--primary);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 13px;
  text-decoration: none;
  transition: left 0.2s var(--ease);
}
.skip-link:focus {
  left: 12px;
}

/* ===================== 顶部导航栏 ===================== */
.app-nav {
  position: sticky;
  top: 0;
  z-index: 2000;
  background: rgba(15, 20, 29, 0.92);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
  transition: box-shadow 0.25s var(--ease), background 0.25s var(--ease);
}
.app-nav.nav-scrolled {
  box-shadow: var(--shadow-1);
  background: rgba(15, 20, 29, 0.98);
}

.nav-inner {
  height: 60px;
  width: 100%;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 品牌区 */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}
.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary), var(--primary-soft));
  color: #fff;
  font-size: 18px;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(58, 111, 247, 0.32);
}
.brand-name {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 15px;
  color: var(--text);
  letter-spacing: 0.5px;
  white-space: nowrap;
}

/* ===================== 桌面横向菜单 ===================== */
.nav-menu {
  flex: 1;
  margin-left: 12px;
}
.nav-list {
  list-style: none;
  display: flex;
  align-items: center;
  gap: 2px;
}
.nav-item {
  position: relative;
}
.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 14px;
  border: none;
  background: transparent;
  border-radius: 10px;
  color: var(--text-dim);
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;
  position: relative;
  transition: color 0.2s var(--ease), background 0.2s var(--ease);
}
.nav-link:hover {
  color: var(--text);
  background: rgba(58, 111, 247, 0.08);
}
/* 悬停下划线动画 */
.nav-link::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 4px;
  height: 2px;
  background: var(--primary);
  border-radius: 2px;
  transform: scaleX(0);
  transform-origin: left center;
  transition: transform 0.25s var(--ease);
}
.nav-link:hover::after,
.nav-link.active::after {
  transform: scaleX(1);
}
.nav-link.active {
  color: var(--primary-soft);
  font-weight: 600;
}
.nav-ico {
  font-size: 16px;
  width: 18px;
  text-align: center;
  flex-shrink: 0;
  line-height: 1;
}
.nav-parent {
  gap: 6px;
}
.nav-caret {
  font-size: 12px;
  margin-left: 2px;
  transition: transform 0.2s var(--ease);
}
.nav-item.is-open .nav-caret {
  transform: rotate(180deg);
}

/* 下拉面板：始终渲染，.is-open 控制显隐，纯 CSS 过渡，无 popper 重排、无闪烁 */
.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 210px;
  list-style: none;
  background: var(--bg-base-2);
  border: 1px solid var(--border-strong);
  border-radius: 12px;
  box-shadow: var(--shadow-2);
  padding: 6px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(8px);
  pointer-events: none;
  transition: opacity 0.2s var(--ease), transform 0.2s var(--ease), visibility 0.2s var(--ease);
  /* 提升到导航局部上下文最高：避免被 el-slider / el-tooltip 等高 z-index 组件穿透 */
  z-index: 2100;
}
.nav-item.is-open .dropdown {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
  pointer-events: auto;
}
.dropdown-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  color: var(--text-dim);
  font-size: 13px;
  text-decoration: none;
  transition: color 0.18s var(--ease), background 0.18s var(--ease);
}
.dropdown-link:hover {
  color: var(--text);
  background: rgba(58, 111, 247, 0.10);
}
.dropdown-link.active {
  color: var(--primary-soft);
  background: rgba(58, 111, 247, 0.14);
  font-weight: 600;
}

/* ===================== 用户信息 ===================== */
.nav-user {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.admin-name {
  color: var(--text);
  font-size: 13px;
  padding: 5px 14px;
  border-radius: 999px;
  background: rgba(58, 111, 247, 0.10);
  border: 1px solid rgba(58, 111, 247, 0.22);
  white-space: nowrap;
}
.logout-btn {
  border: 1px solid var(--border-strong);
  background: var(--surface-2);
  color: var(--text-dim);
  font-size: 13px;
  padding: 5px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.2s var(--ease), border-color 0.2s var(--ease), background 0.2s var(--ease);
}
.logout-btn:hover {
  color: var(--primary-soft);
  border-color: var(--primary);
  background: rgba(58, 111, 247, 0.08);
}

/* ===================== 汉堡按钮（移动端） ===================== */
.hamburger {
  display: none;
  width: 38px;
  height: 38px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 8px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s var(--ease);
}
.hamburger:hover {
  background: rgba(58, 111, 247, 0.10);
}
.hamburger:focus-visible {
  outline: 2px solid var(--primary-soft);
  outline-offset: 2px;
}
.hamburger-box {
  position: relative;
  width: 20px;
  height: 14px;
  display: inline-block;
}
.hamburger-box i {
  position: absolute;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--text);
  border-radius: 2px;
  transition: transform 0.28s var(--ease), opacity 0.2s var(--ease), top 0.28s var(--ease);
}
.hamburger-box i:nth-child(1) { top: 0; }
.hamburger-box i:nth-child(2) { top: 6px; }
.hamburger-box i:nth-child(3) { top: 12px; }
.hamburger-box.open i:nth-child(1) { top: 6px; transform: rotate(45deg); }
.hamburger-box.open i:nth-child(2) { opacity: 0; }
.hamburger-box.open i:nth-child(3) { top: 6px; transform: rotate(-45deg); }

/* ===================== 移动端抽屉 ===================== */
.mobile-drawer {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 280px;
  max-width: 82vw;
  background: var(--bg-base-2);
  border-right: 1px solid var(--border-strong);
  box-shadow: var(--shadow-2);
  transform: translateX(-100%);
  visibility: hidden;
  pointer-events: none;
  transition: transform 0.32s var(--ease), visibility 0.32s var(--ease);
  z-index: 2200; /* 高于 .app-nav(2000)，确保移动端抽屉完全覆盖顶部导航 */
  display: flex;
  flex-direction: column;
}
.mobile-drawer.open {
  transform: translateX(0);
  visibility: visible;
  pointer-events: auto;
}
.drawer-head {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.drawer-nav {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0 20px;
}
.drawer-group {
  margin-bottom: 6px;
}
.drawer-group-label {
  padding: 14px 18px 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1.4px;
  text-transform: uppercase;
  color: var(--text-mute);
}
.drawer-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  color: var(--text-dim);
  font-size: 14px;
  text-decoration: none;
  border-left: 3px solid transparent;
  transition: color 0.18s var(--ease), background 0.18s var(--ease), border-color 0.18s var(--ease);
}
.drawer-link:hover {
  color: var(--text);
  background: rgba(58, 111, 247, 0.08);
}
.drawer-link.active {
  color: var(--primary-soft);
  background: rgba(58, 111, 247, 0.12);
  border-left-color: var(--primary);
  font-weight: 600;
}

.nav-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(6, 9, 14, 0.55);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  z-index: 2100; /* 在 nav(2000) 之上、drawer(2200) 之下 */
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.3s var(--ease), visibility 0.3s var(--ease);
}
.nav-backdrop.show {
  opacity: 1;
  visibility: visible;
}

/* ===================== 主内容区 ===================== */
.app-main {
  flex: 1;
  background-color: var(--bg-base);
  background-image:
    radial-gradient(1100px 600px at 0% 0%, rgba(58, 111, 247, 0.06), transparent 55%),
    radial-gradient(900px 500px at 100% 0%, rgba(245, 165, 36, 0.03), transparent 55%);
  min-height: calc(100vh - 60px);
  padding: 24px;
  color: var(--text);
}
.app-main:focus {
  outline: none;
}
.app-main-fullscreen {
  min-height: 100vh;
  padding: 0;
  background-image: none;
}

/* ===================== 响应式 ===================== */
@media (max-width: 1024px) {
  .nav-menu { display: none; }
  .hamburger { display: inline-flex; }
  .nav-user .admin-name { display: none; }
}
@media (min-width: 1025px) {
  .mobile-drawer,
  .nav-backdrop { display: none; }
}
@media (max-width: 640px) {
  .nav-inner { padding: 0 14px; }
  .brand-name { display: none; }
  .app-main { padding: 14px; }
}
</style>
