<template>
  <div class="big-screen">
    <!-- 顶部标题栏 -->
    <div class="screen-header">
      <div class="header-left">
        <span class="header-time">{{ currentTime }}</span>
      </div>
      <div class="header-center">
        <div class="title-line">
          <span class="title-deco left"></span>
          <h1 class="screen-title">东盟企业风控预警数据大屏</h1>
          <span class="title-deco right"></span>
        </div>
        <p class="screen-subtitle">ASEAN Enterprise Risk Warning Platform</p>
        <el-tag v-if="isDemoData" size="mini" type="warning">当前含演示样例数据</el-tag>
      </div>
      <div class="header-right">
        <span class="live-badge"><i class="el-icon-video-play"></i> 实时监控中</span>
        <button class="screen-mode-btn" @click="toggleFullscreen">
          <i :class="isFullscreen ? 'el-icon-close' : 'el-icon-full-screen'"></i>
          {{ isFullscreen ? '退出全屏' : '进入全屏' }}
        </button>
      </div>
    </div>

    <!-- 主体三列布局 -->
    <div class="screen-body">
      <!-- 左侧列 -->
      <div class="col-left">
        <!-- 统计卡片 -->
        <div class="panel stat-panel">
          <div class="stat-grid">
            <div class="stat-item" v-for="item in statCards" :key="item.title">
              <div class="stat-icon-wrap" :style="{ background: item.bg }">
                <i :class="item.icon"></i>
              </div>
              <div class="stat-info">
                <p class="stat-val" :style="{ color: item.color }">{{ item.value }}</p>
                <p class="stat-label">{{ item.title }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 风险等级分布 -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title"><i class="el-icon-pie-chart"></i> 风险等级分布</span>
          </div>
          <div class="panel-body">
            <div ref="pieChart" class="chart-box"></div>
          </div>
        </div>

        <!-- 行业分布 -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title"><i class="el-icon-s-data"></i> 行业分布 TOP10</span>
          </div>
          <div class="panel-body">
            <div ref="hBarChart" class="chart-box"></div>
          </div>
        </div>
      </div>

      <!-- 中间列（地图C位） -->
      <div class="col-center">
        <div class="panel map-panel">
          <div class="panel-header">
            <span class="panel-title"><i class="el-icon-location-outline"></i> 广西口岸风险企业地理分布</span>
          </div>
          <div class="panel-body map-body">
            <div ref="mapChart" class="chart-box-lg"></div>
          </div>
        </div>

        <!-- 月度趋势 -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title"><i class="el-icon-s-marketing"></i> 月度预警趋势</span>
          </div>
          <div class="panel-body">
            <div ref="lineChart" class="chart-box"></div>
          </div>
        </div>
      </div>

      <!-- 右侧列 -->
      <div class="col-right">
        <!-- 口岸活跃度 -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title"><i class="el-icon-odometer"></i> 口岸贸易活跃度</span>
          </div>
          <div class="panel-body">
            <div ref="barChart" class="chart-box"></div>
          </div>
        </div>

        <!-- 实时预警 LIVE -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title"><i class="el-icon-bell"></i> 实时预警</span>
            <span class="live-tag"><i class="el-icon-video-play"></i> LIVE</span>
          </div>
          <div class="panel-body alert-body">
            <div class="alert-item" v-for="(a, i) in alertFeed" :key="i" :class="'level-' + a.level">
              <span class="alert-dot" :class="'dot-' + a.level"></span>
              <div class="alert-info">
                <span class="alert-time">{{ a.time }}</span>
                <span class="alert-text">{{ a.content }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 高风险企业 -->
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title"><i class="el-icon-warning"></i> 高风险企业</span>
          </div>
          <div class="panel-body risk-body">
            <div class="risk-row risk-header-row">
              <span class="risk-col-name">企业名称</span>
              <span class="risk-col-port">口岸</span>
              <span class="risk-col-score">评分</span>
              <span class="risk-col-type">类型</span>
            </div>
            <div class="risk-row" v-for="(item, i) in highRiskList" :key="i">
              <span class="risk-col-name" :title="item.name">{{ item.name }}</span>
              <span class="risk-col-port">{{ item.port }}</span>
              <span class="risk-col-score" :class="item.riskScore > 85 ? 'danger' : item.riskScore > 75 ? 'warning' : ''">{{ item.riskScore }}</span>
              <span class="risk-col-type">{{ item.riskType }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  getDashboardOverview, getRiskDistribution, getPortActivity,
  getMonthlyTrend, getIndustryRanking, getPortDistribution, getWarningList,
  getSupervisionData
} from '@/api'

export default {
  name: 'Dashboard',
  data() {
    return {
      isFullscreen: false,
      currentTime: '',
      clockTimer: null,
      statCards: [
        { title: '监管企业总数', value: '-', icon: 'el-icon-office-building', color: '#3a6ff7', bg: 'rgba(58,111,247,0.12)' },
        { title: '本月预警', value: '-', icon: 'el-icon-warning', color: '#ff6b6b', bg: 'rgba(255,107,107,0.12)' },
        { title: '高风险企业', value: '-', icon: 'el-icon-circle-close', color: '#ff9f43', bg: 'rgba(255,159,67,0.12)' },
        { title: '口岸覆盖率', value: '-', icon: 'el-icon-circle-check', color: '#2ed573', bg: 'rgba(46,213,115,0.12)' }
      ],
      highRiskList: [],
      alertFeed: [],
      isDemoData: false,
      // chart instances
      mapInstance: null, pieInstance: null, barInstance: null,
      lineInstance: null, hBarInstance: null,
      // mock data
      mockPorts: [
        { portName: '凭祥口岸', longitude: 106.76, latitude: 22.09, count: 18, riskScore: 85 },
        { portName: '东兴口岸', longitude: 107.97, latitude: 21.77, count: 14, riskScore: 78 },
        { portName: '防城港', longitude: 108.35, latitude: 21.77, count: 12, riskScore: 72 },
        { portName: '钦州港', longitude: 108.62, latitude: 21.95, count: 8, riskScore: 55 },
        { portName: '北海港', longitude: 109.12, latitude: 21.49, count: 6, riskScore: 45 },
        { portName: '水口口岸', longitude: 106.73, latitude: 22.38, count: 5, riskScore: 60 },
        { portName: '龙邦口岸', longitude: 106.02, latitude: 22.87, count: 4, riskScore: 50 },
        { portName: '硕龙口岸', longitude: 106.55, latitude: 22.52, count: 3, riskScore: 38 }
      ],
      mockHighRisk: [
        { name: '广西恒信贸易有限公司', port: '凭祥', riskScore: 92, riskType: '虚假贸易' },
        { name: '凭祥顺达进出口', port: '凭祥', riskScore: 88, riskType: '价格欺诈' },
        { name: '崇左龙邦边贸', port: '龙邦', riskScore: 85, riskType: '虚假注册' },
        { name: '东兴万通贸易行', port: '东兴', riskScore: 82, riskType: '资金异常' },
        { name: '防城港恒达货运', port: '防城', riskScore: 78, riskType: '单据造假' },
        { name: '南宁聚源供应链', port: '钦州', riskScore: 75, riskType: '关联欺诈' },
        { name: '北海银海贸易', port: '北海', riskScore: 72, riskType: '逃税嫌疑' }
      ],
      mockAlerts: [
        { time: '14:32:15', level: 'high', content: '凭祥口岸：广西恒信贸易新增异常报关单3笔' },
        { time: '14:28:03', level: 'high', content: '东兴口岸：检测到疑似循环贸易资金流向' },
        { time: '14:15:47', level: 'medium', content: '防城港：恒达货运提单与物流数据不匹配' },
        { time: '14:02:31', level: 'medium', content: '龙邦口岸：新增高风险关联企业2家' },
        { time: '13:55:18', level: 'low', content: '钦州港：聚源供应链月度交易量异常增长' },
        { time: '13:42:06', level: 'high', content: '凭祥口岸：顺达进出口申报价格低于市价60%' },
        { time: '13:30:22', level: 'medium', content: '北海港：银海贸易连续3月零申报但物流活跃' },
        { time: '13:18:45', level: 'low', content: '水口口岸：新增边贸企业信用评分偏低' },
        { time: '13:05:33', level: 'medium', content: '东兴口岸：万通贸易行跨境资金回流异常' },
        { time: '12:52:11', level: 'low', content: '硕龙口岸：进口商品归类存在争议' }
      ]
    }
  },
  mounted() {
    this.syncFullscreen()
    this.updateClock()
    this.clockTimer = setInterval(this.updateClock, 1000)
    this.loadAll()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    clearInterval(this.clockTimer)
    window.removeEventListener('resize', this.handleResize)
    const instances = [this.mapInstance, this.pieInstance, this.barInstance, this.lineInstance, this.hBarInstance]
    instances.forEach(i => { if (i) i.dispose() })
  },
  methods: {
    syncFullscreen() {
      const app = this.$root.$children[0]
      if (!app) return
      this.isFullscreen = app.isFullscreen
      this.$watch(() => app.isFullscreen, (val) => {
        this.isFullscreen = val
      })
    },
    toggleFullscreen() {
      const app = this.$root.$children[0]
      if (app && typeof app.toggleFullscreen === 'function') {
        app.toggleFullscreen()
      }
    },
    updateClock() {
      const now = new Date()
      const y = now.getFullYear()
      const m = String(now.getMonth() + 1).padStart(2, '0')
      const d = String(now.getDate()).padStart(2, '0')
      const hh = String(now.getHours()).padStart(2, '0')
      const mm = String(now.getMinutes()).padStart(2, '0')
      const ss = String(now.getSeconds()).padStart(2, '0')
      const week = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]
      this.currentTime = `${y}-${m}-${d} 星期${week} ${hh}:${mm}:${ss}`
    },
    handleResize() {
      const instances = [this.mapInstance, this.pieInstance, this.barInstance, this.lineInstance, this.hBarInstance]
      instances.forEach(i => { if (i) i.resize() })
    },
    async loadAll() {
      // 并行加载地图注册
      try {
        const geoRes = await fetch('/guangxi.json')
        const geoJson = await geoRes.json()
        this.$echarts.registerMap('guangxi', geoJson)
      } catch (e) { /* ignore */ }

      // 加载数据
      const results = await Promise.allSettled([
        getDashboardOverview(),
        getRiskDistribution(),
        getPortActivity(),
        getMonthlyTrend(),
        getIndustryRanking(),
        getPortDistribution(),
        getWarningList({ page: 1, pageSize: 10 }),
        getSupervisionData()
      ])

      const [overview, riskDist, portAct, trend, industry, portDist, warnings, supervision] = results
      this.isDemoData = results.some(r => r.status !== 'fulfilled' || !r.value || !r.value.data)

      // 概览数据
      if (overview.status === 'fulfilled' && overview.value.data) {
        const d = overview.value.data
        this.statCards[0].value = this.fmt(d.serviceCount)
        this.statCards[1].value = this.fmt(d.warningCount)
        this.statCards[2].value = this.fmt(d.highRiskCount || 0)
        this.statCards[3].value = d.highRiskRatio || d.portCoverage || '-'
      }

      // 监管大屏数据（补充）
      if (supervision.status === 'fulfilled' && supervision.value.data) {
        const s = supervision.value.data
        if (s.totalEnterprises) this.statCards[0].value = this.fmt(s.totalEnterprises)
        if (s.todayAlerts) this.statCards[1].value = this.fmt(s.todayAlerts)
        if (s.highRiskCount) this.statCards[2].value = this.fmt(s.highRiskCount)
        if (s.portCoverage) this.statCards[3].value = s.portCoverage
        this.highRiskList = s.highRiskList || this.mockHighRisk
        this.alertFeed = s.alerts || this.mockAlerts
      } else {
        this.highRiskList = this.mockHighRisk
        this.alertFeed = this.mockAlerts
      }

      // 各图表
      if (riskDist.status === 'fulfilled' && riskDist.value.data) {
        const rd = riskDist.value.data
        this.initPie(rd.high || 0, rd.medium || 0, rd.low || 0)
      } else this.initPie(0, 0, 0)

      if (portAct.status === 'fulfilled' && portAct.value.data) {
        const pa = portAct.value.data || []
        this.initBar(pa.map(i => (i.portName || '').replace('口岸', '').replace('市', '')), pa.map(i => i.count || 0))
      } else this.initBar([], [])

      if (trend.status === 'fulfilled' && trend.value.data) {
        const td = trend.value.data || []
        if (td.length) {
          this.initLine(td.map(i => (i.month || '').split('-')[1] + '月'), td.map(i => i.count || 0))
        } else this.initLine([], [])
      } else this.initLine([], [])

      if (industry.status === 'fulfilled' && industry.value.data) {
        const id = industry.value.data || []
        this.initHBar(id.map(i => i.industry || ''), id.map(i => i.count || 0))
      } else this.initHBar([], [])

      if (portDist.status === 'fulfilled' && portDist.value.data) {
        this.initMap(portDist.value.data || [])
      } else this.initMap(this.mockPorts)

      // 预警表格数据
      if (warnings.status === 'fulfilled' && warnings.value.data) {
        // 预警数据用于 alert feed
        const wl = warnings.value.data?.list || []
        if (wl.length > 0 && !this.alertFeed.length) {
          this.alertFeed = wl.map(w => ({
            time: (w.createTime || '').slice(-8) || '--:--:--',
            level: w.warningLevel === '高风险' || w.warningLevel === 'high' ? 'high'
              : w.warningLevel === '中风险' || w.warningLevel === 'medium' ? 'medium' : 'low',
            content: `${w.enterpriseName || ''}：${w.warningType || ''}`
          }))
        }
      }
    },
    fmt(n) {
      if (n == null || n === '') return '-'
      if (n >= 10000) return (n / 10000).toFixed(1) + '万'
      return String(n)
    },

    // ==================== 图表 ====================

    initMap(data) {
      if (!this.$refs.mapChart) return
      if (this.mapInstance) this.mapInstance.dispose()
      this.mapInstance = this.$echarts.init(this.$refs.mapChart)

      const list = data || []
      const getColor = c => c > 10 ? '#ff6b6b' : c > 5 ? '#ff9f43' : c > 0 ? '#3a6ff7' : '#5a6a80'
      const scatterData = list.map(p => ({
        name: (p.portName || '').replace('口岸', ''),
        value: [p.longitude || 0, p.latitude || 0, p.count || 0],
        itemStyle: { color: getColor(p.count || 0) }
      }))

      this.mapInstance.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(6,30,60,0.92)',
          borderColor: 'rgba(58,111,247,0.3)',
          textStyle: { color: '#e0f0ff', fontSize: 12 },
          formatter(p) {
            if (p.seriesType === 'effectScatter') {
              const c = p.value[2]
              const lvl = c > 10 ? '高风险' : c > 5 ? '中风险' : '低风险'
              const clr = c > 10 ? '#ff6b6b' : c > 5 ? '#ff9f43' : '#3a6ff7'
              return `<div style="font-size:13px;font-weight:bold;margin-bottom:4px">${p.name}口岸</div>
                <div>风险企业: <span style="color:${clr};font-weight:bold">${c}</span> 家</div>
                <div>等级: <span style="color:${clr}">${lvl}</span></div>`
            }
          }
        },
        geo: {
          map: 'guangxi',
          roam: true,
          zoom: 1.15,
          center: [108.3, 22.8],
          label: { show: true, color: 'rgba(255,255,255,0.4)', fontSize: 9 },
          itemStyle: {
            areaColor: 'rgba(58,111,247,0.04)',
            borderColor: 'rgba(58,111,247,0.2)',
            borderWidth: 1,
            shadowColor: 'rgba(58,111,247,0.1)',
            shadowBlur: 20
          },
          emphasis: {
            itemStyle: { areaColor: 'rgba(58,111,247,0.1)', borderColor: '#3a6ff7' }
          }
        },
        series: [{
          type: 'effectScatter',
          coordinateSystem: 'geo',
          data: scatterData,
          symbolSize: v => Math.max(8, Math.min(24, 6 + v[2] * 1.3)),
          showEffectOn: 'render',
          rippleEffect: { brushType: 'stroke', scale: 3, period: 4, color: '#3a6ff7' },
          label: {
            show: true, formatter: '{b}', position: 'right',
            color: '#c8e6ff', fontSize: 10, fontWeight: 'bold',
            textShadowColor: 'rgba(0,0,0,0.6)', textShadowBlur: 2
          },
          level: 2
        }]
      }, true)
    },

    initPie(high, medium, low) {
      if (!this.$refs.pieChart) return
      if (this.pieInstance) this.pieInstance.dispose()
      this.pieInstance = this.$echarts.init(this.$refs.pieChart)
      const total = high + medium + low || 1
      this.pieInstance.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(6,30,60,0.9)',
          borderColor: 'rgba(58,111,247,0.2)',
          textStyle: { color: '#e0f0ff', fontSize: 11 },
          formatter: '{b}: {c} 家 ({d}%)'
        },
        series: [{
          type: 'pie',
          radius: ['55%', '78%'],
          center: ['50%', '48%'],
          itemStyle: { borderColor: 'rgba(6,24,48,1)', borderWidth: 3 },
          label: {
            color: '#aac8e0', fontSize: 11,
            formatter: '{b}\n{d}%'
          },
          emphasis: {
            label: { fontSize: 14, fontWeight: 'bold' },
            scaleSize: 10
          },
          data: [
            { value: low, name: '低风险', itemStyle: { color: '#2ed573' } },
            { value: medium, name: '中风险', itemStyle: { color: '#ff9f43' } },
            { value: high, name: '高风险', itemStyle: { color: '#ff6b6b' } }
          ]
        }]
      }, true)
    },

    initBar(labels, values) {
      if (!this.$refs.barChart) return
      if (this.barInstance) this.barInstance.dispose()
      this.barInstance = this.$echarts.init(this.$refs.barChart)
      const max = Math.max(...values, 1)
      this.barInstance.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(6,30,60,0.9)',
          borderColor: 'rgba(58,111,247,0.2)',
          textStyle: { color: '#e0f0ff', fontSize: 11 }
        },
        grid: { top: 24, right: 16, bottom: labels.length > 5 ? 40 : 28, left: 44 },
        xAxis: {
          type: 'category', data: labels,
          axisLabel: {
            color: '#aac8e0', fontSize: 11,
            interval: 0,
            rotate: labels.length > 5 ? 20 : 0,
            formatter: v => v.length > 4 ? v.slice(0, 3) + '…' : v
          },
          axisLine: { lineStyle: { color: 'rgba(58,111,247,0.15)' } },
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#5a6a80', fontSize: 9 },
          splitLine: { lineStyle: { color: 'rgba(58,111,247,0.06)', type: 'dashed' } }
        },
        series: [{
          type: 'bar', data: values, barWidth: '55%',
          label: {
            show: true, position: 'top',
            color: '#e0f0ff', fontSize: 10,
            formatter: '{c}'
          },
          itemStyle: {
            borderRadius: [2, 2, 0, 0],
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: '#3a6ff7' },
                { offset: 1, color: 'rgba(58,111,247,0.15)' }
              ]
            }
          }
        }]
      }, true)
    },

    initLine(months, values) {
      if (!this.$refs.lineChart) return
      if (this.lineInstance) this.lineInstance.dispose()
      this.lineInstance = this.$echarts.init(this.$refs.lineChart)
      this.lineInstance.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(6,30,60,0.9)',
          borderColor: 'rgba(58,111,247,0.2)',
          textStyle: { color: '#e0f0ff', fontSize: 11 }
        },
        grid: { top: 16, right: 16, bottom: 20, left: 44 },
        xAxis: {
          type: 'category', boundaryGap: false, data: months,
          axisLabel: { color: '#aac8e0', fontSize: 10 },
          axisLine: { lineStyle: { color: 'rgba(58,111,247,0.15)' } }
        },
        yAxis: {
          type: 'value', name: '条',
          nameTextStyle: { color: '#5a6a80', fontSize: 10 },
          axisLabel: { color: '#5a6a80', fontSize: 9 },
          splitLine: { lineStyle: { color: 'rgba(58,111,247,0.06)', type: 'dashed' } }
        },
        series: [{
          type: 'line', data: values, smooth: true, symbol: 'circle', symbolSize: 6,
          lineStyle: { color: '#3a6ff7', width: 2 },
          itemStyle: { color: '#3a6ff7' },
          areaStyle: {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(58,111,247,0.25)' },
                { offset: 1, color: 'rgba(58,111,247,0)' }
              ]
            }
          }
        }]
      }, true)
    },

    initHBar(industries, values) {
      if (!this.$refs.hBarChart) return
      if (this.hBarInstance) this.hBarInstance.dispose()
      this.hBarInstance = this.$echarts.init(this.$refs.hBarChart)
      const colors = values.map((_, i) => i < 3 ? '#ff6b6b' : i < 6 ? '#ff9f43' : '#2ed573')
      const maxVal = Math.max(...values, 1)
      this.hBarInstance.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(6,30,60,0.9)',
          borderColor: 'rgba(58,111,247,0.2)',
          textStyle: { color: '#e0f0ff', fontSize: 11 }
        },
        grid: { top: 4, right: 36, bottom: 12, left: 88 },
        xAxis: {
          type: 'value',
          axisLabel: { color: '#5a6a80', fontSize: 9 },
          splitLine: { lineStyle: { color: 'rgba(58,111,247,0.06)', type: 'dashed' } }
        },
        yAxis: {
          type: 'category', data: industries, inverse: true,
          axisLabel: { color: '#aac8e0', fontSize: 9 },
          axisLine: { show: false }, axisTick: { show: false }
        },
        series: [{
          type: 'bar', data: values.map((v, i) => ({ value: v, itemStyle: { color: colors[i] } })),
          barWidth: '55%',
          itemStyle: { borderRadius: [0, 3, 3, 0] },
          label: { show: true, position: 'right', color: '#aac8e0', fontSize: 9 }
        }]
      }, true)
    }
  }
}
</script>

