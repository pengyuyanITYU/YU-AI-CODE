<template>
  <a-row :wrap="false" id="globalHeader">
    <!-- 左侧：Logo和标题 -->
    <a-col flex="280px">
      <RouterLink to="/">
        <div class="title-bar">
          <div class="logo-wrapper">
            <!-- Logo SVG -->
            <svg class="logo" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="fishGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#2563eb" />
                  <stop offset="50%" stop-color="#3b82f6" />
                  <stop offset="100%" stop-color="#06b6d4" />
                </linearGradient>
                <filter id="glowFilter" x="-50%" y="-50%" width="200%" height="200%">
                  <feGaussianBlur stdDeviation="2.5" result="coloredBlur" />
                  <feMerge>
                    <feMergeNode in="coloredBlur" />
                    <feMergeNode in="SourceGraphic" />
                  </feMerge>
                </filter>
              </defs>
              <g filter="url(#glowFilter)">
                <path
                  d="M75 35C75 35 65 20 45 20C25 20 15 40 15 60C15 80 30 90 50 85C70 80 80 65 80 50"
                  stroke="url(#fishGradient)"
                  stroke-width="8"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  fill="none"
                />
                <circle cx="65" cy="35" r="4" fill="#1e293b" />
                <circle cx="85" cy="30" r="3" fill="url(#fishGradient)" opacity="0.8" />
                <circle cx="92" cy="18" r="2" fill="url(#fishGradient)" opacity="0.6" />
              </g>
            </svg>
            <div class="logo-glow"></div>
          </div>
          <div class="title-content">
            <span class="title">鱼跃</span>
            <span class="subtitle">AI Powered Factory</span>
          </div>
        </div>
      </RouterLink>
    </a-col>

    <!-- 中间：导航菜单 -->
    <!-- 修复点1：保留 menu-col 用于居中 -->
    <a-col flex="auto" class="menu-col">
      <!-- 修复点2：添加 :disabled-overflow="true" 禁止菜单折叠 -->
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        @click="handleMenuClick"
        class="custom-menu"
        :disabled-overflow="true"
      />
    </a-col>

    <!-- 右侧：用户操作区域 -->
    <a-col flex="120px">
      <div class="user-login-status">
        <div v-if="loginUserStore.loginUser.id">
          <a-dropdown placement="bottomRight">
            <a-space class="user-info">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="32" class="user-avatar" />
              <span class="user-name">{{ loginUserStore.loginUser.userName ?? '无名' }}</span>
            </a-space>
            <template #overlay>
              <a-menu class="user-dropdown-menu">
                <a-menu-item key="logout" @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else>
          <a-button type="primary" href="/user/login" shape="round" class="login-btn">登录</a-button>
        </div>
      </div>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'
import { LogoutOutlined, HomeOutlined, AppstoreOutlined, UserOutlined } from '@ant-design/icons-vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const selectedKeys = ref<string[]>(['/'])

router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    icon: () => h(UserOutlined),
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage_parent',
    icon: () => h(AppstoreOutlined),
    label: '应用管理',
    title: '应用管理',
    children: [
      {
        key: '/admin/appManage',
        label: '应用列表',
        title: '应用列表',
      },
      {
        key: '/admin/appReview',
        label: '用户申请',
        title: '用户申请',
      },
    ],
  },
]

const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    if (!menu) return false
    const menuKey = menu.key as string
    // 如果是子菜单
    if ('children' in menu && menu.children) {
      menu.children = filterMenus(menu.children as MenuProps['items'])
      return menu.children && menu.children.length > 0
    }
    // 普通菜单权限检查
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

const menuItems = computed<MenuProps['items']>(() => filterMenus(JSON.parse(JSON.stringify(originItems))))

const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  // 如果是父级菜单（带 _parent 后缀），不跳转
  if (key.endsWith('_parent')) {
    return
  }
  selectedKeys.value = [key]
  if (key.startsWith('/')) {
    router.push(key)
  }
}


const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
/* ---------------------------------
  THEME: Ethereal Fluidity (液态玻璃)
  ---------------------------------
*/

