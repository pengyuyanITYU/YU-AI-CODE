package com.yu.yuaicodemother.model.vo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVersionDiffVO {
    private Integer oldVersion;
    private Integer newVersion;
    private List<FileDiff> diffs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileDiff {
        private String fileName;
        private String oldContent;
        private String newContent;
        private List<String> diffLines;
    }
}
