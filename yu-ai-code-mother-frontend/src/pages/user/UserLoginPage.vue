<template>
  <div id="userLoginPage" class="auth-page">
    <div class="auth-bg">
      <div class="auth-orb auth-orb-1"></div>
      <div class="auth-orb auth-orb-2"></div>
      <div class="auth-grid"></div>
    </div>

    <div class="auth-shell">
      <div class="auth-panel">
        <div class="auth-brand">
          <div class="auth-logo-wrapper">
            <svg class="auth-logo-svg" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="fishGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#3b82f6" />
                  <stop offset="50%" stop-color="#06b6d4" />
                  <stop offset="100%" stop-color="#8b5cf6" />
                </linearGradient>
              </defs>
              <path
                d="M75 35C75 35 65 20 45 20C25 20 15 40 15 60C15 80 30 90 50 85C70 80 80 65 80 50"
                stroke="url(#fishGradient)"
                stroke-width="8"
                stroke-linecap="round"
                stroke-linejoin="round"
                fill="none"
              />
              <circle cx="65" cy="35" r="4" fill="#1e293b" />
            </svg>
          </div>
          <div class="auth-brand-text">
            <div class="auth-brand-title">鱼跃</div>
            <div class="auth-brand-sub">AI Powered Factory</div>
          </div>
        </div>
        <div class="auth-features">
          <div class="auth-feature-item">
            <div class="auth-feature-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
              </svg>
            </div>
            <div class="auth-feature-text">
              <div class="auth-feature-title">极速生成</div>
              <div class="auth-feature-desc">AI 驱动，秒级响应</div>
            </div>
          </div>
          <div class="auth-feature-item">
            <div class="auth-feature-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </div>
            <div class="auth-feature-text">
              <div class="auth-feature-title">智能对话</div>
              <div class="auth-feature-desc">自然语言，轻松交互</div>
            </div>
          </div>
          <div class="auth-feature-item">
            <div class="auth-feature-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <div class="auth-feature-text">
              <div class="auth-feature-title">安全可靠</div>
              <div class="auth-feature-desc">数据加密，隐私保护</div>
            </div>
          </div>
        </div>
        <div class="auth-slogan">
          <div class="auth-slogan-title">让想法跃然成真</div>
          <div class="auth-slogan-desc">登录鱼跃，开启您的 AI 应用创作之旅</div>
        </div>
      </div>

      <div class="auth-card">
        <div class="auth-card-header">
          <div class="auth-title">欢迎回到鱼跃</div>
          <div class="auth-desc">登录您的账号以继续使用</div>
        </div>

        <a-form
          :model="formState"
          name="basic"
          autocomplete="off"
          layout="vertical"
          @finish="handleSubmit"
          class="auth-form"
        >
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input
              v-model:value="formState.userAccount"
              placeholder="请输入账号"
              size="large"
              class="auth-input"
            >
              <template #prefix>
                <UserOutlined class="auth-input-icon" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              placeholder="请输入密码"
              size="large"
              class="auth-input"
            >
              <template #prefix>
                <LockOutlined class="auth-input-icon" />
              </template>
            </a-input-password>
          </a-form-item>

          <div class="auth-row">
            <a-checkbox class="auth-muted">记住我</a-checkbox>
            <RouterLink to="/user/register" class="auth-link">去注册</RouterLink>
          </div>

          <a-form-item class="auth-submit-item">
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              class="auth-submit"
              :loading="loading"
            >
              登 录
            </a-button>
          </a-form-item>
        </a-form>

        <div class="auth-social">
          <span class="auth-divider">其他方式登录</span>
          <div class="auth-social-icons">
            <div class="auth-icon-btn"><GithubOutlined /></div>
            <div class="auth-icon-btn"><WechatOutlined /></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { userLogin } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, GithubOutlined, WechatOutlined } from '@ant-design/icons-vue'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(false)

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  loading.value = true
  try {
    const res = await userLogin(values)
    // 登录成功，把登录态保存到全局状态中
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功，欢迎回来！')
      router.push({
        path: '/',
        replace: true,
      })
    } else {
      message.error('登录失败，' + res.data.message)
    }
  } catch (error) {
    message.error('登录请求失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.auth-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(1400px 900px at 5% 15%, rgba(59, 130, 246, 0.15), transparent 60%),
    radial-gradient(1200px 800px at 95% 5%, rgba(139, 92, 246, 0.15), transparent 60%),
    radial-gradient(1000px 700px at 50% 95%, rgba(6, 182, 212, 0.12), transparent 60%),
    linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
}

.auth-grid {
  position: absolute;
  inset: -2px;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.10) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.10) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(600px 420px at 50% 40%, rgba(0,0,0,0.85), transparent 70%);
  opacity: 0.35;
}

