package com.yu.yuaicodemother.ai;

import com.yu.yuaicodemother.ai.model.HtmlCodeResult;
import com.yu.yuaicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiCodeGeneratorService {

    /**
     * 生成HTML代码
     *
     * @param contents 用户提示词（多模态）
     * @return AI输出结果
     * */
    @Deprecated
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHTMLCode(@UserMessage List<Content> contents);

    /**
     * 生成HTML代码   (外部) memorId用法
     *
     * @param memoryId 记忆ID
     * @param contents 用户提示词
     * @return AI输出结果
     * */
    @Deprecated
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHTMLCode(@MemoryId int memoryId, @UserMessage List<Content> contents);

    /**
     *  生成多文件代码
     *
     * @param contents 用户提示词
     * @return AI输出结果
     * */
    @Deprecated
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(@UserMessage List<Content> contents);


    /**
     *  生成Vue工程代码
     *
     * @param appId 应用ID
     * @param contents 用户提示词
     * @return AI输出结果
     * */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCode(@MemoryId long appId , @UserMessage List<Content> contents);


    /**
     * 生成HTML代码
     *
     * @param contents 用户提示词
     * @return AI输出结果
     * */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHTMLCodeStream(@UserMessage List<Content> contents);

    /**
     *  生成多文件代码
     *
     * @param contents 用户提示词
     * @return AI输出结果
     * */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(@UserMessage List<Content> contents);

}
