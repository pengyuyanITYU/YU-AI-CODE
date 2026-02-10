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
        <a-space direction="vertical" align="end" :size="4">
          <a-tooltip :title="app.visualRange ? '已设置为公开' : '已设置为私有'">
            <span class="status-dot" :class="{ 'status-dot--public': app.visualRange }"></span>
          </a-tooltip>
          <a-tag v-if="app.genStatus !== undefined && app.genStatus !== AppGenStatusEnum.GENERATED_SUCCESS"
                 :color="APP_GEN_STATUS_MAP[app.genStatus as AppGenStatusEnum]?.color"
                 style="margin: 0; font-size: 10px; padding: 0 4px; line-height: 1.6;">
            {{ APP_GEN_STATUS_MAP[app.genStatus as AppGenStatusEnum]?.text }}
          </a-tag>
          <a-tag v-if="app.deployStatus !== undefined && app.deployStatus !== AppDeployStatusEnum.NOT_DEPLOYED"
                 :color="APP_DEPLOY_STATUS_MAP[app.deployStatus as AppDeployStatusEnum]?.color"
                 style="margin: 0; font-size: 10px; padding: 0 4px; line-height: 1.6;">
            {{ APP_DEPLOY_STATUS_MAP[app.deployStatus as AppDeployStatusEnum]?.text }}
          </a-tag>
          <a-tooltip v-if="app.featuredStatus === AppFeaturedStatusEnum.REJECTED && app.reviewMessage"
                     :title="'拒绝原因：' + app.reviewMessage">
            <a-tag :color="APP_FEATURED_STATUS_MAP[app.featuredStatus as AppFeaturedStatusEnum]?.color"
                   style="margin: 0; font-size: 10px; padding: 0 4px; line-height: 1.6; cursor: help;">
              {{ APP_FEATURED_STATUS_MAP[app.featuredStatus as AppFeaturedStatusEnum]?.text }}
            </a-tag>
          </a-tooltip>
          <a-tag v-else-if="app.featuredStatus !== undefined && app.featuredStatus !== AppFeaturedStatusEnum.NOT_APPLIED"
                 :color="APP_FEATURED_STATUS_MAP[app.featuredStatus as AppFeaturedStatusEnum]?.color"
                 style="margin: 0; font-size: 10px; padding: 0 4px; line-height: 1.6;">
            {{ APP_FEATURED_STATUS_MAP[app.featuredStatus as AppFeaturedStatusEnum]?.text }}
          </a-tag>
        </a-space>
      </div>
    </div>
    <div class="app-content">
      <div class="app-info-main">
        <h3 class="app-title">{{ app.appName }}</h3>
        <p class="app-desc" v-if="app.appDesc">{{ app.appDesc }}</p>
      </div>
      <div class="app-footer">
        <div class="app-author-row">
          <span class="app-author">{{ app.user?.userName || (featured ? '官方' : '未知用户') }}</span>
          <span v-if="app.chatCount !== undefined && app.chatCount > 0" class="app-chat-count">
            <MessageOutlined /> {{ app.chatCount }}
          </span>
          <span v-if="featured || app.featuredStatus === AppFeaturedStatusEnum.FEATURED" class="featured-badge">精选</span>
        </div>
        <div class="app-info-actions" v-if="!featured">
          <a-dropdown placement="topRight">
            <a-button class="more-btn" size="small" shape="circle">
              <template #icon><MoreOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="pin" @click="handleTogglePin">
                  <template #icon>
                    <PushpinOutlined v-if="app.userPriority && app.userPriority > 0" />
                    <VerticalAlignTopOutlined v-else />
                  </template>
                  {{ app.userPriority && app.userPriority > 0 ? '取消置顶' : '置顶应用' }}
                </a-menu-item>
                <a-menu-item v-if="app.featuredStatus === AppFeaturedStatusEnum.NOT_APPLIED || app.featuredStatus === AppFeaturedStatusEnum.REJECTED"
                             key="apply" @click="handleApplyFeatured">
                  <template #icon>
                    <StarOutlined />
                  </template>
                  申请精选
                </a-menu-item>
                <a-menu-divider />
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
  DeleteOutlined,
  StarOutlined,
  PushpinOutlined,
  VerticalAlignTopOutlined
} from '@ant-design/icons-vue'
import { AppDeployStatusEnum, APP_DEPLOY_STATUS_MAP, AppGenStatusEnum, APP_GEN_STATUS_MAP, AppFeaturedStatusEnum, APP_FEATURED_STATUS_MAP } from '@/utils/appStatus'

interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
  (e: 'toggle-visual-range', app: API.AppVO): void
  (e: 'delete-app', app: API.AppVO): void
  (e: 'apply-featured', app: API.AppVO): void
  (e: 'toggle-pin', app: API.AppVO): void
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

const handleApplyFeatured = () => {
  emit('apply-featured', props.app)
}

const handleTogglePin = () => {
  emit('toggle-pin', props.app)
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
  height: 100%;
  display: flex;
  flex-direction: column;
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
  flex-shrink: 0;
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

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  box-shadow: 0 0 8px rgba(148, 163, 184, 0.5);
}

.status-dot--public {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
}

.app-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.app-info-main {
  flex: 1;
}

.app-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  line-height: 1.4;
}

.app-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(148, 163, 184, 0.7);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.app-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
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

.app-chat-count {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.6);
  display: flex;
  align-items: center;
  gap: 4px;
}

.featured-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
  color: #fff;
  font-weight: 600;
}

.app-info-actions {
  display: flex;
  align-items: center;
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
