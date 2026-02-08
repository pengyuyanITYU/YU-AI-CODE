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
public class AiCodeGenTypeRoutingServiceTest {

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Test
    public void testRouteCodeGenType() {
        String userPrompt = "Build a simple personal profile page";
        CodeGenTypeRoutingResult result = aiCodeGenTypeRoutingService.routeCodeGenType(
                List.of(TextContent.from(userPrompt)));
        log.info("{} -> {}", userPrompt, result.getType().getValue());

        userPrompt = "Build a corporate website with home, about and contact pages";
        result = aiCodeGenTypeRoutingService.routeCodeGenType(List.of(TextContent.from(userPrompt)));
        log.info("{} -> {}", userPrompt, result.getType().getValue());

        userPrompt = "Build an e-commerce admin with user, product and order modules";
        result = aiCodeGenTypeRoutingService.routeCodeGenType(List.of(TextContent.from(userPrompt)));
        log.info("{} -> {}", userPrompt, result.getType().getValue());
    }
}
