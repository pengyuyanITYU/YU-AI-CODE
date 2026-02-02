package com.yu.yuaicodemother.core.processor;

import com.yu.yuaicodemother.model.enums.CodeGenTypeEnum;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 流式处理策略接口
 */
public interface AiStreamProcessor {

    /**
     * 是否支持该生成类型
     */
    boolean supports(CodeGenTypeEnum type);

    /**
     * 处理流
     * @param source 原始流对象 (Flux<String> 或 TokenStream)
     */
    void process(Object source, FluxSink<String> sink, AtomicBoolean cancelled, Long appId, CodeGenTypeEnum type);
}
