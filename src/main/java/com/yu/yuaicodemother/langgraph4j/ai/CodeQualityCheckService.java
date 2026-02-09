package com.yu.yuaicodemother.langgraph4j.ai;

import com.yu.yuaicodemother.langgraph4j.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.apache.ibatis.annotations.Param;

public interface CodeQualityCheckService {

    /**
     * 检查代码质量
     * AI 会分析代码并返回质量检查结果
     */
    @SystemMessage(fromResource = "prompt/code-quality-check-system-prompt.txt")
    QualityResult checkCodeQuality(@UserMessage @Param("codeContent") String codeContent);
}
