<template>
  <div class="policy-page">
    <!-- 操作栏 -->
    <div class="action-bar">
      <el-row>
        <el-col :span="12">
          <el-form :model="filterForm" :inline="true" size="small">
            <el-form-item label="政策标题">
              <el-input
                v-model="filterForm.keyword"
                placeholder="请输入关键词搜索"
                clearable
                @clear="handleSearch"
              ></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
            </el-form-item>
          </el-form>
        </el-col>
        <el-col :span="12" style="text-align: right;">
          <el-button type="primary" icon="el-icon-plus" @click="openDialog('add')">新增政策</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <el-table
        :data="tableData"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column type="index" label="序号" width="60"></el-table-column>
        <el-table-column prop="title" label="政策标题" width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="category" label="分类" width="110">
          <template slot-scope="scope">
            <el-tag size="small" type="info">{{ scope.row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="发文部门" width="130"></el-table-column>
        <el-table-column prop="region" label="适用区域" width="100"></el-table-column>
        <el-table-column prop="publishDate" label="发布日期" width="120" sortable></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.active"
              active-color="#67C23A"
              @change="toggleStatus(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="openDialog('view', scope.row)">查看</el-button>
            <el-button type="text" size="small" @click="openDialog('edit', scope.row)">编辑</el-button>
            <el-button type="text" size="small" style="color: #F56C6C;" @click="handleDelete(scope.row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="640px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <el-form
        ref="policyForm"
        :model="formData"
        :rules="rules"
        label-width="90px"
        size="small"
      >
        <el-form-item label="政策标题" prop="title">
          <el-input v-model="formData.title" :disabled="isView"></el-input>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="formData.category" style="width: 100%;" :disabled="isView">
            <el-option label="关税政策" value="关税政策"></el-option>
            <el-option label="检验检疫" value="检验检疫"></el-option>
            <el-option label="贸易便利化" value="贸易便利化"></el-option>
            <el-option label="外汇管理" value="外汇管理"></el-option>
            <el-option label="跨境物流" value="跨境物流"></el-option>
            <el-option label="合规监管" value="合规监管"></el-option>
            <el-option label="扶持政策" value="扶持政策"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="发文部门" prop="department">
          <el-input v-model="formData.department" :disabled="isView"></el-input>
        </el-form-item>
        <el-form-item label="适用区域" prop="region">
          <el-select
            v-model="formData.region"
            style="width: 100%;"
            :disabled="isView"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入区域/国家"
          >
            <el-option-group label="广西口岸/区域">
              <el-option v-for="o in regionOptions.gx" :key="o" :label="o" :value="o"></el-option>
            </el-option-group>
            <el-option-group label="东盟国家">
              <el-option v-for="o in regionOptions.asean" :key="o" :label="o" :value="o"></el-option>
            </el-option-group>
            <el-option-group label="区域">
              <el-option v-for="o in regionOptions.zone" :key="o" :label="o" :value="o"></el-option>
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="发布日期" prop="publishDate">
          <el-date-picker
            v-model="formData.publishDate"
            type="date"
            placeholder="选择日期"
            value-format="yyyy-MM-dd"
            style="width: 100%;"
            :disabled="isView"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="政策内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="6"
            :disabled="isView"
          ></el-input>
        </el-form-item>
        <el-form-item label="附件链接">
          <el-input v-model="formData.link" :disabled="isView" placeholder="可选"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" v-if="!isView">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSave">保 存</el-button>
      </span>
      <span slot="footer" v-else>
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { createPolicy, updatePolicy, deletePolicy } from '@/api'

export default {
  name: 'PolicyManage',
  data() {
    return {
      filterForm: {
        keyword: ''
      },
      tableData: [],
      allData: [],
      loading: false,
      page: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogMode: 'add',
      isView: false,
      editingRow: null,
      // 适用区域选项：广西口岸 + 东盟国家 + 区域（含 allow-create 可输入自定义）
      regionOptions: {
        gx: ['全区', '凭祥', '东兴', '防城港', '钦州', '北海', '崇左'],
        asean: ['越南', '泰国', '马来西亚', '印度尼西亚', '新加坡', '菲律宾', '文莱', '老挝', '缅甸', '柬埔寨'],
        zone: ['东盟', '东盟多国', 'RCEP成员国', '其他']
      },
      formData: {
        title: '',
        category: '',
        department: '',
        region: '',
        publishDate: '',
        content: '',
        link: '',
        active: true
      },
      rules: {
        title: [{ required: true, message: '请输入政策标题', trigger: 'blur' }],
        category: [{ required: true, message: '请选择分类', trigger: 'change' }],
        department: [{ required: true, message: '请输入发文部门', trigger: 'blur' }],
        region: [{ required: true, message: '请选择适用区域', trigger: 'change' }],
        publishDate: [{ required: true, message: '请选择发布日期', trigger: 'change' }],
        content: [{ required: true, message: '请输入政策内容', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      if (this.dialogMode === 'add') return '新增政策'
      if (this.dialogMode === 'view') return '政策详情'
      return '编辑政策'
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await this.$http.get('/policy/search', { params: { keyword: this.filterForm.keyword || '', page: this.page, pageSize: this.pageSize } })
        const list = res.data?.list || []
        this.allData = list.map(p => ({
          id: p.id,
          title: p.policyTitle || p.policy_title || '-',
          category: p.policyCategory || p.policy_category || '-',
          department: p.dataSource || p.data_source || '-',
          region: p.targetCountry || p.target_country || '-',
          publishDate: p.publishDate || p.publish_date || '-',
          content: p.policyContent || p.policy_content || '',
          link: p.link || '',
          active: p.isActive === undefined ? true : !!p.isActive
        }))
        this.total = res.data?.total || list.length
      } catch { this.allData = []; this.total = 0 }
      this.tableData = this.allData
      this.loading = false
    },

    handleSearch() {
      this.page = 1
      this.loadData()
    },

    handlePageChange(page) {
      this.page = page
      this.loadData()
    },

    openDialog(mode, row) {
      this.dialogMode = mode
      this.isView = mode === 'view'
      this.editingRow = row || null

      if (row) {
        this.formData = { ...row }
      } else {
        this.formData = {
          title: '',
          category: '',
          department: '',
          region: '',
          publishDate: '',
          content: '',
          link: '',
          active: true
        }
      }

      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.policyForm) {
          this.$refs.policyForm.clearValidate()
        }
      })
    },

    async handleSave() {
      this.$refs.policyForm.validate(async valid => {
        if (!valid) return
        // 前端字段 → 后端字段映射
        const payload = {
          policyTitle: this.formData.title,
          policyCategory: this.formData.category,
          targetCountry: this.formData.region,
          publishDate: this.formData.publishDate,
          policyContent: this.formData.content,
          dataSource: this.formData.department || '平台录入',
          link: this.formData.link
        }
        try {
          if (this.dialogMode === 'add') {
            await createPolicy(payload)
            this.$message.success('新增成功')
          } else if (this.dialogMode === 'edit' && this.editingRow) {
            await updatePolicy(this.editingRow.id, payload)
            this.$message.success('保存成功')
          }
          this.dialogVisible = false
          this.loadData()
        } catch (e) {
          this.$message.error(e.message || '操作失败')
        }
      })
    },

    handleDelete(row) {
      this.$confirm(`确定删除政策"${row.title}"？`, '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deletePolicy(row.id)
          this.$message.success('删除成功')
          this.loadData()
        } catch (e) {
          this.$message.error(e.message || '删除失败')
        }
      }).catch(() => {})
    },

    async toggleStatus(row) {
      try {
        await this.$http.put('/policy/' + row.id + '/status', { active: row.active })
        this.$message.success(row.active ? '已启用' : '已禁用')
      } catch (e) {
        row.active = !row.active
        this.$message.error(e.message || '操作失败')
      }
    }
  }
}
</script>

<style scoped>
.policy-page {
  padding: 0;
}

.action-bar {
  background: rgba(0,20,45,0.6);
  border: 1px solid rgba(58,111,247,0.1);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}

.table-card {
  background: rgba(0,20,45,0.6);
  border: 1px solid rgba(58,111,247,0.1);
  border-radius: 8px;
  padding: 20px;
}

.pagination-wrap {
  margin-top: 15px;
  text-align: right;
}

/* 统一暗色表格 */
.policy-page .el-table { background-color: #0c1a2e !important; }
.policy-page .el-table::before { background-color: #1a3a5c !important; }
.policy-page .el-table th.el-table__cell,
.policy-page .el-table thead th {
  background-color: #0a1525 !important;
  color: #7eb8e0 !important;
  border-bottom: 2px solid #1a4a6e !important;
}
.policy-page .el-table .el-table__body tr.el-table__row > td.el-table__cell,
.policy-page .el-table td.el-table__cell {
  background-color: #0f2238 !important;
  color: #d4eaff !important;
  border-bottom: 1px solid #162d48 !important;
}
.policy-page .el-table .el-table__body tr:hover > td.el-table__cell {
  background-color: #183654 !important;
}
</style>
