package com.yu.yuaicodemother.core.file.processor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class TextFileProcessor implements FileContentProcessor {

    // 1. 移除了 'doc'，因为它不是纯文本，不能直接读
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            "txt", "md", "html", "css", "vue", "js", "ts", "java",
            "py", "go", "c", "cpp", "xml", "json", "yaml", "yml", "sql", "properties"
    );

    // 2. 定义最大读取字符数（约 15k-20k Token），防止撑爆上下文
    // 建议做成 @Value 配置注入，这里暂用常量
    private static final int MAX_TEXT_CHARS = 20000;

    // 3. 定义最大文件体积（例如 1MB），超过此大小的纯文本可能是日志，读入内存有风险
    private static final long MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1MB

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        try {
            // 安全检查：文件过大直接拦截，防止 OOM
            if (file.length() > MAX_FILE_SIZE_BYTES) {
                return FileProcessResult.builder()
                        .fileType(FileTypeEnum.TEXT.getValue())
                        .url(fileUrl)
                        .status(ProcessStatusEnum.FAILED.getValue())
                        .errorMessage("文件过大，为了保证响应速度，暂不支持超过 1MB 的文本文件")
                        .build();
            }

            // 读取内容
            String content = FileUtil.readString(file, StandardCharsets.UTF_8);

            if (StrUtil.isBlank(content)) {
                return FileProcessResult.builder()
                        .fileType(FileTypeEnum.TEXT.getValue())
                        .url(fileUrl)
                        .status(ProcessStatusEnum.EMPTY.getValue())
                        .errorMessage("文件内容为空")
                        .build();
            }

            // 4. 数据清洗：去除连续的空行，节省 Token
            // 解释：将 2 个以上的换行符替换为 2 个换行符
            content = content.replaceAll("\\n\\s*\\n", "\n\n");

            // 5. 截断保护：如果内容过长，进行截断并追加提示
            boolean isTruncated = false;
            if (content.length() > MAX_TEXT_CHARS) {
                content = StrUtil.sub(content, 0, MAX_TEXT_CHARS);
                isTruncated = true;
            }

            // 构造最终给 AI 看的内容
            if (isTruncated) {
                content += "\n\n[System Note: File content truncated due to length limit.]";
            }

            log.info("文本文件处理成功: {}, 最终长度: {}", file.getName(), content.length());

            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.TEXT.getValue())
                    .url(fileUrl)
                    .content(content)
                    .status(ProcessStatusEnum.SUCCESS.getValue())
                    .build();

        } catch (Exception e) {
            log.error("文本文件处理失败: {}", file.getName(), e);
            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.TEXT.getValue())
                    .url(fileUrl)
                    .status(ProcessStatusEnum.FAILED.getValue())
                    .errorMessage("文件读取失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean support(String extension) {
        return extension != null && SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }
}