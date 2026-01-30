package com.yu.yuaicodemother.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
public class RedisChatMemoryStoreConfig {

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore(RedisChatMemoryProperties properties) {
        log.info("开始加载 RedisChatMemoryStore 配置...");
        
        try {
            // 关键参数检查
            if (!StringUtils.hasText(properties.getHost())) {
                throw new IllegalArgumentException("配置加载失败: Redis Host 为空");
            }

            String maskedPassword = StringUtils.hasText(properties.getPassword()) ? "******" : "无";
            log.info("Redis 配置参数: Host={}, Port={}, Password={}, TTL={}, Database={}", 
                    properties.getHost(), properties.getPort(), maskedPassword, properties.getTtl(), properties.getDatabase());

            RedisChatMemoryStore store = RedisChatMemoryStore.builder()
                    .host(properties.getHost())
                    .port(properties.getPort())
                    .password(properties.getPassword())
                    .ttl(properties.getTtl())
                    .build();
            
            log.info("RedisChatMemoryStore 初始化成功");
            return store;
            
        } catch (Exception e) {
            log.error("RedisChatMemoryStore 初始化失败: {}", e.getMessage(), e);
            throw new IllegalStateException("无法初始化 RedisChatMemoryStore", e);
        }
    }
}
