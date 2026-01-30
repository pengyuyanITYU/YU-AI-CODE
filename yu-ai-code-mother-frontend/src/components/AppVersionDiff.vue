<template>
  <a-modal
    v-model:open="visible"
    title="版本对比"
    :width="1200"
    :footer="null"
    :destroyOnClose="true"
    @cancel="handleClose"
  >
    <div class="diff-container">
      <div v-if="loading" class="loading-container">
        <a-spin size="large" />
      </div>
      <div v-else-if="!diffData || !diffData.diffs?.length" class="empty-container">
        <a-empty description="两个版本没有差异" />
      </div>
      <div v-else class="diff-content">
        <div class="diff-header">
          <span class="version-label old">- v{{ diffData.oldVersion }} (旧版本)</span>
          <span class="version-label new">+ v{{ diffData.newVersion }} (新版本)</span>
        </div>
        <div class="file-diffs">
          <a-collapse v-model:activeKey="activeKeys" accordion destroyInactivePanel>
            <a-collapse-panel
              v-for="(file, index) in diffData.diffs"
              :key="index"
              :header="file.fileName"
            >
              <div class="monaco-diff-wrapper" :ref="(el) => setEditorRef(el as HTMLElement, index)"></div>
            </a-collapse-panel>
          </a-collapse>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import * as monaco from 'monaco-editor'

const props = defineProps<{
  open: boolean
  diffData?: API.AppVersionDiffVO
  loading?: boolean
}>()

const emit = defineEmits(['update:open'])

const visible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})

const activeKeys = ref<number | string | undefined>(0)
const editorInstances = new Map<number, monaco.editor.IStandaloneDiffEditor>()
const models: monaco.editor.ITextModel[] = []
const editorRefs = new Map<number, HTMLElement>()

const setEditorRef = (el: HTMLElement | null, index: number) => {
  if (el) {
    editorRefs.set(index, el)
  } else {
    editorRefs.delete(index)
  }
}

const getLanguageFromFileName = (fileName: string): string => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  const languageMap: Record<string, string> = {
    'vue': 'html',
    'ts': 'typescript',
    'tsx': 'typescript',
    'js': 'javascript',
    'jsx': 'javascript',
    'json': 'json',
    'html': 'html',
    'css': 'css',
    'scss': 'scss',
    'less': 'less',
    'md': 'markdown',
    'java': 'java',
    'xml': 'xml',
    'yaml': 'yaml',
    'yml': 'yaml'
  }
  return languageMap[ext || ''] || 'plaintext'
}


const createDiffEditor = (container: HTMLElement, file: API.FileDiff) => {
  const language = getLanguageFromFileName(file.fileName || '')

  const originalModel = monaco.editor.createModel(file.oldContent || '', language)
  const modifiedModel = monaco.editor.createModel(file.newContent || '', language)
  
  // 收集模型以便后续销毁
  models.push(originalModel)
  models.push(modifiedModel)

  const diffEditor = monaco.editor.createDiffEditor(container, {
    originalEditable: false,
    readOnly: true,
    renderSideBySide: true,
    enableSplitViewResizing: true,
    automaticLayout: true,
    scrollBeyondLastLine: false,
    minimap: { enabled: false },
    theme: 'vs-dark',
    fontSize: 13,
    lineHeight: 20
  })

  diffEditor.setModel({
    original: originalModel,
    modified: modifiedModel
  })

  return diffEditor
}

const disposeEditor = (index: number) => {
  const editor = editorInstances.get(index)
  if (editor) {
    editor.dispose()
    editorInstances.delete(index)
  }
}

const disposeAllEditors = () => {
  // 1. 先关闭弹窗,立即释放视觉
  // 2. 然后异步销毁所有编辑器和模型
  const editorsToDispose = Array.from(editorInstances.values())
  const modelsToDispose = [...models]
  
  editorInstances.clear()
  models.length = 0
  
  // 延迟销毁,避免阻塞UI
  setTimeout(() => {
    editorsToDispose.forEach(e => e.dispose())
    modelsToDispose.forEach(m => m.dispose())
  }, 0)
}

