package com.yu.yuaicodemother.model.dto.app;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 上传的文件列表
     */
    private List<AppChatFile> fileList;

    @Serial
    private static final long serialVersionUID = 1L;
}
