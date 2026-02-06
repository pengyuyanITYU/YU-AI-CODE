import { uploadFileApi, processFileApi } from '@/api/fileController'
import { message } from 'ant-design-vue'

export interface UploadedFile {
  url: string
  fileName: string
  fileType: 'image' | 'document' | 'text'
  content?: string
  status: 'uploading' | 'processing' | 'success' | 'failed'
  errorMessage?: string
}

const MAX_FILE_SIZE = 10 * 1024 * 1024
const ALLOWED_EXTENSIONS = [
  'jpg', 'jpeg', 'png',
  'pdf', 'doc', 'docx', 'ppt', 'pptx',
  'txt', 'md', 'html', 'css', 'vue'
]

export async function uploadAndProcessFile(file: File): Promise<UploadedFile | null> {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !ALLOWED_EXTENSIONS.includes(ext)) {
    message.error(`不支持的文件类型: ${ext}`)
    return null
  }

  if (file.size > MAX_FILE_SIZE) {
    message.error('文件大小不能超过10MB')
    return null
  }

  try {
    const uploadRes = await uploadFileApi(file)
    const uploadData = uploadRes.data as any
    if (uploadData.code !== 0 || !uploadData.data) {
      message.error('文件上传失败')
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
      }
    }
    return null
  } catch (error) {
    console.error('文件上传处理失败:', error)
    message.error('文件上传失败')
    return null
  }
}

export function getFileIcon(fileType: string): string {
  const icons: Record<string, string> = {
    image: '🖼️',
    document: '📄',
    text: '📝',
  }
  return icons[fileType] || '📎'
}

export function toFileAttachments(files: UploadedFile[]): API.FileAttachment[] {
  return files
    .filter(f => f.status === 'success')
    .map(f => ({
      url: f.url,
      fileName: f.fileName,
      fileType: f.fileType,
      content: f.content,
    }))
}