#globalHeader {
  height: 72px;
  padding: 0 32px;
  position: sticky;
  top: 0;
  z-index: 100;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.85) 0%, rgba(30, 41, 59, 0.75) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 
    0 4px 6px -1px rgba(0, 0, 0, 0.3),
    0 2px 4px -1px rgba(0, 0, 0, 0.2),
    inset 0 -1px 0 rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* --- Logo 区域 --- */
.title-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  padding: 4px;
}

.logo-wrapper {
  position: relative;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 14px;
  box-shadow:
    0 8px 16px -4px rgba(59, 130, 246, 0.25),
    inset 0 0 0 1px rgba(59, 130, 246, 0.1);
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.logo {
  height: 32px;
  width: 32px;
  z-index: 2;
  animation: float 4s ease-in-out infinite;
  display: block;
}

.logo-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.2) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.5s ease;
  border-radius: inherit;
}

.title-bar:hover .logo-wrapper {
  transform: translateY(-2px) scale(1.05);
  box-shadow:
    0 12px 24px -8px rgba(59, 130, 246, 0.25),
    inset 0 0 0 1px rgba(59, 130, 246, 0.1);
}

.title-bar:hover .logo-glow {
  opacity: 1;
}

.title-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.title {
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Segoe UI", sans-serif;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.2px;
  background: linear-gradient(135deg, #ffffff 0%, rgba(59, 130, 246, 0.9) 100%);
  background-size: 200% auto;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: shine 6s linear infinite;
}

.subtitle {
  font-size: 10px;
  font-weight: 700;
  color: rgba(148, 163, 184, 0.9);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-top: 2px;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

@keyframes shine {
  to { background-position: 200% center; }
}

/*
  ---------------------------------
  修复核心：中间菜单容器
  ---------------------------------
*/
.menu-col {
  display: flex;
  justify-content: center; /* Flex 居中 */
  align-items: center;
  height: 100%;
}

/*
  ---------------------------------
  修复核心：菜单样式
  ---------------------------------
*/
.custom-menu {
  background: transparent !important;
  border-bottom: none !important;
  line-height: 72px;
  display: flex;
  margin-left: -225px;
  /* 关键修改： */
  /* 1. 宽度设为 auto 以适应内容 */
  /* 2. 配合 :disabled-overflow="true" 属性防止变成省略号 */
  width: auto !important;
  min-width: 0;
}

:deep(.ant-menu-item) {
  margin: 0 8px !important;
  padding: 0 20px !important;
  height: 42px !important;
  line-height: 42px !important;
  border-radius: 999px !important;
  border: none !important;
  color: rgba(148, 163, 184, 0.9);
  font-weight: 600;
  font-size: 15px;
  position: relative;
  top: 15px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

:deep(.ant-menu-item:hover) {
  color: rgba(255, 255, 255, 0.98) !important;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
  transform: translateY(-2px);
}

:deep(.ant-menu-item-selected) {
  background: rgba(59, 130, 246, 0.2) !important;
  color: rgba(59, 130, 246, 1) !important;
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.3),
    0 4px 12px rgba(59, 130, 246, 0.25);
}

:deep(.ant-menu-item::after) {
  display: none !important;
}

:deep(.anticon) {
  font-size: 16px;
  margin-right: 8px;
  transition: transform 0.3s ease;
}

/* 右侧用户区域 */
.user-login-status {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  height: 100%;
}

.user-info {
  cursor: pointer;
  padding: 6px 6px 6px 16px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.2);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.2);
  transform: translateY(-1px);
}

.user-name {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
}

.user-avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

.login-btn {
  height: 40px;
  padding: 0 24px;
  font-weight: 600;
  font-size: 14px;
  border: none;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  transition: all 0.3s ease;
}

:deep(.login-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.login-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.1);
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.4);
}

@media (max-width: 768px) {
  .title-content {
    display: none;
  }
}

/* User Dropdown */
:global(.user-dropdown-menu) {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.95) 100%) !important;
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.5);
  padding: 8px;
  border-radius: 12px;
}

:global(.user-dropdown-menu .ant-dropdown-menu-item) {
  color: rgba(255, 255, 255, 0.9);
  padding: 10px 16px;
  border-radius: 8px;
  margin-bottom: 2px;
  transition: all 0.2s ease;
}

:global(.user-dropdown-menu .ant-dropdown-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  transform: translateX(2px);
}
</style>
