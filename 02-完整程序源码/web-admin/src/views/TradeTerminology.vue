<template>
  <div class="terminology-page">
    <div class="filter-card">
      <el-form :model="filterForm" :inline="true" size="small">
        <el-form-item label="分类">
          <el-select v-model="filterForm.category" placeholder="全部分类" clearable @clear="handleSearch">
            <el-option label="报关" value="报关"></el-option>
            <el-option label="税务" value="税务"></el-option>
            <el-option label="物流" value="物流"></el-option>
            <el-option label="支付" value="支付"></el-option>
            <el-option label="法律" value="法律"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="请输入术语关键词" clearable @clear="handleSearch"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          <el-button type="primary" icon="el-icon-plus" @click="openDialog('add')">新增术语</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="termZh" label="中文术语" min-width="110" show-overflow-tooltip></el-table-column>
        <el-table-column prop="termEn" label="英文" min-width="110" show-overflow-tooltip></el-table-column>
        <el-table-column prop="termVi" label="越南语" min-width="100" show-overflow-tooltip></el-table-column>
        <el-table-column prop="termTh" label="泰语" min-width="100" show-overflow-tooltip></el-table-column>
        <el-table-column prop="termMy" label="马来语" min-width="100" show-overflow-tooltip></el-table-column>
        <el-table-column prop="termIn" label="印尼语" min-width="100" show-overflow-tooltip></el-table-column>
        <el-table-column prop="termKm" label="柬埔寨语" min-width="100" show-overflow-tooltip></el-table-column>
        <el-table-column prop="category" label="分类" width="90">
          <template slot-scope="scope">
            <el-tag size="small" effect="dark">{{ scope.row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hsCode" label="HS编码" width="140" show-overflow-tooltip></el-table-column>
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip></el-table-column>
        <el-table-column prop="dataSource" label="数据来源" width="100"></el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="openDialog('edit', scope.row)">编辑</el-button>
            <el-button type="text" size="small" style="color: #F56C6C;" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background :current-page="page" :page-size="pageSize" :total="total"
          layout="total, prev, pager, next, jumper" @current-change="handlePageChange"></el-pagination>
      </div>
    </div>

    <el-dialog :title="dialogMode === 'add' ? '新增术语' : '编辑术语'" :visible.sync="dialogVisible" width="640px" top="5vh" :close-on-click-modal="false">
      <el-form ref="termForm" :model="formData" :rules="rules" label-width="90px" size="small">
        <el-form-item label="中文术语" prop="termZh">
          <el-input v-model="formData.termZh" placeholder="请输入中文术语"></el-input>
        </el-form-item>
        <el-form-item label="英文" prop="termEn">
          <el-input v-model="formData.termEn" placeholder="请输入英文术语"></el-input>
        </el-form-item>
        <el-form-item label="越南语">
          <el-input v-model="formData.termVi" placeholder="请输入越南语术语"></el-input>
        </el-form-item>
        <el-form-item label="泰语">
          <el-input v-model="formData.termTh" placeholder="请输入泰语术语"></el-input>
        </el-form-item>
        <el-form-item label="马来语">
          <el-input v-model="formData.termMy" placeholder="请输入马来语术语"></el-input>
        </el-form-item>
        <el-form-item label="印尼语">
          <el-input v-model="formData.termIn" placeholder="请输入印尼语术语"></el-input>
        </el-form-item>
        <el-form-item label="柬埔寨语">
          <el-input v-model="formData.termKm" placeholder="请输入柬埔寨语术语"></el-input>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="formData.category" style="width: 100%;" placeholder="请选择分类">
            <el-option label="报关" value="报关"></el-option>
            <el-option label="税务" value="税务"></el-option>
            <el-option label="物流" value="物流"></el-option>
            <el-option label="支付" value="支付"></el-option>
            <el-option label="法律" value="法律"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="HS编码">
          <el-input v-model="formData.hsCode" placeholder="请输入HS编码（可选）"></el-input>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入术语说明"></el-input>
        </el-form-item>
        <el-form-item label="数据来源">
          <el-input v-model="formData.dataSource" placeholder="请输入数据来源"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSave">保 存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'TradeTerminology',
  data() {
    return {
      filterForm: { category: '', keyword: '' },
      tableData: [],
      allData: [],
      loading: false,
      page: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogMode: 'add',
      editingRow: null,
      formData: { termZh: '', termEn: '', termVi: '', termTh: '', termMy: '', termIn: '', termKm: '', category: '', hsCode: '', description: '', dataSource: '' },
      rules: {
        termZh: [{ required: true, message: '请输入中文术语', trigger: 'blur' }],
        termEn: [{ required: true, message: '请输入英文术语', trigger: 'blur' }],
        category: [{ required: true, message: '请选择分类', trigger: 'change' }]
      }
    }
  },
  created() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await this.$http.get('/terminology/list', {
          params: { page: this.page, pageSize: this.pageSize, category: this.filterForm.category, keyword: this.filterForm.keyword }
        })
        const list = res.data?.list || res.data?.rows || []
        this.allData = list.map(item => ({
          id: item.id,
          termZh: item.termZh || item.term_zh || '',
          termEn: item.termEn || item.term_en || '',
          termVi: item.termVi || item.term_vi || '',
          termTh: item.termTh || item.term_th || '',
          termMy: item.termMy || item.term_my || '',
          termIn: item.termIn || item.term_in || '',
          termKm: item.termKm || item.term_km || '',
          category: item.category || '',
          hsCode: item.hsCode || item.hs_code || '',
          description: item.description || '',
          dataSource: item.dataSource || item.data_source || ''
        }))
        this.total = res.data?.total || this.allData.length
      } catch { this.allData = []; this.total = 0 }
      this.filterData()
      this.loading = false
    },
    filterData() {
      let data = [...this.allData]
      if (this.filterForm.keyword) {
        const kw = this.filterForm.keyword.toLowerCase()
        data = data.filter(item =>
          (item.termZh && item.termZh.toLowerCase().includes(kw)) ||
          (item.termEn && item.termEn.toLowerCase().includes(kw)) ||
          (item.termVi && item.termVi.toLowerCase().includes(kw)) ||
          (item.termTh && item.termTh.toLowerCase().includes(kw)) ||
          (item.termMy && item.termMy.toLowerCase().includes(kw)) ||
          (item.termIn && item.termIn.toLowerCase().includes(kw)) ||
          (item.termKm && item.termKm.toLowerCase().includes(kw)) ||
          (item.description && item.description.toLowerCase().includes(kw))
        )
      }
      if (this.filterForm.category) { data = data.filter(item => item.category === this.filterForm.category) }
      this.total = data.length
      const start = (this.page - 1) * this.pageSize
      this.tableData = data.slice(start, start + this.pageSize)
    },
    handleSearch() { this.page = 1; this.loadData() },
    handleReset() { this.filterForm = { category: '', keyword: '' }; this.page = 1; this.loadData() },
    handlePageChange(page) { this.page = page; this.filterData() },
    openDialog(mode, row) {
      this.dialogMode = mode
      this.editingRow = row || null
      if (row) { this.formData = { ...row } }
      else { this.formData = { termZh: '', termEn: '', termVi: '', termTh: '', category: '', hsCode: '', description: '', dataSource: '' } }
      this.dialogVisible = true
      this.$nextTick(() => { if (this.$refs.termForm) this.$refs.termForm.clearValidate() })
    },
    async handleSave() {
      this.$refs.termForm.validate(async valid => {
        if (!valid) return
        try {
          if (this.dialogMode === 'add') {
            await this.$http.post('/terminology', this.formData)
            this.$message.success('新增成功')
          } else {
            await this.$http.put('/terminology/' + this.editingRow.id, this.formData)
            this.$message.success('保存成功')
          }
          this.dialogVisible = false
          this.loadData()
        } catch (e) { this.$message.error(e.message || '操作失败') }
      })
    },
    handleDelete(row) {
      this.$confirm('确定删除术语"' + row.termZh + '"？', '删除确认', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(async () => {
        try {
          await this.$http.delete('/terminology/' + row.id)
          this.$message.success('删除成功')
          this.loadData()
        } catch (e) { this.$message.error(e.message || '删除失败') }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.terminology-page { padding: 0; }
.filter-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px 20px 5px; margin-bottom: 16px; }
.table-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px; }
.pagination-wrap { margin-top: 15px; text-align: right; }
.terminology-page .el-table { background-color: #0c1a2e !important; }
.terminology-page .el-table::before { background-color: #1a3a5c !important; }
.terminology-page .el-table th.el-table__cell,
.terminology-page .el-table thead th { background-color: #0a1525 !important; color: #7eb8e0 !important; border-bottom: 2px solid #1a4a6e !important; }
.terminology-page .el-table .el-table__body tr.el-table__row > td.el-table__cell,
.terminology-page .el-table td.el-table__cell { background-color: #0f2238 !important; color: #d4eaff !important; border-bottom: 1px solid #162d48 !important; }
.terminology-page .el-table .el-table__body tr:hover > td.el-table__cell { background-color: #183654 !important; }
</style>
