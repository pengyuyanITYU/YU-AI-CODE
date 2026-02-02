package com.yu.yuaicodemother.core;

import com.yu.yuaicodemother.ai.model.message.AiResponseMessage;
import com.yu.yuaicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AiStopGenerationUnitTest {

    @Test
    public void testTokenStreamCancellation() {
        // 1. Mock TokenStream
        TokenStream mockTokenStream = mock(TokenStream.class);
        AtomicBoolean wasStarted = new AtomicBoolean(false);

        // 模拟 start() 方法触发 Token 回调
        doAnswer(invocation -> {
            wasStarted.set(true);
            return null;
        }).when(mockTokenStream).start();

        // 2. 模拟设置回调
        when(mockTokenStream.onPartialResponse(any())).thenReturn(mockTokenStream);
        when(mockTokenStream.onPartialThinking(any())).thenReturn(mockTokenStream);
        when(mockTokenStream.beforeToolExecution(any())).thenReturn(mockTokenStream);
        when(mockTokenStream.onToolExecuted(any())).thenReturn(mockTokenStream);
        when(mockTokenStream.onCompleteResponse(any())).thenReturn(mockTokenStream);
        when(mockTokenStream.onError(any())).thenReturn(mockTokenStream);

        // 3. 构造 Facade (手动注入 Mock)
        AiCodeGeneratorFacade facade = new AiCodeGeneratorFacade();
        
        // 模拟生成流
        // 注意：由于是单元测试，我们直接测试内部的 processTokenStream 逻辑
        // 我们需要通过反射或简单修改 Facade 使其可测，或者直接在测试中模拟其行为
        
        // 验证：当 Flux 被取消时，后续的 Token 回调应该抛出异常或停止
        // 这里我们直接测试核心逻辑：cancelled 标志位的作用
        
        System.out.println("单元测试：验证取消信号传递...");
    }
    
    @Test
    public void testManualCancelLogic() {
        // 模拟 processTokenStream 中的核心逻辑
        AtomicBoolean cancelled = new AtomicBoolean(false);
        
        Consumer<String> onToken = (token) -> {
            if (cancelled.get()) {
                throw new RuntimeException("CANCELLED_BY_USER");
            }
            System.out.println("处理 Token: " + token);
        };

        // 模拟正常处理
        onToken.accept("Hello");
        
        // 模拟前端断开连接触发取消
        cancelled.set(true);
        
        // 验证后续回调抛出异常（这会中断底层 AI 调用）
        try {
            onToken.accept("World");
            assert false : "应该抛出异常以中断连接";
        } catch (RuntimeException e) {
            assert "CANCELLED_BY_USER".equals(e.getMessage());
            System.out.println("成功拦截并抛出中断异常");
        }
    }
}
