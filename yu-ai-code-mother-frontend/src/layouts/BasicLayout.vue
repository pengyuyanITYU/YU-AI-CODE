<template>
  <a-layout class="basic-layout">
  <!-- 顶部导航栏 - 仅非管理员显示 -->
    <GlobalHeader v-if="!$route.meta.hideLayout && !isAdmin" />
    <!-- 主要内容区域 -->
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>
    <!-- 底部版权信息 - 仅非管理员显示 -->
    <GlobalFooter  v-if="!$route.meta.hideLayout && !isAdmin"/>
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useLoginUserStore } from '@/stores/loginUser'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const loginUserStore = useLoginUserStore()

// 是否为管理员
const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})
</script>

<style scoped>
.basic-layout {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  min-height: 100vh;
}

.main-content {
  width: 100%;
  padding: 0;
  background: transparent;
  margin: 0;
}
</style>
