<template>
  <div id="appManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="应用名称">
        <a-input v-model:value="searchParams.appName" placeholder="输入应用名称" />
      </a-form-item>
      <a-form-item label="创建者">
        <a-input v-model:value="searchParams.userId" placeholder="输入用户ID" />
      </a-form-item>
      <a-form-item label="生成类型">
        <a-select
          v-model:value="searchParams.codeGenType"
          placeholder="选择生成类型"
          style="width: 150px"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option
            v-for="option in CODE_GEN_TYPE_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="精选状态">
        <a-select
          v-model:value="searchParams.featuredStatus"
          placeholder="选择状态"
          style="width: 120px"
        >
          <a-select-option :value="undefined">全部</a-select-option>
          <a-select-option
            v-for="(value, key) in APP_FEATURED_STATUS_MAP"
            :key="key"
            :value="Number(key)"
          >
            {{ value.text }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      @change="doTableChange"
      :scroll="{ x: 1200 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="80" :height="60" />
          <div v-else class="no-cover">无封面</div>
        </template>
        <template v-else-if="column.dataIndex === 'initPrompt'">
          <a-tooltip :title="record.initPrompt">
            <div class="prompt-text">{{ record.initPrompt }}</div>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'codeGenType'">
          {{ formatCodeGenType(record.codeGenType) }}
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <a-input-number 
            v-model:value="record.priority" 
            :min="0" 
            :max="999" 
            size="small" 
            @change="(val: number) => updatePriority(record, val)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'featuredStatus'">
          <a-tag :color="APP_FEATURED_STATUS_MAP[record.featuredStatus as AppFeaturedStatusEnum]?.color">
            {{ APP_FEATURED_STATUS_MAP[record.featuredStatus as AppFeaturedStatusEnum]?.text }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'genStatus'">
          <a-tag :color="APP_GEN_STATUS_MAP[record.genStatus as AppGenStatusEnum]?.color">
            {{ APP_GEN_STATUS_MAP[record.genStatus as AppGenStatusEnum]?.text }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'deployStatus'">
          <a-tag :color="APP_DEPLOY_STATUS_MAP[record.deployStatus as AppDeployStatusEnum]?.color">
            {{ APP_DEPLOY_STATUS_MAP[record.deployStatus as AppDeployStatusEnum]?.text }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'deployedTime'">
          <span v-if="record.deployedTime">
            {{ formatTime(record.deployedTime) }}
          </span>
          <span v-else class="text-gray">未部署</span>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'user'">
          <UserInfo :user="record.user" size="small" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="editApp(record)"> 编辑 </a-button>
            <a-dropdown v-if="record.featuredStatus === AppFeaturedStatusEnum.PENDING">
              <a-button type="link" size="small"> 审核 <DownOutlined /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="showReviewModal(record, AppFeaturedStatusEnum.FEATURED)">
                    通过精选
                  </a-menu-item>
                  <a-menu-item @click="showReviewModal(record, AppFeaturedStatusEnum.REJECTED)" danger>
                    拒绝申请
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button v-else-if="record.featuredStatus === AppFeaturedStatusEnum.FEATURED" 
                      type="link" size="small" @click="doReview(record, AppFeaturedStatusEnum.NOT_APPLIED)">
              取消精选
            </a-button>
            <a-popconfirm title="确定要删除这个应用吗？" @confirm="deleteApp(record.id)">
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 审核对话框 -->
    <a-modal
      v-model:visible="reviewModalVisible"
      title="应用精选审核"
      @ok="handleReviewSubmit"
      :confirm-loading="reviewLoading"
      ok-text="确认"
      cancel-text="取消"
    >
      <a-form :model="reviewForm" layout="vertical">
        <a-form-item label="审核备注" :required="reviewForm.featuredStatus === AppFeaturedStatusEnum.REJECTED">
          <a-textarea
            v-model:value="reviewForm.reviewMessage"
            placeholder="请输入审核意见或拒绝原因"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownOutlined } from '@ant-design/icons-vue'
import { listAppVoByPageByAdmin, deleteAppByAdmin, updateAppByAdmin, reviewApp } from '@/api/appController'
import { CODE_GEN_TYPE_OPTIONS, formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'
import { 
  AppDeployStatusEnum, 
  APP_DEPLOY_STATUS_MAP, 
  AppGenStatusEnum, 
  APP_GEN_STATUS_MAP,
  AppFeaturedStatusEnum,
  APP_FEATURED_STATUS_MAP
} from '@/utils/appStatus'
import UserInfo from '@/components/UserInfo.vue'

const router = useRouter()

// 审核表单状态
const reviewModalVisible = ref(false)
const reviewLoading = ref(false)
const reviewForm = reactive({
  id: 0,
  featuredStatus: AppFeaturedStatusEnum.FEATURED,
  reviewMessage: '',
})

const showReviewModal = (app: API.AppVO, status: number) => {
  reviewForm.id = app.id as number
  reviewForm.featuredStatus = status
  reviewForm.reviewMessage = ''
  reviewModalVisible.value = true
}

const handleReviewSubmit = async () => {
  if (reviewForm.featuredStatus === AppFeaturedStatusEnum.REJECTED && !reviewForm.reviewMessage) {
    message.warning('拒绝申请时必须填写原因')
    return
  }
  reviewLoading.value = true
  try {
    const res = await reviewApp(reviewForm)
    if (res.data.code === 0) {
      message.success('审核处理成功')
      reviewModalVisible.value = false
      fetchData()
    } else {
      message.error('处理失败：' + res.data.message)
    }
  } catch (error) {
    message.error('系统错误')
  } finally {
    reviewLoading.value = false
  }
}


const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
    fixed: 'left',
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
    width: 150,
  },
  {
    title: '封面',
    dataIndex: 'cover',
    width: 100,
  },
  {
    title: '初始提示词',
    dataIndex: 'initPrompt',
    width: 200,
  },
  {
    title: '生成类型',
    dataIndex: 'codeGenType',
    width: 100,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    width: 80,
  },
  {
    title: '精选状态',
    dataIndex: 'featuredStatus',
    width: 100,
  },
  {
    title: '生成状态',
    dataIndex: 'genStatus',
    width: 100,
  },
  {
    title: '部署状态',
    dataIndex: 'deployStatus',
    width: 100,
  },
  {
    title: '部署时间',
    dataIndex: 'deployedTime',
    width: 160,
  },
  {
    title: '创建者',
    dataIndex: 'user',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 160,
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right',
  },
]

// 数据
const data = ref<API.AppVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  try {
    const res = await listAppVoByPageByAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取数据失败：', error)
    message.error('获取数据失败')
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化处理
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 编辑应用
const editApp = (app: API.AppVO) => {
  router.push(`/app/edit/${app.id}`)
}

// 审核精选状态
const doReview = async (app: API.AppVO, status: number) => {
  if (!app.id) return

  try {
    const res = await updateAppByAdmin({
      id: app.id,
      featuredStatus: status,
    })

    if (res.data.code === 0) {
      message.success('操作成功')
      fetchData()
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (error) {
    console.error('操作失败：', error)
    message.error('操作失败')
  }
}

// 更新全局优先级
const updatePriority = async (app: API.AppVO, priority: number) => {
  if (!app.id) return

  try {
    const res = await updateAppByAdmin({
      id: app.id,
      priority,
    })

    if (res.data.code === 0) {
      message.success('优先级已更新')
      fetchData()
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } catch (error) {
    console.error('更新优先级失败：', error)
    message.error('操作失败')
  }
}

// 删除应用
const deleteApp = async (id: number | undefined) => {
  if (!id) return

  try {
    const res = await deleteAppByAdmin({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      // 刷新数据
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}
</script>

<style scoped>
#appManagePage {
  padding: 24px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  margin-top: 16px;
  min-height: calc(100vh - 88px);
  position: relative;
}

#appManagePage::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1400px 900px at 5% 15%, rgba(59, 130, 246, 0.15), transparent 60%),
    radial-gradient(1200px 800px at 95% 5%, rgba(139, 92, 246, 0.15), transparent 60%);
  z-index: 0;
  pointer-events: none;
}

:deep(.ant-form),
:deep(.ant-table),
:deep(.ant-divider) {
  position: relative;
  z-index: 1;
}

:deep(.ant-form-item-label > label) {
  color: rgba(255, 255, 255, 0.9);
}

:deep(.ant-input),
:deep(.ant-select-selector) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(148, 163, 184, 0.2);
  color: rgba(255, 255, 255, 0.95);
}

:deep(.ant-input::placeholder) {
  color: rgba(148, 163, 184, 0.6);
}

:deep(.ant-input:hover),
:deep(.ant-select:hover .ant-select-selector) {
  border-color: rgba(59, 130, 246, 0.5);
}

:deep(.ant-input:focus),
:deep(.ant-select-focused .ant-select-selector) {
  border-color: rgba(59, 130, 246, 0.6);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
}

:deep(.ant-table) {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.85) 100%);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 12px;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

:deep(.ant-table-container) {
  background: transparent;
}

:deep(.ant-table-content) {
  background: transparent;
}

:deep(.ant-table-thead > tr > th) {
  background: rgba(255, 255, 255, 0.05) !important;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
  color: rgba(255, 255, 255, 0.98);
}

:deep(.ant-table-thead > tr > th::before) {
  background: transparent;
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  color: rgba(255, 255, 255, 0.9);
  vertical-align: middle;
  background: transparent;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: rgba(255, 255, 255, 0.05) !important;
}

:deep(.ant-table-tbody > tr) {
  background: transparent;
}

:deep(.ant-table-placeholder) {
  background: transparent;
  color: rgba(148, 163, 184, 0.7);
}

:deep(.ant-empty-description) {
  color: rgba(148, 163, 184, 0.7);
}

:deep(.ant-divider) {
  border-color: rgba(148, 163, 184, 0.15);
}

:deep(.ant-form-item) {
  margin-bottom: 16px;
}

:deep(.ant-image) {
  border-radius: 6px;
  overflow: hidden;
}

:deep(.ant-image-img) {
  background: rgba(255, 255, 255, 0.05);
}

:deep(.ant-select-selection-placeholder) {
  color: rgba(148, 163, 184, 0.6);
}

.no-cover {
  width: 80px;
  height: 60px;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(148, 163, 184, 0.6);
  font-size: 12px;
  border-radius: 4px;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.prompt-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-gray {
  color: rgba(148, 163, 184, 0.7);
}

.featured-btn {
  background: #faad14;
  border-color: #faad14;
  color: white;
}

.featured-btn:hover {
  background: #d48806;
  border-color: #d48806;
}
</style>
