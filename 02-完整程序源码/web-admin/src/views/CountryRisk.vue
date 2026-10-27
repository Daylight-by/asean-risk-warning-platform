<template>
  <div class="country-risk-page">
    <div class="page-header">
      <h3 class="page-title">国别风控配置</h3>
      <p class="page-desc">管理各合作国家的风险维度权重配置，用于跨境贸易风险评估</p>
    </div>

    <div class="cards-container" v-loading="loading">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in configs" :key="item.id" style="margin-bottom: 16px;">
          <div class="risk-card">
            <div class="risk-card-header">
              <span class="country-flag">{{ item.flag }}</span>
              <span class="country-name">{{ item.countryName }}</span>
            </div>
            <div class="risk-card-body">
              <div class="weight-item" v-for="(w, idx) in item.weights" :key="idx">
                <span class="weight-label">{{ w.name }}</span>
                <el-progress :percentage="w.value" :stroke-width="8" :color="getProgressColor(w.value)" :show-text="true" class="weight-progress"></el-progress>
              </div>
            </div>
            <div class="risk-card-footer">
              <div class="focus-areas">
                <span class="footer-label">重点关注：</span>
                <el-tag v-for="area in item.focusAreas" :key="area" size="mini" effect="dark" style="margin: 2px;">{{ area }}</el-tag>
              </div>
              <div class="special-rules" v-if="item.specialRules">
                <span class="footer-label">特殊规则：</span>
                <span class="rules-text">{{ item.specialRules }}</span>
              </div>
            </div>
            <div class="risk-card-action">
              <el-button type="text" size="small" icon="el-icon-edit" @click="openEdit(item)">编辑配置</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog :title="'编辑风控配置 - ' + (editForm.countryName || '')" :visible.sync="editDialogVisible" width="560px" top="5vh" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="120px" size="small">
        <el-form-item label="国家">
          <el-input :value="editForm.countryName" disabled></el-input>
        </el-form-item>
        <el-form-item v-for="(w, idx) in editForm.weights" :key="idx" :label="w.name">
          <el-slider v-model="w.value" :min="0" :max="100" :step="5" show-input></el-slider>
        </el-form-item>
        <el-form-item label="重点关注领域">
          <el-input v-model="editForm.focusAreasStr" placeholder="多个领域用逗号分隔"></el-input>
        </el-form-item>
        <el-form-item label="特殊风控规则">
          <el-input v-model="editForm.specialRules" type="textarea" :rows="3" placeholder="请输入特殊风控规则"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSave">保 存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'CountryRisk',
  data() {
    return {
      loading: false,
      configs: [],
      editDialogVisible: false,
      editForm: {
        id: null,
        countryName: '',
        weights: [],
        focusAreasStr: '',
        specialRules: ''
      },
      defaultConfigs: [
        { id: 1, countryName: '越南', flag: 'VN', weights: [{ name: '政治风险', value: 60 }, { name: '汇率风险', value: 70 }, { name: '法律风险', value: 55 }, { name: '物流风险', value: 45 }, { name: '信用风险', value: 65 }], focusAreas: ['政治风险', '汇率风险'], specialRules: '关注越南北部边境贸易政策变化' },
        { id: 2, countryName: '泰国', flag: 'TH', weights: [{ name: '政治风险', value: 40 }, { name: '汇率风险', value: 50 }, { name: '法律风险', value: 45 }, { name: '物流风险', value: 35 }, { name: '信用风险', value: 40 }], focusAreas: ['法律风险'], specialRules: '注意泰国BOI投资优惠政策变动' },
        { id: 3, countryName: '柬埔寨', flag: 'KH', weights: [{ name: '政治风险', value: 70 }, { name: '汇率风险', value: 60 }, { name: '法律风险', value: 75 }, { name: '物流风险', value: 65 }, { name: '信用风险', value: 80 }], focusAreas: ['信用风险', '法律风险', '政治风险'], specialRules: '加强尽职调查，关注空壳公司风险' },
        { id: 4, countryName: '马来西亚', flag: 'MY', weights: [{ name: '政治风险', value: 30 }, { name: '汇率风险', value: 40 }, { name: '法律风险', value: 35 }, { name: '物流风险', value: 30 }, { name: '信用风险', value: 35 }], focusAreas: ['汇率风险'], specialRules: '关注马来西亚数字经济政策' },
        { id: 5, countryName: '印尼', flag: 'ID', weights: [{ name: '政治风险', value: 45 }, { name: '汇率风险', value: 55 }, { name: '法律风险', value: 60 }, { name: '物流风险', value: 50 }, { name: '信用风险', value: 50 }], focusAreas: ['法律风险', '物流风险'], specialRules: '印尼群岛物流风险较高，注意运输时效' },
        { id: 6, countryName: '老挝', flag: 'LA', weights: [{ name: '政治风险', value: 55 }, { name: '汇率风险', value: 65 }, { name: '法律风险', value: 70 }, { name: '物流风险', value: 60 }, { name: '信用风险', value: 70 }], focusAreas: ['法律风险', '汇率风险'], specialRules: '老挝基础设施薄弱，物流成本偏高' },
        { id: 7, countryName: '缅甸', flag: 'MM', weights: [{ name: '政治风险', value: 85 }, { name: '汇率风险', value: 75 }, { name: '法律风险', value: 80 }, { name: '物流风险', value: 70 }, { name: '信用风险', value: 85 }], focusAreas: ['政治风险', '信用风险', '法律风险'], specialRules: '高风险国家，需额外审批和担保措施' }
      ]
    }
  },
  created() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await this.$http.get('/country-risk/configs')
        const list = res.data?.list || res.data || []
        if (list.length > 0) {
          this.configs = list.map(item => ({
            id: item.id,
            countryName: item.countryName || item.country_name || '',
            flag: item.flag || (item.countryName || item.country_name || '').substring(0, 2),
            weights: item.weights || [],
            focusAreas: item.focusAreas || item.focus_areas || [],
            specialRules: item.specialRules || item.special_rules || ''
          }))
        } else {
          this.configs = this.defaultConfigs
        }
      } catch {
        this.configs = this.defaultConfigs
      }
      this.loading = false
    },
    getProgressColor(value) {
      if (value >= 70) return '#F56C6C'
      if (value >= 50) return '#E6A23C'
      return '#67C23A'
    },
    openEdit(item) {
      this.editForm = {
        id: item.id,
        countryName: item.countryName,
        weights: item.weights.map(w => ({ ...w })),
        focusAreasStr: (item.focusAreas || []).join('，'),
        specialRules: item.specialRules || ''
      }
      this.editDialogVisible = true
    },
    async handleSave() {
      try {
        const payload = {
          weights: this.editForm.weights,
          focusAreas: this.editForm.focusAreasStr.split(/[,，]/).map(s => s.trim()).filter(Boolean),
          specialRules: this.editForm.specialRules
        }
        await this.$http.put('/country-risk/' + this.editForm.id, payload)
        this.$message.success('保存成功')
        this.editDialogVisible = false
        this.loadData()
      } catch (e) {
        this.$message.error(e.message || '保存失败')
      }
    }
  }
}
</script>

