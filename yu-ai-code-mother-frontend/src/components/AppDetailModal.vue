<template>
  <a-modal v-model:open="visible" title="应用详情" :footer="null" width="500px">
    <div class="app-detail-content">
      <!-- 应用基础信息 -->
      <div class="app-basic-info">
        <div class="info-item">
          <span class="info-label">创建者：</span>
          <UserInfo :user="app?.user" size="small" />
        </div>
        <div class="info-item">
          <span class="info-label">创建时间：</span>
          <span>{{ formatTime(app?.createTime) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">生成类型：</span>
          <a-tag v-if="app?.codeGenType" color="blue">
            {{ formatCodeGenType(app.codeGenType) }}
          </a-tag>
          <span v-else>未知类型</span>
        </div>
        <div class="info-item" v-if="app?.genStatus !== undefined">
          <span class="info-label">生成状态：</span>
          <a-tag :color="APP_GEN_STATUS_MAP[app.genStatus as AppGenStatusEnum]?.color">
            {{ APP_GEN_STATUS_MAP[app.genStatus as AppGenStatusEnum]?.text }}
          </a-tag>
        </div>
        <div class="info-item" v-if="app?.deployStatus !== undefined">
          <span class="info-label">部署状态：</span>
          <a-tag :color="APP_DEPLOY_STATUS_MAP[app.deployStatus as AppDeployStatusEnum]?.color">
            {{ APP_DEPLOY_STATUS_MAP[app.deployStatus as AppDeployStatusEnum]?.text }}
          </a-tag>
          <a-space v-if="showActions && app?.deployKey" style="margin-left: 8px">
            <a-button 
              v-if="app.deployStatus === AppDeployStatusEnum.OFFLINE" 
              size="small" 
              type="link" 
              :loading="deployControlLoading"
              @click="handleControlDeploy(AppDeployStatusEnum.ONLINE)"
            >
              上线
            </a-button>
            <a-button 
              v-if="app.deployStatus === AppDeployStatusEnum.ONLINE" 
              size="small" 
              type="link" 
              danger 
              :loading="deployControlLoading"
              @click="handleControlDeploy(AppDeployStatusEnum.OFFLINE)"
            >
              下线
            </a-button>
          </a-space>
        </div>
      </div>

      <!-- 操作栏（仅本人或管理员可见） -->
      <div v-if="showActions" class="app-actions">
        <a-space>
          <a-button type="primary" @click="handleEdit">
            <template #icon>
              <EditOutlined />
            </template>
            修改
          </a-button>
          <a-popconfirm
            title="确定要删除这个应用吗？"
            @confirm="handleDelete"
            ok-text="确定"
            cancel-text="取消"
          >
            <a-button danger>
              <template #icon>
                <DeleteOutlined />
              </template>
              删除
            </a-button>
          </a-popconfirm>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import UserInfo from './UserInfo.vue'
import { formatTime } from '@/utils/time'
import {formatCodeGenType} from "../utils/codeGenTypes.ts";
import { AppDeployStatusEnum, APP_DEPLOY_STATUS_MAP, AppGenStatusEnum, APP_GEN_STATUS_MAP } from '@/utils/appStatus'
import { controlDeploy } from '@/api/appController'
import { message } from 'ant-design-vue'

interface Props {
  open: boolean
  app?: API.AppVO
  showActions?: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'edit'): void
  (e: 'delete'): void
  (e: 'refresh'): void
}

const props = withDefaults(defineProps<Props>(), {
  showActions: false,
})

const emit = defineEmits<Emits>()

const deployControlLoading = ref(false)

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const handleEdit = () => {
  emit('edit')
}

const handleDelete = () => {
  emit('delete')
}

const handleControlDeploy = async (status: AppDeployStatusEnum) => {
  if (!props.app?.id) return
  deployControlLoading.value = true
  try {
    const res = await controlDeploy({
      appId: props.app.id,
      deployStatus: status,
    })
    if (res.data.code === 0) {
      message.success(status === AppDeployStatusEnum.ONLINE ? '已上线' : '已下线')
      emit('refresh')
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (e) {
    console.error('操作失败', e)
    message.error('操作失败')
  } finally {
    deployControlLoading.value = false
  }
}
</script>

<style scoped>

.app-detail-content {
  padding: 8px 0;
}

.app-basic-info {
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.info-label {
  width: 80px;
  color: rgba(148, 163, 184, 0.9);
  font-size: 14px;
  flex-shrink: 0;
}

.app-actions {
  padding-top: 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
}
</style>
