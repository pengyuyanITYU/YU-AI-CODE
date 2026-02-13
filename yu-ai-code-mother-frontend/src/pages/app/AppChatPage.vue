<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
        <a-tag v-if="appInfo?.codeGenType" color="blue" class="code-gen-type-tag">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
        <a-tooltip :title="tokenTooltipText" placement="bottom">
          <div class="token-stats">
            <span class="token-icon">⁋</span>
            <span class="token-count">{{ formatTokenCount(displayedTokens) }} tokens</span>
          </div>
        </a-tooltip>
      </div>
      <div class="header-right">
        <a-tooltip title="应用详情">
          <a-button type="text" class="header-icon-btn" @click="showAppDetail">
            <template #icon>
              <InfoCircleOutlined />
            </template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="版本历史">
          <a-button v-if="isOwner" type="text" class="header-icon-btn" @click="showVersionPanel">
            <template #icon>
              <HistoryOutlined />
            </template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="导出对话记录">
          <a-button type="text" class="header-icon-btn" @click="exportChatHistory" :loading="exporting">
            <template #icon>
              <FileMarkdownOutlined />
            </template>
          </a-button>
        </a-tooltip>
        <a-button
            type="primary"
            ghost
            @click="downloadCode"
            :loading="downloading"
            :disabled="!isOwner"
        >
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>

        <!-- 部署控制：如果是 ONLINE，显示下线；否则显示部署 -->
        <template v-if="isOwner">
            <a-button
                v-if="appInfo?.deployStatus === AppDeployStatusEnum.ONLINE"
                type="primary"
                danger
                @click="handleToggleDeploy(AppDeployStatusEnum.OFFLINE)"
                :loading="deploying"
            >
              <template #icon>
                <CloudDownloadOutlined />
              </template>
              下线
            </a-button>
            <a-button
                v-else
                type="primary"
                @click="handleToggleDeploy(AppDeployStatusEnum.ONLINE)"
                :loading="deploying"
            >
              <template #icon>
                <CloudUploadOutlined />
              </template>
              部署
            </a-button>
        </template>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              加载更多历史消息
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">
                <MarkdownRenderer
                  v-if="getParsedUserMessage(message).text"
                  :content="sanitizeUserMessageText(getParsedUserMessage(message).text)"
                />
                <div
                  v-if="getParsedUserMessage(message).attachments.length > 0"
                  class="message-attachments"
                >
                  <div
                    v-for="(attachment, attachmentIndex) in getParsedUserMessage(message).attachments"
                    :key="`${index}_${attachmentIndex}`"
                    class="message-attachment-item"
                  >
                    <template v-if="isImageAttachment(attachment)">
                      <a-image
                        :src="attachment.url"
                        :alt="attachment.fileName || 'image'"
                        class="chat-image-attachment"
                      />
                      <div class="attachment-name">{{ attachment.fileName || '未命名图片' }}</div>
                    </template>
                    <div v-else class="attachment-summary">{{ formatAttachmentSummary(attachment) }}</div>
                  </div>
                </div>
              </div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" />
              </div>
              <div class="message-content">
                <MarkdownRenderer v-if="message.content" :content="message.content" />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
            v-if="selectedElementInfo"
            class="selected-element-alert"
            type="info"
            closable
            @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  页面路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea
                  v-model:value="userInput"
                  :placeholder="getInputPlaceholder()"
                  :rows="4"
                  :maxlength="1000"
                  @keydown.enter.prevent="sendMessage"
                  :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
                v-else
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating"
            />
            <!-- 文件列表展示 -->
            <div v-if="fileList.length > 0" class="file-list">
                  <div v-for="(file, index) in fileList" :key="index" class="file-item">
                <a-image
                  v-if="file.fileType === 'image'"
                  :src="file.url"
                  :alt="file.fileName"
                  class="upload-image-thumb"
                />
                <span v-else class="file-icon">{{ getFileIcon(file.fileType) }}</span>
                <span class="file-name" :title="file.fileName">{{ file.fileName }}</span>
                <CloseOutlined class="remove-icon" @click="removeFile(index)" />
              </div>
            </div>

            <div class="input-actions">
              <div class="action-left">
                <input
                  type="file"
                  ref="fileInput"
                  class="hidden-input"
                  @change="handleFileUpload"
                  accept=".jpg,.jpeg,.png,.pdf,.doc,.docx,.txt,.md,.html,.css,.vue"
                />
                <a-tooltip title="上传参考文件">
                  <a-button
                    type="text"
                    class="action-btn"
                    :loading="uploading"
                    @click="triggerFileUpload"
                    :disabled="!isOwner"
                  >
                    <template #icon><PaperClipOutlined /></template>
                  </a-button>
                </a-tooltip>
              </div>

              <a-button
                  v-if="isGenerating"
                  type="primary"
                  danger
                  @click="stopGeneration"
              >
                <template #icon>
                  <StopOutlined />
                </template>
              </a-button>
              <a-button
                  v-else
                  type="primary"
                  @click="sendMessage"
                  :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>生成后的网页展示</h3>
          <div class="preview-actions">
            <div class="version-nav" v-if="isOwner">
               <a-tooltip :title="previousVersion ? `返回上一版 (v${previousVersion.version})` : '无上一版本'">
                <a-button
                  type="text"
                  class="version-nav-btn"
                  :disabled="!previousVersion || rollbackLoading"
                  @click="previousVersion && handleQuickRollback(previousVersion.version!)"
                >
                  <template #icon><LeftOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="nextVersion ? `前往下一版 (v${nextVersion.version})` : '无下一版本'">
                <a-button
                  type="text"
                  class="version-nav-btn"
                  :disabled="!nextVersion || rollbackLoading"
                  @click="nextVersion && handleQuickRollback(nextVersion.version!)"
                >
                  <template #icon><RightOutlined /></template>
                </a-button>
              </a-tooltip>
            </div>
            <a-button
                v-if="isOwner && previewUrl"
                type="link"
                :danger="isEditMode"
                @click="toggleEditMode"
                :class="{ 'edit-mode-active': isEditMode }"
                style="padding: 0; height: auto; margin-right: 12px"
            >
              <template #icon>
                <EditOutlined />
              </template>
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
            </a-button>
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <iframe
              v-else
              :src="previewUrl"
              class="preview-iframe"
              frameborder="0"
              @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
        v-model:open="appDetailVisible"
        :app="appInfo"
        :show-actions="isOwner || isAdmin"
        @edit="editApp"
        @delete="deleteApp"
        @refresh="fetchAppInfo"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
        v-model:open="deployModalVisible"
        :deploy-url="deployUrl"
        @open-site="openDeployedSite"
    />

    <!-- 版本历史面板 -->
    <AppVersionPanel
        v-model:open="versionPanelVisible"
        :app-id="appId"
        :current-version="appInfo?.currentVersion"
        @rollback="handleVersionRollback"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  getAppVoById,
  deleteApp as deleteAppApi,
  controlDeploy,
} from '@/api/appController'
import { listVersions, rollbackVersion } from '@/api/appVersionController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import { AppDeployStatusEnum } from '@/utils/appStatus'
import { uploadAndProcessFile, type UploadedFile, getFileIcon, consumeInitialFilesFromSession, getImageDimensions } from '@/utils/fileUploadManager'
import {
  calculateInputTokens,
  formatTokenCount,
  getTokenTooltipText,
  type TokenBreakdown,
  type AccumulatedTokens
} from '@/utils/tokenEstimator'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import AppVersionPanel from '@/components/AppVersionPanel.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  CloudDownloadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  EditOutlined,
  HistoryOutlined,
  LeftOutlined,
  RightOutlined,
  PaperClipOutlined,
  CloseOutlined,
  StopOutlined,
  FileMarkdownOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<any>()

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

