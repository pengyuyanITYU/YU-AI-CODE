package com.yu.yuaicodemother.core.processor;

import cn.hutool.json.JSONUtil;
import com.yu.yuaicodemother.ai.model.message.AiResponseMessage;
import com.yu.yuaicodemother.ai.model.message.BeforeToolExecuted;
import com.yu.yuaicodemother.ai.model.message.ToolExecutedMessage;
import com.yu.yuaicodemother.constant.AppConstant;
import com.yu.yuaicodemother.core.builder.VueProjectBuilder;
import com.yu.yuaicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Vue 项目流处理器
 */
@Component
public class VueProjectStreamProcessor extends AbstractStreamProcessor {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Override
    public boolean supports(CodeGenTypeEnum type) {
        return CodeGenTypeEnum.VUE_PROJECT.equals(type);
    }

    @Override
    public void process(Object source, FluxSink<String> sink, AtomicBoolean cancelled, Long appId, CodeGenTypeEnum type) {
        TokenStream tokenStream = (TokenStream) source;
        tokenStream.onPartialResponse(partial -> {
            checkCancelled(cancelled);
            sink.next(JSONUtil.toJsonStr(new AiResponseMessage(partial)));
        })
        .onPartialThinking(thinking -> {
            checkCancelled(cancelled);
            sink.next(JSONUtil.toJsonStr(thinking.text()));
        })
        .beforeToolExecution(execution -> {
            checkCancelled(cancelled);
            sink.next(JSONUtil.toJsonStr(new BeforeToolExecuted(execution.request())));
        })
        .onToolExecuted(execution -> {
            checkCancelled(cancelled);
            sink.next(JSONUtil.toJsonStr(new ToolExecutedMessage(execution)));
        })
        .onCompleteResponse(response -> {
            if (cancelled.get()) {
                sink.complete();
                return;
            }
            String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
            vueProjectBuilder.buildProject(projectPath);
            sink.complete();
        })
        .onError(error -> handleStreamError(error, sink, cancelled, appId))
        .start();
    }
}