<style scoped>
/* ========== 大屏基础 ========== */
.big-screen {
  width: 100%;
  height: 100%;
  min-height: calc(100vh - 60px);
  background: radial-gradient(ellipse at 50% 0%, #0b1a30 0%, #060e1c 60%, #030912 100%);
  color: #e0f0ff;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ========== 顶部标题 ========== */
.screen-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: linear-gradient(180deg, rgba(58,111,247,0.06) 0%, rgba(0,0,0,0) 100%);
  border-bottom: 1px solid rgba(58,111,247,0.08);
  flex-shrink: 0;
}

.header-left {
  width: 260px;
  text-align: left;
}

.header-time {
  font-size: 13px;
  color: #5a7a9a;
  font-family: 'Courier New', monospace;
  letter-spacing: 1px;
}

.header-center {
  text-align: center;
}

.title-line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.title-deco {
  display: block;
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(58,111,247,0.5));
}

.title-deco.left {
  background: linear-gradient(90deg, transparent, rgba(58,111,247,0.5));
}

.title-deco.right {
  background: linear-gradient(90deg, rgba(58,111,247,0.5), transparent);
}

.screen-title {
  font-size: 22px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 4px;
  text-shadow: 0 0 30px rgba(58,111,247,0.3);
  margin: 0;
  white-space: nowrap;
}

