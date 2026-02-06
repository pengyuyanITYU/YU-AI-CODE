package com.yu.yuaicodemother.core.file.processor;

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
public class ImageFileProcessor implements FileContentProcessor {

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        log.info("图片文件处理: {}, URL: {}", file.getName(), fileUrl);

        return FileProcessResult.builder()
                .fileType(FileTypeEnum.IMAGE.getValue())
                .url(fileUrl)
                .content(null)
                .status(ProcessStatusEnum.SUCCESS.getValue())
                .build();
    }

    @Override
    public boolean support(String extension) {
        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }
}
