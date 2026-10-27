<template>
  <div class="settlement-credit-page">
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="12" :md="8" v-for="item in statCards" :key="item.title">
        <div class="stat-card" :style="{ borderLeftColor: item.color }">
          <div class="stat-info">
            <p class="stat-label">{{ item.title }}</p>
            <p class="stat-value" :style="{ color: item.color }">{{ item.value }}</p>
          </div>
          <div class="stat-icon" :style="{ color: item.color }">
            <i :class="item.icon"></i>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="chart-card">
      <h3 class="chart-title">跨境结算趋势</h3>
      <div ref="lineChart" class="chart-container"></div>
    </div>

    <div class="filter-card">
      <el-form :model="filterForm" :inline="true" size="small">
        <el-form-item label="企业名称">
          <el-input v-model="filterForm.name" placeholder="请输入企业名称" clearable @clear="handleSearch"></el-input>
        </el-form-item>
        <el-form-item label="结算类型">
          <el-select v-model="filterForm.type" placeholder="全部类型" clearable @clear="handleSearch">
            <el-option label="人民币结算" value="人民币结算"></el-option>
            <el-option label="美元结算" value="美元结算"></el-option>
            <el-option label="东盟货币结算" value="东盟货币结算"></el-option>
            <el-option label="跨境易货" value="跨境易货"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="企业名称" min-width="160" show-overflow-tooltip></el-table-column>
        <el-table-column prop="type" label="结算类型" width="120"></el-table-column>
        <el-table-column prop="amount" label="金额(万元)" width="120" sortable>
          <template slot-scope="scope">
            <span style="font-weight: bold;">{{ scope.row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="direction" label="方向" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.direction === '出口' ? 'success' : 'warning'" size="small">{{ scope.row.direction }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="counterCountry" label="对手国" width="100"></el-table-column>
        <el-table-column prop="date" label="日期" width="120" sortable></el-table-column>
        <el-table-column prop="abnormal" label="是否异常" width="90" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.abnormal ? 'danger' : 'success'" size="small">{{ scope.row.abnormal ? '异常' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditScore" label="信用分" width="90" align="center">
          <template slot-scope="scope">
            <span :style="{ color: getCreditColor(scope.row.creditScore), fontWeight: 'bold' }">{{ scope.row.creditScore }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @current-change="handlePageChange"
        ></el-pagination>
      </div>
    </div>
  </div>
</template>

<script>
import { getSettlementList, getSettlementTrend } from '@/api'

export default {
  name: 'SettlementCredit',
  data() {
    return {
      filterForm: { name: '', type: '' },
      statCards: [
        { title: '平均信用分', value: '-', icon: 'el-icon-medal', color: '#409EFF' },
        { title: '异常记录数', value: '-', icon: 'el-icon-warning-outline', color: '#F56C6C' },
        { title: '总交易笔数', value: '-', icon: 'el-icon-s-data', color: '#67C23A' }
      ],
      tableData: [],
      allData: [],
      loading: false,
      page: 1,
      pageSize: 10,
      total: 0,
      lineInstance: null
    }
  },
  created() {
    this.loadData()
  },
  mounted() {
    this.loadTrend()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.lineInstance) this.lineInstance.dispose()
  },
  methods: {
    handleResize() {
      if (this.lineInstance) this.lineInstance.resize()
    },
    async loadData() {
      this.loading = true
      try {
        const res = await getSettlementList({ page: this.page, pageSize: this.pageSize })
        if (res.data && res.data.list && res.data.list.length > 0) {
          this.allData = res.data.list
          this.total = res.data.total || res.data.list.length
          this.updateStats()
          this.filterData()
          this.loading = false
          return
        }
      } catch (e) { /* API 异常，显示空状态 */ }
      this.allData = []
      this.total = 0
      this.updateStats()
      this.filterData()
      this.loading = false
    },
    async loadTrend() {
      let months = []
      let exportVals = []
      let importVals = []
      try {
        const res = await getSettlementTrend()
        if (res.data && res.data.length > 0) {
          months = res.data.map(d => d.month)
          exportVals = res.data.map(d => d.exportAmount)
          importVals = res.data.map(d => d.importAmount)
        }
      } catch (e) { /* use mock */ }
      if (months.length === 0) {
        months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月']
        exportVals = [3200, 4100, 3800, 5200, 4600, 5800, 6100]
        importVals = [2800, 3200, 3500, 4100, 3900, 4500, 4800]
      }
      this.$nextTick(() => {
        if (!this.$refs.lineChart) return
        if (this.lineInstance) this.lineInstance.dispose()
        this.lineInstance = this.$echarts.init(this.$refs.lineChart)
        this.lineInstance.setOption({
          tooltip: { trigger: 'axis', backgroundColor: '#0b1a30', borderColor: 'rgba(0,212,255,0.3)', textStyle: { color: '#d4eaff' } },
          legend: { data: ['出口结算', '进口结算'], textStyle: { color: '#8ab4d0', fontSize: 12 }, top: 0 },
          grid: { top: 40, right: 20, bottom: 30, left: 60 },
          xAxis: { type: 'category', boundaryGap: false, data: months, axisLabel: { color: '#7eb8e0' }, axisLine: { lineStyle: { color: '#1a4a6e' } } },
          yAxis: { type: 'value', name: '金额(万元)', nameTextStyle: { color: '#5a7a9a', fontSize: 11 }, axisLabel: { color: '#5a7a9a' }, splitLine: { lineStyle: { color: 'rgba(0,212,255,0.06)', type: 'dashed' } } },
          series: [
            {
              name: '出口结算', type: 'line', data: exportVals, smooth: true,
              itemStyle: { color: '#409EFF' },
              areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.02)' }] } }
            },
            {
              name: '进口结算', type: 'line', data: importVals, smooth: true,
              itemStyle: { color: '#67C23A' },
              areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(103,194,58,0.3)' }, { offset: 1, color: 'rgba(103,194,58,0.02)' }] } }
            }
          ]
        }, true)
      })
    },
    updateStats() {
      const scores = this.allData.map(d => d.creditScore)
      const avg = scores.length > 0 ? Math.round(scores.reduce((a, b) => a + b, 0) / scores.length) : 0
      this.statCards[0].value = avg
      this.statCards[1].value = this.allData.filter(d => d.abnormal).length
      this.statCards[2].value = this.allData.length
    },
    filterData() {
      let data = [...this.allData]
      if (this.filterForm.name) data = data.filter(item => item.name.includes(this.filterForm.name))
      if (this.filterForm.type) data = data.filter(item => item.type === this.filterForm.type)
      this.total = data.length
      const start = (this.page - 1) * this.pageSize
      this.tableData = data.slice(start, start + this.pageSize)
    },
    handleSearch() {
      this.page = 1
      this.filterData()
    },
    handleReset() {
      this.filterForm = { name: '', type: '' }
      this.page = 1
      this.filterData()
    },
    handlePageChange(page) {
      this.page = page
      this.filterData()
    },
    getCreditColor(score) {
      if (score >= 80) return '#67C23A'
      if (score >= 60) return '#E6A23C'
      return '#F56C6C'
    }
  }
}
</script>

<style scoped>
.settlement-credit-page { padding: 0; }
.stat-cards { margin-bottom: 16px; }
.stat-card { background: linear-gradient(135deg, rgba(58,111,247,0.06) 0%, rgba(0,120,200,0.02) 100%); border: 1px solid rgba(58,111,247,0.12); border-radius: 8px; padding: 20px; display: flex; align-items: center; justify-content: space-between; border-left: 4px solid #3a6ff7; transition: all .25s; }
.stat-card:hover { transform: translateY(-2px); border-color: rgba(58,111,247,0.3); box-shadow: 0 4px 16px rgba(58,111,247,0.08); }
.stat-label { font-size: 13px; color: rgba(255,255,255,0.5); margin-bottom: 6px; }
.stat-value { font-size: 28px; font-weight: bold; line-height: 1.2; }
.stat-icon { font-size: 42px; opacity: .15; }
.chart-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.chart-title { font-size: 15px; color: #c8e6ff; margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px solid rgba(58,111,247,0.08); font-weight: 600; }
.chart-container { width: 100%; height: 320px; }
.filter-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px 20px 5px; margin-bottom: 16px; }
.table-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px; }
.pagination-wrap { margin-top: 15px; text-align: right; }

/* 统一暗色表格 */
.settlement-credit-page .el-table { background-color: #0c1a2e !important; }
.settlement-credit-page .el-table::before { background-color: #1a3a5c !important; }
.settlement-credit-page .el-table th.el-table__cell,
.settlement-credit-page .el-table thead th {
  background-color: #0a1525 !important;
  color: #7eb8e0 !important;
  border-bottom: 2px solid #1a4a6e !important;
}
.settlement-credit-page .el-table .el-table__body tr.el-table__row > td.el-table__cell,
.settlement-credit-page .el-table td.el-table__cell {
  background-color: #0f2238 !important;
  color: #d4eaff !important;
  border-bottom: 1px solid #162d48 !important;
}
.settlement-credit-page .el-table .el-table__body tr:hover > td.el-table__cell {
  background-color: #183654 !important;
}
</style>