.screen-subtitle {
  font-size: 10px;
  color: rgba(58,111,247,0.45);
  letter-spacing: 2px;
  margin-top: 2px;
}

.header-right {
  width: 320px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.live-badge {
  font-size: 12px;
  color: #2ed573;
  background: rgba(46,213,115,0.1);
  border: 1px solid rgba(46,213,115,0.2);
  padding: 4px 12px;
  border-radius: 12px;
  letter-spacing: 1px;
  white-space: nowrap;
}

.screen-mode-btn {
  background: rgba(58,111,247,0.1);
  border: 1px solid rgba(58,111,247,0.3);
  color: #3a6ff7;
  padding: 5px 14px;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.25s;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.screen-mode-btn:hover {
  background: rgba(58,111,247,0.2);
  border-color: rgba(58,111,247,0.5);
  box-shadow: 0 0 12px rgba(58,111,247,0.2);
}

.screen-mode-btn i {
  font-size: 13px;
}

/* ========== 三列主体 ========== */
.screen-body {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 10px 16px 12px;
  overflow: hidden;
}

.col-left, .col-right {
  width: 300px;
  min-width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.col-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

/* ========== 面板 ========== */
.panel {
  background: rgba(0,20,45,0.5);
  border: 1px solid rgba(58,111,247,0.08);
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  background: rgba(58,111,247,0.04);
  border-bottom: 1px solid rgba(58,111,247,0.06);
  flex-shrink: 0;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #c8e6ff;
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-title i {
  color: #3a6ff7;
  font-size: 14px;
}

.panel-body {
  flex: 1;
  padding: 8px;
  overflow: hidden;
}

.chart-box {
  width: 100%;
  height: 200px;
}

.chart-box-lg {
  width: 100%;
  height: 100%;
}

/* ========== 统计面板 ========== */
.stat-panel {
  flex-shrink: 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(58,111,247,0.03);
  border: 1px solid rgba(58,111,247,0.06);
  border-radius: 4px;
  transition: all 0.3s;
}

.stat-item:hover {
  border-color: rgba(58,111,247,0.2);
  background: rgba(58,111,247,0.05);
}

.stat-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-info {
  overflow: hidden;
}

.stat-val {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-label {
  font-size: 11px;
  color: #5a7a9a;
  margin-top: 2px;
}

/* ========== 地图面板 ========== */
.map-panel {
  flex: 1;
  min-height: 0;
}

.map-body {
  padding: 4px;
}

/* ========== 预警 Feed ========== */
.live-tag {
  font-size: 11px;
  color: #ff6b6b;
  background: rgba(255,107,107,0.12);
  border: 1px solid rgba(255,107,107,0.2);
  padding: 2px 8px;
  border-radius: 10px;
  letter-spacing: 1px;
  animation: pulse 2s infinite;
}

.alert-body {
  padding: 6px 10px;
  overflow-y: auto;
  max-height: 220px;
}

.alert-body::-webkit-scrollbar {
  width: 3px;
}

.alert-body::-webkit-scrollbar-thumb {
  background: rgba(58,111,247,0.15);
  border-radius: 2px;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 8px;
  margin-bottom: 4px;
  border-radius: 3px;
  background: rgba(58,111,247,0.02);
  border-left: 2px solid transparent;
  transition: all 0.2s;
}

.alert-item:hover {
  background: rgba(58,111,247,0.05);
}

.alert-item.level-high {
  border-left-color: #ff6b6b;
}

.alert-item.level-medium {
  border-left-color: #ff9f43;
}

.alert-item.level-low {
  border-left-color: #ffd93d;
}

.alert-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.dot-high { background: #ff6b6b; box-shadow: 0 0 6px #ff6b6b; }
.dot-medium { background: #ff9f43; box-shadow: 0 0 6px #ff9f43; }
.dot-low { background: #ffd93d; box-shadow: 0 0 6px #ffd93d; }

.alert-info {
  flex: 1;
  min-width: 0;
}

.alert-time {
  font-size: 10px;
  color: #4a6a8a;
  margin-right: 6px;
  font-family: 'Courier New', monospace;
}

.alert-text {
  font-size: 11px;
  color: #aac8e0;
  word-break: break-all;
}

/* ========== 高风险企业列表 ========== */
.risk-body {
  padding: 4px 8px;
  overflow-y: auto;
  max-height: 200px;
}

.risk-body::-webkit-scrollbar {
  width: 3px;
}

.risk-body::-webkit-scrollbar-thumb {
  background: rgba(58,111,247,0.15);
  border-radius: 2px;
}

.risk-row {
  display: flex;
  align-items: center;
  padding: 5px 6px;
  font-size: 11px;
  border-bottom: 1px solid rgba(58,111,247,0.04);
  transition: background 0.2s;
}

.risk-row:hover {
  background: rgba(58,111,247,0.04);
}

.risk-header-row {
  color: #4a6a8a;
  font-size: 10px;
  font-weight: 600;
  border-bottom-color: rgba(58,111,247,0.1);
}

.risk-col-name {
  flex: 1;
  color: #aac8e0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 4px;
}

.risk-col-port {
  width: 44px;
  text-align: center;
  color: #5a7a9a;
}

.risk-col-score {
  width: 36px;
  text-align: center;
  color: #ff6b6b;
  font-weight: 700;
  font-family: 'Courier New', monospace;
}

.risk-col-score.warning {
  color: #ff9f43;
}

.risk-col-type {
  width: 56px;
  text-align: right;
  color: #5a7a9a;
  font-size: 10px;
}

.risk-header-row .risk-col-score {
  color: #4a6a8a;
  font-weight: 600;
}
</style>
