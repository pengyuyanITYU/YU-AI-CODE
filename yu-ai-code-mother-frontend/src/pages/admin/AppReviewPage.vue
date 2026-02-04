<template>
  <div id="appReviewPage">
    <div class="page-header">
      <h2 class="page-title">精选申请审核</h2>
      <p class="page-subtitle">处理用户提交的应用精选申请</p>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      @change="doTableChange"
      :loading="loading"
      :scroll="{ x: 1200 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="80" :height="60" />
          <div v-else class="no-cover">无封面</div>
        </template>
        <template v-else-if="column.dataIndex === 'user'">
          <UserInfo :user="record.user" size="small" />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="primary" size="small" @click="showReviewModal(record)">
              审核申请
            </a-button>
            <a-button size="small" @click="viewAppDetail(record)">
              查看详情
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 审核对话框 -->
    <a-modal
      v-model:visible="reviewModalVisible"
      title="应用精选审核"
      @ok="handleReview"
      :confirm-loading="reviewLoading"
      ok-text="确认"
      cancel-text="取消"
    >
      <a-form :model="reviewForm" layout="vertical">
        <a-form-item label="审核结果" required>
          <a-radio-group v-model:value="reviewForm.featuredStatus">
            <a-radio :value="AppFeaturedStatusEnum.FEATURED">通过精选</a-radio>
            <a-radio :value="AppFeaturedStatusEnum.REJECTED">拒绝申请</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item 
          label="审核备注" 
          :required="reviewForm.featuredStatus === AppFeaturedStatusEnum.REJECTED"
          :extra="reviewForm.featuredStatus === AppFeaturedStatusEnum.REJECTED ? '拒绝申请时必须填写原因' : '通过时可选填'"
        >
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
import { message } from 'ant-design-vue'
import { listAppVoByPageByAdmin, reviewApp } from '@/api/appController'
import { formatTime } from '@/utils/time'
import { AppFeaturedStatusEnum } from '@/utils/appStatus'
import UserInfo from '@/components/UserInfo.vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const data = ref<API.AppVO[]>([])
const total = ref(0)

// 搜索条件：固定为申请中
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  featuredStatus: AppFeaturedStatusEnum.PENDING,
})

// 审核表单
const reviewModalVisible = ref(false)
const reviewLoading = ref(false)
const currentRecord = ref<API.AppVO | null>(null)
const reviewForm = reactive({
  featuredStatus: AppFeaturedStatusEnum.FEATURED,
  reviewMessage: '',
})

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80, fixed: 'left' },
  { title: '应用名称', dataIndex: 'appName', width: 150 },
  { title: '封面', dataIndex: 'cover', width: 100 },
  { title: '申请人', dataIndex: 'user', width: 120 },
  { title: '申请时间', dataIndex: 'updateTime', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' },
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin(searchParams)
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    }
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条申请`,
}))

const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const showReviewModal = (record: API.AppVO) => {
  currentRecord.value = record
  reviewForm.featuredStatus = AppFeaturedStatusEnum.FEATURED
  reviewForm.reviewMessage = ''
  reviewModalVisible.value = true
}

const handleReview = async () => {
  if (!currentRecord.value?.id) return
  
  if (reviewForm.featuredStatus === AppFeaturedStatusEnum.REJECTED && !reviewForm.reviewMessage) {
    message.warning('拒绝申请时必须填写原因')
    return
  }

  reviewLoading.value = true
  try {
    const res = await reviewApp({
      id: currentRecord.value.id,
      featuredStatus: reviewForm.featuredStatus,
      reviewMessage: reviewForm.reviewMessage,
    })
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

const viewAppDetail = (record: API.AppVO) => {
  // 跳转到对话页预览或详情页
  window.open(`/app/chat/${record.id}`, '_blank')
}
</script>

<style scoped>
#appReviewPage {
  padding: 24px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  min-height: calc(100vh - 72px);
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.page-subtitle {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

:deep(.ant-table) {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.85) 100%);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 12px;
  backdrop-filter: blur(20px);
}

:deep(.ant-table-thead > tr > th) {
  background: rgba(255, 255, 255, 0.05) !important;
  color: #fff;
}

:deep(.ant-table-tbody > tr > td) {
  color: rgba(255, 255, 255, 0.9);
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
}
</style>
