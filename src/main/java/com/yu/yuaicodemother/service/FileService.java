package com.yu.yuaicodemother.service;

import com.yu.yuaicodemother.model.vo.file.FileProcessResult;

public interface FileService {

    FileProcessResult processFile(String fileUrl, String originalFileName);
}
