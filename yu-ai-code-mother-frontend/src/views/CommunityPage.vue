<template>
  <div class="community-page">
    <a-card title="技术社区">
      <p>与其他程序员交流学习，共同成长</p>
      <a-divider />
      <a-tabs>
        <a-tab-pane key="discussion" tab="技术讨论">
          <a-list item-layout="horizontal" :data-source="discussions">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta
                  :title="item.title"
                  :description="item.description"
                >
                  <template #avatar>
                    <a-avatar :src="item.avatar" />
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-tab-pane>
        <a-tab-pane key="question" tab="问题求助">
          <p>在这里可以提出你遇到的技术问题</p>
          <a-button type="primary">发起提问</a-button>
        </a-tab-pane>
        <a-tab-pane key="share" tab="经验分享">
          <p>分享你的编程经验和心得</p>
          <a-button type="primary">发布分享</a-button>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
const discussions = [
  {
    title: 'Vue 3 组合式API最佳实践',
    description: '讨论Vue 3中组合式API的使用技巧和注意事项',
    avatar: 'https://joesch.moe/api/v1/random'
  },
  {
    title: '前端性能优化总结',
    description: '分享一些前端性能优化的实用技巧',
    avatar: 'https://joesch.moe/api/v1/random'
  },
  {
    title: 'TypeScript 进阶技巧',
    description: '深入探讨TypeScript的高级特性和使用场景',
    avatar: 'https://joesch.moe/api/v1/random'
  }
]
</script>

<style scoped>
.community-page {
  max-width: 1200px;
  margin: 0 auto;
}
</style>