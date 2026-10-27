Exit code: 0
Wall time: 2.3 seconds
Output:
<template>
  <div class="warning-page">
    <!-- 筛选区 -->
    <div class="filter-card">
      <el-form :model="filterForm" :inline="true" size="small">
        <el-form-item label="预警类型">
          <el-select v-model="filterForm.type" placeholder="全部类型" clearable @clear="handleSearch">
            <el-option label="交易异常" value="交易异常"></el-option>
            <el-option label="票据异常" value="票据异常"></el-option>
            <el-option label="关联风险" value="关联风险"></el-option>
            <el-option label="税务风险" value="税务风险"></el-option>
            <el-option label="合规风险" value="合规风险"></el-option>
            <el-option label="物流异常" value="物流异常"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="filterForm.level" placeholder="全部等级" clearable @clear="handleSearch">
            <el-option label="高风险" value="高风险"></el-option>
            <el-option label="中风险" value="中风险"></el-option>
            <el-option label="低风险" value="低风险"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd"
            @change="handleSearch"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable @clear="handleSearch">
            <el-option label="未处理" value="未处理"></el-option>
            <el-option label="处理中" value="处理中"></el-option>
            <el-option label="已处理" value="已处理"></el-option>
            <el-option label="已关闭" value="已关闭"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filterForm.sortField" style="width:128px" @change="onSortFieldChange">
            <el-option label="状态优先级" value="statusPriority"></el-option>
            <el-option label="预警时间" value="createTime"></el-option>
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

    <!-- 预警列表 -->
    <div class="table-card">
      <el-table
        :data="tableData"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column type="index" label="序号" width="60"></el-table-column>
        <el-table-column prop="enterpriseName" label="企业名称" width="140" show-overflow-tooltip></el-table-column>
        <el-table-column prop="type" label="预警类型" width="100">
          <template slot-scope="scope">
            <el-tag size="small" type="info">{{ scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="风险等级" width="100">
          <template slot-scope="scope">
            <el-tag
              :type="getRiskTagType(scope.row.level)"
              size="small"
            >{{ scope.row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="预警标题" width="180" show-overflow-tooltip></el-table-column>
        <el-table-column prop="source" label="检测源" width="110">
          <template slot-scope="scope">
            <el-tag :type="scope.row.source === '系统自动' ? '' : 'info'" size="small">
              {{ scope.row.source }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="预警时间" width="160"></el-table-column>
        <el-table-column prop="status" label="处理状态" width="100">
          <template slot-scope="scope">
            <el-tag
              :type="getStatusTagType(scope.row.status)"
              size="small"
            >{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="showDetail(scope.row)">详情</el-button>
            <el-button
              v-if="scope.row.status === '未处理'"
              type="text"
              size="small"
              style="color: #F56C6C;"
              @click="handleProcess(scope.row)"
            >处理</el-button>
            <el-button
              v-if="scope.row.status === '已处理'"
              type="text"
              size="small"
              style="color: #67C23A;"
              @click="handleClose(scope.row)"
            >关闭</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog
      title="预警详情"
      :visible.sync="detailVisible"
      width="580px"
      top="5vh"
    >
      <div v-if="currentWarning" class="detail-content">
        <div class="detail-item">
          <span class="detail-label">预警编号：</span>
          <span>{{ currentWarning.id }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">企业名称：</span>
          <span>{{ currentWarning.enterpriseName }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">预警类型：</span>
          <el-tag size="small">{{ currentWarning.type }}</el-tag>
        </div>
        <div class="detail-item">
          <span class="detail-label">风险等级：</span>
          <el-tag :type="getRiskTagType(currentWarning.level)" size="small">{{ currentWarning.level }}</el-tag>
        </div>
        <div class="detail-item">
          <span class="detail-label">预警标题：</span>
          <span>{{ currentWarning.title }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">检测源：</span>
          <span>{{ currentWarning.source }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">预警时间：</span>
          <span>{{ currentWarning.time }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">处理状态：</span>
          <el-tag :type="getStatusTagType(currentWarning.status)" size="small">{{ currentWarning.status }}</el-tag>
        </div>
        <template v-if="currentWarning.status === '已处理' || currentWarning.status === '已关闭'">
          <el-divider></el-divider>
          <div class="detail-item">
            <span class="detail-label">处理人：</span>
            <span>{{ currentWarning.handler || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">处理时间：</span>
            <span>{{ currentWarning.handleTime || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">处理说明：</span>
          </div>
          <p class="detail-desc">{{ currentWarning.handleResult || '（无）' }}</p>
        </template>
        <el-divider></el-divider>
        <div class="detail-item">
          <span class="detail-label">详情描述：</span>
        </div>
        <p class="detail-desc">{{ currentWarning.description }}</p>
      </div>
      <span slot="footer">
        <el-button @click="detailVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 处理对话框：填写处理说明，提交后直接变为已处理 -->
    <el-dialog
      title="预警处理"
      :visible.sync="processDialogVisible"
      width="520px"
      top="8vh"
      :close-on-click-modal="false"
    >
      <el-form ref="processForm" :model="processForm" :rules="processRules" label-width="90px" size="small">
        <el-form-item label="预警编号">
          <span style="color:#909399;">#{{ currentRow.id }} - {{ currentRow.enterpriseName }}</span>
        </el-form-item>
        <el-form-item label="预警标题">
          <span style="color:#909399;">{{ currentRow.title }}</span>
        </el-form-item>
        <el-form-item label="处理说明" prop="handleResult">
          <el-input
            v-model="processForm.handleResult"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写处理措施、调查结论、后续跟进等"
          ></el-input>
        </el-form-item>
        <el-form-item label="处理人">
          <span style="color:#909399;">{{ processForm.handler }}（系统自动取当前登录用户）</span>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="processDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="processLoading" @click="submitProcess">确认处理</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'WarningManage',
  data() {
    return {
      filterForm: {
        type: '',
        level: '',
        dateRange: null,
        status: '',
        sortField: 'statusPriority',
        sortOrder: 'asc'
      },
      tableData: [],
      allData: [],
      loading: false,
      page: 1,
      pageSize: 10,
      total: 0,
      detailVisible: false,
      currentWarning: null,
      // 处理对话框
      processDialogVisible: false,
      processLoading: false,
      currentRow: {},
      processForm: {
        id: null,
        handleResult: '',
        handler: sessionStorage.getItem('username') || ''
      },
      processRules: {
        handleResult: [{ required: true, message: '请填写处理说明', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await this.$http.get('/warning/list', { params: { page: this.page, pageSize: this.pageSize, type: this.filterForm.type, level: this.filterForm.level, status: this.filterForm.status, startDate: this.filterForm.dateRange ? this.filterForm.dateRange[0] : '', endDate: this.filterForm.dateRange ? this.filterForm.dateRange[1] : '', sortField: this.filterForm.sortField, sortOrder: this.filterForm.sortOrder } })
        const list = res.data?.list || []
        this.tableData = list.map(w => {
          // 【面试】createTime兼容毫秒时间戳(number)和ISO字符串两种格式
          let timeStr = '-'
          if (w.createTime) {
            if (typeof w.createTime === 'number') {
              const d = new Date(w.createTime)
              timeStr = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
            } else {
              timeStr = String(w.createTime).replace('T', ' ').substring(0, 16)
            }
          }
          return {
            id: w.id,
            enterpriseName: w.enterpriseName || '-',
            type: w.warningType || '-',
            level: w.warningLevel || '-',
            title: (w.warningContent || w.content) ? (w.warningContent || w.content || '').substring(0, 30) + '...' : '-',
            source: w.dataSource || '系统自动',
            time: timeStr,
            status: w.status || '未处理',
            handler: w.handler || '-',
            handleTime: w.handleTime ? this.formatTime(w.handleTime) : '-',
            handleResult: w.handleResult || '',
            description: w.warningContent || w.content || ''
          }
        })
        this.total = res.data?.total || list.length
      } catch { this.tableData = []; this.total = 0 }
      this.loading = false
    },

    filterData() {
      let data = [...this.allData]

      if (this.filterForm.type) {
        data = data.filter(item => item.type === this.filterForm.type)
      }
      if (this.filterForm.level) {
        data = data.filter(item => item.level === this.filterForm.level)
      }
      if (this.filterForm.status) {
        data = data.filter(item => item.status === this.filterForm.status)
      }
      if (this.filterForm.dateRange && this.filterForm.dateRange.length === 2) {
        const [start, end] = this.filterForm.dateRange
        data = data.filter(item => item.time >= start && item.time <= end + ' 23:59')
      }

      this.total = data.length
      const start = (this.page - 1) * this.pageSize
      this.tableData = data.slice(start, start + this.pageSize)
    },

    handleSearch() {
      this.page = 1
      this.loadData()
    },

    onSortFieldChange() {
      // 状态优先级默认升序（未处理最上）；时间类默认降序（最新/最近在前）
      this.filterForm.sortOrder = this.filterForm.sortField === 'statusPriority' ? 'asc' : 'desc'
      this.handleSearch()
    },

    toggleSortOrder() {
      this.filterForm.sortOrder = this.filterForm.sortOrder === 'asc' ? 'desc' : 'asc'
      this.handleSearch()
    },

    handleReset() {
      this.filterForm = { type: '', level: '', dateRange: null, status: '' }
      this.page = 1
      this.loadData()
    },

    handlePageChange(page) {
      this.page = page
      this.loadData()
    },

    handleSizeChange(size) {
      this.pageSize = size
      this.page = 1
      this.loadData()
    },

    showDetail(row) {
      this.currentWarning = row
      this.detailVisible = true
    },

    async handleProcess(row) {
      this.currentRow = { ...row }
      this.processForm = {
        id: row.id,
        handleResult: '',
        handler: sessionStorage.getItem('username') || ''
      }
      this.processDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.processForm) this.$refs.processForm.clearValidate()
      })
    },

    async submitProcess() {
      try { await this.$refs.processForm.validate() } catch { return }
      this.processLoading = true
      try {
        await this.$http.post('/warning/' + this.processForm.id + '/process', {
          handler: this.processForm.handler,
          handleResult: this.processForm.handleResult
        })
        this.$message.success('已处理完成')
        this.processDialogVisible = false
        await this.loadData()
      } catch (e) {
        this.$message.error(e?.response?.data?.message || e.message || '处理失败')
      } finally {
        this.processLoading = false
      }
    },

    async handleClose(row) {
      try {
        await this.$confirm('确定关闭此预警？', '关闭确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch { return }
      try {
        await this.$http.put('/warning/' + row.id + '/status', { status: '已关闭' })
        row.status = '已关闭'
        this.$message.success('预警已关闭')
        await this.loadData()
      } catch (e) {
        this.$message.error(e?.response?.data?.message || e.message || '操作失败')
      }
    },

    formatTime(t) {
      if (!t) return '-'
      const s = String(t).replace('T', ' ')
      return s.length >= 16 ? s.substring(0, 16) : s
    },

    getRiskTagType(level) {
      if (level === '高风险') return 'danger'
      if (level === '中风险') return 'warning'
      return 'success'
    },

    getStatusTagType(status) {
      if (status === '未处理') return 'danger'
      if (status === '处理中') return 'warning'
      if (status === '已处理') return 'success'
      return 'info'
    }
  }
}
</script>

<style scoped>
.warning-page {
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
  max-height: calc(100vh - 280px);
  overflow-y: auto;
}

.pagination-wrap {
  margin-top: 15px;
  text-align: right;
}

.detail-content {
  padding: 10px 0;
}

.detail-item {
  margin-bottom: 12px;
  font-size: 14px;
  color: #c8e6ff;
}

.detail-label {
  color: #8899aa;
  display: inline-block;
  width: 100px;
}

.detail-desc {
  color: #aac8e0;
  line-height: 1.8;
  font-size: 14px;
  margin-top: 5px;
}

/* 统一暗色表格 */
.warning-page .el-table { background-color: #0c1a2e !important; }
.warning-page .el-table::before { background-color: #1a3a5c !important; }
.warning-page .el-table th.el-table__cell,
.warning-page .el-table thead th {
  background-color: #0a1525 !important;
  color: #7eb8e0 !important;
  border-bottom: 2px solid #1a4a6e !important;
}
.warning-page .el-table .el-table__body tr.el-table__row > td.el-table__cell,
.warning-page .el-table td.el-table__cell {
  background-color: #0f2238 !important;
  color: #d4eaff !important;
  border-bottom: 1px solid #162d48 !important;
}
.warning-page .el-table .el-table__body tr:hover > td.el-table__cell {
  background-color: #183654 !important;
}
</style>


