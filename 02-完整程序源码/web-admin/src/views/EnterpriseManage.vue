<template>
  <div class="enterprise-page">
    <!-- 搜索筛选区 -->
    <div class="filter-card">
      <el-form :model="filterForm" :inline="true" size="small">
        <el-form-item label="企业名称">
          <el-input
            v-model="filterForm.name"
            placeholder="请输入企业名称"
            clearable
            @clear="handleSearch"
          ></el-input>
        </el-form-item>
        <el-form-item label="口岸">
          <el-select v-model="filterForm.port" placeholder="全部口岸" clearable @clear="handleSearch">
            <el-option label="凭祥" value="凭祥"></el-option>
            <el-option label="东兴" value="东兴"></el-option>
            <el-option label="防城港" value="防城港"></el-option>
            <el-option label="钦州" value="钦州"></el-option>
            <el-option label="北海" value="北海"></el-option>
            <el-option label="水口" value="水口"></el-option>
            <el-option label="龙邦" value="龙邦"></el-option>
            <el-option label="硕龙" value="硕龙"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="行业">
          <el-select v-model="filterForm.industry" placeholder="全部行业" clearable @clear="handleSearch">
            <el-option label="农产品贸易" value="农产品贸易"></el-option>
            <el-option label="电子产品" value="电子产品"></el-option>
            <el-option label="纺织品" value="纺织品"></el-option>
            <el-option label="机械设备" value="机械设备"></el-option>
            <el-option label="化工产品" value="化工产品"></el-option>
            <el-option label="建材" value="建材"></el-option>
            <el-option label="日用品" value="日用品"></el-option>
            <el-option label="物流运输" value="物流运输"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="filterForm.riskLevel" placeholder="全部等级" clearable @clear="handleSearch">
            <el-option label="高风险" value="高风险"></el-option>
            <el-option label="中风险" value="中风险"></el-option>
            <el-option label="低风险" value="低风险"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 企业列表 -->
    <div class="table-card">
      <el-table
        :data="tableData"
        style="width: 100%"
        v-loading="loading"
        :default-sort="{ prop: 'riskScore', order: 'descending' }"
      >
        <el-table-column prop="name" label="企业名称" min-width="150" show-overflow-tooltip></el-table-column>
        <el-table-column prop="port" label="口岸" width="80"></el-table-column>
        <el-table-column prop="industry" label="行业" width="100"></el-table-column>
        <el-table-column prop="registerCapital" label="注册资本" width="100"></el-table-column>
        <el-table-column prop="tradeVolume" label="年交易额" width="110" sortable></el-table-column>
        <el-table-column prop="riskScore" label="风险评分" width="90" sortable>
          <template slot-scope="scope">
            <span :style="{ color: getRiskColor(scope.row.riskScore) }" class="score-text">
              {{ scope.row.riskScore }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="风险等级" width="90">
          <template slot-scope="scope">
            <el-tag
              :type="getRiskTagType(scope.row.level)"
              size="small"
            >{{ scope.row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="监管状态" width="90">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === '重点监控' ? 'danger' : 'success'" size="small">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="showDetail(scope.row)">风控详情</el-button>
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

    <!-- 风控详情弹窗 -->
    <el-dialog
      title="企业风控详情"
      :visible.sync="detailVisible"
      width="680px"
      top="5vh"
    >
      <div v-if="currentEnterprise" class="detail-content">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="detail-item">
              <span class="detail-label">企业名称：</span>
              <span>{{ currentEnterprise.name }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item">
              <span class="detail-label">统一信用代码：</span>
              <span>{{ currentEnterprise.creditCode }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item">
              <span class="detail-label">所属口岸：</span>
              <span>{{ currentEnterprise.port }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item">
              <span class="detail-label">所属行业：</span>
              <span>{{ currentEnterprise.industry }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item">
              <span class="detail-label">注册资本：</span>
              <span>{{ currentEnterprise.registerCapital }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item">
              <span class="detail-label">年交易额：</span>
              <span>{{ currentEnterprise.tradeVolume }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item">
              <span class="detail-label">风险评分：</span>
              <span :style="{ color: getRiskColor(currentEnterprise.riskScore), fontWeight: 'bold', fontSize: '16px' }">
                {{ currentEnterprise.riskScore }}
              </span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item">
              <span class="detail-label">风险等级：</span>
              <el-tag :type="getRiskTagType(currentEnterprise.level)" size="small">
                {{ currentEnterprise.level }}
              </el-tag>
            </div>
          </el-col>
        </el-row>
        <el-divider></el-divider>
        <h4>风险指标明细</h4>
        <el-table :data="riskIndicators" size="small" style="margin-top: 10px;">
          <el-table-column prop="name" label="指标名称" width="160"></el-table-column>
          <el-table-column prop="value" label="数值" width="100"></el-table-column>
          <el-table-column prop="threshold" label="阈值" width="100"></el-table-column>
          <el-table-column label="状态" width="100">
            <template slot-scope="scope">
              <el-tag
                :type="scope.row.exceed ? 'danger' : 'success'"
                size="mini"
              >{{ scope.row.exceed ? '超限' : '正常' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="desc" label="说明"></el-table-column>
        </el-table>
        <el-divider></el-divider>
        <h4>预警历史</h4>
        <el-table :data="warningHistory" size="small" style="margin-top: 10px;">
          <el-table-column prop="time" label="时间" width="160"></el-table-column>
          <el-table-column prop="type" label="预警类型" width="120"></el-table-column>
          <el-table-column prop="level" label="级别" width="80">
            <template slot-scope="scope">
              <el-tag :type="getRiskTagType(scope.row.level)" size="mini">{{ scope.row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="desc" label="描述"></el-table-column>
        </el-table>
        <el-divider></el-divider>
        <h4>欺诈时序预测</h4>
        <div v-loading="fraudLoading" class="fraud-prediction-section">
          <el-row :gutter="12">
            <el-col :span="6" v-for="item in fraudPrediction" :key="item.name">
              <div class="fraud-card">
                <div class="fraud-card-name">{{ item.name }}</div>
                <div class="fraud-card-level">
                  <el-tag :type="getRiskTagType(item.level)" effect="dark" size="small">{{ item.level }}</el-tag>
                </div>
                <div class="fraud-card-score">
                  <span :style="{ color: getRiskColor(item.score) }">{{ item.score }}</span>
                  <span class="fraud-card-unit">分</span>
                </div>
                <div class="fraud-card-desc">{{ item.description }}</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="detailVisible = false">关 闭</el-button>
        <el-button type="warning" icon="el-icon-document" @click="generateComplianceReport">生成合规报告</el-button>
        <el-button type="primary" @click="detailVisible = false">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'EnterpriseManage',
  data() {
    return {
      filterForm: {
        name: '',
        port: '',
        industry: '',
        riskLevel: ''
      },
      tableData: [],
      allData: [],
      loading: false,
      page: 1,
      pageSize: 10,
      total: 0,
      detailVisible: false,
      currentEnterprise: null,
      riskIndicators: [],
      warningHistory: [],
      fraudPrediction: [],
      fraudLoading: false
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        // 后端 /enterprise/list 支持分页，但本页筛选(name/port/industry/riskLevel)为前端实现，
        // 故一次性拉取全量(上限200)后在 filterData() 中做前端筛选+分页，避免数据集>100时后续页空白
        const res = await this.$http.get('/enterprise/list', { params: { page: 1, pageSize: 200 } })
        const list = res.data?.list || []
        this.allData = list.map(e => {
          const riskScore = e.riskScore != null ? e.riskScore : null
          let level = '未知'
          if (riskScore != null) {
            level = riskScore >= 70 ? '高风险' : riskScore >= 40 ? '中风险' : '低风险'
          }
          return {
            id: e.id,
            name: e.name,
            creditCode: e.creditCode,
            port: e.port || '',
            industry: e.industry || '',
            registerCapital: e.registerCapital != null ? e.registerCapital + '万' : '-',
            tradeVolume: e.tradeVolume != null ? Number(e.tradeVolume).toLocaleString('zh-CN', {minimumFractionDigits: 0, maximumFractionDigits: 0}) + '万' : '-',
            riskScore: riskScore != null ? riskScore : '-',
            level: level,
            status: riskScore != null && riskScore >= 70 ? '重点监控' : '正常监管'
          }
        })
      } catch { this.allData = [] }
      this.filterData()
      this.loading = false
    },

    filterData() {
      let data = [...this.allData]

      if (this.filterForm.name) {
        data = data.filter(item => item.name.includes(this.filterForm.name))
      }
      if (this.filterForm.port) {
        data = data.filter(item => item.port === this.filterForm.port)
      }
      if (this.filterForm.industry) {
        data = data.filter(item => item.industry === this.filterForm.industry)
      }
      if (this.filterForm.riskLevel) {
        data = data.filter(item => item.level === this.filterForm.riskLevel)
      }

      this.total = data.length
      const start = (this.page - 1) * this.pageSize
      this.tableData = data.slice(start, start + this.pageSize)
    },

    handleSearch() {
      this.page = 1
      this.loadData()
    },

    handleReset() {
      this.filterForm = { name: '', port: '', industry: '', riskLevel: '' }
      this.page = 1
      this.loadData()
    },

    handlePageChange(page) {
      this.page = page
      this.filterData()
    },

    showDetail(row) {
      this.currentEnterprise = row
      this.detailVisible = true

      // 风险指标：基于企业真实综合评分派生（非随机），评分越低各维度风险越高
      const rs = typeof row.riskScore === 'number' ? row.riskScore : 75
      const dr = Math.max(0, Math.min(100, 100 - rs)) // 风险偏离度
      this.riskIndicators = [
        { name: '交易异常率', value: (dr * 0.3).toFixed(1) + '%', threshold: '15%', exceed: dr * 0.3 > 15, desc: '基于综合评分推算的异常交易占比' },
        { name: '资金流水波动率', value: (dr * 0.5).toFixed(1) + '%', threshold: '30%', exceed: dr * 0.5 > 30, desc: '基于综合评分推算的资金变动幅度' },
        { name: '关联企业风险传导', value: Math.round(dr), threshold: '60', exceed: dr > 60, desc: '关联企业风险指数（综合评分反向派生）' },
        { name: '报关数据一致性', value: rs + '%', threshold: '90%', exceed: rs < 90, desc: '报关单与物流数据匹配度（综合评分）' },
        { name: '税务合规评分', value: Math.round(rs), threshold: '80', exceed: rs < 80, desc: '税务申报及缴纳合规性（综合评分）' }
      ]

      // 预警历史：调后端 /warning/list 真实数据，按企业过滤
      this.loadWarningHistory(row.creditCode)

      // 调用欺诈预测API
      this.loadFraudPrediction(row.creditCode)
    },

    async loadWarningHistory(creditCode) {
      try {
        const res = await this.$http.get('/warning/list', { params: { page: 1, pageSize: 100 } })
        const list = res.data && res.data.list ? res.data.list : []
        this.warningHistory = list
          .filter(w => !creditCode || w.creditCode === creditCode || w.enterpriseId === creditCode)
          .slice(0, 5)
          .map(w => ({
            time: w.createTime ? new Date(w.createTime).toISOString().slice(0, 16).replace('T', ' ') : '—',
            type: w.warningType || '—',
            level: w.warningLevel || '—',
            desc: w.warningContent || w.content || '—'
          }))
      } catch {
        this.warningHistory = []
      }
    },

    async loadFraudPrediction(creditCode) {
      this.fraudLoading = true
      try {
        const res = await this.$http.get('/enterprise/fraud-prediction/' + creditCode)
        // 后端 FraudPredictionService.predictFraudRisk 返回字段：
        // valueFluctuation / valueFluctuationRisk / countryChanges / countryChangeRisk
        // settlementAnomaly / settlementAnomalyRisk / totalFraudRisk / fraudRiskLevel
        const d = (res && res.data) || {}
        const norm = (lv) => {
          if (lv === '高风险' || lv === '高') return '高风险'
          if (lv === '中风险' || lv === '中') return '中风险'
          return '低风险'
        }
        const num = (v) => (typeof v === 'number' ? Math.round(v * 100) / 100 : 0)
        this.fraudPrediction = [
          {
            name: '货值波动风险',
            score: num(d.valueFluctuation),
            level: norm(d.valueFluctuationRisk),
            description: '月均货值波动 ' + num(d.valueFluctuation) + '%'
          },
          {
            name: '合作国变更风险',
            // 后端返回的是贸易伙伴变更次数(整数)，转成 0-100 评分便于统一展示
            score: Math.min(100, (d.countryChanges || 0) * 10),
            level: norm(d.countryChangeRisk),
            description: '近6个月变更 ' + (d.countryChanges || 0) + ' 个贸易伙伴'
          },
          {
            name: '结算流水异常',
            score: num(d.settlementAnomaly),
            level: norm(d.settlementAnomalyRisk),
            description: '结算变异系数 ' + num(d.settlementAnomaly) + '%'
          },
          {
            name: '综合欺诈风险',
            score: num(d.totalFraudRisk),
            level: norm(d.fraudRiskLevel),
            description: '未来3个月综合风险 ' + num(d.totalFraudRisk) + '%'
          }
        ]
      } catch (e) {
        console.error('欺诈时序预测加载失败', e)
        this.fraudPrediction = [
          { name: '货值波动风险', score: 0, level: '低风险', description: '数据获取失败，请稍后重试' },
          { name: '合作国变更风险', score: 0, level: '低风险', description: '数据获取失败，请稍后重试' },
          { name: '结算流水异常', score: 0, level: '低风险', description: '数据获取失败，请稍后重试' },
          { name: '综合欺诈风险', score: 0, level: '低风险', description: '数据获取失败，请稍后重试' }
        ]
      }
      this.fraudLoading = false
    },

    async generateComplianceReport() {
      if (!this.currentEnterprise) return
      try {
        await this.$http.post('/enterprise/compliance-report', {
          creditCode: this.currentEnterprise.creditCode,
          targetCountry: ''
        })
        this.$message.success('合规报告已生成')
        this.$router.push('/compliance-report/' + this.currentEnterprise.creditCode)
      } catch (e) {
        this.$message.error('合规报告生成失败：' + (e.response?.data?.message || e.message || '请稍后重试'))
      }
    },

    getRiskColor(score) {
      if (score == null || score === '-') return '#909399'
      if (score >= 70) return '#F56C6C'
      if (score >= 40) return '#E6A23C'
      return '#67C23A'
    },

    getRiskTagType(level) {
      if (level === '高风险') return 'danger'
      if (level === '中风险') return 'warning'
      return 'success'
    }
  }
}
</script>

<style scoped>
.enterprise-page {
  padding: 0;
}

.filter-card {
  background: rgba(0,20,45,0.6);
  border: 1px solid rgba(58,111,247,0.1);
  border-radius: 8px;
  padding: 20px 20px 5px;
  margin-bottom: 16px;
}

.table-card {
  background: rgba(0,20,45,0.6);
  border: 1px solid rgba(58,111,247,0.1);
  border-radius: 8px;
  padding: 20px;
}

.score-text {
  font-weight: bold;
  font-size: 15px;
}

.pagination-wrap {
  margin-top: 15px;
  text-align: right;
}

.detail-content {
  padding: 10px 0;
}

.detail-item {
  margin-bottom: 14px;
  font-size: 14px;
  color: #c8e6ff;
}

.detail-label {
  color: #8899aa;
  display: inline-block;
  width: 100px;
}

/* ====== 统一暗色表格（::v-deep 强穿透）====== */
.enterprise-page .el-table { background-color: #0c1a2e !important; }
.enterprise-page .el-table::before { background-color: #1a3a5c !important; }
.enterprise-page .el-table th.el-table__cell,
.enterprise-page .el-table thead th {
  background-color: #0a1525 !important;
  color: #7eb8e0 !important;
  border-bottom: 2px solid #1a4a6e !important;
}
.enterprise-page .el-table .el-table__body tr.el-table__row > td.el-table__cell,
.enterprise-page .el-table td.el-table__cell {
  background-color: #0f2238 !important;
  color: #d4eaff !important;
  border-bottom: 1px solid #162d48 !important;
}
.enterprise-page .el-table .el-table__body tr:hover > td.el-table__cell {
  background-color: #183654 !important;
}
.enterprise-page .el-table .el-table__empty-block {
  background-color: #0c1a2e !important;
}

/* 欺诈预测卡片 */
.fraud-prediction-section { margin-top: 10px; }
.fraud-card {
  background: rgba(0,20,45,0.5);
  border: 1px solid rgba(58,111,247,0.1);
  border-radius: 6px;
  padding: 12px;
  text-align: center;
}
.fraud-card-name { color: #8899aa; font-size: 12px; margin-bottom: 6px; }
.fraud-card-level { margin-bottom: 6px; }
.fraud-card-score { font-size: 22px; font-weight: bold; margin-bottom: 4px; }
.fraud-card-unit { font-size: 12px; color: #6a8aa8; margin-left: 2px; }
.fraud-card-desc { color: #6a8aa8; font-size: 11px; line-height: 1.4; }
</style>
