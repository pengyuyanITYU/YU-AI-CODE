package com.yu.yuaicodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AiCodeGenerateAppNameService {

    /**
     * 生成应用名称
     * @param userMessage 用户输入
     * @return 应用名称
     * */
    @SystemMessage(fromResource = "prompt/generateAppName-system-prompt.txt")
    String generateAppName(@UserMessage String userMessage);
}
