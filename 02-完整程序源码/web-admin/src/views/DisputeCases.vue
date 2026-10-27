<template>
  <div class="dispute-page">
    <div class="filter-card">
      <el-form :model="filterForm" :inline="true" size="small">
        <el-form-item label="类型">
          <el-select v-model="filterForm.type" placeholder="全部类型" clearable @clear="handleSearch">
            <el-option label="合同纠纷" value="合同纠纷"></el-option>
            <el-option label="知识产权" value="知识产权"></el-option>
            <el-option label="产品质量" value="产品质量"></el-option>
            <el-option label="支付纠纷" value="支付纠纷"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="请输入关键词" clearable @clear="handleSearch"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          <el-button type="warning" icon="el-icon-magic-stick" @click="matchDialogVisible = true">智能匹配</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="caseNo" label="案例编号" width="120"></el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip></el-table-column>
        <el-table-column prop="caseType" label="类型" width="140">
          <template slot-scope="scope">
            <el-tag size="small" effect="dark">{{ scope.row.caseType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tradeCategory" label="贸易品类" width="110"></el-table-column>
        <el-table-column prop="countries" label="涉及国家" width="150" show-overflow-tooltip></el-table-column>
        <el-table-column prop="judgment" label="判决结果" min-width="140" show-overflow-tooltip></el-table-column>
        <el-table-column prop="court" label="审理法院" width="120" show-overflow-tooltip></el-table-column>
        <el-table-column prop="publishDate" label="发布日期" width="110"></el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="showDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background :current-page="page" :page-size="pageSize" :total="total"
          layout="total, prev, pager, next, jumper" @current-change="handlePageChange"></el-pagination>
      </div>
    </div>

    <!-- 智能匹配弹窗 -->
    <el-dialog title="智能案例匹配" :visible.sync="matchDialogVisible" width="600px" top="5vh">
      <el-form :model="matchForm" label-width="100px" size="small">
        <el-form-item label="贸易品类">
          <el-input v-model="matchForm.tradeCategory" placeholder="如：农产品、电子产品、纺织品"></el-input>
        </el-form-item>
        <el-form-item label="合作国家">
          <el-input v-model="matchForm.countries" placeholder="如：越南、泰国、柬埔寨"></el-input>
        </el-form-item>
      </el-form>
      <div v-if="matchResults.length > 0" style="margin-top: 10px;">
        <h4 style="color: #c8e6ff; margin-bottom: 10px;">匹配结果</h4>
        <el-table :data="matchResults" size="small" max-height="300">
          <el-table-column prop="caseNo" label="案例编号" width="100"></el-table-column>
          <el-table-column prop="title" label="标题" show-overflow-tooltip></el-table-column>
          <el-table-column prop="caseType" label="类型" width="90"></el-table-column>
          <el-table-column prop="similarity" label="匹配度" width="90">
            <template slot-scope="scope">
              <span :style="{ color: scope.row.similarity >= 70 ? '#67C23A' : scope.row.similarity >= 40 ? '#E6A23C' : '#F56C6C', fontWeight: 600 }">
                {{ scope.row.similarity }}%
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <span slot="footer">
        <el-button @click="matchDialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="handleMatch" :loading="matchLoading">开始匹配</el-button>
      </span>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog title="案例详情" :visible.sync="detailVisible" width="640px" top="5vh">
      <div v-if="currentCase" class="detail-content">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="detail-item"><span class="detail-label">案例编号：</span>{{ currentCase.caseNo }}</div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item"><span class="detail-label">类型：</span>
              <el-tag size="small" effect="dark">{{ currentCase.caseType }}</el-tag>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item"><span class="detail-label">贸易品类：</span>{{ currentCase.tradeCategory }}</div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item"><span class="detail-label">涉及国家：</span>{{ currentCase.countries }}</div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item"><span class="detail-label">审理法院：</span>{{ currentCase.court }}</div>
          </el-col>
          <el-col :span="12">
            <div class="detail-item"><span class="detail-label">发布日期：</span>{{ currentCase.publishDate }}</div>
          </el-col>
        </el-row>
        <el-divider></el-divider>
        <h4 style="color: #c8e6ff; margin-bottom: 8px;">案例摘要</h4>
        <p class="detail-text">{{ currentCase.summary || '暂无摘要' }}</p>
        <el-divider></el-divider>
        <h4 style="color: #c8e6ff; margin-bottom: 8px;">判决结果</h4>
        <p class="detail-text">{{ currentCase.judgment || '暂无' }}</p>
        <el-divider></el-divider>
        <h4 style="color: #c8e6ff; margin-bottom: 8px;">风险建议</h4>
        <p class="detail-text">{{ currentCase.riskAdvice || '暂无' }}</p>
      </div>
      <span slot="footer">
        <el-button @click="detailVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'DisputeCases',
  data() {
    return {
      filterForm: { type: '', keyword: '' },
      tableData: [],
      allData: [],
      loading: false,
      page: 1,
      pageSize: 10,
      total: 0,
      detailVisible: false,
      currentCase: null,
      matchDialogVisible: false,
      matchLoading: false,
      matchForm: { tradeCategory: '', countries: '' },
      matchResults: []
    }
  },
  created() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await this.$http.get('/dispute/list', {
          params: { page: this.page, pageSize: this.pageSize, type: this.filterForm.type, keyword: this.filterForm.keyword }
        })
        const list = res.data?.list || res.data?.rows || []
        this.allData = list.map(item => ({
          id: item.id,
          caseNo: item.caseNo || item.case_no || '',
          title: item.title || '',
          caseType: item.caseType || '',
          tradeCategory: item.tradeCategory || item.trade_category || '',
          countries: item.countries || '',
          judgment: item.judgment || '',
          court: item.court || '',
          publishDate: item.publishDate || item.publish_date || '',
          summary: item.summary || '',
          riskAdvice: item.riskAdvice || item.risk_advice || ''
        }))
        this.total = res.data?.total || this.allData.length
      } catch { this.allData = []; this.total = 0 }
      this.tableData = this.allData
      this.loading = false
    },
    handleSearch() { this.page = 1; this.loadData() },
    handleReset() { this.filterForm = { type: '', keyword: '' }; this.page = 1; this.loadData() },
    handlePageChange(page) { this.page = page; this.loadData() },
    async showDetail(row) {
      try {
        const res = await this.$http.get('/dispute/' + row.id)
        this.currentCase = { ...row, ...(res.data || {}) }
      } catch {
        this.currentCase = row
      }
      this.detailVisible = true
    },
    async handleMatch() {
      if (!this.matchForm.tradeCategory && !this.matchForm.countries) {
        this.$message.warning('请至少输入贸易品类或合作国家')
        return
      }
      this.matchLoading = true
      try {
        const res = await this.$http.post('/dispute/match', this.matchForm)
        this.matchResults = (res.data?.list || res.data || []).map(item => ({
          caseNo: item.caseNo || item.case_no || '',
          title: item.title || '',
          caseType: item.caseType || '',
          similarity: item.similarity || item.score || 0
        }))
        if (this.matchResults.length === 0) {
          this.$message.info('未找到匹配的案例')
        }
      } catch (e) {
        this.$message.error(e.message || '匹配失败')
      }
      this.matchLoading = false
    }
  }
}
</script>

<style scoped>
.dispute-page { padding: 0; }
.filter-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px 20px 5px; margin-bottom: 16px; }
.table-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px; }
.pagination-wrap { margin-top: 15px; text-align: right; }
.detail-content { padding: 10px 0; }
.detail-item { margin-bottom: 14px; font-size: 14px; color: #c8e6ff; }
.detail-label { color: #8899aa; display: inline-block; width: 80px; }
.detail-text { color: #a0c0dd; font-size: 13px; line-height: 1.8; margin: 0; }
.dispute-page .el-table { background-color: #0c1a2e !important; }
.dispute-page .el-table::before { background-color: #1a3a5c !important; }
.dispute-page .el-table th.el-table__cell,
.dispute-page .el-table thead th { background-color: #0a1525 !important; color: #7eb8e0 !important; border-bottom: 2px solid #1a4a6e !important; }
.dispute-page .el-table .el-table__body tr.el-table__row > td.el-table__cell,
.dispute-page .el-table td.el-table__cell { background-color: #0f2238 !important; color: #d4eaff !important; border-bottom: 1px solid #162d48 !important; }
.dispute-page .el-table .el-table__body tr:hover > td.el-table__cell { background-color: #183654 !important; }
</style>