.auth-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.65;
  animation: authFloat 10s infinite ease-in-out alternate;
}

.auth-orb-1 {
  width: 420px;
  height: 420px;
  top: -140px;
  left: -120px;
  background: rgba(56, 189, 248, 0.9);
}

.auth-orb-2 {
  width: 360px;
  height: 360px;
  bottom: -140px;
  right: -140px;
  background: rgba(168, 85, 247, 0.9);
  animation-delay: -5s;
}

@keyframes authFloat {
  0% { transform: translate(0, 0); }
  100% { transform: translate(26px, 38px); }
}

.auth-shell {
  position: relative;
  z-index: 1;
  width: min(980px, calc(100% - 32px));
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 0;
  align-items: stretch;
}

.auth-panel {
  border-radius: 24px 0 0 24px;
  padding: 48px 40px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.75) 0%, rgba(30, 41, 59, 0.65) 100%);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-right: none;
  box-shadow: 
    0 32px 80px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
  min-height: 560px;
  position: relative;
  overflow: hidden;
}

.auth-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.5), transparent);
}

.auth-brand {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 48px;
}

.auth-logo-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border: 1px solid rgba(148, 163, 184, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.2);
}

.auth-logo-svg {
  width: 36px;
  height: 36px;
}

.auth-brand-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.auth-brand-title {
  color: rgba(255, 255, 255, 0.98);
  font-weight: 800;
  font-size: 24px;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #ffffff 0%, rgba(59, 130, 246, 0.9) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-brand-sub {
  color: rgba(148, 163, 184, 0.9);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.auth-features {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
  margin: 40px 0;
}

.auth-feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(148, 163, 184, 0.12);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.auth-feature-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, #3b82f6, #8b5cf6);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.auth-feature-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateX(6px);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.15);
}

.auth-feature-item:hover::before {
  opacity: 1;
}

.auth-feature-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(139, 92, 246, 0.15) 100%);
  border-radius: 12px;
  flex-shrink: 0;
  color: rgba(59, 130, 246, 0.9);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.auth-feature-text {
  flex: 1;
}

.auth-feature-title {
  color: rgba(255, 255, 255, 0.98);
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 6px;
  letter-spacing: -0.3px;
}

.auth-feature-desc {
  color: rgba(148, 163, 184, 0.85);
  font-size: 13px;
  line-height: 1.6;
}

.auth-slogan-title {
  color: rgba(255, 255, 255, 0.98);
  font-size: 36px;
  font-weight: 800;
  letter-spacing: -0.8px;
  line-height: 1.2;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #ffffff 0%, rgba(59, 130, 246, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-slogan-desc {
  color: rgba(148, 163, 184, 0.9);
  font-size: 15px;
  line-height: 1.7;
  max-width: 100%;
}

.auth-card {
  border-radius: 0 24px 24px 0;
  padding: 48px 40px;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.85) 100%);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-left: none;
  box-shadow: 
    0 32px 80px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  animation: authIn 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.auth-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.5), transparent);
}

