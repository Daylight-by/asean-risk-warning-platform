Exit code: 0
Wall time: 2.1 seconds
Output:
<template>
  <div class="report-page">
    <div class="report-toolbar">
      <el-button type="primary" icon="el-icon-printer" @click="handlePrint">打印 / 导出PDF</el-button>
      <el-button icon="el-icon-back" @click="$router.go(-1)">返回</el-button>
    </div>

    <div class="report-container" v-loading="loading" ref="reportArea">
      <div class="report-header-section">
        <h1>跨境贸易合规报告</h1>
        <p class="report-subtitle">Cross-Border Trade Compliance Report</p>
        <div class="report-meta">
          <span>生成时间：{{ reportData.generatedAt || currentDate }}</span>
          <span>信用代码：{{ creditCode }}</span>
          <span v-if="targetCountry">目标国家：{{ targetCountry }}</span>
        </div>
      </div>

      <!-- 企业基本信息 -->
      <div class="report-section">
        <h2 class="section-title">一、企业基本信息</h2>
        <div class="info-grid">
          <div class="info-item"><span class="info-label">企业名称</span><span class="info-value">{{ reportData.enterpriseName || '-' }}</span></div>
          <div class="info-item"><span class="info-label">统一信用代码</span><span class="info-value">{{ reportData.creditCode || creditCode }}</span></div>
          <div class="info-item"><span class="info-label">所属行业</span><span class="info-value">{{ reportData.industry || '-' }}</span></div>
          <div class="info-item"><span class="info-label">注册资本</span><span class="info-value">{{ reportData.registerCapital || '-' }}</span></div>
          <div class="info-item"><span class="info-label">所属口岸</span><span class="info-value">{{ reportData.port || '-' }}</span></div>
          <div class="info-item"><span class="info-label">年交易额</span><span class="info-value">{{ reportData.tradeVolume || '-' }}</span></div>
        </div>
      </div>

      <!-- 风险评分 -->
      <div class="report-section">
        <h2 class="section-title">二、风险评分</h2>
        <div class="score-overview">
          <div class="score-circle" :style="{ borderColor: getScoreColor(reportData.riskScore) }">
            <span class="score-number" :style="{ color: getScoreColor(reportData.riskScore) }">{{ reportData.riskScore || '-' }}</span>
            <span class="score-label">综合风险分</span>
          </div>
          <div class="score-detail">
            <div class="score-bar-item" v-for="item in (reportData.scoreDetails || [])" :key="item.name">
              <span class="score-bar-label">{{ item.name }}</span>
              <div class="score-bar-track">
                <div class="score-bar-fill" :style="{ width: item.value + '%', backgroundColor: getScoreColor(item.value) }"></div>
              </div>
              <span class="score-bar-value">{{ item.value }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 数据可信与处置闭环 -->
      <div class="report-section">
        <h2 class="section-title">三、数据证据与闭环处置</h2>
        <div class="evidence-meta">
          <span>数据状态：{{ reportData.sourceMetadata && reportData.sourceMetadata.status || 'DEMO' }}</span>
          <span>来源：{{ reportData.sourceMetadata && reportData.sourceMetadata.sourceType || '本地样例数据与规则演示' }}</span>
          <span>采集时间：{{ reportData.sourceMetadata && reportData.sourceMetadata.collectionTime || '-' }}</span>
        </div>
        <div class="evidence-list">
          <div class="evidence-item" v-for="(item, idx) in (reportData.riskEvidence || [])" :key="idx">
            <span class="evidence-factor">{{ item.factor }}</span>
            <span class="evidence-source">来源：{{ item.source }}</span>
            <span class="evidence-check">核验：{{ item.verification }}</span>
          </div>
        </div>
        <div class="recommendation-list">
          <div class="recommendation-item" v-for="(item, idx) in (reportData.recommendations || [])" :key="idx">
            <el-tag size="mini" :type="item.priority === '高' ? 'danger' : item.priority === '中' ? 'warning' : 'success'">{{ item.priority }}优先</el-tag>
            <b>{{ item.dimension }}</b><span>{{ item.action }}</span><em>{{ item.status }}</em>
          </div>
        </div>
        <div class="review-state">
          复核状态：<b>{{ reportData.closedLoop && reportData.closedLoop.reviewStatus || '待整改' }}</b>
          <span>下一步：{{ reportData.closedLoop && reportData.closedLoop.nextAction || '生成报告并提交复核' }}</span>
        </div>
      </div>

      <!-- 欺诈预测 -->
      <div class="report-section">
        <h2 class="section-title">四、欺诈时序预测</h2>
        <div class="fraud-grid">
          <div class="fraud-item" v-for="item in (reportData.fraudPrediction || [])" :key="item.name">
            <span class="fraud-name">{{ item.name }}</span>
            <el-tag :type="getRiskTagType(item.level)" effect="dark" size="small">{{ item.level }}</el-tag>
            <span class="fraud-desc">{{ item.description || '' }}</span>
          </div>
        </div>
      </div>

      <!-- 国别风险提示 -->
      <div class="report-section" v-if="targetCountry">
        <h2 class="section-title">五、国别风险提示（{{ targetCountry }}）</h2>
        <div class="country-risk-info">
          <div class="risk-tip" v-for="(tip, idx) in (reportData.countryRiskTips || [])" :key="idx">
            <el-tag type="warning" effect="dark" size="mini">{{ tip.category }}</el-tag>
            <span class="tip-text">{{ tip.content }}</span>
          </div>
          <div v-if="!reportData.countryRiskTips || reportData.countryRiskTips.length === 0" class="empty-hint">暂无国别风险提示</div>
        </div>
      </div>

      <!-- 合规检查项 -->
      <div class="report-section">
        <h2 class="section-title">{{ targetCountry ? '六' : '五' }}、合规检查项</h2>
        <table class="check-table">
          <thead>
            <tr><th>检查项</th><th>状态</th><th>说明</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in (reportData.complianceChecks || [])" :key="item.name">
              <td>{{ item.name }}</td>
              <td>
                <el-tag :type="item.passed ? 'success' : 'danger'" effect="dark" size="mini">{{ item.passed ? '通过' : '未通过' }}</el-tag>
              </td>
              <td>{{ item.remark || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="!reportData.complianceChecks || reportData.complianceChecks.length === 0" class="empty-hint">暂无合规检查数据</div>
      </div>

      <div class="report-footer">
        <p>本报告由东盟跨境贸易风控平台自动生成，仅供参考。</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ComplianceReport',
  data() {
    return {
      loading: false,
      creditCode: '',
      targetCountry: '',
      reportData: {},
      currentDate: new Date().toLocaleString('zh-CN')
    }
  },
  created() {
    this.creditCode = this.$route.params.creditCode || ''
    this.targetCountry = this.$route.params.targetCountry || ''
    if (this.creditCode) {
      this.loadReport()
    }
  },
  methods: {
    async loadReport() {
      this.loading = true
      try {
        const res = await this.$http.post('/enterprise/compliance-report', {
          creditCode: this.creditCode,
          targetCountry: this.targetCountry
        })
        this.reportData = res.data || {}
      } catch {
        this.reportData = {
          enterpriseName: '加载中...',
          riskScore: '-',
          scoreDetails: [],
          fraudPrediction: [],
          countryRiskTips: [],
          complianceChecks: []
          ,riskEvidence: [], recommendations: [], closedLoop: {}, sourceMetadata: {}
        }
      }
      this.loading = false
    },
    getScoreColor(score) {
      if (score >= 70) return '#F56C6C'
      if (score >= 40) return '#E6A23C'
      return '#67C23A'
    },
    getRiskTagType(level) {
      if (level === '高风险') return 'danger'
      if (level === '中风险') return 'warning'
      return 'success'
    },
    handlePrint() {
      window.print()
    }
  }
}
</script>

<style scoped>
.report-page { padding: 0; }
.report-toolbar { background: rgba(0,20,45,0.6); border: 1px solid rgba(58,111,247,0.1); border-radius: 8px; padding: 16px 20px; margin-bottom: 16px; display: flex; gap: 10px; }
.report-container { background: #fff; border-radius: 8px; padding: 40px; color: #333; min-height: 600px; }
.report-header-section { text-align: center; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 2px solid #2f5be0; }
.report-header-section h1 { color: #0a1628; font-size: 24px; margin: 0 0 6px 0; }
.report-subtitle { color: #666; font-size: 14px; margin: 0 0 12px 0; }
.report-meta { display: flex; justify-content: center; gap: 24px; color: #888; font-size: 13px; }
.report-section { margin-bottom: 24px; }
.section-title { color: #0a1628; font-size: 16px; font-weight: 600; margin: 0 0 14px 0; padding-bottom: 8px; border-bottom: 1px solid #e0e0e0; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
.info-item { display: flex; flex-direction: column; }
.info-label { color: #888; font-size: 12px; margin-bottom: 2px; }
.info-value { color: #333; font-size: 14px; font-weight: 500; }
.score-overview { display: flex; align-items: center; gap: 30px; }
.score-circle { width: 100px; height: 100px; border-radius: 50%; border: 4px solid #ccc; display: flex; flex-direction: column; align-items: center; justify-content: center; flex-shrink: 0; }
.score-number { font-size: 28px; font-weight: bold; }
.score-label { font-size: 11px; color: #888; }
.score-detail { flex: 1; }
.score-bar-item { display: flex; align-items: center; margin-bottom: 8px; }
.score-bar-label { width: 100px; font-size: 13px; color: #555; flex-shrink: 0; }
.score-bar-track { flex: 1; height: 12px; background: #f0f0f0; border-radius: 6px; overflow: hidden; margin: 0 10px; }
.score-bar-fill { height: 100%; border-radius: 6px; transition: width 0.5s; }
.score-bar-value { width: 30px; font-size: 13px; color: #333; text-align: right; }
.fraud-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.fraud-item { background: #f8f9fa; border-radius: 6px; padding: 12px; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.fraud-name { font-size: 14px; font-weight: 500; color: #333; min-width: 120px; }
.fraud-desc { font-size: 12px; color: #888; width: 100%; }
.country-risk-info { }
.risk-tip { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 10px; padding: 8px; background: #fff8e6; border-radius: 4px; border-left: 3px solid #E6A23C; }
.tip-text { font-size: 13px; color: #555; }
.check-table { width: 100%; border-collapse: collapse; }
.check-table th, .check-table td { border: 1px solid #e0e0e0; padding: 8px 12px; text-align: left; font-size: 13px; }
.check-table th { background: #f5f5f5; color: #333; font-weight: 600; }
.check-table td { color: #555; }
.empty-hint { color: #999; font-size: 13px; font-style: italic; }
.report-footer { margin-top: 30px; padding-top: 16px; border-top: 1px solid #e0e0e0; text-align: center; }
.report-footer p { color: #999; font-size: 12px; margin: 0; }
.evidence-meta { display: flex; gap: 18px; flex-wrap: wrap; background: #f6f8fb; padding: 10px 12px; color: #667; font-size: 12px; border-radius: 5px; }
.evidence-item { display: grid; grid-template-columns: 1.2fr 1fr 2fr; gap: 10px; padding: 9px 0; border-bottom: 1px solid #eee; font-size: 12px; }
.evidence-factor { color: #333; font-weight: 600; }
.evidence-source, .evidence-check { color: #777; }
.recommendation-list { margin-top: 12px; }
.recommendation-item { display: flex; align-items: center; gap: 8px; padding: 8px 0; font-size: 13px; border-bottom: 1px dashed #eee; }
.recommendation-item span { color: #555; }
.recommendation-item em { margin-left: auto; color: #999; font-style: normal; font-size: 12px; }
.review-state { margin-top: 12px; padding: 10px 12px; background: #eef6ff; color: #456; font-size: 13px; }
.review-state b { color: #2f5be0; margin-right: 16px; }

@media print {
  .report-toolbar { display: none !important; }
  .report-page { padding: 0; }
  .report-container { padding: 20px; }
}
</style>

