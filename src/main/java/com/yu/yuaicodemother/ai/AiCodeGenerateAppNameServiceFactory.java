package com.yu.yuaicodemother.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AiCodeGenerateAppNameServiceFactory {

    @Resource(name = "generateAppNameChatModel")
    private ChatModel generateAppName;

    @Bean
    public AiCodeGenerateAppNameService aiCodeGenerateAppNameService() {
       return AiServices.builder(AiCodeGenerateAppNameService.class)
               .chatModel(generateAppName)
               .build();
    }
}
