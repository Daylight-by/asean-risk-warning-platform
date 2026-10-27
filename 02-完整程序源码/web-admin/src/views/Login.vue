Exit code: 0
Wall time: 2.3 seconds
Output:
<template>
  <div class="login-container">
    <!-- 背景动效 -->
    <div class="bg-grid"></div>
    <div class="bg-glow"></div>

    <div class="login-card">
      <div class="card-header">
        <div class="logo-icon">
          <i class="el-icon-s-data"></i>
        </div>
        <h2 class="login-title">东盟跨境中小商户智能风控预警平台</h2>
        <p class="login-subtitle">ASEAN Risk Control Platform · 政务监管端</p>
      </div>

      <el-form
        ref="loginForm"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter.native="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            prefix-icon="el-icon-user"
          ></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <p class="login-tip">初始账号: admin / 密码: 123456</p>
      <div class="card-footer">
        <span>数据来源：广西公共数据开放平台</span>
        <span>南宁海关 · 广西市场监管局</span>
      </div>
    </div>
  </div>
</template>

<script>
import { login } from '@/api/index'

export default {
  name: 'Login',
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false
    }
  },
  methods: {
    async handleLogin() {
      this.$refs.loginForm.validate(async (valid) => {
        if (!valid) return

        this.loading = true
        try {
          const res = await this.$http.post('/login', this.form)
          const token = res.data?.token || res.token
          if (token) {
            sessionStorage.setItem('token', token)
            sessionStorage.setItem('username', this.form.username)
            this.$message.success('登录成功')
            this.$router.push('/dashboard')
          } else {
            this.$message.error('登录失败：未返回有效凭证')
          }
        } catch (e) {
          this.$message.error(e.response?.data?.message || e.message || '账号或密码错误')
        }
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #060e1a;
  position: relative;
  overflow: hidden;
}

/* 网格背景 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(58,111,247,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(58,111,247,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
}

/* 中心辉光 */
.bg-glow {
  position: absolute;
  width: 800px;
  height: 800px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0,120,200,0.12) 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: glowPulse 6s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.6; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 1; transform: translate(-50%, -50%) scale(1.1); }
}

.login-card {
  width: 440px;
  padding: 48px 40px 36px;
  background: rgba(8,20,36,0.92);
  border: 1px solid rgba(58,111,247,0.12);
  border-radius: 16px;
  box-shadow: 0 0 60px rgba(0,120,200,0.08), 0 20px 60px rgba(0,0,0,0.4);
  position: relative;
  z-index: 1;
  backdrop-filter: blur(20px);
}

.card-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 18px;
  background: linear-gradient(135deg, rgba(58,111,247,0.15), rgba(0,120,200,0.08));
  border: 1px solid rgba(58,111,247,0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: #3a6ff7;
  box-shadow: 0 0 24px rgba(58,111,247,0.1);
}

.login-title {
  font-size: 18px;
  color: #e0f0ff;
  font-weight: 600;
  letter-spacing: 1px;
  margin: 0 0 8px;
  line-height: 1.4;
}

.login-subtitle {
  font-size: 12px;
  color: rgba(58,111,247,0.45);
  letter-spacing: 1px;
  margin: 0;
}

.login-form {
  margin-top: 0;
}

/* 输入框暗色风格 */
.login-card >>> .el-input__inner {
  background-color: rgba(0,20,45,0.6) !important;
  border-color: rgba(58,111,247,0.12) !important;
  color: #e0f0ff !important;
  height: 44px;
  border-radius: 8px;
}

.login-card >>> .el-input__inner:focus {
  border-color: rgba(58,111,247,0.4) !important;
  box-shadow: 0 0 0 2px rgba(58,111,247,0.08);
}

.login-card >>> .el-input__inner::placeholder {
  color: #3a5a7a !important;
}

.login-card >>> .el-input__prefix {
  color: rgba(58,111,247,0.4) !important;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 6px;
  border-radius: 8px !important;
  background: linear-gradient(135deg, #2f5be0, #00a3d9) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(0,120,200,0.3);
  transition: all 0.3s;
}

.login-btn:hover {
  box-shadow: 0 6px 24px rgba(0,160,220,0.4);
  transform: translateY(-1px);
}

.login-tip {
  text-align: center;
  font-size: 12px;
  color: rgba(58,111,247,0.3);
  margin-top: 8px;
}

.card-footer {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(58,111,247,0.06);
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 11px;
  color: rgba(255,255,255,0.2);
}
</style>


