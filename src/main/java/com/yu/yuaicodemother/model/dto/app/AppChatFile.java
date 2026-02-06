package com.yu.yuaicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用对话文件信息
 */
@Data
public class AppChatFile implements Serializable {
    
    /**
     * 文件 URL
     */
    private String url;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件类型 (image/document/text)
     */
    private String fileType;

    private static final long serialVersionUID = 1L;
}
