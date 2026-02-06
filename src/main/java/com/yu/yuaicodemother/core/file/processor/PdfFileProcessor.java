package com.yu.yuaicodemother.core.file.processor;

import cn.hutool.core.util.StrUtil;
import com.yu.yuaicodemother.manager.TencentOcrManager;
import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Slf4j
public class PdfFileProcessor implements FileContentProcessor {

    @Resource
    private TencentOcrManager tencentOcrManager;

    // Tika 实例通常是线程安全的，但在极高并发下建议使用 AutoDetectParser 配合 ContentHandler
    // 对于中低频场景，new Tika() 没问题
    private final Tika tika = new Tika();

    // 最大字符限制（约 15k-20k Token），防止撑爆 LLM 上下文
    private static final int MAX_CHARS = 30000;

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        String text = "";
        try {
            // 1. 尝试使用 Tika 解析 (提取文本层)
            // 设置最大长度限制，避免 Tika 耗费过多内存处理超大文件
            tika.setMaxStringLength(MAX_CHARS + 1000);
            text = tika.parseToString(file);

        } catch (IOException | TikaException e) {
            log.warn("Tika 解析 PDF 异常，将尝试 OCR: {}", file.getName());
        }

        // 2. 检查提取结果，如果为空或极短，判定为扫描版/图片型 PDF，启动 OCR
        if (StrUtil.isBlank(text) || text.trim().length() < 50) {
            log.info("PDF 内容为空或疑似扫描版，启动 OCR 识别: {}", file.getName());
            try {
                // 假设你的 OcrManager 能处理 File 对象
                text = tencentOcrManager.recognizePdf(file);
            } catch (Exception ocrError) {
                log.error("OCR 识别失败: {}", file.getName(), ocrError);
                return buildResult(fileUrl, null, ProcessStatusEnum.FAILED,
                        "PDF 解析失败且 OCR 识别无效，请确认文件是否损坏或加密");
            }
        }

        // 3. 再次检查 OCR 结果
        if (StrUtil.isBlank(text)) {
            return buildResult(fileUrl, null, ProcessStatusEnum.EMPTY, "无法提取 PDF 内容");
        }

        // 4. 关键步骤：清洗文本 (保留代码结构)
        text = cleanTextPreservingFormat(text);

        // 5. 截断保护
        boolean truncated = false;
        if (text.length() > MAX_CHARS) {
            text = StrUtil.sub(text, 0, MAX_CHARS);
            truncated = true;
        }

        // 6. 追加系统提示
        if (truncated) {
            text += "\n\n[System Note: PDF content truncated due to length limit.]";
        }

        log.info("PDF 处理成功: {}, 最终长度: {}", file.getName(), text.length());
        return buildResult(fileUrl, text, ProcessStatusEnum.SUCCESS, null);
    }

    @Override
    public boolean support(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    /**
     * 构建返回结果的辅助方法
     */
    private FileProcessResult buildResult(String url, String content, ProcessStatusEnum status, String errorMsg) {
        return FileProcessResult.builder()
                .fileType(FileTypeEnum.DOCUMENT.getValue())
                .url(url)
                .content(content)
                .status(status.getValue())
                .errorMessage(errorMsg)
                .build();
    }

    /**
     * 清洗文本，但保留段落和代码结构
     */
    private String cleanTextPreservingFormat(String text) {
        if (text == null) return "";

        // 1. 统一换行符 (Windows \r\n -> \n)
        String cleaned = text.replace("\r\n", "\n").replace("\r", "\n");

        // 2. 去除连续 3 个以上的空行，变成 2 个空行 (保留段落感)
        // 这样不会破坏代码块内部的空行，但能缩减大段空白
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        // 3. (可选) 去除行首行尾多余空白，但小心不要破坏 Python 缩进
        // 如果文件主要是代码，建议不要 trim 每一行；如果是纯文本文档，可以 trim。
        // 为了稳妥（既支持代码也支持文档），我们只去除整个字符串首尾的空白
        return cleaned.trim();
    }
}