Exit code: 0
Wall time: 2.2 seconds
Output:
<template>
  <div class="compliance-check-page">
    <div class="form-card">
      <h3 class="chart-title">中小微边贸企业合规自助体检</h3>
      <el-form :model="checkForm" :rules="rules" ref="checkForm" label-width="100px" size="small">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="统一信用代码" prop="creditCode">
              <el-input v-model="checkForm.creditCode" placeholder="请输入统一社会信用代码"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="企业名称" prop="name">
              <el-input v-model="checkForm.name" placeholder="请输入企业名称"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目标国家" prop="country">
              <el-select v-model="checkForm.country" placeholder="请选择目标国家" style="width: 100%;">
                <el-option label="越南" value="越南"></el-option>
                <el-option label="泰国" value="泰国"></el-option>
                <el-option label="马来西亚" value="马来西亚"></el-option>
                <el-option label="印尼" value="印尼"></el-option>
                <el-option label="柬埔寨" value="柬埔寨"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" icon="el-icon-document-checked" :loading="checking" @click="startCheck">开始体检</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          <el-button type="success" icon="el-icon-download" :disabled="!checkResult" @click="generateReport">生成报告</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="result-area" v-if="checkResult" v-loading="checking">
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="result-card">
            <h3 class="chart-title">合规评分</h3>
            <div ref="gaugeChart" class="gauge-container"></div>
          </div>
        </el-col>
        <el-col :span="16">
          <div class="result-card">
            <h3 class="chart-title">资质审查结果</h3>
            <el-table :data="checkResult.qualification" size="small" style="width: 100%;">
              <el-table-column prop="item" label="审查项目" width="160"></el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.pass ? 'success' : 'danger'" size="small">{{ scope.row.pass ? '通过' : '未通过' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="说明"></el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <div class="result-card">
            <h3 class="chart-title">关税风险</h3>
            <el-table :data="checkResult.tariffRisk" size="small" style="width: 100%;">
              <el-table-column prop="category" label="商品类目" width="140"></el-table-column>
              <el-table-column prop="hsCode" label="HS编码" width="120"></el-table-column>
              <el-table-column prop="rate" label="关税税率" width="100"></el-table-column>
              <el-table-column prop="risk" label="风险等级" width="100">
                <template slot-scope="scope">
                  <el-tag :type="getTariffRiskType(scope.row.risk)" size="small">{{ scope.row.risk }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="note" label="备注"></el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="result-card">
            <h3 class="chart-title">海关红线预警</h3>
            <el-table :data="checkResult.customsAlerts" size="small" style="width: 100%;">
              <el-table-column prop="type" label="预警类型" width="120"></el-table-column>
              <el-table-column prop="level" label="级别" width="100">
                <template slot-scope="scope">
                  <el-tag :type="getAlertLevelType(scope.row.level)" size="small" effect="dark">{{ scope.row.level }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="desc" label="描述"></el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="empty-tip" v-if="!checkResult && !checking">
      <i class="el-icon-document" style="font-size: 48px; color: rgba(0,212,255,0.2);"></i>
      <p style="color: #5a7a9a; margin-top: 12px;">请输入企业信息并点击"开始体检"进行合规检查</p>
    </div>
  </div>
</template>

<script>
import { runComplianceCheck } from '@/api'

export default {
  name: 'ComplianceCheck',
  data() {
    return {
      checkForm: { creditCode: '', name: '', country: '' },
      rules: {
        creditCode: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
        name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
        country: [{ required: true, message: '请选择目标国家', trigger: 'change' }]
      },
      checking: false,
      checkResult: null,
      gaugeInstance: null
    }
  },
  beforeDestroy() {
    if (this.gaugeInstance) this.gaugeInstance.dispose()
  },
  methods: {
    startCheck() {
      this.$refs.checkForm.validate(async (valid) => {
        if (!valid) return
        this.checking = true
        this.checkResult = null
        try {
          const res = await runComplianceCheck(this.checkForm)
          if (res.data && res.data.score != null) {
            this.checkResult = res.data
          } else {
            this.useMockResult()
          }
        } catch (e) {
          this.useMockResult()
        }
        this.checking = false
        this.$nextTick(() => { this.renderGauge() })
      })
    },
    useMockResult() {
      const country = this.checkForm.country || '越南'
      // 接口异常时的参考结果（非随机，取中位基准值）
      const forexPass = true
      const licensePass = false
      this.checkResult = {
        score: 70,
        qualification: [
          { item: '营业执照有效性', pass: true, remark: '营业执照在有效期内' },
          { item: '进出口经营资质', pass: true, remark: '已取得对外贸易经营者备案' },
          { item: '海关注册登记', pass: true, remark: '海关注册编码有效' },
          { item: '外汇收支名录', pass: forexPass, remark: forexPass ? '已在名录内' : '未在外汇收支名录中，需补办' },
          { item: country + '进口许可', pass: licensePass, remark: licensePass ? '已获得目标国进口许可' : '需确认目标国进口许可证状态' }
        ],
        tariffRisk: [
          { category: '农产品', hsCode: '1006.30', rate: '5%', risk: '低风险', note: '适用RCEP优惠税率' },
          { category: '纺织品', hsCode: '6109.10', rate: '15%', risk: '中风险', note: '需确认原产地规则' },
          { category: '电子产品', hsCode: '8517.12', rate: '0%', risk: '低风险', note: 'ITA协议免税' },
          { category: '化工产品', hsCode: '3204.17', rate: '10%', risk: '高风险', note: '涉及反倾销调查清单' }
        ],
        customsAlerts: [
          { type: '价格预警', level: '红色', desc: '申报价格低于海关参考价30%以上，可能被估价质疑' },
          { type: '归类风险', level: '橙色', desc: '商品归类存在争议，建议申请预归类裁定' },
          { type: '原产地规则', level: '黄色', desc: '需确保满足区域价值成分(RVC)40%以上要求' },
          { type: '禁限管制', level: '橙色', desc: '目标国对该商品有进口配额限制' }
        ]
      }
    },
    renderGauge() {
      this.$nextTick(() => {
        if (!this.$refs.gaugeChart || !this.checkResult) return
        if (this.gaugeInstance) this.gaugeInstance.dispose()
        this.gaugeInstance = this.$echarts.init(this.$refs.gaugeChart)
        const score = this.checkResult.score
        let color = '#67C23A'
        if (score < 60) color = '#F56C6C'
        else if (score < 80) color = '#E6A23C'
        this.gaugeInstance.setOption({
          series: [{
            type: 'gauge',
            startAngle: 210,
            endAngle: -30,
            min: 0,
            max: 100,
            radius: '90%',
            progress: { show: true, width: 14, itemStyle: { color: color } },
            axisLine: { lineStyle: { width: 14, color: [[1, 'rgba(0,40,80,0.5)']] } },
            axisTick: { show: false },
            splitLine: { show: false },
            axisLabel: { show: false },
            pointer: { show: false },
            anchor: { show: false },
            title: { show: true, offsetCenter: [0, '30%'], fontSize: 14, color: '#5a7a9a' },
            detail: {
              valueAnimation: true,
              offsetCenter: [0, '-5%'],
              fontSize: 36,
              fontWeight: 'bold',
              color: color,
              formatter: '{value}分'
            },
            data: [{ value: score, name: score >= 80 ? '合规良好' : score >= 60 ? '需关注' : '风险较高' }]
          }]
        }, true)
      })
    },
    generateReport() {
      this.$message.success('合规体检报告生成中，请稍候在下载中心查看')
    },
    handleReset() {
      this.$refs.checkForm.resetFields()
      this.checkResult = null
      if (this.gaugeInstance) { this.gaugeInstance.dispose(); this.gaugeInstance = null }
    },
    getTariffRiskType(risk) {
      if (risk === '高风险') return 'danger'
      if (risk === '中风险') return 'warning'
      return 'success'
    },
    getAlertLevelType(level) {
      if (level === '红色') return 'danger'
      if (level === '橙色') return 'warning'
      return ''
    }
  }
}
</script>

<style scoped>
.compliance-check-page { padding: 0; }
.form-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.chart-title { font-size: 15px; color: #c8e6ff; margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px solid rgba(58,111,247,0.08); font-weight: 600; }
.result-card { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 20px; }
.gauge-container { width: 100%; height: 240px; }
.empty-tip { background: rgba(0,20,45,0.4); border: 1px solid rgba(58,111,247,0.08); border-radius: 8px; padding: 60px 20px; text-align: center; color: #5a7a9a; }

/* 统一暗色表格 */
.compliance-check-page .el-table { background-color: #0c1a2e !important; }
.compliance-check-page .el-table::before { background-color: #1a3a5c !important; }
.compliance-check-page .el-table th.el-table__cell,
.compliance-check-page .el-table thead th {
  background-color: #0a1525 !important;
  color: #7eb8e0 !important;
  border-bottom: 2px solid #1a4a6e !important;
}
.compliance-check-page .el-table .el-table__body tr.el-table__row > td.el-table__cell,
.compliance-check-page .el-table td.el-table__cell {
  background-color: #0f2238 !important;
  color: #d4eaff !important;
  border-bottom: 1px solid #162d48 !important;
}
.compliance-check-page .el-table .el-table__body tr:hover > td.el-table__cell {
  background-color: #183654 !important;
}
</style>


