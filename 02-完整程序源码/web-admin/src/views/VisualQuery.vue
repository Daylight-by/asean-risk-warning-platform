<template>
  <div class="vq-page">
    <!-- 左侧：查询构建器（导航/向导） -->
    <div class="vq-builder">
      <div class="vq-builder-head">
        <i class="el-icon-data-analysis"></i>
        <div>
          <div class="vq-title">可视化查询</div>
          <div class="vq-sub">零代码拖选 · 自助洞察公共数据</div>
        </div>
      </div>

      <!-- 步骤1 数据源 -->
      <div class="vq-section">
        <div class="vq-section-title">
          <span class="vq-step-num">1</span>
          <span class="vq-step-label">选择数据源</span>
        </div>
        <el-select v-model="currentSource" placeholder="请选择数据源" @change="onSourceChange" class="vq-full">
          <el-option v-for="s in sources" :key="s.key" :label="s.name" :value="s.key">
            <span class="vq-opt-name">{{ s.name }}</span>
            <span class="vq-opt-desc">{{ s.description }}</span>
          </el-option>
        </el-select>
      </div>

      <!-- 步骤2 筛选条件 -->
      <div class="vq-section">
        <div class="vq-section-title">
          <span class="vq-step-num">2</span>
          <span class="vq-step-label">筛选条件</span>
          <el-button type="text" size="mini" icon="el-icon-plus" @click="addFilter">添加</el-button>
        </div>
        <div class="vq-filter-list">
          <div v-for="(f, idx) in filters" :key="f.uid" class="vq-filter-row">
            <el-select v-model="f.field" size="mini" class="vq-f-field" @change="onFilterFieldChange(f)">
              <el-option v-for="c in sourceColumns" :key="c.field" :label="c.label" :value="c.field" />
            </el-select>
            <el-select v-model="f.op" size="mini" class="vq-f-op">
              <el-option v-for="o in operatorOptions(f)" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <template v-if="filterColumn(f).options && filterColumn(f).options.length">
              <el-select v-model="f.value" size="mini" class="vq-f-val" placeholder="值">
                <el-option v-for="opt in filterColumn(f).options" :key="opt" :label="opt" :value="opt" />
              </el-select>
            </template>
            <template v-else>
              <el-input v-model="f.value" size="mini" class="vq-f-val" placeholder="值" />
            </template>
            <i class="el-icon-close vq-f-del" @click="removeFilter(idx)"></i>
          </div>
        </div>
        <div v-if="!filters.length" class="vq-hint">暂无筛选条件，将查询全部数据</div>
      </div>

      <!-- 步骤3 输出与展示 -->
      <div class="vq-section">
        <div class="vq-section-title">
          <span class="vq-step-num">3</span>
          <span class="vq-step-label">{{ chartType === 'table' ? '选择输出列' : '选择分组维度' }}</span>
        </div>
        <el-radio-group v-model="chartType" size="mini" @change="onChartTypeChange" class="vq-chart-types">
          <el-radio-button label="table"><i class="el-icon-s-grid"></i> 表格</el-radio-button>
          <el-radio-button label="bar"><i class="el-icon-s-data"></i> 柱状图</el-radio-button>
          <el-radio-button label="line"><i class="el-icon-s-marketing"></i> 折线图</el-radio-button>
          <el-radio-button label="pie"><i class="el-icon-pie-chart"></i> 饼图</el-radio-button>
        </el-radio-group>

        <template v-if="chartType === 'table'">
          <el-checkbox-group v-model="selectedColumns" class="vq-col-group">
            <el-checkbox v-for="c in sourceColumns" :key="c.field" :label="c.field" size="mini">
              {{ c.label }}
            </el-checkbox>
          </el-checkbox-group>
        </template>
        <template v-else>
          <el-select v-model="groupBy" placeholder="选择分组字段" class="vq-full">
            <el-option v-for="c in groupableColumns" :key="c.field" :label="c.label" :value="c.field" />
          </el-select>
          <div class="vq-hint">按该维度聚合计数，自动生成{{ chartType === 'pie' ? '饼' : '柱/折' }}图</div>
        </template>
      </div>

      <el-button type="primary" class="vq-run" :loading="loading" @click="runQuery">
        <i class="el-icon-video-play"></i> 运行查询
      </el-button>

      <!-- 我的看板 -->
      <div class="vq-section vq-dash">
        <div class="vq-section-title">
          <span class="vq-step-num">★</span>
          <span class="vq-step-label">我的看板</span>
        </div>
        <div class="vq-dash-bar">
          <el-input v-model="dashboardName" size="mini" placeholder="看板名称" class="vq-dash-input" />
          <el-button size="mini" type="success" plain icon="el-icon-collection" @click="saveDashboard">保存</el-button>
        </div>
        <div v-if="!savedDashboards.length" class="vq-hint">尚未保存看板</div>
        <div v-else class="vq-dash-list">
          <div v-for="d in savedDashboards" :key="d.id" class="vq-dash-item">
            <span class="vq-dash-name">{{ d.name }}</span>
            <span class="vq-dash-ops">
              <i class="el-icon-refresh" title="加载" @click="loadDashboard(d)"></i>
              <i class="el-icon-delete" title="删除" @click="deleteDashboard(d.id)"></i>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧：结果区 -->
    <div class="vq-result">
      <div class="vq-result-head">
        <div class="vq-result-left">
          <span class="vq-result-title">{{ resultTitle }}</span>
          <span v-if="result" class="vq-result-total">共 {{ result.total }} 条</span>
          <el-tag v-if="sourceMeta" size="mini" class="vq-source-tag">{{ sourceMeta.name }}</el-tag>
        </div>
        <div class="vq-result-ops">
          <el-button
            v-if="result && result.rows && result.rows.length"
            class="vq-export-btn"
            icon="el-icon-download"
            @click="downloadCsv"
          >导出 CSV</el-button>
        </div>
      </div>

      <div v-if="!result && !loading" class="vq-empty">
        <i class="el-icon-data-line"></i>
        <p>选择数据源与条件，点击「运行查询」生成可视化结果</p>
      </div>

      <div v-else-if="loading" class="vq-loading">
        <i class="el-icon-loading"></i>
        <p>查询中…</p>
      </div>

      <template v-else>
        <div v-if="result.mode === 'chart'" ref="chart" class="vq-chart"></div>

        <div v-if="result.mode === 'table'" class="vq-table-wrap">
          <el-table :data="result.rows" size="mini" stripe max-height="520" class="vq-table">
            <el-table-column
              v-for="col in tableColumns"
              :key="col.field || col.prop"
              :prop="col.field || col.prop"
              :label="col.label"
              show-overflow-tooltip
            />
          </el-table>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { getQuerySources, executeQuery, getDashboards, saveDashboard, deleteDashboard } from '@/api'

