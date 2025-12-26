package com.yu.yuaicodemother.ai;

import com.yu.yuaicodemother.ai.model.HtmlCodeResult;
import com.yu.yuaicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    /**
     * 生成HTML代码
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     * */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHTMLCode(String userMessage);

    /**
     *  生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     * */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);


    /**
     * 生成HTML代码
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     * */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHTMLCodeStream(String userMessage);

    /**
     *  生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     * */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

}
