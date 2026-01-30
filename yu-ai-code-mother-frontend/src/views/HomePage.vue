<template>
  <div class="home-page">
    <a-card title="欢迎来到编程导航">
      <p>这是首页内容区域</p>
      <p>在这里可以展示一些重要的信息或者功能介绍</p>
      <a-button type="primary" @click="handleClick">开始使用</a-button>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'

const handleClick = () => {
  message.success('欢迎使用编程导航！')
}
</script>

<style scoped>
.home-page {
  max-width: 1200px;
  margin: 0 auto;
}
</style>