interface MessageAttachment {
  fileName?: string
  type?: string
  fileType?: string
  url?: string
}

interface ParsedUserMessage {
  text: string
  attachments: MessageAttachment[]
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
const fileList = ref<UploadedFile[]>([])
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const parsedUserMessageCache = new Map<string, ParsedUserMessage>()
const abortController = ref<AbortController | null>(null)

// Token 计算相关
const tokenBreakdown = computed<TokenBreakdown>(() => {
  return calculateInputTokens(userInput.value, fileList.value)
})

const accumulatedTokens = computed<AccumulatedTokens>(() => {
  return {
    totalInputTokens: appInfo.value?.totalInputTokens || 0,
    totalOutputTokens: appInfo.value?.totalOutputTokens || 0,
    totalTokens: appInfo.value?.totalTokens || 0
  }
})

// 判断是否已有对话历史（有任何消息即算有历史）
const hasConversationHistory = computed(() => {
  // 只要有消息（哪怕是用户发了但AI没回），就不算第一次提问
  return messages.value.length > 0
})

// 显示的token数：
// - 第一次提问（无历史）：0
// - 有历史且在输入中：累计tokens + 当前输入tokens
// - AI生成完成后：总消耗的tokens
const displayedTokens = computed(() => {
  const totalConsumed = appInfo.value?.totalTokens || 0

  // 如果已经有对话历史，说明不是第一次提问
  if (hasConversationHistory.value) {
    // 如果正在生成中，显示当前累计值；否则显示已完成的总消耗
    if (isGenerating.value) {
      // 生成中：显示之前累计的 + 当前输入的（但还没收到AI回复，所以不预估输出）
      return totalConsumed + tokenBreakdown.value.currentInputTotal
    } else {
      // 生成完成：显示总消耗的tokens
      return totalConsumed
    }
  } else {
    // 第一次提问，没有历史记录
    // 如果用户已经开始输入（有文字或文件），显示当前输入的tokens
    // 否则显示 0
    if (userInput.value.trim() || fileList.value.length > 0) {
      return tokenBreakdown.value.currentInputTotal
    }
    return 0
  }
})

const tokenTooltipText = computed(() => {
  return getTokenTooltipText(tokenBreakdown.value, accumulatedTokens.value)
})

const triggerFileUpload = () => {
  fileInput.value?.click()
}

// 处理文件上传
const handleFileUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return

