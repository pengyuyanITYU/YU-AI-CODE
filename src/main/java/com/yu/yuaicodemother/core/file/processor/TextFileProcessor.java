package com.yu.yuaicodemother.core.file.processor;

import cn.hutool.core.io.FileUtil;
import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class TextFileProcessor implements FileContentProcessor {

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            "txt", "md", "html", "css", "vue", "doc", "js", "java", "py", "xml", "json", "yaml", "yml"
    );

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        try {
            String content = FileUtil.readUtf8String(file);

            if (content == null || content.trim().isEmpty()) {
                return FileProcessResult.builder()
                        .fileType(FileTypeEnum.TEXT.getValue())
                        .url(fileUrl)
                        .status(ProcessStatusEnum.EMPTY.getValue())
                        .errorMessage("文件内容为空")
                        .build();
            }

            log.info("文本文件处理成功: {}, 内容长度: {}", file.getName(), content.length());

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
        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }
}
