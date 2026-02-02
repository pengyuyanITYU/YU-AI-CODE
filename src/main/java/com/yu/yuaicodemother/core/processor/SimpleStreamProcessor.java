package com.yu.yuaicodemother.core.processor;

import com.yu.yuaicodemother.core.CodeParserExecutor;
import com.yu.yuaicodemother.core.saver.CodeFileSaverExecutor;
import com.yu.yuaicodemother.model.enums.CodeGenTypeEnum;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 简单代码流处理器 (HTML / MULTI_FILE)
 */
@Component
public class SimpleStreamProcessor extends AbstractStreamProcessor {

    @Override
    public boolean supports(CodeGenTypeEnum type) {
        return CodeGenTypeEnum.HTML.equals(type) || CodeGenTypeEnum.MULTI_FILE.equals(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Object source, FluxSink<String> sink, AtomicBoolean cancelled, Long appId, CodeGenTypeEnum type) {
        Flux<String> codeStream = (Flux<String>) source;
        StringBuilder codeBuilder = new StringBuilder();

        codeStream.doOnNext(chunk -> {
            checkCancelled(cancelled);
            codeBuilder.append(chunk);
            sink.next(chunk);
        })
        .doOnComplete(() -> {
            if (cancelled.get()) {
                sink.complete();
                return;
            }
            try {
                String completeCode = codeBuilder.toString();
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, type);
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, type, appId);
                log.info("代码保存成功: {}", savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("代码保存失败: {}", e.getMessage());
            }
            sink.complete();
        })
        .doOnError(error -> handleStreamError(error, sink, cancelled, appId))
        .subscribe();
    }
}
