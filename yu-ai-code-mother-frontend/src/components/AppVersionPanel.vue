<template>
  <a-drawer
    v-model:open="visible"
    title="版本历史"
    placement="right"
    :width="400"
    @close="handleClose"
  >
    <div class="version-panel">
      <div v-if="loading" class="loading-container">
        <a-spin />
      </div>
      <div v-else-if="versions.length === 0" class="empty-container">
        <a-empty description="暂无版本记录" />
      </div>
      <div v-else class="version-list">
        <div class="compare-section" v-if="versions.length >= 2">
          <a-select v-model:value="compareV1" placeholder="选择版本1" style="width: 120px">
            <a-select-option v-for="v in versions" :key="v.version" :value="v.version">
              v{{ v.version }}
            </a-select-option>
          </a-select>
          <span class="compare-vs">vs</span>
          <a-select v-model:value="compareV2" placeholder="选择版本2" style="width: 120px">
            <a-select-option v-for="v in versions" :key="v.version" :value="v.version">
              v{{ v.version }}
            </a-select-option>
          </a-select>
          <a-button type="primary" size="small" @click="handleCompare" :disabled="!canCompare">
            对比
          </a-button>
        </div>
        <a-timeline>
          <a-timeline-item
            v-for="(version, index) in sortedVersions"
            :key="version.id"
            :color="version.version === currentVersion ? 'green' : 'blue'"
          >
            <div class="version-item" :class="{ 'current': version.version === currentVersion }">
              <div class="version-header">
                <span class="version-number">v{{ version.version }}</span>
                <div class="version-badges">
                  <a-tag v-if="version.version === currentVersion" class="current-tag">当前</a-tag>
                  <a-tag v-else-if="index === 0" class="latest-tag">最新</a-tag>
                </div>
              </div>
              <div class="version-meta">
                <span class="version-time">{{ formatTime(version.createTime) }}</span>
              </div>
              <div class="version-changelog" v-if="version.changeLog">
                {{ version.changeLog }}
              </div>
              <div class="version-actions" v-if="version.version !== currentVersion">
                <a-button size="small" @click="handleRollback(version.version!)" :loading="rollbackLoading">
                  <template #icon>
                    <RollbackOutlined />
                  </template>
                  回退到此版本
                </a-button>
              </div>
            </div>
          </a-timeline-item>
        </a-timeline>
      </div>
    </div>

    <AppVersionDiff
      v-if="diffVisible"
      v-model:open="diffVisible"
      :diff-data="diffData"
      :loading="diffLoading"
    />
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { listVersions, rollbackVersion, compareVersions } from '@/api/appVersionController'
import AppVersionDiff from './AppVersionDiff.vue'
import dayjs from 'dayjs'
import { RollbackOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  open: boolean
  appId: number
  currentVersion?: number
}>()

const emit = defineEmits(['update:open', 'rollback'])

const visible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})

const loading = ref(false)
const versions = ref<API.AppVersionVO[]>([])
const rollbackLoading = ref(false)
const compareV1 = ref<number>()
const compareV2 = ref<number>()
const diffVisible = ref(false)
const diffLoading = ref(false)
const diffData = ref<API.AppVersionDiffVO>()

const canCompare = computed(() => {
  return compareV1.value && compareV2.value && compareV1.value !== compareV2.value
})

// 按版本号倒序排列（最新在前）
const sortedVersions = computed(() => {
  return [...versions.value].sort((a, b) => (b.version || 0) - (a.version || 0))
})

watch(() => props.open, (newVal) => {
  if (newVal && props.appId) {
    loadVersions()
  }
})

const loadVersions = async () => {
  loading.value = true
  try {
    const res = await listVersions({ appId: props.appId })
    if (res.data?.code === 0) {
      versions.value = res.data.data || []
    }
  } catch (e) {
    message.error('加载版本列表失败')
  } finally {
    loading.value = false
  }
}

const handleRollback = async (version: number) => {
  rollbackLoading.value = true
  try {
    const res = await rollbackVersion({ appId: props.appId, version })
    if (res.data?.code === 0) {
      message.success('回退成功')
      emit('rollback', version)
    } else {
      message.error(res.data?.message || '回退失败')
    }
  } catch (e) {
    message.error('回退失败')
  } finally {
    rollbackLoading.value = false
  }
}

const handleCompare = async () => {
  if (!canCompare.value) return
  diffLoading.value = true
  diffVisible.value = true
  try {
    const res = await compareVersions({
      appId: props.appId,
      v1: compareV1.value!,
      v2: compareV2.value!
    })
    if (res.data?.code === 0) {
      diffData.value = res.data.data
    } else {
      message.error(res.data?.message || '对比失败')
    }
  } catch (e) {
    message.error('对比失败')
  } finally {
    diffLoading.value = false
  }
}

