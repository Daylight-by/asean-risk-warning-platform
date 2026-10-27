<template>
  <div class="fraud-alert-page">
    <!-- 顶部统计卡片：调 /stats 接口，处置闭环核心数据 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="12" :md="6" v-for="item in statCards" :key="item.title">
        <div class="stat-card" :style="{ borderLeftColor: item.color }">
          <div class="stat-info">
            <p class="stat-label">{{ item.title }}</p>
            <p class="stat-value" :style="{ color: item.color }">{{ item.value }}</p>
            <p v-if="item.sub" class="stat-sub">{{ item.sub }}</p>
          </div>
          <div class="stat-icon" :style="{ color: item.color }">
            <i :class="item.icon"></i>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="filter-card">
      <el-form :model="filterForm" :inline="true" size="small">
        <el-form-item label="企业名称">
          <el-input v-model="filterForm.name" placeholder="请输入企业名称" clearable @clear="handleSearch"></el-input>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="filterForm.riskLevel" placeholder="全部等级" clearable @clear="handleSearch">
            <el-option label="红色预警" value="红色预警"></el-option>
            <el-option label="橙色预警" value="橙色预警"></el-option>
            <el-option label="黄色关注" value="黄色关注"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable @clear="handleSearch">
            <el-option label="待核实" value="待核实"></el-option>
            <el-option label="已处理" value="已处理"></el-option>
            <el-option label="已排除" value="已排除"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filterForm.sortField" style="width:128px" @change="onSortFieldChange">
            <el-option label="状态优先级" value="statusPriority"></el-option>
            <el-option label="创建时间" value="createTime"></el-option>
            <el-option label="处理时间" value="handleTime"></el-option>
          </el-select>
          <el-button
            size="small"
            :icon="filterForm.sortOrder === 'asc' ? 'el-icon-sort-up' : 'el-icon-sort-down'"
            style="margin-left:8px;"
            @click="toggleSortOrder"
          >{{ filterForm.sortOrder === 'asc' ? '升序' : '降序' }}</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="企业名称" width="140" show-overflow-tooltip></el-table-column>
        <el-table-column prop="fraudType" label="欺诈类型" width="120"></el-table-column>
        <el-table-column prop="probability" label="风险概率" width="90" align="center">
          <template slot-scope="scope">
            <span :style="{ color: getProbabilityColor(scope.row.probability), fontWeight: 'bold' }">
              {{ scope.row.probability }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险等级" width="95">
          <template slot-scope="scope">
            <el-tag :type="getRiskTagType(scope.row.riskLevel)" size="small" effect="dark">{{ scope.row.riskLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="预警内容" width="180" show-overflow-tooltip></el-table-column>
        <el-table-column prop="window" label="预测窗口" width="90" align="center"></el-table-column>
        <el-table-column prop="status" label="状态" width="85">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <!-- 处理记录：已处理行显示简短摘要，完整内容点"详情"看（避免 tooltip 定位错乱） -->
        <el-table-column label="处理记录" width="200">
          <template slot-scope="scope">
            <template v-if="scope.row.status === '已处理'">
              <span class="handle-record">
                <i class="el-icon-user-solid" style="color:#67C23A; margin-right:4px;"></i>{{ scope.row.handler || '-' }}
                <span style="color:#909399; margin-left:6px;">{{ formatTime(scope.row.handleTime) }}</span>
              </span>
            </template>
            <span v-else style="color:#909399; font-size:12px;">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <template v-if="scope.row.status === '待核实'">
              <el-button type="text" size="small" style="color: #F56C6C;" @click="openProcessDialog(scope.row)">处置</el-button>
              <el-button type="text" size="small" style="color: #67C23A;" @click="handleReject(scope.row)">排除</el-button>
            </template>
            <template v-else>
              <el-button type="text" size="small" style="color: #3a6ff7;" @click="openDetailDialog(scope.row)">详情</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          :current-page="page"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        ></el-pagination>
      </div>
    </div>

    <!-- 处置闭环对话框：选动作 + 填意见 + 自动取当前登录用户为处理人 -->
    <el-dialog
      title="欺诈预警处置"
      :visible.sync="processDialogVisible"
      width="520px"
      top="8vh"
      :close-on-click-modal="false"
    >
      <el-form ref="processForm" :model="processForm" :rules="processRules" label-width="90px" size="small">
        <el-form-item label="预警编号">
          <span style="color:#909399;">#{{ currentRow.id }} - {{ currentRow.name }}</span>
        </el-form-item>
        <el-form-item label="处置动作" prop="action">
          <el-select v-model="processForm.action" style="width: 100%;" placeholder="请选择处置动作">
            <el-option label="冻结结算" value="冻结结算">
              <span style="float:left;">冻结结算</span>
              <span style="float:right; color:#909399; font-size:12px;">暂停企业跨境结算权限</span>
            </el-option>
            <el-option label="降级信用" value="降级信用">
              <span style="float:left;">降级信用</span>
              <span style="float:right; color:#909399; font-size:12px;">下调企业信用等级</span>
            </el-option>
            <el-option label="发送整改通知" value="发送整改通知">
              <span style="float:left;">发送整改通知</span>
              <span style="float:right; color:#909399; font-size:12px;">要求企业限期整改</span>
            </el-option>
            <el-option label="核实无罪" value="核实无罪">
              <span style="float:left;">核实无罪</span>
              <span style="float:right; color:#909399; font-size:12px;">证据不足，解除预警</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="处理意见" prop="remark">
          <el-input
            v-model="processForm.remark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写处置依据、调查结论、后续跟进等"
          ></el-input>
        </el-form-item>
        <el-form-item label="处理人">
          <span style="color:#909399;">{{ processForm.handler }}（系统自动取当前登录用户）</span>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="processDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="processLoading" @click="submitProcess">确认处置</el-button>
      </span>
    </el-dialog>

    <!-- 详情对话框：展示完整预警信息 + 处置闭环四要素（已处理/已排除时） -->
    <el-dialog
      title="欺诈预警详情"
      :visible.sync="detailDialogVisible"
      width="640px"
      top="6vh"
    >
      <div v-if="detailRow" class="detail-content">
        <div class="detail-section-title">预警信息</div>
        <el-row :gutter="20">
          <el-col :span="12"><div class="detail-item"><span class="detail-label">企业名称：</span>{{ detailRow.name }}</div></el-col>
          <el-col :span="12"><div class="detail-item"><span class="detail-label">欺诈类型：</span>{{ detailRow.fraudType }}</div></el-col>
          <el-col :span="12"><div class="detail-item"><span class="detail-label">风险概率：</span>
            <span :style="{ color: getProbabilityColor(detailRow.probability), fontWeight: 'bold' }">{{ detailRow.probability }}</span>
          </div></el-col>
          <el-col :span="12"><div class="detail-item"><span class="detail-label">风险等级：</span>
            <el-tag :type="getRiskTagType(detailRow.riskLevel)" size="small" effect="dark">{{ detailRow.riskLevel }}</el-tag>
          </div></el-col>
          <el-col :span="12"><div class="detail-item"><span class="detail-label">预测窗口：</span>{{ detailRow.window || '-' }}</div></el-col>
          <el-col :span="12"><div class="detail-item"><span class="detail-label">状态：</span>
            <el-tag :type="getStatusType(detailRow.status)" size="small">{{ detailRow.status }}</el-tag>
          </div></el-col>
        </el-row>
        <div class="detail-item detail-full">
          <span class="detail-label">预警内容：</span>{{ detailRow.content }}
        </div>

        <template v-if="detailRow.status === '已处理' || detailRow.status === '已排除'">
          <el-divider></el-divider>
          <div class="detail-section-title">处置记录</div>
          <el-row :gutter="20">
            <el-col :span="12"><div class="detail-item"><span class="detail-label">处置动作：</span>
              <el-tag size="small" :type="detailRow.status === '已处理' ? 'danger' : 'success'">{{ detailRow.handleAction || (detailRow.status === '已排除' ? '排除' : '-') }}</el-tag>
            </div></el-col>
            <el-col :span="12"><div class="detail-item"><span class="detail-label">处理人：</span>{{ detailRow.handler || '-' }}</div></el-col>
            <el-col :span="24"><div class="detail-item"><span class="detail-label">处理时间：</span>{{ formatTime(detailRow.handleTime) }}</div></el-col>
            <el-col :span="24"><div class="detail-item detail-full">
              <span class="detail-label">处理意见：</span>
              <div class="detail-remark">{{ detailRow.handleResult || '（无）' }}</div>
            </div></el-col>
          </el-row>
        </template>
      </div>
      <span slot="footer">
        <el-button type="primary" @click="detailDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getFraudAlertList, updateFraudAlertStatus, processFraudAlert, getFraudAlertStats } from '@/api'

export default {
  name: 'FraudAlert',
  data() {
    return {
      filterForm: { name: '', riskLevel: '', status: '', sortField: 'statusPriority', sortOrder: 'asc' },
      // 四张统计卡：今日新增 / 待核实 / 已处理 / 处理率（来自 /stats 接口）
      statCards: [
        { title: '今日新增', value: 0, icon: 'el-icon-plus', color: '#3a6ff7' },
        { title: '待核实', value: 0, icon: 'el-icon-warning-outline', color: '#F56C6C' },
        { title: '已处理', value: 0, icon: 'el-icon-check', color: '#67C23A' },
        { title: '处理率', value: '0%', sub: '已处理/(已处理+待核实)', icon: 'el-icon-data-line', color: '#E6A23C' }
      ],
      tableData: [],
      allData: [],
      rawTotal: 0,
      loading: false,
      page: 1,
      pageSize: 10,
      total: 0,
      // 处置对话框
      processDialogVisible: false,
      processLoading: false,
      currentRow: {},
      processForm: {
        id: null,
        action: '',
        remark: '',
        handler: sessionStorage.getItem('username') || ''
      },
      processRules: {
        action: [{ required: true, message: '请选择处置动作', trigger: 'change' }],
        remark: [{ required: true, message: '请填写处理意见', trigger: 'blur' }]
      },
      // 详情对话框
      detailDialogVisible: false,
      detailRow: null
    }
  },
  created() {
    this.loadData()
    this.loadStats()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getFraudAlertList({ page: this.page, pageSize: this.pageSize, sortField: this.filterForm.sortField, sortOrder: this.filterForm.sortOrder })
        if (res.data && res.data.list) {
          this.allData = res.data.list
          this.rawTotal = res.data.total || res.data.list.length
          this.total = this.rawTotal
          this.tableData = this.allData
        } else {
          this.allData = []
          this.tableData = []
          this.total = 0
        }
      } catch (e) {
        this.allData = []
        this.tableData = []
        this.total = 0
      }
      this.loading = false
    },
    async loadStats() {
      try {
        const res = await getFraudAlertStats()
        const d = res.data || {}
        this.statCards[0].value = d.todayNew || 0
        this.statCards[1].value = d.pending || 0
        this.statCards[2].value = d.processed || 0
        this.statCards[3].value = (d.processingRate || 0) + '%'
      } catch (e) {
        // 静默失败，保留默认值
      }
    },
    filterData() {
      let data = [...this.allData]
      if (this.filterForm.name) data = data.filter(item => item.name && item.name.includes(this.filterForm.name))
      if (this.filterForm.riskLevel) data = data.filter(item => item.riskLevel === this.filterForm.riskLevel)
      if (this.filterForm.status) data = data.filter(item => item.status === this.filterForm.status)
      // 无筛选时用后端真实总数(rawTotal)，保证分页正确；有筛选时按当前页筛选结果计数
      const hasFilter = this.filterForm.name || this.filterForm.riskLevel || this.filterForm.status
      this.total = hasFilter ? data.length : this.rawTotal
      // 搜索/筛选后重置到第1页，避免切片越界
      this.page = 1
      this.tableData = data.slice(0, this.pageSize)
    },
    async handleSearch() {
      this.page = 1
      await this.loadData()
      this.filterData()
    },
    async handleReset() {
      this.filterForm = { name: '', riskLevel: '', status: '', sortField: 'statusPriority', sortOrder: 'asc' }
      this.page = 1
      await this.loadData()
      this.filterData()
    },

    onSortFieldChange() {
      // 状态优先级默认升序（待核实最上）；时间类默认降序（最新/最近在前）
      this.filterForm.sortOrder = this.filterForm.sortField === 'statusPriority' ? 'asc' : 'desc'
      this.loadData()
    },

    toggleSortOrder() {
      this.filterForm.sortOrder = this.filterForm.sortOrder === 'asc' ? 'desc' : 'asc'
      this.loadData()
    },
    handlePageChange(p) { this.page = p; this.loadData() },
    handleSizeChange(size) { this.pageSize = size; this.page = 1; this.loadData() },

    // 打开处置对话框
    openProcessDialog(row) {
      this.currentRow = { ...row }
      this.processForm = {
        id: row.id,
        action: '',
        remark: '',
        handler: sessionStorage.getItem('username') || ''
      }
      this.processDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.processForm) this.$refs.processForm.clearValidate()
      })
    },
    openDetailDialog(row) {
      this.detailRow = { ...row }
      this.detailDialogVisible = true
    },
    // 提交处置：调 /process 接口
    async submitProcess() {
      try { await this.$refs.processForm.validate() } catch { return }
      this.processLoading = true
      try {
        await processFraudAlert({
          id: this.processForm.id,
          handler: this.processForm.handler,
          action: this.processForm.action,
          remark: this.processForm.remark
        })
        this.$message.success('已处置：' + this.processForm.action)
        this.processDialogVisible = false
        // 处置后刷新列表+统计
        await this.loadData()
        await this.loadStats()
      } catch (e) {
        this.$message.error(e?.response?.data?.message || e.message || '处置失败')
      } finally {
        this.processLoading = false
      }
    },

    async handleReject(row) {
      try {
        await this.$confirm('确认排除该欺诈预警（标记为误报）？', '排除操作', { type: 'warning' })
      } catch { return }
      try {
        await updateFraudAlertStatus(row.id, '已排除')
        row.status = '已排除'
        this.$message.success('已排除该预警')
        await this.loadStats()
      } catch (e) {
        this.$message.error(e?.response?.data?.message || e.message || '操作失败')
      }
    },
    formatTime(t) {
      if (!t) return '-'
      // 处理 LocalDateTime 字符串 "2026-07-19T10:21:26" / "2026-07-19 10:21:26"
      const s = String(t).replace('T', ' ')
      return s.length >= 16 ? s.substring(0, 16) : s
    },
    getRiskTagType(level) {
      if (level === '红色预警') return 'danger'
      if (level === '橙色预警') return 'warning'
      if (level === '黄色关注') return ''
      return 'info'
    },
    getStatusType(status) {
      if (status === '待核实') return 'info'
      if (status === '已处理') return 'success'
      if (status === '已排除') return ''
      return 'info'
    },
    getProbabilityColor(prob) {
      const num = parseInt(prob)
      if (num >= 80) return '#F56C6C'
      if (num >= 60) return '#E6A23C'
      return '#67C23A'
    }
  }
}
</script>

