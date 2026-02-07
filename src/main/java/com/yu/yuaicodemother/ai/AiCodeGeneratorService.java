package com.yu.yuaicodemother.ai;

import com.yu.yuaicodemother.ai.model.HtmlCodeResult;
import com.yu.yuaicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    /**
     * 生成HTML代码
     *
     * @param userMessage 用户提示词（多模态）
     * @return AI输出结果
     * */
    @Deprecated
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHTMLCode(UserMessage userMessage);

    /**
     * 生成HTML代码   (外部) memorId用法
     *
     * @param memoryId 记忆ID
     * @param userMessage 用户提示词
     * @return AI输出结果
     * */
    @Deprecated
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHTMLCode(@MemoryId int memoryId, UserMessage userMessage);

    /**
     *  生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     * */
    @Deprecated
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(UserMessage userMessage);


    /**
     *  生成Vue工程代码
     *
     * @param appId 应用ID
     * @param userMessage 用户提示词
     * @return AI输出结果
     * */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCode(@MemoryId long appId , UserMessage userMessage);


    /**
     * 生成HTML代码
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     * */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHTMLCodeStream(UserMessage userMessage);

    /**
     *  生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     * */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(UserMessage userMessage);

}