  const file = target.files[0]
  // 重置 input
  target.value = ''

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录后上传文件')
    return
  }

  // 如果是图片，先获取尺寸
  let imageDimensions: { width: number; height: number } | null = null
  if (file.type.startsWith('image/')) {
    imageDimensions = await getImageDimensions(file)
    if (!imageDimensions) {
      message.error('该文件已损坏，请上传完整图片')
      return
    }
  }

  uploading.value = true
  try {
    const uploadedFile = await uploadAndProcessFile(file)
    if (uploadedFile) {
      // 如果是图片，添加尺寸信息
      if (imageDimensions) {
        uploadedFile.width = imageDimensions.width
        uploadedFile.height = imageDimensions.height
      }
      fileList.value.push(uploadedFile)
      message.success(`文件 ${file.name} 上传成功`)
    }
  } catch (error) {
    console.error('文件上传失败:', error)
  } finally {
    uploading.value = false
  }
}

// 移除文件
const removeFile = (index: number) => {
  fileList.value.splice(index, 1)
}

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 导出相关
const exporting = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 版本管理相关
const versionPanelVisible = ref(false)
const versions = ref<API.AppVersionVO[]>([])
const loadingVersions = ref(false)
const rollbackLoading = ref(false)

const sortedVersions = computed(() => {
  return [...versions.value].sort((a, b) => (b.version || 0) - (a.version || 0))
})

const currentVersionIndex = computed(() => {
  if (!appInfo.value?.currentVersion) return -1
  return sortedVersions.value.findIndex(v => v.version === appInfo.value?.currentVersion)
})

const previousVersion = computed(() => {
  if (currentVersionIndex.value === -1 || currentVersionIndex.value === sortedVersions.value.length - 1) return null
  return sortedVersions.value[currentVersionIndex.value + 1]
})

const nextVersion = computed(() => {
  if (currentVersionIndex.value === -1 || currentVersionIndex.value === 0) return null
  return sortedVersions.value[currentVersionIndex.value - 1]
})

const loadVersions = async () => {
    if (!appId.value) return
    loadingVersions.value = true
    try {
        const res = await listVersions({ appId: appId.value as any })
        if (res.data?.code === 0) {
            versions.value = res.data.data || []
        }
    } catch (e) {
        console.error('加载版本列表失败', e)
    } finally {
        loadingVersions.value = false
    }
}

const handleQuickRollback = async (version: number) => {
    if (!appId.value) return
    rollbackLoading.value = true
    try {
        const res = await rollbackVersion({ appId: appId.value as any, version })
        if (res.data?.code === 0) {
            message.success(`已切换至版本 v${version}`)
            // 重新获取信息并刷新
            await fetchAppInfo()
        } else {
            message.error(res.data?.message || '切换版本失败')
        }
    } catch (e) {
        message.error('切换版本失败')
    } finally {
        rollbackLoading.value = false
    }
}

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 显示版本面板
const showVersionPanel = () => {
  versionPanelVisible.value = true
}