const formatTime = (time?: string) => {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.version-panel {
  padding: 20px;
  color: rgba(255, 255, 255, 0.9);
}

:deep(.ant-drawer-content) {
  background: linear-gradient(180deg, #0d1117 0%, #161b22 100%);
}

:deep(.ant-drawer-header) {
  background: linear-gradient(180deg, #161b22 0%, #0d1117 100%);
  border-bottom: 1px solid rgba(48, 54, 61, 0.8);
  padding: 20px 24px;
}

:deep(.ant-drawer-title) {
  color: rgba(255, 255, 255, 0.95);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

:deep(.ant-drawer-close) {
  color: rgba(139, 148, 158, 0.8);
  transition: all 0.3s ease;
}

:deep(.ant-drawer-close:hover) {
  color: #fff;
  transform: rotate(90deg);
}

.loading-container,
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

:deep(.ant-empty-description) {
  color: rgba(139, 148, 158, 0.7);
  font-size: 14px;
}

.compare-section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(56, 139, 253, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
  border-radius: 12px;
  border: 1px solid rgba(56, 139, 253, 0.2);
  backdrop-filter: blur(10px);
}

:deep(.compare-section .ant-select) {
  flex: 1;
}

:deep(.compare-section .ant-select-selector) {
  background: rgba(22, 27, 34, 0.8) !important;
  border: 1px solid rgba(48, 54, 61, 0.8) !important;
  border-radius: 8px !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.compare-section .ant-select:hover .ant-select-selector) {
  border-color: rgba(56, 139, 253, 0.5) !important;
}

:deep(.compare-section .ant-select-focused .ant-select-selector) {
  border-color: #388bfd !important;
  box-shadow: 0 0 0 3px rgba(56, 139, 253, 0.15) !important;
}

.compare-vs {
  color: #8b949e;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 4px 8px;
  background: rgba(139, 148, 158, 0.15);
  border-radius: 6px;
}

:deep(.ant-timeline) {
  padding-left: 8px;
}

:deep(.ant-timeline-item-tail) {
  left: 8px;
  border-left: 2px solid rgba(48, 54, 61, 0.6);
}

:deep(.ant-timeline-item-head) {
  width: 16px;
  height: 16px;
  left: 1px;
  border-width: 3px;
}

:deep(.ant-timeline-item-head-green) {
  border-color: #3fb950;
  background: rgba(63, 185, 80, 0.2);
  box-shadow: 0 0 12px rgba(63, 185, 80, 0.4);
}

:deep(.ant-timeline-item-head-blue) {
  border-color: #388bfd;
  background: rgba(56, 139, 253, 0.2);
}

.version-item {
  padding: 16px;
  margin-left: 12px;
  background: linear-gradient(135deg, rgba(22, 27, 34, 0.9) 0%, rgba(13, 17, 23, 0.95) 100%);
  border-radius: 12px;
  border: 1px solid rgba(48, 54, 61, 0.6);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.version-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #388bfd 0%, #8b5cf6 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.version-item:hover {
  transform: translateX(4px);
  border-color: rgba(56, 139, 253, 0.4);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), 0 0 20px rgba(56, 139, 253, 0.1);
}

.version-item:hover::before {
  opacity: 1;
}

.version-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.version-number {
  font-weight: 700;
  font-size: 16px;
  background: linear-gradient(135deg, #fff 0%, #8b949e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

:deep(.version-badges) {
  display: flex;
  gap: 6px;
}

:deep(.version-header .current-tag) {
  background: linear-gradient(135deg, rgba(63, 185, 80, 0.2) 0%, rgba(46, 160, 67, 0.3) 100%);
  border: 1px solid rgba(63, 185, 80, 0.4);
  color: #3fb950;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  box-shadow: 0 0 12px rgba(63, 185, 80, 0.3);
}

:deep(.version-header .latest-tag) {
  background: linear-gradient(135deg, rgba(56, 139, 253, 0.2) 0%, rgba(139, 92, 246, 0.3) 100%);
  border: 1px solid rgba(56, 139, 253, 0.4);
  color: #58a6ff;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.version-item.current {
  border-color: rgba(63, 185, 80, 0.5);
  box-shadow: 0 0 20px rgba(63, 185, 80, 0.15);
}

.version-item.current::before {
  opacity: 1;
  background: linear-gradient(90deg, #3fb950 0%, #2ea043 100%);
}

.version-meta {
  font-size: 12px;
  color: #8b949e;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.version-meta::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 4px;
  background: #6e7681;
  border-radius: 50%;
}

.version-changelog {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 12px;
  line-height: 1.6;
  padding: 10px 12px;
  background: rgba(110, 118, 129, 0.1);
  border-radius: 8px;
  border-left: 3px solid rgba(56, 139, 253, 0.5);
}

.version-actions {
  margin-top: 12px;
}

:deep(.version-actions .ant-btn) {
  background: linear-gradient(135deg, rgba(56, 139, 253, 0.15) 0%, rgba(139, 92, 246, 0.15) 100%);
  border: 1px solid rgba(56, 139, 253, 0.3);
  color: #388bfd;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

:deep(.version-actions .ant-btn:hover) {
  background: linear-gradient(135deg, rgba(56, 139, 253, 0.25) 0%, rgba(139, 92, 246, 0.25) 100%);
  border-color: rgba(56, 139, 253, 0.5);
  color: #58a6ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(56, 139, 253, 0.2);
}

:deep(.ant-spin-dot-item) {
  background-color: #388bfd;
}
</style>
