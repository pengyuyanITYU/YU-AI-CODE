package com.yu.yuaicodemother.ai;

import com.yu.yuaicodemother.ai.model.HtmlCodeResult;
import com.yu.yuaicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.data.message.TextContent;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHTMLCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHTMLCode(
                List.of(TextContent.from("Build a simple personal blog page in less than 20 lines")));
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(
                List.of(TextContent.from("Build a lightweight message board app in less than 50 lines")));
        Assertions.assertNotNull(result);
    }

    @Test
    void testChatMemory() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHTMLCode(1,
                List.of(TextContent.from("Build a tiny tools site in less than 20 lines")));
        Assertions.assertNotNull(result);

        result = aiCodeGeneratorService.generateHTMLCode(1,
                List.of(TextContent.from("Do not generate code, tell me what you just did")));
        Assertions.assertNotNull(result);

        result = aiCodeGeneratorService.generateHTMLCode(2,
                List.of(TextContent.from("Build a tiny tools site in less than 20 lines")));
        Assertions.assertNotNull(result);

        result = aiCodeGeneratorService.generateHTMLCode(2,
                List.of(TextContent.from("Do not generate code, tell me what you just did")));
        Assertions.assertNotNull(result);
    }
}