@keyframes authIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.auth-card-header {
  margin-bottom: 18px;
}

.auth-title {
  font-size: 28px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.98);
  letter-spacing: -0.5px;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #ffffff 0%, rgba(59, 130, 246, 0.9) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-desc {
  font-size: 14px;
  color: rgba(148, 163, 184, 0.9);
  font-weight: 500;
}

.auth-form {
  margin-top: 14px;
}

:deep(.auth-input.ant-input-affix-wrapper) {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.6) 0%, rgba(15, 23, 42, 0.8) 100%) !important;
  border: 1px solid rgba(148, 163, 184, 0.15) !important;
  border-radius: 14px;
  padding: 12px 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

:deep(.auth-input.ant-input-affix-wrapper .ant-input) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0 !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

:deep(.auth-input.ant-input-affix-wrapper .ant-input::placeholder) {
  color: rgba(148, 163, 184, 0.5) !important;
  font-weight: 400;
}

:deep(.auth-input.ant-input-affix-wrapper:hover) {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.9) 100%) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
  box-shadow: 
    0 8px 24px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(59, 130, 246, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}

:deep(.auth-input.ant-input-affix-wrapper:focus),
:deep(.auth-input.ant-input-affix-wrapper-focused) {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.95) 100%) !important;
  border-color: rgba(59, 130, 246, 0.6) !important;
  box-shadow: 
    0 0 0 4px rgba(59, 130, 246, 0.15),
    0 0 24px rgba(59, 130, 246, 0.2),
    0 12px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  transform: translateY(-1px);
}

:deep(.auth-input .ant-input-password-icon) {
  color: rgba(148, 163, 184, 0.7);
  transition: all 0.2s ease;
}

:deep(.auth-input .ant-input-password-icon:hover) {
  color: rgba(59, 130, 246, 0.9);
  transform: scale(1.1);
}

.auth-input-icon {
  color: rgba(148, 163, 184, 0.8);
  margin-right: 6px;
}

.auth-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 6px 0 18px;
  font-size: 13px;
}

.auth-muted {
  color: rgba(148, 163, 184, 0.9);
}

:deep(.auth-muted .ant-checkbox-checked .ant-checkbox-inner) {
  background-color: #3b82f6;
  border-color: #3b82f6;
}

:deep(.auth-muted .ant-checkbox-wrapper) {
  color: rgba(148, 163, 184, 0.9);
}

.auth-link {
  color: rgba(59, 130, 246, 0.9);
  font-weight: 600;
  transition: color 0.18s ease;
}

.auth-link:hover {
  color: rgba(59, 130, 246, 1);
}

.auth-submit-item {
  margin-bottom: 12px;
}

.auth-submit {
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  border: none;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.3px;
  box-shadow: 0 12px 28px rgba(59, 130, 246, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.auth-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 36px rgba(59, 130, 246, 0.4);
  background: linear-gradient(135deg, #2563eb 0%, #7c3aed 100%);
}

.auth-social {
  text-align: center;
  margin-top: 16px;
}

.auth-divider {
  display: block;
  color: rgba(148, 163, 184, 0.7);
  font-size: 12px;
  margin-bottom: 14px;
  position: relative;
}

.auth-divider::before,
.auth-divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background: rgba(148, 163, 184, 0.2);
}

.auth-divider::before { left: 0; }
.auth-divider::after { right: 0; }

.auth-social-icons {
  display: flex;
  justify-content: center;
  gap: 14px;
}

.auth-icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(148, 163, 184, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease, color 0.18s ease, border-color 0.18s ease;
}

.auth-icon-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: rgba(59, 130, 246, 1);
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 10px 22px rgba(59, 130, 246, 0.2);
  transform: translateY(-2px);
}

@media (max-width: 900px) {
  .auth-shell {
    grid-template-columns: 1fr;
    max-width: 520px;
  }
  .auth-panel {
    display: none;
  }
}
</style>
