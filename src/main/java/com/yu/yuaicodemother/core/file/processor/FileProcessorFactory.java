package com.yu.yuaicodemother.core.file.processor;

import com.yu.yuaicodemother.exception.BusinessException;
import com.yu.yuaicodemother.exception.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileProcessorFactory {

    @Resource
    private List<FileContentProcessor> processors;

    public FileContentProcessor getProcessor(String extension) {
        return processors.stream()
                .filter(p -> p.support(extension.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR,
                        "不支持的文件类型: " + extension));
    }
}
