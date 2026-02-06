package com.yu.yuaicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.yu.yuaicodemother.core.file.processor.FileContentProcessor;
import com.yu.yuaicodemother.core.file.processor.FileProcessorFactory;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import com.yu.yuaicodemother.service.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;


@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private FileProcessorFactory processorFactory;

    @Override
    public FileProcessResult processFile(String fileUrl, String originalFileName) {
        File tempFile = null;
        try {
            String extension = FileUtil.extName(originalFileName).toLowerCase();

            tempFile = downloadFileFromUrl(fileUrl, originalFileName);

            FileContentProcessor processor = processorFactory.getProcessor(extension);

            FileProcessResult result = processor.process(tempFile, fileUrl);
            result.setFileName(originalFileName);

            log.info("文件处理完成: {}, 类型: {}, 状态: {}",
                    originalFileName, result.getFileType(), result.getStatus());

            return result;

        } catch (Exception e) {
            log.error("文件处理失败: {}", originalFileName, e);
            return FileProcessResult.builder()
                    .fileName(originalFileName)
                    .status(ProcessStatusEnum.FAILED.getValue())
                    .errorMessage("文件处理失败: " + e.getMessage())
                    .build();
        } finally {
            if (tempFile != null && tempFile.exists()) {
                FileUtil.del(tempFile);
            }
        }
    }

    private File downloadFileFromUrl(String fileUrl, String fileName) throws Exception {
        File tempFile = File.createTempFile("process_", "_" + fileName);
        
        // 使用 Hutool 下载文件，自动处理 SSL 等问题
        long size = HttpUtil.downloadFile(fileUrl, tempFile);
        if (size <= 0) {
            throw new RuntimeException("下载文件为空");
        }

        return tempFile;
    }
}