watch(activeKeys, async (newKeyRaw, oldKeyRaw) => {
  // 1. 销毁旧的编辑器
  if (oldKeyRaw !== undefined && oldKeyRaw !== null) {
    const oldKey = Number(oldKeyRaw)
    disposeEditor(oldKey)
  }

  await nextTick()
  
  // 2. 创建新激活的编辑器
  if (newKeyRaw !== undefined && newKeyRaw !== null) {
    const numKey = Number(newKeyRaw)
    if (!editorInstances.has(numKey) && props.diffData?.diffs?.[numKey]) {
      const container = editorRefs.get(numKey)
      if (container) {
        const editor = createDiffEditor(container, props.diffData.diffs[numKey])
        editorInstances.set(numKey, editor)
      }
    }
  }
})

watch(() => props.diffData, async () => {
  disposeAllEditors()
  activeKeys.value = 0
  await nextTick()
  if (props.diffData?.diffs?.length) {
    const container = editorRefs.get(0)
    if (container) {
      const editor = createDiffEditor(container, props.diffData.diffs[0])
      editorInstances.set(0, editor)
    }
  }
}, { deep: true })

watch(() => props.open, (newVal) => {
  if (!newVal) {
    disposeAllEditors()
  }
})

onBeforeUnmount(() => {
  disposeAllEditors()
})

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.diff-container {
  min-height: 500px;
}

.loading-container,
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.diff-header {
  display: flex;
  gap: 32px;
  margin-bottom: 20px;
  padding: 14px 20px;
  background: linear-gradient(135deg, rgba(22, 27, 34, 0.9) 0%, rgba(13, 17, 23, 0.95) 100%);
  border-radius: 12px;
  border: 1px solid rgba(48, 54, 61, 0.6);
  position: relative;
  overflow: hidden;
}

.diff-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #f85149 0%, #3fb950 100%);
}

.version-label {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.version-label::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.version-label.old {
  color: #f85149;
  background: rgba(248, 81, 73, 0.1);
  border: 1px solid rgba(248, 81, 73, 0.3);
}

.version-label.old::before {
  background: #f85149;
  box-shadow: 0 0 8px rgba(248, 81, 73, 0.6);
}

.version-label.new {
  color: #3fb950;
  background: rgba(63, 185, 80, 0.1);
  border: 1px solid rgba(63, 185, 80, 0.3);
}

.version-label.new::before {
  background: #3fb950;
  box-shadow: 0 0 8px rgba(63, 185, 80, 0.6);
}

.monaco-diff-wrapper {
  height: 500px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(48, 54, 61, 0.8);
}

:deep(.ant-collapse) {
  background: transparent;
  border: none;
}

:deep(.ant-collapse-item) {
  background: linear-gradient(135deg, rgba(22, 27, 34, 0.9) 0%, rgba(13, 17, 23, 0.95) 100%);
  border: 1px solid rgba(48, 54, 61, 0.6);
  border-radius: 12px !important;
  margin-bottom: 16px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.ant-collapse-item:hover) {
  border-color: rgba(56, 139, 253, 0.4);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

:deep(.ant-collapse-header) {
  color: rgba(255, 255, 255, 0.9) !important;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 14px;
  font-weight: 500;
  padding: 16px 20px !important;
  transition: all 0.3s ease;
}

:deep(.ant-collapse-header:hover) {
  background: rgba(56, 139, 253, 0.05);
}

:deep(.ant-collapse-arrow) {
  color: #8b949e !important;
  transition: all 0.3s ease;
}

:deep(.ant-collapse-item-active .ant-collapse-arrow) {
  color: #388bfd !important;
}

:deep(.ant-collapse-content) {
  background: #0d1117;
  border-top: 1px solid rgba(48, 54, 61, 0.6);
}

:deep(.ant-collapse-content-box) {
  padding: 0 !important;
}

:deep(.ant-modal-content) {
  background: linear-gradient(180deg, #161b22 0%, #0d1117 100%);
  border: 1px solid rgba(48, 54, 61, 0.8);
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6);
}

:deep(.ant-modal-header) {
  background: transparent;
  border-bottom: 1px solid rgba(48, 54, 61, 0.6);
  padding: 20px 24px;
}

:deep(.ant-modal-title) {
  color: rgba(255, 255, 255, 0.95);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

:deep(.ant-modal-close) {
  color: rgba(139, 148, 158, 0.8);
  transition: all 0.3s ease;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

:deep(.ant-modal-close:hover) {
  color: #fff;
  background: rgba(139, 148, 158, 0.15);
  transform: rotate(90deg);
}

:deep(.ant-modal-body) {
  padding: 24px;
}

:deep(.ant-spin-dot-item) {
  background-color: #388bfd;
}

:deep(.ant-empty-description) {
  color: #8b949e;
  font-size: 14px;
}
</style>
