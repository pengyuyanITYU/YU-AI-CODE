package com.yu.yuaicodemother.model.vo.app;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppVersionVO {
    private Long id;
    private Long appId;
    private Integer version;
    private String changeLog;
    private LocalDateTime createTime;
}
