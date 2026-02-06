package com.yu.yuaicodemother.core.file.processor;

import com.yu.yuaicodemother.model.vo.file.FileProcessResult;

import java.io.File;

public interface FileContentProcessor {

    FileProcessResult process(File file, String fileUrl);

    boolean support(String extension);
}
