package com.yu.yuaicodemother.core;

import com.yu.yuaicodemother.ai.AiCodeGeneratorService;
import com.yu.yuaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.yu.yuaicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AiStopGenerationTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    public void testStopGeneration() {
        Long appId = 1L; // 假设测试 ID
        String message = "请生成一个极其复杂的、包含 1000 行代码的 HTML 页面，包含大量的 CSS 动画。";
        
        AtomicInteger tokenCount = new AtomicInteger(0);
        
        // 模拟前端流式请求
        Flux<String> resultFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, CodeGenTypeEnum.HTML, appId);
        
        // 使用 StepVerifier 模拟中途取消
        StepVerifier.create(resultFlux)
                .thenConsumeWhile(token -> {
                    int count = tokenCount.incrementAndGet();
                    System.out.println("收到第 " + count + " 个 Token: " + (token.length() > 20 ? token.substring(0, 20) : token));
                    // 收到 5 个 token 后立即取消订阅（模拟前端 close）
                    return count < 5;
                })
                .thenCancel() 
                .verify(Duration.ofSeconds(30));

        System.out.println("测试结束：已主动触发取消信号");
        
        // 等待几秒观察日志，确认没有后续 token 产生（抛出异常后不再进入回调）
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        assertThat(tokenCount.get()).isLessThan(20); 
    }
}
