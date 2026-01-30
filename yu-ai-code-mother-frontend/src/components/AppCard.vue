<template>
  <div class="app-card" :class="{ 'app-card--featured': featured }">
    <div class="app-preview">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
      <div v-else class="app-placeholder">
        <span>🤖</span>
      </div>
      <div class="app-overlay">
        <div class="overlay-actions">
          <a-button type="primary" shape="round" @click="handleViewChat">
            <template #icon><MessageOutlined /></template>
            对话
          </a-button>
          <a-button v-if="app.deployKey" shape="round" @click="handleViewWork">
            <template #icon><EyeOutlined /></template>
            预览
          </a-button>
          <a-button v-if="!featured" shape="round" @click="handleEdit">
            <template #icon><EditOutlined /></template>
            编辑
          </a-button>
        </div>
      </div>
      <div class="status-badge" v-if="!featured">
        <a-tooltip :title="app.visualRange ? '已设置为公开' : '已设置为私有'">
          <span class="status-dot" :class="{ 'status-dot--public': app.visualRange }"></span>
        </a-tooltip>
      </div>
    </div>
    <div class="app-info">
      <div class="app-info-main">
        <a-avatar :src="app.user?.userAvatar" :size="36" class="app-avatar">
          {{ app.user?.userName?.charAt(0) || 'U' }}
        </a-avatar>
        <div class="app-meta">
          <h3 class="app-title" :title="app.appName">{{ app.appName || '未命名应用' }}</h3>
          <div class="app-author-row">
            <span class="app-author">{{ app.user?.userName || (featured ? '官方' : '未知用户') }}</span>
            <span v-if="featured" class="featured-badge">精选</span>
          </div>
        </div>
      </div>
      <div class="app-actions" v-if="!featured">
        <a-dropdown :trigger="['click']">
          <a-button shape="circle" size="small" class="more-btn">
            <template #icon><MoreOutlined /></template>
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="toggle" @click="handleToggleVisualRange">
                <template #icon>
                  <GlobalOutlined v-if="!app.visualRange" />
                  <LockOutlined v-else />
                </template>
                {{ app.visualRange ? '设为私有' : '设为公开' }}
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="delete" @click="showDeleteConfirm" danger>
                <template #icon><DeleteOutlined /></template>
                删除应用
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Modal } from 'ant-design-vue'
import { 
  MessageOutlined, 
  EyeOutlined, 
  EditOutlined, 
  MoreOutlined,
  GlobalOutlined,
  LockOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'

interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
  (e: 'toggle-visual-range', app: API.AppVO): void
  (e: 'delete-app', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<Emits>()
const router = useRouter()

const handleViewChat = () => {
  emit('view-chat', props.app.id)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}

const handleEdit = () => {
  if (props.app.id) {
    router.push(`/app/edit/${props.app.id}`)
  }
}

const handleToggleVisualRange = () => {
  emit('toggle-visual-range', props.app)
}

const showDeleteConfirm = () => {
  Modal.confirm({
    title: '确定删除这个应用吗？',
    content: '删除后将无法恢复，请谨慎操作。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk() {
      emit('delete-app', props.app)
    },
  })
}
</script>

<style scoped>
.app-card {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.85) 100%);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.15);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 32px 80px rgba(59, 130, 246, 0.25);
  border-color: rgba(59, 130, 246, 0.3);
}

.app-preview {
  height: 180px;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.app-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.app-card:hover .app-preview img {
  transform: scale(1.05);
}

.app-placeholder {
  font-size: 48px;
  color: rgba(148, 163, 184, 0.4);
}

.app-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.3) 0%, rgba(0, 0, 0, 0.7) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.overlay-actions {
  display: flex;
  gap: 8px;
}

.overlay-actions :deep(.ant-btn) {
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.overlay-actions :deep(.ant-btn-default) {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.overlay-actions :deep(.ant-btn-default:hover) {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.4);
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
}

.status-dot {
  display: block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.6);
  box-shadow: 0 0 8px rgba(148, 163, 184, 0.4);
  transition: all 0.3s ease;
}

.status-dot--public {
  background: #22c55e;
  box-shadow: 0 0 12px rgba(34, 197, 94, 0.5);
}

.app-info {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.app-info-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.app-avatar {
  flex-shrink: 0;
  border: 2px solid rgba(148, 163, 184, 0.2);
}

.app-meta {
  flex: 1;
  min-width: 0;
}

.app-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: rgba(255, 255, 255, 0.98);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.app-author-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-author {
  font-size: 13px;
  color: rgba(148, 163, 184, 0.8);
}

.featured-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
  color: #fff;
  font-weight: 600;
}

.app-actions {
  flex-shrink: 0;
}

.more-btn {
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(148, 163, 184, 0.2) !important;
  color: rgba(148, 163, 184, 0.9) !important;
  transition: all 0.2s ease;
}

.more-btn:hover {
  background: rgba(59, 130, 246, 0.15) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
  color: rgba(59, 130, 246, 1) !important;
}

:deep(.ant-dropdown-menu) {
  background: rgba(30, 41, 59, 0.95) !important;
  border: 1px solid rgba(148, 163, 184, 0.15) !important;
  backdrop-filter: blur(12px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4) !important;
}

:deep(.ant-dropdown-menu-item) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-dropdown-menu-item:hover) {
  background: rgba(59, 130, 246, 0.15) !important;
}

:deep(.ant-dropdown-menu-item-danger) {
  color: #ef4444 !important;
}

:deep(.ant-dropdown-menu-item-danger:hover) {
  background: rgba(239, 68, 68, 0.15) !important;
}
</style>
