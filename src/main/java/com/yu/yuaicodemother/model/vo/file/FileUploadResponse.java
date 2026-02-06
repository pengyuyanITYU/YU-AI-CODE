package com.yu.yuaicodemother.model.vo.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse implements Serializable {

    private String url;
    private String fileName;
    private Long fileSize;

    @Serial
    private static final long serialVersionUID = 1L;
}
