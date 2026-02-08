import { uploadFileApi, processFileApi } from '@/api/fileController'
import { message } from 'ant-design-vue'

export interface UploadedFileMetadata {
  parseMethod?: string
  charCount?: number
  truncated?: boolean
  pageCount?: number
  ocrUsed?: boolean
  fileSizeKB?: number
  [key: string]: unknown
}

export interface UploadedFile {
  url: string
  fileName: string
  fileType: 'image' | 'document' | 'text'
  content?: string
  status: 'uploading' | 'processing' | 'success' | 'failed'
  errorMessage?: string
  metadata?: UploadedFileMetadata
}

export interface FileAttachmentPayload {
  url: string
  fileName: string
  fileType: 'image' | 'document' | 'text'
  content?: string
}

const MAX_FILE_SIZE = 10 * 1024 * 1024
const ALLOWED_EXTENSIONS = [
  'jpg', 'jpeg', 'png',
  'pdf', 'doc', 'docx', 'ppt', 'pptx',
  'txt', 'md', 'html', 'css', 'vue',
]

const INIT_FILES_STORAGE_PREFIX = 'app_chat_init_files_'

export async function uploadAndProcessFile(file: File): Promise<UploadedFile | null> {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !ALLOWED_EXTENSIONS.includes(ext)) {
    message.error(`Unsupported file type: ${ext}`)
    return null
  }

  if (file.size > MAX_FILE_SIZE) {
    message.error('File size must be less than 10MB')
    return null
  }

  try {
    const uploadRes = await uploadFileApi(file)
    const uploadData = uploadRes.data as any
    if (uploadData.code !== 0 || !uploadData.data) {
      message.error('File upload failed')
      return null
    }

    const processRes = await processFileApi(uploadData.data.url, uploadData.data.fileName)
    const processData = processRes.data as any
    if (processData.code === 0 && processData.data) {
      return {
        url: processData.data.url,
        fileName: processData.data.fileName,
        fileType: processData.data.fileType as 'image' | 'document' | 'text',
        content: processData.data.content,
        status: processData.data.status === 'success' ? 'success' : 'failed',
        errorMessage: processData.data.errorMessage,
        metadata: processData.data.metadata,
      }
    }

    return null
  } catch (error) {
    console.error('File upload or process failed:', error)
    message.error('File upload failed')
    return null
  }
}

export function getFileIcon(fileType: string): string {
  const icons: Record<string, string> = {
    image: '🖼️',
    document: '📄',
    text: '📄',
  }
  return icons[fileType] || '📎'
}

export function toFileAttachments(files: UploadedFile[]): FileAttachmentPayload[] {
  return files
    .filter((f) => f.status === 'success')
    .map((f) => ({
      url: f.url,
      fileName: f.fileName,
      fileType: f.fileType,
      content: f.content,
    }))
}

export function saveInitialFilesToSession(appId: string | number, files: UploadedFile[]): void {
  if (typeof window === 'undefined' || files.length === 0) {
    return
  }

  const payload = files.map((file) => ({
    url: file.url,
    fileName: file.fileName,
    fileType: file.fileType,
    status: file.status,
    content: file.content,
    metadata: file.metadata,
  }))

  sessionStorage.setItem(getInitialFilesSessionKey(appId), JSON.stringify(payload))
}

export function consumeInitialFilesFromSession(appId: string | number): UploadedFile[] {
  if (typeof window === 'undefined') {
    return []
  }

  const key = getInitialFilesSessionKey(appId)
  const raw = sessionStorage.getItem(key)
  if (!raw) {
    return []
  }

  sessionStorage.removeItem(key)

  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }

    return parsed
      .filter((item) => item?.url && item?.fileName && item?.fileType)
      .map((item) => ({
        url: item.url,
        fileName: item.fileName,
        fileType: item.fileType,
        status: item.status || 'success',
        content: item.content,
        metadata: item.metadata,
      }))
  } catch (error) {
    console.error('Failed to parse initial files from session:', error)
    return []
  }
}

function getInitialFilesSessionKey(appId: string | number): string {
  return `${INIT_FILES_STORAGE_PREFIX}${appId}`
}
