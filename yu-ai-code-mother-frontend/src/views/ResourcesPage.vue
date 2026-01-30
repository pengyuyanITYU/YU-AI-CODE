<template>
  <div class="resources-page">
    <a-card title="编程资源">
      <p>这里汇集了各种优质的编程学习资源</p>
      <a-divider />
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-card hoverable>
            <template #title>
              <span>前端开发</span>
            </template>
            <p>Vue、React、Angular等前端技术资源</p>
            <a-button type="link">查看更多</a-button>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-card hoverable>
            <template #title>
              <span>后端开发</span>
            </template>
            <p>Java、Python、Node.js等后端技术资源</p>
            <a-button type="link">查看更多</a-button>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-card hoverable>
            <template #title>
              <span>数据库</span>
            </template>
            <p>MySQL、MongoDB、Redis等数据库资源</p>
            <a-button type="link">查看更多</a-button>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-card hoverable>
            <template #title>
              <span>工具推荐</span>
            </template>
            <p>开发工具、效率工具推荐</p>
            <a-button type="link">查看更多</a-button>
          </a-card>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.resources-page {
  max-width: 1200px;
  margin: 0 auto;
}
</style>