<style scoped>
.country-risk-page { padding: 0; }
.page-header { margin-bottom: 20px; }
.page-title { color: #c8e6ff; font-size: 18px; font-weight: 600; margin: 0 0 6px 0; }
.page-desc { color: #6a8aa8; font-size: 13px; margin: 0; }
.cards-container { min-height: 200px; }
.risk-card {
  background: rgba(0,20,45,0.6);
  border: 1px solid rgba(58,111,247,0.1);
  border-radius: 8px;
  padding: 16px;
  transition: border-color 0.3s;
  height: 100%;
}
.risk-card:hover { border-color: rgba(58,111,247,0.25); }
.risk-card-header { display: flex; align-items: center; margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid rgba(58,111,247,0.08); }
.country-flag { display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; background: linear-gradient(135deg, rgba(58,111,247,0.15), rgba(0,120,200,0.08)); border: 1px solid rgba(58,111,247,0.2); border-radius: 6px; color: #3a6ff7; font-size: 11px; font-weight: bold; margin-right: 10px; }
.country-name { color: #e0f0ff; font-size: 15px; font-weight: 600; }
.risk-card-body { margin-bottom: 12px; }
.weight-item { display: flex; align-items: center; margin-bottom: 8px; }
.weight-label { color: #8899aa; font-size: 12px; width: 65px; flex-shrink: 0; }
.weight-progress { flex: 1; }
.risk-card-footer { margin-bottom: 10px; }
.focus-areas { margin-bottom: 6px; }
.footer-label { color: #6a8aa8; font-size: 12px; }
.rules-text { color: #a0c0dd; font-size: 12px; }
.special-rules { margin-top: 4px; }
.risk-card-action { text-align: right; border-top: 1px solid rgba(58,111,247,0.06); padding-top: 10px; }
</style>