<style scoped>
.fraud-alert-page { padding: 0; }
.stat-cards { margin-bottom: 16px; }
.stat-card { background: linear-gradient(135deg, rgba(58,111,247,0.06) 0%, rgba(0,120,200,0.02) 100%); border: 1px solid rgba(58,111,247,0.12); border-radius: 8px; padding: 20px; display: flex; align-items: center; justify-content: space-between; border-left: 4px solid #3a6ff7; transition: all .25s; min-height: 96px; }
.stat-card:hover { transform: translateY(-2px); border-color: rgba(58,111,247,0.3); box-shadow: 0 4px 16px rgba(58,111,247,0.08); }
.stat-label { font-size: 13px; color: rgba(255,255,255,0.5); margin-bottom: 6px; }
.stat-value { font-size: 28px; font-weight: bold; line-height: 1.2; }
.stat-sub { font-size: 11px; color: rgba(255,255,255,0.35); margin-top: 4px; }
.stat-icon { font-size: 42px; opacity: .15; }
.filter-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px 20px 5px; margin-bottom: 16px; }
.table-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px; max-height: calc(100vh - 280px); overflow-y: auto; }
.pagination-wrap { margin-top: 15px; text-align: right; }
.handle-record { font-size: 12px; color: #d4eaff; cursor: help; white-space: nowrap; }

/* 统一暗色表格 */
.fraud-alert-page .el-table { background-color: #0c1a2e !important; }
.fraud-alert-page .el-table::before { background-color: #1a3a5c !important; }
.fraud-alert-page .el-table th.el-table__cell,
.fraud-alert-page .el-table thead th {
  background-color: #0a1525 !important;
  color: #7eb8e0 !important;
  border-bottom: 2px solid #1a4a6e !important;
}
.fraud-alert-page .el-table .el-table__body tr.el-table__row > td.el-table__cell,
.fraud-alert-page .el-table td.el-table__cell {
  background-color: #0f2238 !important;
  color: #d4eaff !important;
  border-bottom: 1px solid #162d48 !important;
}
.fraud-alert-page .el-table .el-table__body tr:hover > td.el-table__cell {
  background-color: #183654 !important;
}

/* 详情对话框：暗色适配 */
.fraud-alert-page .detail-content { color: #d4eaff; }
.fraud-alert-page .detail-section-title {
  font-size: 13px; color: #7eb8e0; font-weight: 600;
  letter-spacing: 1px; margin-bottom: 12px;
  border-left: 3px solid #3a6ff7; padding-left: 8px;
}
.fraud-alert-page .detail-item {
  font-size: 13px; line-height: 1.9; margin-bottom: 4px;
  color: #d4eaff;
}
.fraud-alert-page .detail-item .detail-label {
  color: #7eb8e0; margin-right: 4px;
}
.fraud-alert-page .detail-item.detail-full { width: 100%; }
.fraud-alert-page .detail-remark {
  background: rgba(58,111,247,0.06);
  border: 1px solid rgba(58,111,247,0.18);
  border-radius: 6px;
  padding: 10px 12px;
  margin-top: 4px;
  white-space: pre-wrap;
  word-break: break-all;
  color: #d4eaff;
}
</style>
