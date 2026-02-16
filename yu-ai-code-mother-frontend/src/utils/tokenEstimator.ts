import { encodingForModel } from 'js-tiktoken'
import type { UploadedFile } from './fileUploadManager'

// Token 估算器 - 针对阿里云通义千问模型优化
// 使用 cl100k_base 编码器（与 GPT-4/Tongyi 兼容）

let encoder: ReturnType<typeof encodingForModel> | null = null

/**
 * Token 详情统计
 */
export interface TokenBreakdown {
  /** 输入文本 tokens */
  inputTextTokens: number
  /** 图片 tokens */
  imageTokens: number
  /** 文档 tokens */
  documentTokens: number
  /** 当前输入总 tokens */
  currentInputTotal: number
  /** 预估输出 tokens (按输入的 2 倍估算) */
  estimatedOutputTokens: number
  /** 本次请求预估总消耗 */
  estimatedRequestTotal: number
  /** 图片数量 */
  imageCount: number
}

/**
 * 累计 Token 统计
 */
export interface AccumulatedTokens {
  /** 累计输入 tokens */
  totalInputTokens: number
  /** 累计输出 tokens */
  totalOutputTokens: number
  /** 累计总 tokens */
  totalTokens: number
}

/**
 * 获取编码器实例（懒加载）
 */
function getEncoder(): ReturnType<typeof encodingForModel> {
  if (!encoder) {
    encoder = encodingForModel('gpt-4')
  }
  return encoder
}

/**
 * 计算文本的 token 数量
 */
export function calculateTextTokens(text: string): number {
  if (!text || text.trim().length === 0) {
    return 0
  }
  try {
    const enc = getEncoder()
    const tokens = enc.encode(text)
    return tokens.length
  } catch (error) {
    console.error('Token calculation error:', error)
    return Math.ceil(text.length / 4)
  }
}

/**
 * 计算图片的 token 数量（OpenAI Vision 规则）
 * 
 * 规则：
 * - detail = "low" → 85 tokens
 * - detail = "high"/"auto" → tiling 模式：
 *   1. 缩放：长边≤2048px，短边≥768px
 *   2. tiles = ceil(宽/512) × ceil(高/512)
 *   3. tokens = 85 + 170 × tiles
 * 
 * @param width - 图片宽度（像素）
 * @param height - 图片高度（像素）
 * @param detail - 图片质量模式（默认 high）
 * @returns 消耗的 tokens 数
 */
export function calculateImageTokens(
  width: number,
  height: number,
  detail: 'low' | 'high' | 'auto' = 'high'
): number {
  // 规则1: low 模式固定 85 tokens
  if (detail === 'low') {
    return 85
  }

  // 规则2: high/auto 模式使用 tiling
  let w = width
  let h = height

  // 缩放逻辑（保持宽高比）
  const maxSide = Math.max(w, h)
  const minSide = Math.min(w, h)

  // 如果任意一边 > 2048px，缩小到长边正好等于 2048px
  if (maxSide > 2048) {
    const scale = 2048 / maxSide
    w = Math.round(w * scale)
    h = Math.round(h * scale)
  }

  // 缩放后，如果短边 < 768px，放大到短边正好等于 768px
  const newMinSide = Math.min(w, h)
  if (newMinSide < 768) {
    const scale = 768 / newMinSide
    const newW = Math.round(w * scale)
    const newH = Math.round(h * scale)
    // 确保不超过 2048（理论上不会超过，但做个保护）
    if (Math.max(newW, newH) <= 2048) {
      w = newW
      h = newH
    }
  }

  // 以 512×512 为单位切分
  const tilesW = Math.ceil(w / 512)
  const tilesH = Math.ceil(h / 512)
  const tileCount = tilesW * tilesH

  // tokens = 85 + 170 × tile数量
  return 85 + 170 * tileCount
}

/**
 * 计算图片 tokens（旧版兼容，按数量估算）
 * 当没有尺寸信息时使用，每张图片默认 825 tokens
 */
export function calculateImageTokensByCount(imageCount: number): number {
  const TOKENS_PER_IMAGE = 825
  return imageCount * TOKENS_PER_IMAGE
}

/**
 * 计算文档的 token 数量
 */
export function calculateDocumentTokens(content: string | undefined): number {
  if (!content) return 0
  return calculateTextTokens(content)
}

/**
 * 估算输出 tokens
 */
export function estimateOutputTokens(inputTokens: number): number {
  return Math.ceil(inputTokens * 2)
}

/**
 * 计算当前输入的总 token 数量
 */
export function calculateInputTokens(
  userInput: string,
  files: UploadedFile[]
): TokenBreakdown {
  const inputTextTokens = calculateTextTokens(userInput)

  let imageCount = 0
  let imageTokens = 0
  let documentTokens = 0

  for (const file of files) {
    if (file.status !== 'success') continue

    if (file.fileType === 'image') {
      imageCount++
      // 如果有宽高信息，使用 OpenAI Vision 规则计算；否则按旧版估算
      if (file.width && file.height) {
        imageTokens += calculateImageTokens(file.width, file.height, 'high')
      } else {
        imageTokens += 825
      }
    } else if (file.fileType === 'document' || file.fileType === 'text') {
      documentTokens += calculateDocumentTokens(file.content)
    }
  }

  const currentInputTotal = inputTextTokens + imageTokens + documentTokens
  const estimatedOutputTokens = estimateOutputTokens(currentInputTotal)
  const estimatedRequestTotal = currentInputTotal + estimatedOutputTokens

  return {
    inputTextTokens,
    imageTokens,
    documentTokens,
    currentInputTotal,
    estimatedOutputTokens,
    estimatedRequestTotal,
    imageCount
  }
}

/**
 * 格式化 token 数字显示
 */
export function formatTokenCount(count: number): string {
  if (count === 0) return '0'
  return count.toLocaleString()
}

/**
 * 获取 Token 详情提示文本
 */
export function getTokenTooltipText(
  breakdown: TokenBreakdown,
  accumulated: AccumulatedTokens
): string {
  const lines: string[] = []

  lines.push('📊 Token 消耗预估')
  lines.push('')

  // 当前输入部分
  lines.push('【当前输入】')
  if (breakdown.inputTextTokens > 0) {
    lines.push(`  文本: ${breakdown.inputTextTokens.toLocaleString()} tokens`)
  }
  if (breakdown.imageTokens > 0) {
    lines.push(`  图片: ${breakdown.imageTokens.toLocaleString()} tokens (${breakdown.imageCount} 张)`)
  }
  if (breakdown.documentTokens > 0) {
    lines.push(`  文档: ${breakdown.documentTokens.toLocaleString()} tokens`)
  }
  lines.push(`  小计: ${breakdown.currentInputTotal.toLocaleString()} tokens`)
  lines.push('')

  // 累计消耗
  lines.push('【累计消耗】')
  lines.push(`  输入: ${accumulated.totalInputTokens.toLocaleString()} tokens`)
  lines.push(`  输出: ${accumulated.totalOutputTokens.toLocaleString()} tokens`)
  lines.push(`  总计: ${accumulated.totalTokens.toLocaleString()} tokens`)

  return lines.join('\n')
}
