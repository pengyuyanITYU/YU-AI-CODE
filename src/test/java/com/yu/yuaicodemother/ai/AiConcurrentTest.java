package com.yu.yuaicodemother.ai;

import com.yu.yuaicodemother.ai.model.CodeGenTypeRoutingResult;
import dev.langchain4j.data.message.TextContent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
public class AiConcurrentTest {

    @Resource
    private AiCodeGenTypeRoutingServiceFactory routingServiceFactory;

    @Test
    public void testConcurrentRoutingCalls() throws InterruptedException {
        String[] prompts = {
                "Build a simple HTML page",
                "Build a multi-page website project",
                "Build a Vue management system"
        };

        Thread[] threads = new Thread[prompts.length];
        for (int i = 0; i < prompts.length; i++) {
            final String prompt = prompts[i];
            final int index = i + 1;
            threads[i] = Thread.ofVirtual().start(() -> {
                AiCodeGenTypeRoutingService service = routingServiceFactory.aiCodeGenTypeRoutingService();
                CodeGenTypeRoutingResult result = service.routeCodeGenType(List.of(TextContent.from(prompt)));
                log.info("Thread {}: {} -> {}", index, prompt, result.getType().getValue());
            });
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
