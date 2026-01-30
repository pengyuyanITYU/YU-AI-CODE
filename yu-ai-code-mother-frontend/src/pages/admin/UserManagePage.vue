<template>
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" />
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
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" :width="120" />
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <div v-if="record.userRole === 'admin'">
            <a-tag color="green">管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue">普通用户</a-tag>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button danger @click="doDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteUser, listUserVoByPage } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 展示的数据
const data = ref<API.UserVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  const res = await listUserVoByPage({
    ...searchParams,
  })
  if (res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

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

// 表格分页变化时的操作
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#userManagePage {
  padding: 24px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  margin-top: 16px;
  min-height: calc(100vh - 88px);
  position: relative;
}

#userManagePage::before {
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

:deep(.ant-input) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(148, 163, 184, 0.2);
  color: rgba(255, 255, 255, 0.95);
}

:deep(.ant-input::placeholder) {
  color: rgba(148, 163, 184, 0.6);
}

:deep(.ant-input:hover) {
  border-color: rgba(59, 130, 246, 0.5);
}

:deep(.ant-input:focus) {
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
</style>
