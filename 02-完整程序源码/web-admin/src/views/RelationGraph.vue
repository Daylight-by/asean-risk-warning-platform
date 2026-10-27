<template>
  <div class="relation-graph-page">
    <div class="filter-card">
      <el-form :model="searchForm" :inline="true" size="small">
        <el-form-item label="统一信用代码">
          <el-input
            v-model="searchForm.creditCode"
            placeholder="请输入企业统一信用代码"
            clearable
            @keyup.enter.native="handleSearch"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          <el-button type="warning" icon="el-icon-s-opportunity" @click="multiLangDialogVisible = true">多语种搜索</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="chart-card">
      <h3 class="chart-title">跨境关联关系图谱</h3>
      <el-alert v-if="isDemoData" title="当前为演示样例关系图；输入企业统一信用代码后查询真实接口结果" type="warning" :closable="false" show-icon />
      <div ref="graphChart" class="chart-container" v-loading="graphLoading"></div>
    </div>

    <div class="table-card">
      <h3 class="chart-title">关联实体列表</h3>
      <el-table :data="relationTable" style="width: 100%" v-loading="tableLoading">
        <el-table-column prop="entity" label="关联实体" min-width="180" show-overflow-tooltip></el-table-column>
        <el-table-column prop="country" label="所在国" width="120"></el-table-column>
        <el-table-column prop="relationType" label="关联类型" width="120"></el-table-column>
        <el-table-column prop="level" label="层级" width="80" align="center"></el-table-column>
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template slot-scope="scope">
            <el-tag :type="getRiskTagType(scope.row.riskLevel)" size="small">{{ scope.row.riskLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskTags" label="风险标签" min-width="160">
          <template slot-scope="scope">
            <template v-if="scope.row.riskTags && scope.row.riskTags.trim()">
              <el-tag
                v-for="tag in scope.row.riskTags.split(',')"
                :key="tag"
                size="mini"
                :type="getRiskTagType(tag.trim())"
                style="margin-right: 4px;"
              >{{ tag.trim() }}</el-tag>
            </template>
            <span v-else class="no-tag-hint">暂无</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 多语种搜索弹窗 -->
    <el-dialog title="多语种企业搜索" :visible.sync="multiLangDialogVisible" width="600px" top="10vh">
      <el-form :model="multiLangForm" :inline="true" size="small" style="margin-bottom: 12px;">
        <el-form-item>
          <el-input v-model="multiLangForm.keyword" placeholder="输入企业名称（中/英/越南语/泰语等）" clearable style="width: 340px;" @keyup.enter.native="handleMultiLangSearch"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleMultiLangSearch" :loading="multiLangLoading">搜索</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="multiLangResults" size="small" v-loading="multiLangLoading" max-height="320" v-if="multiLangResults.length > 0">
        <el-table-column prop="nameZh" label="中文名" min-width="120" show-overflow-tooltip></el-table-column>
        <el-table-column prop="nameEn" label="英文名" min-width="120" show-overflow-tooltip></el-table-column>
        <el-table-column prop="nameVi" label="越南语名" min-width="120" show-overflow-tooltip></el-table-column>
        <el-table-column prop="creditCode" label="信用代码" width="180"></el-table-column>
        <el-table-column label="操作" width="80">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="selectMultiLangResult(scope.row)">查询</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!multiLangLoading && multiLangResults.length === 0 && multiLangSearched" style="text-align: center; color: #5a7a9a; padding: 20px;">
        未找到匹配的企业
      </div>
      <span slot="footer">
        <el-button @click="multiLangDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getRelationGraph } from '@/api'

export default {
  name: 'RelationGraph',
  data() {
    return {
      searchForm: { creditCode: '' },
      graphLoading: false,
      tableLoading: false,
      relationTable: [],
      graphInstance: null,
      multiLangDialogVisible: false,
      multiLangLoading: false,
      multiLangSearched: false,
      multiLangForm: { keyword: '' },
      multiLangResults: [],
      isDemoData: true,
      mockNodes: [
        { name: '广西恒信贸易', category: 0, symbolSize: 50, riskLevel: '高风险' },
        { name: '越南ABC公司', category: 1, symbolSize: 40, riskLevel: '高风险' },
        { name: '凭祥达通物流', category: 0, symbolSize: 35, riskLevel: '中风险' },
        { name: '泰国Siam Trading', category: 1, symbolSize: 38, riskLevel: '中风险' },
        { name: '东兴万通进出口', category: 0, symbolSize: 30, riskLevel: '低风险' },
        { name: '柬埔寨Angkor Ltd', category: 1, symbolSize: 32, riskLevel: '高风险' },
        { name: '防城港恒达货运', category: 0, symbolSize: 28, riskLevel: '中风险' },
        { name: '印尼Nusa Express', category: 1, symbolSize: 34, riskLevel: '低风险' },
        { name: '南宁聚源供应链', category: 0, symbolSize: 36, riskLevel: '中风险' },
        { name: '马来西亚Borneo Trade', category: 1, symbolSize: 30, riskLevel: '低风险' }
      ],
      mockLinks: [
        { source: '广西恒信贸易', target: '越南ABC公司', relation: '股权关联', lineStyle: { color: '#F56C6C' } },
        { source: '广西恒信贸易', target: '凭祥达通物流', relation: '物流合作', lineStyle: { color: '#E6A23C' } },
        { source: '广西恒信贸易', target: '东兴万通进出口', relation: '贸易往来', lineStyle: { color: '#67C23A' } },
        { source: '越南ABC公司', target: '泰国Siam Trading', relation: '转口贸易', lineStyle: { color: '#E6A23C' } },
        { source: '凭祥达通物流', target: '柬埔寨Angkor Ltd', relation: '仓储合作', lineStyle: { color: '#F56C6C' } },
        { source: '东兴万通进出口', target: '防城港恒达货运', relation: '物流分包', lineStyle: { color: '#E6A23C' } },
        { source: '泰国Siam Trading', target: '印尼Nusa Express', relation: '供应链关联', lineStyle: { color: '#67C23A' } },
        { source: '南宁聚源供应链', target: '广西恒信贸易', relation: '股权关联', lineStyle: { color: '#F56C6C' } },
        { source: '南宁聚源供应链', target: '马来西亚Borneo Trade', relation: '贸易往来', lineStyle: { color: '#67C23A' } },
        { source: '柬埔寨Angkor Ltd', target: '印尼Nusa Express', relation: '资金往来', lineStyle: { color: '#F56C6C' } }
      ]
    }
  },
  mounted() {
    this.initGraph()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.graphInstance) this.graphInstance.dispose()
  },
  methods: {
    handleResize() {
      if (this.graphInstance) this.graphInstance.resize()
    },
    async handleSearch() {
      if (!this.searchForm.creditCode) {
        this.$message.warning('请输入信用代码')
        return
      }
      this.graphLoading = true
      this.tableLoading = true
      try {
        const res = await getRelationGraph({ creditCode: this.searchForm.creditCode })
        if (res.data && res.data.nodes && res.data.nodes.length > 0) {
          this.isDemoData = false
          this.renderGraph(res.data.nodes, res.data.links)
          this.relationTable = res.data.relations || []
        } else {
          this.useMockData()
        }
      } catch (e) {
        this.useMockData()
      }
      this.graphLoading = false
      this.tableLoading = false
    },
    handleReset() {
      this.searchForm = { creditCode: '' }
      this.useMockData()
    },
    useMockData() {
      this.isDemoData = true
      this.renderGraph(this.mockNodes, this.mockLinks)
      this.relationTable = [
        { entity: '越南ABC公司', country: '越南', relationType: '股权关联', level: 1, riskLevel: '高风险', riskTags: '空壳公司,虚假贸易' },
        { entity: '凭祥达通物流', country: '中国', relationType: '物流合作', level: 1, riskLevel: '中风险', riskTags: '报关异常' },
        { entity: '泰国Siam Trading', country: '泰国', relationType: '转口贸易', level: 2, riskLevel: '中风险', riskTags: '价格偏离' },
        { entity: '柬埔寨Angkor Ltd', country: '柬埔寨', relationType: '仓储合作', level: 2, riskLevel: '高风险', riskTags: '资金异常,关联交易' },
        { entity: '东兴万通进出口', country: '中国', relationType: '贸易往来', level: 1, riskLevel: '低风险', riskTags: '' },
        { entity: '防城港恒达货运', country: '中国', relationType: '物流分包', level: 2, riskLevel: '中风险', riskTags: '单据不一致' },
        { entity: '印尼Nusa Express', country: '印尼', relationType: '供应链关联', level: 3, riskLevel: '低风险', riskTags: '' },
        { entity: '南宁聚源供应链', country: '中国', relationType: '股权关联', level: 1, riskLevel: '中风险', riskTags: '关联交易' },
        { entity: '马来西亚Borneo Trade', country: '马来西亚', relationType: '贸易往来', level: 2, riskLevel: '低风险', riskTags: '' }
      ]
    },
    initGraph() {
      this.useMockData()
    },
    renderGraph(nodes, links) {
      this.$nextTick(() => {
        if (!this.$refs.graphChart) return
        if (this.graphInstance) this.graphInstance.dispose()
        this.graphInstance = this.$echarts.init(this.$refs.graphChart)
        const categories = [{ name: '国内企业' }, { name: '境外企业' }]
        const colorMap = { '高风险': '#F56C6C', '中风险': '#E6A23C', '低风险': '#67C23A' }
        const chartNodes = nodes.map(n => ({
          name: n.name, category: n.category, symbolSize: n.symbolSize || 35,
          itemStyle: { color: colorMap[n.riskLevel] || '#409EFF' },
          label: { show: true, fontSize: 12, color: '#e0f0ff', fontWeight: 500 }
        }))
        const chartLinks = links.map(l => ({
          source: l.source, target: l.target,
          label: { show: true, formatter: l.relation, fontSize: 11, color: '#8ab4d0' },
          lineStyle: (l.lineStyle && l.lineStyle.color) ? { ...l.lineStyle, width: 1.8, opacity: 0.85 } : { color: '#5a9ac7', curveness: 0.2, width: 1.8, opacity: 0.85 }
        }))
        this.graphInstance.setOption({
          tooltip: {
            trigger: 'item',
            backgroundColor: '#0b1a30',
            borderColor: 'rgba(0,212,255,0.3)',
            textStyle: { color: '#d4eaff' },
            formatter: function(params) {
              if (params.dataType === 'node') return '<strong>' + params.name + '</strong><br/>类型: ' + categories[params.data.category].name
              return params.data.source + ' → ' + params.data.target
            }
          },
          legend: { data: categories.map(c => c.name), bottom: 10, textStyle: { color: '#a0c0dd', fontSize: 13 } },
          animationDuration: 1500,
          animationEasingUpdate: 'quinticInOut',
          series: [{
            type: 'graph', layout: 'force', data: chartNodes, links: chartLinks, categories: categories,
            roam: true, draggable: true,
            force: { repulsion: 350, edgeLength: [150, 280], gravity: 0.12 },
            label: { position: 'right', formatter: '{b}', fontSize: 12, color: '#e0f0ff' },
            edgeLabel: { fontSize: 11, color: '#8ab4d0' },
            emphasis: { focus: 'adjacency', lineStyle: { width: 3 } },
            lineStyle: { curveness: 0.2, opacity: 0.85 }
          }]
        }, true)
      })
    },
    getRiskTagType(level) {
      if (level === '高风险' || level === '红色预警') return 'danger'
      if (level === '中风险' || level === '橙色预警') return 'warning'
      if (level === '低风险' || level === '黄色关注') return 'success'
      return 'info'
    },
    async handleMultiLangSearch() {
      if (!this.multiLangForm.keyword) {
        this.$message.warning('请输入搜索关键词')
        return
      }
      this.multiLangLoading = true
      this.multiLangSearched = true
      try {
        const res = await this.$http.get('/multi-lang/search', { params: { keyword: this.multiLangForm.keyword } })
        const list = res.data?.list || res.data || []
        this.multiLangResults = list.map(item => ({
          nameZh: item.nameZh || item.name_zh || item.name || '',
          nameEn: item.nameEn || item.name_en || '',
          nameVi: item.nameVi || item.name_vi || '',
          creditCode: item.creditCode || item.credit_code || ''
        }))
      } catch {
        this.multiLangResults = []
      }
      this.multiLangLoading = false
    },
    selectMultiLangResult(row) {
      this.searchForm.creditCode = row.creditCode
      this.multiLangDialogVisible = false
      this.$nextTick(() => {
        this.handleSearch()
      })
    }
  }
}
</script>

<style scoped>
.relation-graph-page { padding: 0; }
.filter-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px 20px 5px; margin-bottom: 16px; }
.chart-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.chart-title { font-size: 15px; color: #c8e6ff; margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px solid rgba(58,111,247,0.08); font-weight: 600; }
.chart-container { width: 100%; height: 450px; }
.table-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px; }

/* 统一暗色表格 */
.relation-graph-page .el-table { background-color: #0c1a2e !important; }
.relation-graph-page .el-table::before { background-color: #1a3a5c !important; }
.relation-graph-page .el-table th.el-table__cell,
.relation-graph-page .el-table thead th {
  background-color: #0a1525 !important;
  color: #7eb8e0 !important;
  border-bottom: 2px solid #1a4a6e !important;
}
.relation-graph-page .el-table .el-table__body tr.el-table__row > td.el-table__cell,
.relation-graph-page .el-table td.el-table__cell {
  background-color: #0f2238 !important;
  color: #d4eaff !important;
  border-bottom: 1px solid #162d48 !important;
}
.relation-graph-page .el-table .el-table__body tr:hover > td.el-table__cell {
  background-color: #183654 !important;
}
.no-tag-hint { color: #5a7a9a; font-size: 12px; font-style: italic; }
</style>
