package com.yu.yuaicodemother.core.processor;

import com.yu.yuaicodemother.model.enums.CodeGenTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractStreamProcessor implements AiStreamProcessor {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected void checkCancelled(AtomicBoolean cancelled) {
        if (cancelled.get()) {
            throw new RuntimeException("CANCELLED_BY_USER");
        }
    }

    protected void handleStreamError(Throwable error, FluxSink<String> sink, AtomicBoolean cancelled, Long appId) {
        if (cancelled.get() || "CANCELLED_BY_USER".equals(error.getMessage())) {
            log.info("AI 生成已中断 (appId: {})", appId);
            sink.complete();
        } else {
            log.error("AI 生成异常 (appId: {})", appId, error);
            sink.error(error);
        }
    }
}
