package com.yu.yuaicodemother.model.vo.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessResult implements Serializable {

    private String fileType;
    private String fileName;
    private String url;
    private String content;
    /**
     * 多模态场景下，PDF 每页转图片后的 Base64 列表
     */
    private java.util.List<String> imageBase64s;
    private String status;
    private String errorMessage;
    private Map<String, Object> metadata;

    @Serial
    private static final long serialVersionUID = 1L;
}
