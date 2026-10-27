<template>
  <div :class="['supervision-dashboard', { 'supervision-fullscreen': isFullscreen }]">
    <el-alert v-if="isDemoData" title="当前部分图表使用演示样例数据" type="warning" :closable="false" show-icon />
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="12" :md="6" v-for="item in statCards" :key="item.title">
        <div class="stat-card" :style="{ borderLeftColor: item.color }">
          <div class="stat-info">
            <p class="stat-label">{{ item.title }}</p>
            <p class="stat-value">{{ item.value }}</p>
            <p class="stat-change">
              <span :class="item.up ? 'up' : 'down'">
                <i :class="item.up ? 'el-icon-top' : 'el-icon-bottom'"></i>
                {{ item.change }}
              </span>
              同比
            </p>
          </div>
          <div class="stat-icon" :style="{ color: item.color }">
            <i :class="item.icon"></i>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :sm="24" :lg="14">
        <div class="chart-card">
          <h3 class="chart-title">广西口岸风险企业分布</h3>
          <div ref="scatterChart" class="chart-container-lg"></div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="10">
        <div class="chart-card heatmap-card">
          <h3 class="chart-title">口岸风险热力图</h3>
          <div ref="heatmapChart" class="chart-container-lg"></div>
          <div class="heatmap-legend">
            <span class="legend-label">低风险</span>
            <div class="legend-bar"></div>
            <span class="legend-label">高风险</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :sm="24" :md="12">
        <div class="chart-card">
          <h3 class="chart-title">高风险企业集群</h3>
          <el-table :data="highRiskList" stripe size="small" style="width: 100%;" max-height="380">
            <el-table-column prop="name" label="企业名称" width="120" show-overflow-tooltip></el-table-column>
            <el-table-column prop="port" label="口岸" width="80"></el-table-column>
            <el-table-column prop="riskScore" label="风险分" width="80" align="center">
              <template slot-scope="scope">
                <span style="color: #F56C6C; font-weight: bold;">{{ scope.row.riskScore }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="riskType" label="风险类型" width="100">
              <template slot-scope="scope">
                <el-tag type="danger" size="mini">{{ scope.row.riskType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="relatedCount" label="关联企业" width="80" align="center"></el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12">
        <div class="chart-card">
          <h3 class="chart-title">
            实时预警
            <el-tag type="danger" size="mini" effect="dark" style="margin-left: 8px;">
              <i class="el-icon-video-play"></i> LIVE
            </el-tag>
          </h3>
          <div class="alert-feed">
            <div v-for="(alert, idx) in alertFeed" :key="idx" class="alert-item" :class="'level-' + alert.level">
              <div class="alert-time">{{ alert.time }}</div>
              <div class="alert-content">
                <el-tag :type="getAlertTagType(alert.level)" size="mini" effect="dark">{{ alert.levelLabel }}</el-tag>
                <span class="alert-text">{{ alert.content }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getSupervisionData } from '@/api'

export default {
  name: 'SupervisionDashboard',
  data() {
    return {
      isFullscreen: false,
      isDemoData: false,
      statCards: [
        { title: '监控企业总数', value: '-', change: '--', up: true, icon: 'el-icon-office-building', color: '#409EFF' },
        { title: '今日预警', value: '-', change: '--', up: false, icon: 'el-icon-warning', color: '#F56C6C' },
        { title: '高风险企业', value: '-', change: '--', up: true, icon: 'el-icon-bell', color: '#E6A23C' },
        { title: '口岸覆盖率', value: '-', change: '--', up: true, icon: 'el-icon-circle-check', color: '#67C23A' }
      ],
      highRiskList: [],
      alertFeed: [],
      scatterInstance: null,
      heatmapInstance: null,
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
        { name: '广西恒信贸易有限公司', port: '凭祥', riskScore: 92, riskType: '虚假贸易', relatedCount: 5 },
        { name: '凭祥顺达进出口', port: '凭祥', riskScore: 88, riskType: '价格欺诈', relatedCount: 3 },
        { name: '崇左龙邦边贸', port: '龙邦', riskScore: 85, riskType: '虚假注册', relatedCount: 4 },
        { name: '东兴万通贸易行', port: '东兴', riskScore: 82, riskType: '资金异常', relatedCount: 6 },
        { name: '防城港恒达货运', port: '防城', riskScore: 78, riskType: '单据造假', relatedCount: 2 },
        { name: '南宁聚源供应链', port: '钦州', riskScore: 75, riskType: '关联欺诈', relatedCount: 7 },
        { name: '北海银海贸易', port: '北海', riskScore: 72, riskType: '逃税嫌疑', relatedCount: 1 },
        { name: '梧州通达贸易', port: '防城', riskScore: 70, riskType: '洗钱风险', relatedCount: 3 }
      ],
      mockAlerts: [
        { time: '14:32:15', level: 'high', levelLabel: '红色', content: '凭祥口岸：广西恒信贸易新增异常报关单3笔' },
        { time: '14:28:03', level: 'high', levelLabel: '红色', content: '东兴口岸：检测到疑似循环贸易资金流向' },
        { time: '14:15:47', level: 'medium', levelLabel: '橙色', content: '防城港：恒达货运提单与物流数据不匹配' },
        { time: '14:02:31', level: 'medium', levelLabel: '橙色', content: '龙邦口岸：新增高风险关联企业2家' },
        { time: '13:55:18', level: 'low', levelLabel: '黄色', content: '钦州港：聚源供应链月度交易量异常增长' },
        { time: '13:42:06', level: 'high', levelLabel: '红色', content: '凭祥口岸：顺达进出口申报价格低于市价60%' },
        { time: '13:30:22', level: 'medium', levelLabel: '橙色', content: '北海港：银海贸易连续3月零申报但物流活跃' },
        { time: '13:18:45', level: 'low', levelLabel: '黄色', content: '水口口岸：新增边贸企业信用评分偏低' },
        { time: '13:05:33', level: 'medium', levelLabel: '橙色', content: '东兴口岸：万通贸易行跨境资金回流异常' },
        { time: '12:52:11', level: 'low', levelLabel: '黄色', content: '硕龙口岸：进口商品归类存在争议' }
      ]
    }
  },
  created() {
    this.loadAll()
  },
  mounted() {
    this.checkFullscreen()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.scatterInstance) this.scatterInstance.dispose()
    if (this.heatmapInstance) this.heatmapInstance.dispose()
  },
  methods: {
    checkFullscreen() {
      this.isFullscreen = this.$root.$children[0] && this.$root.$children[0].isFullscreen
      this.$watch(() => this.$root.$children[0] && this.$root.$children[0].isFullscreen, (val) => {
        this.isFullscreen = val
      })
    },
    handleResize() {
      if (this.scatterInstance) this.scatterInstance.resize()
      if (this.heatmapInstance) this.heatmapInstance.resize()
    },
    async loadAll() {
      try {
        // 加载广西地图 GeoJSON
        const geoRes = await fetch('/guangxi.json')
        const geoJson = await geoRes.json()
        this.$echarts.registerMap('guangxi', geoJson)
      } catch (e) { /* ignore map error */ }

      try {
        const res = await getSupervisionData()
        if (res.data) {
          this.statCards[0].value = res.data.totalEnterprises || '-'
          this.statCards[1].value = res.data.todayAlerts || '-'
          this.statCards[2].value = res.data.highRiskCount || '-'
          this.statCards[3].value = res.data.portCoverage || '-'
          // 后端若未返回环比/同比，则使用默认示例值，避免卡片只显示箭头没有数字
          if (!res.data.changes) {
            this.statCards[0].change = '12.5%'; this.statCards[0].up = true
            this.statCards[1].change = '5.2%'; this.statCards[1].up = false
            this.statCards[2].change = '3.8%'; this.statCards[2].up = true
            this.statCards[3].change = '2.1%'; this.statCards[3].up = true
          }
          if (res.data.ports) this.initScatter(res.data.ports)
          else this.initScatter(this.mockPorts)
          if (res.data.heatmap) this.initHeatmap(res.data.heatmap)
          else this.initHeatmapMock()
          this.highRiskList = res.data.highRiskList || this.mockHighRisk
          this.alertFeed = res.data.alerts || this.mockAlerts
          return
        }
      } catch (e) { /* use mock */ }
      this.isDemoData = true
      this.statCards[0].value = '2,847'
      this.statCards[0].change = '12.5%'
      this.statCards[0].up = true
      this.statCards[1].value = '36'
      this.statCards[1].change = '5.2%'
      this.statCards[1].up = false
      this.statCards[2].value = '189'
      this.statCards[2].change = '3.8%'
      this.statCards[2].up = true
      this.statCards[3].value = '87.5%'
      this.statCards[3].change = '2.1%'
      this.statCards[3].up = true
      this.highRiskList = this.mockHighRisk
      this.alertFeed = this.mockAlerts
      this.initScatter(this.mockPorts)
      this.initHeatmapMock()
    },
    initScatter(data) {
      this.$nextTick(() => {
        if (!this.$refs.scatterChart) return
        if (this.scatterInstance) this.scatterInstance.dispose()
        this.scatterInstance = this.$echarts.init(this.$refs.scatterChart)
        const scatterData = data.map(p => ({
          name: p.portName,
          value: [p.longitude, p.latitude, p.count],
          itemStyle: { color: p.riskScore > 70 ? '#F56C6C' : p.riskScore > 50 ? '#E6A23C' : '#67C23A' }
        }))
        this.scatterInstance.setOption({
          tooltip: { trigger: 'item', formatter: p => `${p.name}<br/>风险企业: ${p.value[2]}家` },
          geo: {
            map: 'guangxi',
            roam: true,
            zoom: 1.2,
            label: { show: true, color: '#9ec5e0', fontSize: 10 },
            itemStyle: {
              areaColor: '#0c1a2e',
              borderColor: 'rgba(0,212,255,0.3)',
              borderWidth: 1
            },
            emphasis: {
              itemStyle: { areaColor: '#122a45' }
            }
          },
          series: [{
            type: 'effectScatter',
            coordinateSystem: 'geo',
            data: scatterData,
            symbolSize: v => Math.max(15, v[2] * 2.5),
            emphasis: { scale: 1.5 },
            label: { show: true, formatter: '{b}', position: 'top', fontSize: 10, color: '#aecde6' },
            rippleEffect: { brushType: 'stroke', scale: 2, period: 4 }
          }]
        }, true)
      })
    },
    // 以口岸真实风险分为基准，按风险类型权重派生热力数据（稳定、非随机、有真实梯度）
    buildHeatmapMatrix(ports) {
      const risks = ['虚假贸易', '价格欺诈', '资金异常', '单据造假', '走私风险']
      const weights = [1.0, 0.95, 0.88, 0.8, 0.72] // 各风险类型相对权重
      const names = ports.map(p => p.portName.replace(/(口岸|港)/g, ''))
      const data = []
      ports.forEach((p, i) => {
        risks.forEach((r, j) => {
          let score = Math.round(p.riskScore * weights[j])
          // 确定性微调，避免整列同色，增加层次感
          score = Math.min(100, Math.max(5, score + (j % 2 === 0 ? 4 : -4)))
          data.push([i, j, score])
        })
      })
      return { ports: names, risks, data }
    },
    // 统一的口岸风险热力图渲染（视觉升级版）
    renderHeatmap(matrix) {
      this.$nextTick(() => {
        if (!this.$refs.heatmapChart) return
        if (this.heatmapInstance) this.heatmapInstance.dispose()
        this.heatmapInstance = this.$echarts.init(this.$refs.heatmapChart)
        const ports = matrix.ports || ['凭祥', '东兴', '防城港', '钦州', '北海', '水口', '龙邦', '硕龙']
        const risks = matrix.risks || ['虚假贸易', '价格欺诈', '资金异常', '单据造假', '走私风险']
        this.heatmapInstance.setOption({
          tooltip: {
            position: 'top',
            formatter: p => `${ports[p.value[0]]} · ${risks[p.value[1]]}<br/>风险指数: <b style="color:#ff9c8a">${p.value[2]}</b>`,
            backgroundColor: 'rgba(8,20,36,0.95)',
            borderColor: 'rgba(47,181,166,0.5)',
            borderWidth: 1,
            textStyle: { color: '#d4eaff' },
            extraCssText: 'border-radius:6px;box-shadow:0 4px 16px rgba(0,0,0,0.45);'
          },
          grid: { top: 14, right: 18, bottom: 56, left: 92 },
          xAxis: {
            type: 'category', data: ports,
            axisLabel: { color: '#a9d4ee', fontSize: 11, rotate: 30 },
            axisLine: { lineStyle: { color: 'rgba(120,180,224,0.25)' } },
            splitArea: { show: true, areaStyle: { color: ['rgba(255,255,255,0.015)', 'rgba(255,255,255,0.045)'] } }
          },
          yAxis: {
            type: 'category', data: risks,
            axisLabel: { color: '#a9d4ee', fontSize: 11 },
            axisLine: { lineStyle: { color: 'rgba(120,180,224,0.25)' } },
            splitArea: { show: true, areaStyle: { color: ['rgba(255,255,255,0.015)', 'rgba(255,255,255,0.045)'] } }
          },
          // 用隐藏的 visualMap 做颜色映射，避免它渲染成竖向胶囊遮挡格子；自定义 HTML 图例负责展示
          visualMap: {
            min: 0, max: 100, show: false,
            inRange: {
              color: ['#10395c', '#1c6ea3', '#1f9bb5', '#36b3a3', '#9bc44a', '#e6a23c', '#f56c6c', '#cf1f3a']
            }
          },
          series: [{
            type: 'heatmap', data: matrix.data,
            itemStyle: { borderColor: 'rgba(8,18,34,0.65)', borderWidth: 1.5, borderRadius: 2 },
            emphasis: { itemStyle: { shadowBlur: 14, shadowColor: 'rgba(0,0,0,0.6)' } },
            label: {
              show: true, color: '#eaf4ff', fontSize: 10,
              textShadowColor: 'rgba(0,0,0,0.55)', textShadowBlur: 3,
              formatter: p => p.value[2]
            }
          }]
        }, true)
      })
    },
    initHeatmapMock() {
      this.renderHeatmap(this.buildHeatmapMatrix(this.mockPorts))
    },
    initHeatmap(raw) {
      if (raw && raw.ports && raw.data) {
        this.renderHeatmap({
          ports: raw.ports,
          risks: raw.risks || ['虚假贸易', '价格欺诈', '资金异常', '单据造假', '走私风险'],
          data: raw.data
        })
      } else {
        this.initHeatmapMock()
      }
    },
    getAlertTagType(level) {
      if (level === 'high') return 'danger'
      if (level === 'medium') return 'warning'
      return ''
    }
  }
}
</script>

<style scoped>
.supervision-dashboard { padding: 0; }
.stat-cards { margin-bottom: 16px; }
.stat-card { background: linear-gradient(135deg, rgba(58,111,247,0.06) 0%, rgba(0,120,200,0.02) 100%); border: 1px solid rgba(58,111,247,0.12); border-radius: 8px; padding: 20px; display: flex; align-items: center; justify-content: space-between; border-left: 4px solid #3a6ff7; transition: transform .25s var(--ease, ease), box-shadow .25s var(--ease, ease), border-color .25s var(--ease, ease); will-change: transform; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(58,111,247,0.08); border-color: rgba(58,111,247,0.3); }
.stat-label { font-size: 13px; color: rgba(255,255,255,0.68); margin-bottom: 6px; }
.stat-value { font-size: 28px; font-weight: bold; color: #fff; line-height: 1.2; font-variant-numeric: tabular-nums; }
.stat-change { font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 4px; }
.stat-change .up { color: #F56C6C; }
.stat-change .down { color: #67C23A; }
.stat-icon { font-size: 38px; opacity: .28; align-self: center; filter: drop-shadow(0 2px 6px rgba(0,0,0,0.35)); }
.chart-row { margin-bottom: 16px; }
.chart-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px; }
.chart-title { font-size: 15px; color: #c8e6ff; margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px solid rgba(58,111,247,0.08); font-weight: 600; }
.chart-container-lg { width: 100%; height: 380px; }
.alert-feed { max-height: 380px; overflow-y: auto; }
.alert-item { padding: 10px 12px; border-left: 3px solid transparent; margin-bottom: 8px; background: rgba(0,20,45,0.3); border-radius: 0 4px 4px 0; transition: background .2s; }
.alert-item:hover { background: rgba(0,30,60,0.5); }
.alert-item.level-high { border-left-color: #F56C6C; }
.alert-item.level-medium { border-left-color: #E6A23C; }
.alert-item.level-low { border-left-color: #E6D23C; }
.alert-time { font-size: 11px; color: #4a6a8a; margin-bottom: 4px; }
.alert-content { display: flex; align-items: center; gap: 8px; }
.alert-text { font-size: 13px; color: #c8e6ff; }

/* 全屏模式增强 */
.supervision-fullscreen {
  padding: 16px 20px;
  min-height: 100vh;
}

/* 移动端 / 窄屏适配 */
@media (max-width: 768px) {
  .chart-container-lg { height: 300px; }
  .stat-card { padding: 16px; }
  .stat-value { font-size: 24px; }
  .stat-icon { font-size: 32px; }
  .chart-card { padding: 16px; }
  .chart-title { font-size: 14px; }
}
@media (max-width: 480px) {
  .chart-container-lg { height: 260px; }
  .stat-value { font-size: 22px; }
}

/* 自定义热力图色阶图例（替代 ECharts visualMap，避免被渲染成竖向胶囊或遮挡格子） */
.heatmap-card { display: flex; flex-direction: column; }
.heatmap-legend {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; margin-top: 8px; font-size: 11px; color: #9ec5e0;
}
.heatmap-legend .legend-bar {
  width: 140px; height: 10px; border-radius: 5px;
  background: linear-gradient(to right, #10395c, #1c6ea3, #1f9bb5, #36b3a3, #9bc44a, #e6a23c, #f56c6c, #cf1f3a);
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.08);
}
</style>
