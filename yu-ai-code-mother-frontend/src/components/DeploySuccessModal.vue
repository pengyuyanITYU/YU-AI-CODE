<template>
  <a-modal v-model:open="visible" title="部署成功" :footer="null" width="600px">
    <div class="deploy-success">
      <div class="success-icon">
        <CheckCircleOutlined style="color: #52c41a; font-size: 48px" />
      </div>
      <h3>网站部署成功！</h3>
      <p>你的网站已经成功部署，可以通过以下链接访问：</p>
      <div class="deploy-url">
        <a-input :value="deployUrl" readonly class="deploy-url-input">
          <template #suffix>
            <a-button type="text" @click="handleCopyUrl">
              <CopyOutlined />
            </a-button>
          </template>
        </a-input>
      </div>
      <div class="deploy-actions">
        <a-button type="primary" @click="handleOpenSite">访问网站</a-button>
        <a-button @click="handleClose">关闭</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleOutlined, CopyOutlined } from '@ant-design/icons-vue'

interface Props {
  open: boolean
  deployUrl: string
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'open-site'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const handleCopyUrl = async () => {
  try {
    await navigator.clipboard.writeText(props.deployUrl)
    message.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败：', error)
    message.error('复制失败')
  }
}

const handleOpenSite = () => {
  emit('open-site')
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>

.deploy-success {
  text-align: center;
  padding: 24px;
}

.success-icon {
  margin-bottom: 16px;
}

.deploy-success h3 {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.98);
}

.deploy-success p {
  margin: 0 0 24px;
  color: rgba(148, 163, 184, 0.9);
}

.deploy-url {
  margin-bottom: 24px;
}

:deep(.deploy-url-input),
:deep(.deploy-url-input.ant-input-affix-wrapper),
:deep(.deploy-url .ant-input-affix-wrapper) {
  background-color: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(148, 163, 184, 0.2) !important;
  color: rgba(255, 255, 255, 0.95) !important;
  border-radius: 8px;
}

:deep(.deploy-url-input input),
:deep(.deploy-url .ant-input-affix-wrapper input) {
  background: transparent !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

:deep(.deploy-url-input:hover),
:deep(.deploy-url .ant-input-affix-wrapper:hover) {
  background-color: rgba(255, 255, 255, 0.12) !important;
  border-color: rgba(59, 130, 246, 0.6) !important;
}

.deploy-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
