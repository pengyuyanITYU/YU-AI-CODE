package com.yu.yuaicodemother.core.file.processor;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class ImageFileProcessor implements FileContentProcessor {

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );
    // 2MB = 2 * 1024 * 1024 bytes
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        log.info("开始处理图片文件: {}, 原始路径: {}", file.getName(), fileUrl);

        try {
            // 熔断保护：文件过大直接返回 URL，不进行 Base64 编码
            if (file.length() > MAX_FILE_SIZE_BYTES) {
                log.warn("Image file too large ({} bytes), skipping compression: {}", file.length(), file.getName());
                return FileProcessResult.builder()
                        .fileType(FileTypeEnum.IMAGE.getValue())
                        .url(fileUrl)
                        .content(null) // Base64 为空
                        .status(ProcessStatusEnum.SUCCESS.getValue()) // 状态为成功
                        .build();
            }

            // 1. 准备输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // 2. 智能压缩逻辑 (依赖 Thumbnailator)
            // 限制大小为 1024x1024，质量 0.8，避免 Token 消耗过大
            // toOutputStream 会自动处理图片旋转等问题
            Thumbnails.of(file)
                    .size(1024, 1024)
                    .outputQuality(0.8f)
                    .toOutputStream(outputStream);

            // 3. 转换为 Base64 字符串
            byte[] compressedBytes = outputStream.toByteArray();
            String base64Data = Base64.encode(compressedBytes);

            // 4. 获取并修正 MIME 类型 (Hutool 可能返回 jpg，需转换为 image/jpeg)
            String extension = FileUtil.extName(file).toLowerCase();
            String mimeType = getMimeType(extension);

            // 5. 拼接标准的 Data URI Scheme (这是 LangChain4j 或前端展示需要的格式)
            // 格式: data:image/png;base64,xxxxxx
            String finalContent = StrUtil.format("data:{};base64,{}", mimeType, base64Data);

            log.info("图片处理完成，原始大小: {}KB, 压缩后: {}KB",
                    file.length() / 1024, compressedBytes.length / 1024);

            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.IMAGE.getValue())
                    .url(fileUrl) // 保留原始 URL 用于记录
                    .content(finalContent) // 核心修改：这里填入 Base64 字符串
                    .status(ProcessStatusEnum.SUCCESS.getValue())
                    .build();

        } catch (Exception e) {
            log.error("图片处理失败: {}", file.getName(), e);
            // 失败时降级处理，标记状态为失败，或者仅返回 URL 让上层决定
            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.IMAGE.getValue())
                    .url(fileUrl)
                    .content(null)
                    .status(ProcessStatusEnum.FAILED.getValue())
                    .build();
        }
    }

    @Override
    public boolean support(String extension) {
        return extension != null && SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 简单的后缀转 MIME 类型辅助方法
     */
    private String getMimeType(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "image/jpeg"; // jpg, jpeg 等默认处理
        };
    }
}