// 处理版本回退
const handleVersionRollback = (version: number) => {
  versionPanelVisible.value = false
  updatePreview()
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
            .map((chat) => ({
              type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
              content: chat.message || '',
              createTime: chat.createTime,
            }))
            .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      // 加载版本列表
      if (isOwner.value) {
        loadVersions()
      }
      // 如果有至少2条对话记录，展示对应的网站
      if (messages.value.length >= 2) {
        updatePreview()
      }
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
          appInfo.value.initPrompt &&
          isOwner.value &&
          messages.value.length === 0 &&
          historyLoaded.value
      ) {
        const initialFiles = consumeInitialFilesFromSession(id)
        await sendInitialMessage(appInfo.value.initPrompt, initialFiles)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string, initialFiles: UploadedFile[] = []) => {
  // 构造展示内容 (多模态 JSON)
  const mmContent = {
    text: prompt,
    attachments: initialFiles.map(f => ({
      fileName: f.fileName,
      type: f.fileType, // 直接使用原始类型 (image/document)
      url: f.url
    }))
  }

  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: JSON.stringify(mmContent),
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, initialFiles, aiMessageIndex)
}

// 停止生成
const stopGeneration = () => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  isGenerating.value = false
  message.info('已停止生成')
}

// 发送消息
const sendMessage = async () => {
  if ((!userInput.value.trim() && fileList.value.length === 0) || isGenerating.value) {
    return
  }

  let message = userInput.value.trim()
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }

  // 保存当前要发送的文件列表副本
  const currentFiles = [...fileList.value]

  userInput.value = ''
  fileList.value = [] // 清空文件列表

  // 添加用户消息（包含元素信息）
  const mmContent = {
    text: message,
    attachments: currentFiles.map(f => ({
      fileName: f.fileName,
      type: f.fileType, // 直接使用原始类型 (image/document)
      url: f.url
    }))
  }

  messages.value.push({
    type: 'user',
    content: JSON.stringify(mmContent),
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, currentFiles, aiMessageIndex)
}

