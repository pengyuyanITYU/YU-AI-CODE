package com.yu.yuaicodemother.ai;

import com.yu.yuaicodemother.ai.model.CodeGenTypeRoutingResult;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.SystemMessage;

/**
 * AI代码生成类型智能路由服务
 * 使用结构化输出直接返回枚举类型
 *
 * @author 鱼🐟
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     *
     * @param userPrompt 用户输入的需求描述
     * @return 推荐的代码生成类型
     */
    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    CodeGenTypeRoutingResult routeCodeGenType(UserMessage userPrompt);
}
