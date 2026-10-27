Exit code: 0
Wall time: 2.1 seconds
Output:
<template>
  <div class="system-page">
    <!-- 风控阈值配置 -->
    <div class="config-card">
      <h3 class="card-title">
        <i class="el-icon-setting"></i> 风控阈值配置
      </h3>
      <el-form
        ref="thresholdForm"
        :model="thresholdForm"
        label-width="180px"
        size="small"
        class="config-form"
      >
        <el-form-item label="交易异常预警阈值">
          <el-slider
            v-model="thresholdForm.tradeAbnormal"
            :min="100"
            :max="500"
            :step="10"
            show-input
          ></el-slider>
          <span class="form-tip">单日交易金额超出月均值百分比</span>
        </el-form-item>
        <el-form-item label="资金流水波动预警阈值">
          <el-slider
            v-model="thresholdForm.capitalFlow"
            :min="10"
            :max="100"
            :step="5"
            show-input
          ></el-slider>
          <span class="form-tip">近3个月资金变动幅度百分比</span>
        </el-form-item>
        <el-form-item label="关联企业风险传导阈值">
          <el-slider
            v-model="thresholdForm.relatedRisk"
            :min="30"
            :max="100"
            :step="5"
            show-input
          ></el-slider>
          <span class="form-tip">关联企业风险指数均值</span>
        </el-form-item>
        <el-form-item label="报关数据一致性阈值">
          <el-slider
            v-model="thresholdForm.customsMatch"
            :min="70"
            :max="100"
            :step="1"
            show-input
          ></el-slider>
          <span class="form-tip">报关单与物流数据匹配度百分比</span>
        </el-form-item>
        <el-form-item label="税务合规评分阈值">
          <el-slider
            v-model="thresholdForm.taxScore"
            :min="50"
            :max="100"
            :step="5"
            show-input
          ></el-slider>
          <span class="form-tip">税务申报及缴纳合规性最低分</span>
        </el-form-item>
        <el-form-item label="高风险评分阈值">
          <el-slider
            v-model="thresholdForm.highRiskScore"
            :min="60"
            :max="100"
            :step="5"
            show-input
          ></el-slider>
          <span class="form-tip">企业综合风险评分大于等于此值即为高风险</span>
        </el-form-item>
        <el-form-item label="中风险评分阈值">
          <el-slider
            v-model="thresholdForm.midRiskScore"
            :min="30"
            :max="70"
            :step="5"
            show-input
          ></el-slider>
          <span class="form-tip">企业综合风险评分在此区间即为中风险</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-check" @click="saveThreshold">保存阈值配置</el-button>
          <el-button icon="el-icon-refresh" @click="resetThreshold">恢复默认</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据源连接配置 -->
    <div class="config-card">
      <h3 class="card-title">
        <i class="el-icon-connection"></i> 数据源连接配置
      </h3>
      <el-form
        ref="datasourceForm"
        :model="datasourceForm"
        label-width="140px"
        size="small"
        class="config-form"
      >
        <el-form-item label="海关数据接口">
          <el-input v-model="datasourceForm.customsApi">
            <template slot="prepend">URL</template>
          </el-input>
        </el-form-item>
        <el-form-item label="API密钥">
          <el-input v-model="datasourceForm.customsApiKey" type="password" show-password>
            <template slot="prepend">
              <i class="el-icon-key"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="success" size="small" icon="el-icon-link" @click="testConnection('customs')">测试连接</el-button>
        </el-form-item>

        <el-divider></el-divider>

        <el-form-item label="税务数据接口">
          <el-input v-model="datasourceForm.taxApi">
            <template slot="prepend">URL</template>
          </el-input>
        </el-form-item>
        <el-form-item label="API密钥">
          <el-input v-model="datasourceForm.taxApiKey" type="password" show-password>
            <template slot="prepend">
              <i class="el-icon-key"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="success" size="small" icon="el-icon-link" @click="testConnection('tax')">测试连接</el-button>
        </el-form-item>

        <el-divider></el-divider>

        <el-form-item label="工商数据接口">
          <el-input v-model="datasourceForm.businessApi">
            <template slot="prepend">URL</template>
          </el-input>
        </el-form-item>
        <el-form-item label="API密钥">
          <el-input v-model="datasourceForm.businessApiKey" type="password" show-password>
            <template slot="prepend">
              <i class="el-icon-key"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="success" size="small" icon="el-icon-link" @click="testConnection('business')">测试连接</el-button>
        </el-form-item>

        <el-divider></el-divider>

        <el-form-item label="预警通知">
          <el-switch
            v-model="datasourceForm.notifyEnabled"
            active-text="启用"
            inactive-text="关闭"
          ></el-switch>
        </el-form-item>
        <el-form-item label="通知方式" v-if="datasourceForm.notifyEnabled">
          <el-checkbox-group v-model="datasourceForm.notifyChannels">
            <el-checkbox label="站内信">站内信</el-checkbox>
            <el-checkbox label="短信">短信</el-checkbox>
            <el-checkbox label="邮件">邮件</el-checkbox>
            <el-checkbox label="企业微信">企业微信</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="自动扫描间隔" v-if="datasourceForm.notifyEnabled">
          <el-select v-model="datasourceForm.scanInterval">
            <el-option label="每15分钟" :value="15"></el-option>
            <el-option label="每30分钟" :value="30"></el-option>
            <el-option label="每1小时" :value="60"></el-option>
            <el-option label="每4小时" :value="240"></el-option>
            <el-option label="每天" :value="1440"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="el-icon-check" @click="saveDatasource">保存数据源配置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SystemConfig',
  data() {
    return {
      thresholdForm: {
        tradeAbnormal: 300,
        capitalFlow: 50,
        relatedRisk: 60,
        customsMatch: 90,
        taxScore: 80,
        highRiskScore: 70,
        midRiskScore: 40
      },
      defaultThreshold: {
        tradeAbnormal: 300,
        capitalFlow: 50,
        relatedRisk: 60,
        customsMatch: 90,
        taxScore: 80,
        highRiskScore: 70,
        midRiskScore: 40
      },
      datasourceForm: {
        customsApi: '',
        customsApiKey: '',
        taxApi: '',
        taxApiKey: '',
        businessApi: '',
        businessApiKey: '',
        notifyEnabled: true,
        notifyChannels: ['站内信', '短信'],
        scanInterval: 30
      }
    }
  },
  methods: {
    async saveThreshold() {
      try {
        await this.$http.post('/system/threshold', this.thresholdForm)
        this.$message.success('风控阈值配置已保存')
      } catch (e) {
        // 后端接口待完善，仅前端提示
        this.$message.success('风控阈值配置已保存（本地）')
      }
    },

    resetThreshold() {
      this.thresholdForm = { ...this.defaultThreshold }
      this.$message.success('已恢复默认阈值配置')
    },

    async saveDatasource() {
      try {
        // 调用后端接口保存配置
        const res = await this.$http.post('/datasource/configs', this.datasourceForm)
        if (res.success !== false) {
          this.$message.success('数据源配置已保存')
        } else {
          this.$message.error('保存失败: ' + (res.message || '未知错误'))
        }
      } catch (error) {
        this.$message.error('保存失败: ' + (error.message || '网络错误'))
      }
    },

    async testConnection(source) {
      const nameMap = {
        customs: '海关数据接口',
        tax: '税务数据接口',
        business: '工商数据接口'
      }

      const urlMap = {
        customs: this.datasourceForm.customsApi,
        tax: this.datasourceForm.taxApi,
        business: this.datasourceForm.businessApi
      }

      const keyMap = {
        customs: this.datasourceForm.customsApiKey,
        tax: this.datasourceForm.taxApiKey,
        business: this.datasourceForm.businessApiKey
      }

      const loading = this.$loading({
        lock: true,
        text: `正在测试${nameMap[source]}连接...`,
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.6)'
      })

      try {
        // 调用后端接口测试连接
        const res = await this.$http.post('/datasource/test', {
          type: source,
          url: urlMap[source],
          apiKey: keyMap[source]
        })

        loading.close()

        if (res.success !== false) {
          this.$message.success(`${nameMap[source]}连接测试成功`)
        } else {
          this.$message.error(`${nameMap[source]}连接测试失败: ${res.message || '未知错误'}`)
        }
      } catch (error) {
        loading.close()
        this.$message.error(`${nameMap[source]}连接测试失败: ${error.message || '网络错误'}`)
      }
    }
  }
}
</script>

<style scoped>
.system-page {
  max-width: 900px;
}

.config-card {
  background: rgba(0,20,45,0.6);
  border: 1px solid rgba(58,111,247,0.1);
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  color: #c8e6ff;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(58,111,247,0.08);
  font-weight: 600;
}

.card-title i {
  margin-right: 8px;
  color: #3a6ff7;
}

.config-form {
  padding-right: 40px;
}

.form-tip {
  font-size: 12px;
  color: #5a7a9a;
  margin-left: 10px;
}
</style>