// 生成代码 - 使用 fetch 处理 POST 流式响应
const generateCode = async (userMessage: string, files: UploadedFile[], aiMessageIndex: number) => {
  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const url = `${baseURL}/app/chat/gen/code`

    // 创建新的 AbortController
    abortController.value = new AbortController()

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        appId: appId.value,
        message: userMessage,
        fileList: files.map(f => ({
          url: f.url,
          fileName: f.fileName,
          fileType: f.fileType
        }))
      }),
      signal: abortController.value.signal
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('Response body is null')
    }

    const decoder = new TextDecoder()
    let fullContent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value)
      const lines = chunk.split('\n\n')

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (!data) continue

          if (data === '[DONE]') {
            // Stream finished
            continue
          }

          try {
            const parsed = JSON.parse(data)

            // 处理 done 事件
            if (parsed.event === 'done') {
               continue
            }

            const content = parsed.d
            if (content !== undefined && content !== null) {
              fullContent += content
              messages.value[aiMessageIndex].content = fullContent
              messages.value[aiMessageIndex].loading = false
              scrollToBottom()
            }
          } catch (e) {
            // Ignore parse errors for incomplete chunks
          }
        }
      }
    }

    // 完成后处理
    isGenerating.value = false
    abortController.value = null

    // 延迟更新预览
    setTimeout(async () => {
      await fetchAppInfo()
      updatePreview()
    }, 1000)

  } catch (error: any) {
    if (error.name === 'AbortError') {
      console.log('生成已取消')
      messages.value[aiMessageIndex].loading = false
      return
    }
    console.error('生成代码失败：', error)
    handleError(error, aiMessageIndex)
  } finally {
     abortController.value = null
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 导出对话历史
const exportChatHistory = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  exporting.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/chatHistory/export/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`导出失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `chat-history-${appId.value}.md`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('对话记录导出成功')
  } catch (error) {
    console.error('导出失败：', error)
    message.error('导出失败，请重试')
  } finally {
    exporting.value = false
  }
}

// 切换部署状态（上线/下线）
const handleToggleDeploy = async (status: AppDeployStatusEnum) => {
  if (!appId.value) return
  deploying.value = true
  try {
    const res = await controlDeploy({
      appId: appId.value,
      deployStatus: status,
    })
    if (res.data.code === 0) {
      message.success(status === AppDeployStatusEnum.ONLINE ? '已上线' : '已下线')
      if (status === AppDeployStatusEnum.ONLINE && res.data.data) {
        deployUrl.value = res.data.data
        deployModalVisible.value = true
      }
      await fetchAppInfo()
      if (status === AppDeployStatusEnum.ONLINE) {
        updatePreview()
      } else {
        // 下线后，清除预览地址（虽然 Owner 可以看，但在按钮操作后建议清空一下触发重新渲染或保持 UI 逻辑一致）
        // 如果想让 Owner 继续看预览，可以不置空，StaticResourceController 会放行
        // 这里建议保持 previewUrl 不变，因为 StaticResourceController 已经处理了 Owner 预览权限
      }
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (e) {
    console.error('操作失败', e)
    message.error('操作失败')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

const formatAttachmentSummary = (file: MessageAttachment) => {
  const type = String(file?.type || file?.fileType || '').toLowerCase()
  const typeLabel = type === 'image' ? '图片' : type === 'text' ? '文本' : '文档'
  const icon = type === 'image' ? '🖼️' : type === 'text' ? '📝' : '📄'
  const fileName = file?.fileName || '未命名附件'
  return `${icon} ${fileName}（${typeLabel}）`
}

const sanitizeUserMessageText = (content: string) => {
  if (!content) {
    return ''
  }

  return content.replace(/https?:\/\/[^\s)]+/g, '[附件链接已隐藏]')
}

const isImageAttachment = (file: MessageAttachment) => {
  const type = String(file?.type || file?.fileType || '').toLowerCase()
  return type === 'image' && Boolean(file?.url)
}

const getParsedUserMessage = (message: Message): ParsedUserMessage => {
  return parseUserMessageContent(message.content)
}

const parseUserMessageContent = (content: string): ParsedUserMessage => {
  const cached = parsedUserMessageCache.get(content)
  if (cached) {
    return cached
  }

  const emptyResult: ParsedUserMessage = {
    text: '',
    attachments: [],
  }

  if (!content) {
    parsedUserMessageCache.set(content, emptyResult)
    return emptyResult
  }

  if (content.startsWith('{') && content.endsWith('}')) {
    try {
      const mmContent = JSON.parse(content)
      const attachments: MessageAttachment[] = Array.isArray(mmContent?.attachments)
        ? mmContent.attachments
            .filter((item: any) => item && (item.url || item.fileName))
            .map((item: any) => ({
              fileName: item.fileName,
              type: item.type,
              fileType: item.fileType,
              url: item.url,
            }))
        : []

      if (typeof mmContent?.text === 'string' || attachments.length > 0) {
        const parsed: ParsedUserMessage = {
          text: typeof mmContent?.text === 'string' ? mmContent.text : '',
          attachments,
        }
        parsedUserMessageCache.set(content, parsed)
        return parsed
      }
    } catch {
    }
  }

  if (content.startsWith('UserMessage') && content.includes('text = "')) {
    const match = content.match(/text = "([^"]*)"/)
    const parsed: ParsedUserMessage = {
      text: match && match[1] ? match[1] : content,
      attachments: [],
    }
    parsedUserMessageCache.set(content, parsed)
    return parsed
  }

  const parsed: ParsedUserMessage = {
    text: content,
    attachments: [],
  }
  parsedUserMessageCache.set(content, parsed)
  return parsed
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  // EventSource 会在组件卸载时自动清理
})
</script>

<style scoped>
/* Add new styles for header icon buttons */
.header-icon-btn {
  color: rgba(255, 255, 255, 0.85);
  font-size: 18px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px; /* Slightly rounded squares like toolbar buttons */
  transition: all 0.2s;
  background: transparent;
  border: none;
}

.header-icon-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  position: relative;
  overflow: hidden;
}

#appChatPage::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1400px 900px at 5% 15%, rgba(59, 130, 246, 0.15), transparent 60%),
    radial-gradient(1200px 800px at 95% 5%, rgba(139, 92, 246, 0.15), transparent 60%);
  z-index: 0;
  pointer-events: none;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.85) 0%, rgba(30, 41, 59, 0.75) 100%);
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.token-stats {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  transition: all 0.2s ease;
}

.token-stats:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(148, 163, 184, 0.3);
}

.token-icon {
  font-size: 14px;
}

.token-count {
  font-weight: 500;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

.code-gen-type-tag {
  font-size: 12px;
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.98);
  background: linear-gradient(135deg, #ffffff 0%, rgba(59, 130, 246, 0.9) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 8px;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.85) 100%);
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.messages-container {
  flex: 0.9;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
  background: transparent;
}

.message-item {
  margin-bottom: 12px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
}

.user-message .message-content {
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: white;
}

.ai-message .message-content {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.2);
  padding: 8px 12px;
}
.message-attachments {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-attachment-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-image-attachment {
  width: 220px;
  max-width: 100%;
}

:deep(.chat-image-attachment .ant-image-img) {
  width: 220px;
  max-width: 100%;
  border-radius: 8px;
  object-fit: cover;
}

.attachment-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.attachment-summary {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  padding: 6px 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(148, 163, 184, 0.9);
}

/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

/* 输入区域 */
.input-container {
  padding: 16px;
  background: transparent;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
}

.input-wrapper {
  position: relative;
}

:deep(.input-wrapper .ant-input) {
  padding-right: 50px;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(148, 163, 184, 0.2);
  color: rgba(255, 255, 255, 0.95);
}

:deep(.input-wrapper .ant-input::placeholder) {
  color: rgba(148, 163, 184, 0.6);
}

:deep(.input-wrapper .ant-input:hover) {
  border-color: rgba(59, 130, 246, 0.5);
}

:deep(.input-wrapper .ant-input:focus) {
  border-color: rgba(59, 130, 246, 0.6);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
  left: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-left {
  display: flex;
  align-items: center;
}

.hidden-input {
  display: none;
}

.action-btn {
  color: rgba(148, 163, 184, 0.6);
  transition: all 0.3s;
}

.action-btn:hover {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
}

.file-list {
  padding: 0 16px 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 4px;
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.9);
  max-width: 150px;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-image-thumb {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}

:deep(.upload-image-thumb .ant-image-img) {
  width: 28px;
  height: 28px;
  object-fit: cover;
}

.remove-icon {
  font-size: 10px;
  color: rgba(148, 163, 184, 0.6);
  cursor: pointer;
  transition: color 0.2s;
}

.remove-icon:hover {
  color: #ef4444;
}

/* 右侧预览区域 */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.85) 100%);
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.98);
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: rgba(148, 163, 184, 0.9);
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: rgba(148, 163, 184, 0.9);
}

.preview-loading p {
  margin-top: 16px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.selected-element-alert {
  margin: 0 16px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 12px 16px;
  }

  .app-name {
    font-size: 16px;
  }

  .main-content {
    padding: 8px;
    gap: 8px;
  }

  .message-content {
    max-width: 85%;
  }

  /* 选中元素信息样式 */
  .selected-element-alert {
    margin: 0 16px;
  }

  .selected-element-info {
    line-height: 1.4;
  }

  .element-header {
    margin-bottom: 8px;
  }

  .element-details {
    margin-top: 8px;
  }

  .element-item {
    margin-bottom: 4px;
    font-size: 13px;
  }

  .element-item:last-child {
    margin-bottom: 0;
  }

  .element-tag {
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 14px;
    font-weight: 600;
    color: #007bff;
  }

  .element-id {
    color: #28a745;
    margin-left: 4px;
  }

  .element-class {
    color: #ffc107;
    margin-left: 4px;
  }

  .element-selector-code {
    font-family: 'Monaco', 'Menlo', monospace;
    background: #f6f8fa;
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 12px;
    color: #d73a49;
    border: 1px solid #e1e4e8;
  }

  /* 编辑模式按钮样式 */
  .edit-mode-active {
    background-color: #52c41a !important;
    border-color: #52c41a !important;
    color: white !important;
  }

  .edit-mode-active:hover {
    background-color: #73d13d !important;
    border-color: #73d13d !important;
  }
}

.version-nav {
  display: flex;
  align-items: center;
  margin-right: 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  padding: 2px;
}

.version-nav-btn {
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.version-nav-btn:hover:not(:disabled) {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.version-nav-btn:disabled {
  color: rgba(255, 255, 255, 0.2);
  cursor: not-allowed;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}
</style>