export default {
  name: 'VisualQuery',
  data() {
    return {
      sources: [],
      currentSource: '',
      sourceMeta: null,
      filters: [],
      selectedColumns: [],
      chartType: 'table',
      groupBy: '',
      loading: false,
      result: null,
      chartInstance: null,
      savedDashboards: [],
      dashboardName: '',
      uidSeq: 1
    }
  },
  computed: {
    sourceColumns() {
      return this.sourceMeta ? this.sourceMeta.columns : []
    },
    groupableColumns() {
      return this.sourceColumns.filter(c => c.groupable)
    },
    tableColumns() {
      if (!this.result) return []
      if (this.result.mode === 'table') {
        return this.result.columns || []
      }
      return [
        { field: 'category', label: this.groupLabel || '维度' },
        { field: 'value', label: '计数' }
      ]
    },
    groupLabel() {
      if (!this.groupBy || !this.sourceMeta) return ''
      const c = this.sourceMeta.columns.find(x => x.field === this.groupBy)
      return c ? c.label : ''
    },
    resultTitle() {
      if (!this.result) return '查询结果'
      if (this.result.mode === 'chart') {
        const map = { bar: '柱状图', line: '折线图', pie: '饼图' }
        return '可视化结果 · ' + (map[this.result.chartType] || '')
      }
      return '查询结果 · 表格'
    }
  },
  created() {
    this.loadSources()
    this.loadDashboards()
  },
  beforeDestroy() {
    if (this.chartInstance) this.chartInstance.dispose()
  },
  methods: {
    /* ---------- 数据加载 ---------- */
    async loadSources() {
      try {
        const res = await getQuerySources()
        this.sources = res.data || []
        if (this.sources.length) {
          this.currentSource = this.sources[0].key
          this.onSourceChange(this.currentSource)
        }
      } catch (e) {
        this.$message.error('加载数据源失败')
      }
    },
    async loadDashboards() {
      try {
        const res = await getDashboards()
        this.savedDashboards = res.data || []
      } catch (e) {
        this.savedDashboards = []
      }
    },

    /* ---------- 数据源切换 ---------- */
    onSourceChange(key) {
      this.sourceMeta = this.sources.find(s => s.key === key) || null
      this.selectedColumns = this.sourceColumns.filter(c => c.defaultDisplay).map(c => c.field)
      this.groupBy = this.groupableColumns.length ? this.groupableColumns[0].field : ''
      this.filters = []
    },

    /* ---------- 筛选条件 ---------- */
    filterColumn(f) {
      return this.sourceColumns.find(c => c.field === f.field) || { type: 'string', options: [] }
    },
    defaultOp(type) {
      return type === 'number' ? 'eq' : type === 'date' ? 'gte' : type === 'enum' ? 'eq' : 'eq'
    },
    operatorOptions(f) {
      const type = this.filterColumn(f).type
      const all = [
        { value: 'eq', label: '等于', types: ['string', 'number', 'date', 'enum'] },
        { value: 'ne', label: '不等于', types: ['string', 'number'] },
        { value: 'like', label: '包含', types: ['string', 'date'] },
        { value: 'gt', label: '大于', types: ['number'] },
        { value: 'gte', label: '大于等于', types: ['number', 'date'] },
        { value: 'lt', label: '小于', types: ['number'] },
        { value: 'lte', label: '小于等于', types: ['number', 'date'] }
      ]
      return all.filter(o => o.types.includes(type))
    },
    addFilter() {
      if (!this.sourceColumns.length) return
      const first = this.sourceColumns[0]
      this.filters.push({
        uid:   this.uidSeq++,
        field: first.field,
        op:    this.defaultOp(first.type),
        value: ''
      })
    },
    removeFilter(idx) {
      this.filters.splice(idx, 1)
    },
    onFilterFieldChange(f) {
      const col = this.filterColumn(f)
      f.op = this.defaultOp(col.type)
      f.value = ''
    },
    onChartTypeChange() {
      if (this.chartType !== 'table' && !this.groupBy && this.groupableColumns.length) {
        this.groupBy = this.groupableColumns[0].field
      }
    },

    /* ---------- 执行查询 ---------- */
    async runQuery() {
      if (!this.currentSource) {
        this.$message.warning('请先选择数据源')
        return
      }
      const cleanFilters = this.filters
        .filter(f => f.field && f.op && f.value !== '' && f.value !== null)
        .map(f => ({ field: f.field, op: f.op, value: String(f.value).trim() }))

      const payload = {
        source:    this.currentSource,
        chartType: this.chartType,
        groupBy:   this.chartType === 'table' ? '' : this.groupBy,
        columns:   this.chartType === 'table' ? this.selectedColumns : [],
        filters:   cleanFilters
      }

      this.loading = true
      try {
        const res = await executeQuery(payload)
        this.result = res.data
        if (this.result.mode === 'chart') {
          this.$nextTick(() => this.renderChart())
        } else if (this.chartInstance) {
          this.chartInstance.dispose()
          this.chartInstance = null
        }
      } catch (e) {
        if (e && !e.response) {
          this.$message.error('查询失败：' + (e.message || '网络异常'))
        }
      } finally {
        this.loading = false
      }
    },

    /* ---------- 图表渲染 ---------- */
    renderChart() {
      if (!this.result || !this.result.chart) return
      if (!this.chartInstance) {
        this.chartInstance = this.$echarts.init(this.$refs.chart)
      }
      const chart = this.result.chart
      const categories = chart.categories || []
      const seriesData = (chart.series && chart.series[0]) || []
      const primary = '#3a6ff7'
      const accent = '#f5a524'
      const axisLine = 'rgba(255,255,255,0.08)'
      const textColor = '#c9d4e3'
      const labelFormatter = v => {
        const s = String(v)
        return s.length > 8 ? s.slice(0, 8) + '…' : s
      }

      let option
      if (this.result.chartType === 'pie') {
        option = {
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { type: 'scroll', orient: 'horizontal', bottom: 0, textStyle: { color: textColor } },
          color: [primary, accent, '#22d3a8', '#a78bfa', '#f472b6', '#facc15', '#38bdf8', '#fb7185', '#34d399', '#c084fc'],
          series: [{
            type: 'pie',
            radius: ['38%', '68%'],
            center: ['50%', '46%'],
            itemStyle: { borderColor: 'rgba(11,15,23,0.6)', borderWidth: 2 },
            label: { color: textColor },
            data: categories.map((c, i) => ({ name: c, value: seriesData[i] || 0 }))
          }]
        }
      } else {
        const isLine = this.result.chartType === 'line'
        const many = categories.length > 12
        option = {
          tooltip: { trigger: 'axis' },
          grid: { left: 48, right: 24, top: 24, bottom: many ? 90 : 72 },
          xAxis: {
            type: 'category',
            data: categories,
            axisLine: { lineStyle: { color: axisLine } },
            axisTick: { alignWithLabel: true },
            axisLabel: {
              color: textColor,
              interval: 0,
              rotate: categories.length > 5 ? 45 : 0,
              fontSize: 11,
              formatter: labelFormatter
            }
          },
          yAxis: {
            type: 'value',
            axisLine: { show: false },
            splitLine: { lineStyle: { color: axisLine } },
            axisLabel: { color: textColor }
          },
          dataZoom: many
            ? [
                {
                  type: 'slider',
                  xAxisIndex: 0,
                  height: 18,
                  bottom: 12,
                  start: 0,
                  end: Math.min(100, (12 / categories.length) * 100),
                  borderColor: 'transparent',
                  fillerColor: 'rgba(58,111,247,0.2)',
                  handleStyle: { color: primary },
                  dataBackground: {
                    lineStyle: { color: primary },
                    areaStyle: { color: 'rgba(58,111,247,0.1)' }
                  },
                  textStyle: { color: textColor }
                }
              ]
            : undefined,
          series: [{
            type: isLine ? 'line' : 'bar',
            data: seriesData,
            smooth: isLine,
            symbol: isLine ? 'circle' : 'none',
            barWidth: '46%',
            itemStyle: {
              color: isLine
                ? primary
                : {
                    type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                    colorStops: [
                      { offset: 0, color: primary },
                      { offset: 1, color: 'rgba(58,111,247,0.25)' }
                    ]
                  }
            },
            lineStyle: { color: primary, width: 3 },
            areaStyle: isLine ? { color: 'rgba(58,111,247,0.12)' } : undefined
          }]
        }
      }
      this.chartInstance.setOption(option, true)
    },

    /* ---------- CSV 导出 ---------- */
    downloadCsv() {
      if (!this.result || !this.result.rows || !this.result.rows.length) return
      const rows = this.result.rows
      let headers, keys
      if (this.result.mode === 'table') {
        headers = this.result.columns.map(c => c.label)
        keys = this.result.columns.map(c => c.field)
      } else {
        headers = [this.groupLabel || '维度', '计数']
        keys = ['category', 'value']
      }
      const csv = this.toCsv(rows, headers, keys)
      const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `查询结果_${this.currentSource}_${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
      this.$message.success('已导出 CSV')
    },
    toCsv(rows, headers, keys) {
      const escape = v => {
        if (v == null) return ''
        const s = String(v)
        if (s.includes(',') || s.includes('"') || s.includes('\n') || s.includes('\r')) {
          return '"' + s.replace(/"/g, '""') + '"'
        }
        return s
      }
      const lines = [headers.map(escape).join(',')]
      rows.forEach(r => {
        lines.push(keys.map(k => escape(r[k])).join(','))
      })
      return lines.join('\n')
    },

    /* ---------- 看板持久化（后端） ---------- */
    saveDashboard() {
      const name = (this.dashboardName || '').trim() || ('看板-' + new Date().toLocaleTimeString())
      const config = {
        source:    this.currentSource,
        chartType: this.chartType,
        groupBy:   this.groupBy,
        columns:   this.selectedColumns,
        filters:   this.filters.map(f => ({ field: f.field, op: f.op, value: f.value }))
      }
      saveDashboard({ name, config }).then(res => {
        const item = res.data
        const list = this.savedDashboards.filter(d => String(d.id) !== String(item.id))
        list.unshift(item)
        this.savedDashboards = list
        this.$message.success('已保存看板：' + name)
        this.dashboardName = ''
      }).catch(() => this.$message.error('保存看板失败'))
    },
    loadDashboard(d) {
      const c = d.config || {}
      this.currentSource = c.source
      this.onSourceChange(c.source)
      this.chartType = c.chartType
      this.groupBy = c.groupBy
      this.selectedColumns = c.columns || []
      this.filters = (c.filters || []).map(f => ({
        uid: this.uidSeq++, field: f.field, op: f.op, value: f.value
      }))
      this.$nextTick(() => this.runQuery())
    },
    deleteDashboard(id) {
      deleteDashboard(id).then(() => {
        this.savedDashboards = this.savedDashboards.filter(d => String(d.id) !== String(id))
        this.$message.success('已删除看板')
      }).catch(() => this.$message.error('删除看板失败'))
    }
  }
}
</script>

<style scoped>
.vq-page {
  display: flex;
  gap: 22px;
  align-items: stretch;
  height: 100%;
}

/* ============ 左侧构建器（导航） ============ */
.vq-builder {
  width: 460px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
  border-radius: 14px;
  padding: 22px;
  overflow-y: auto;
}
.vq-builder-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 6px;
}
.vq-builder-head > i {
  font-size: 26px;
  color: #3a6ff7;
  width: 46px; height: 46px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(58, 111, 247, 0.22), rgba(58, 111, 247, 0.05));
}
.vq-title { font-size: 17px; font-weight: 700; color: #e6edf3; letter-spacing: 0.3px; }
.vq-sub { font-size: 12px; color: #7c8aa0; margin-top: 4px; }

/* 步骤卡片 */
.vq-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: rgba(255, 255, 255, 0.025);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 18px;
}
.vq-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #e6edf3;
}
.vq-step-num {
  width: 24px; height: 24px;
  border-radius: 7px;
  background: linear-gradient(135deg, rgba(58, 111, 247, 0.35), rgba(58, 111, 247, 0.12));
  color: #8eb2ff;
  font-size: 12px; font-weight: 700;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.vq-step-label { flex: 1; }
.vq-section-title .el-button { margin-left: auto; padding: 0; }

.vq-full { width: 100%; }
.vq-hint { font-size: 12px; color: #6b788c; line-height: 1.6; }

/* 筛选条件 — 使用 grid 填满宽度 */
.vq-filter-list { display: flex; flex-direction: column; gap: 10px; }
.vq-filter-row {
  display: grid;
  grid-template-columns: 1.5fr 0.9fr 1.6fr 22px;
  gap: 10px;
  align-items: center;
}
.vq-filter-row .el-select,
.vq-filter-row .el-input { width: 100%; }
.vq-f-del {
  justify-self: center;
  color: #7c8aa0;
  cursor: pointer;
  font-size: 14px;
  transition: color 0.2s;
}
.vq-f-del:hover { color: #f56c6c; }

/* 展示方式 — 全宽等分 */
.vq-chart-types { display: flex; width: 100%; }
.vq-chart-types ::v-deep .el-radio-button { flex: 1; }
.vq-chart-types ::v-deep .el-radio-button__inner {
  width: 100%;
  padding-left: 0; padding-right: 0;
}
.vq-chart-types ::v-deep .el-radio-button__inner i { margin-right: 4px; }

/* 输出列 — 两列等宽、统一卡片、标签自适应截断 */
.vq-col-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.vq-col-group ::v-deep .el-checkbox {
  margin: 0;
  width: 100%;
  min-height: 36px;
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  transition: border-color 0.18s var(--ease), background 0.18s var(--ease);
}
.vq-col-group ::v-deep .el-checkbox:hover {
  border-color: var(--border-strong, rgba(255, 255, 255, 0.14));
  background: rgba(58, 111, 247, 0.06);
}
.vq-col-group ::v-deep .el-checkbox.is-checked {
  border-color: var(--primary, #3a6ff7);
  background: rgba(58, 111, 247, 0.12);
}
.vq-col-group ::v-deep .el-checkbox__input {
  flex-shrink: 0;
}
.vq-col-group ::v-deep .el-checkbox__label {
  flex: 1;
  min-width: 0;
  padding-left: 8px;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vq-run {
  width: 100%; height: 44px; font-size: 14px; font-weight: 600;
  background: linear-gradient(135deg, #3a6ff7, #2b5fe0);
  border: none; border-radius: 10px;
  box-shadow: 0 6px 18px rgba(58, 111, 247, 0.32);
  transition: filter 0.2s;
}
.vq-run:hover { filter: brightness(1.08); }

/* 看板 */
.vq-dash { gap: 12px; }
.vq-dash-bar { display: flex; align-items: center; gap: 10px; }
.vq-dash-input { flex: 1; }
.vq-dash-list { display: flex; flex-direction: column; gap: 8px; }
.vq-dash-item {
  display: flex; align-items: center; justify-content: space-between;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px; padding: 10px 12px;
  transition: all 0.2s;
}
.vq-dash-item:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(58, 111, 247, 0.25);
}
.vq-dash-name { font-size: 13px; color: #c9d4e3; }
.vq-dash-ops { display: flex; gap: 14px; }
.vq-dash-ops i { cursor: pointer; color: #7c8aa0; font-size: 14px; transition: color 0.2s; }
.vq-dash-ops i:hover { color: #3a6ff7; }

/* ============ 右侧结果区 ============ */
.vq-result {
  flex: 1; min-width: 0;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
  border-radius: 14px;
  padding: 20px;
  display: flex; flex-direction: column;
}
.vq-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-shrink: 0;
  min-height: 40px;
  gap: 12px;
}
.vq-result-left {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
}
.vq-result-title { font-size: 15px; font-weight: 700; color: #e6edf3; }
.vq-result-total { font-size: 12px; color: #7c8aa0; }
.vq-source-tag { background: rgba(58, 111, 247, 0.1); border-color: rgba(58, 111, 247, 0.25); color: #8eb2ff; }
.vq-result-ops { display: flex; gap: 10px; }

/* 导出 CSV 按钮：实心渐变 + 投影，确保清晰醒目、不模糊 */
.vq-export-btn {
  background: linear-gradient(135deg, #3a6ff7, #2b5fe0) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.3px;
  padding: 9px 18px;
  border-radius: 9px;
  box-shadow: 0 4px 14px rgba(58, 111, 247, 0.35);
  transition: filter 0.2s, transform 0.2s;
}
.vq-export-btn:hover { filter: brightness(1.12); transform: translateY(-1px); }
.vq-export-btn:active { transform: translateY(0); }
.vq-export-btn i { margin-right: 6px; font-weight: 700; font-size: 14px; }

.vq-empty, .vq-loading {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #5d6b7e; gap: 14px;
}
.vq-empty i { font-size: 54px; color: #3a6ff7; opacity: 0.35; }
.vq-empty p { font-size: 13px; color: #6b788c; max-width: 320px; text-align: center; line-height: 1.7; margin: 0; }
.vq-loading i { font-size: 46px; color: #3a6ff7; opacity: 0.5; }
.vq-loading i { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.vq-chart { width: 100%; height: 400px; margin-bottom: 16px; flex-shrink: 0; }
.vq-table-wrap { flex: 1; overflow: auto; min-height: 120px; }
.vq-table { background: transparent; }
.vq-table ::v-deep .el-table__row { cursor: default; }
.vq-table ::v-deep .el-table th {
  background: rgba(255, 255, 255, 0.04) !important;
  color: #9fb0c3; font-weight: 600;
}
.vq-table ::v-deep .el-table td, .vq-table ::v-deep .el-table th {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: #c9d4e3;
}
.vq-table ::v-deep .el-table--striped .el-table__row--striped td {
  background: rgba(255, 255, 255, 0.02);
}
.vq-opt-name { float: left; color: #e6edf3; }
.vq-opt-desc { float: right; color: #7c8aa0; font-size: 11px; margin-left: 14px; }

/* ============ 响应式适配 ============ */
@media (max-width: 900px) {
  .vq-page {
    flex-direction: column;
    height: auto;
    overflow: visible;
  }
  .vq-builder {
    width: 100%;
    max-height: none;
  }
  .vq-result {
    min-height: 420px;
  }
  .vq-chart {
    height: 320px;
  }
}
@media (max-width: 640px) {
  .vq-builder { padding: 16px; gap: 14px; }
  .vq-section { padding: 14px; gap: 12px; }
  .vq-filter-row { grid-template-columns: 1fr 0.9fr 1fr 22px; gap: 8px; }
  .vq-col-group { grid-template-columns: 1fr; }
  .vq-result { padding: 16px; }
  .vq-result-head { flex-wrap: wrap; }
}
</style>
